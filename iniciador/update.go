package main

import (
	"archive/zip"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"strings"
	"time"
)

var errNetwork = errors.New("download indisponivel")
var errIncompleteRelease = errors.New("release ainda incompleta; tente novamente depois")
var errIntegrity = errors.New("integridade invalida; pacote nao instalado")
var errNoPackage = errors.New("nao foi possivel baixar o pacote; confira a internet e tente novamente")

// Somente falhas de leitura da rede permitem usar a versao instalada.
// Erros de escrita local e de validacao continuam interrompendo a troca.
type downloadReader struct{ io.Reader }

func (r downloadReader) Read(p []byte) (int, error) {
	n, e := r.Reader.Read(p)
	if e != nil && e != io.EOF {
		return n, fmt.Errorf("%w: transferencia interrompida: %v", errNetwork, e)
	}
	return n, e
}

// Reinicia o prazo de travamento a cada bloco recebido e informa o progresso aproximadamente por MB.
type progressReader struct {
	reader                io.Reader
	total, read, reported int64
	stall                 *time.Timer
	report                func(int64, int64)
}

func (r *progressReader) Read(p []byte) (int, error) {
	n, e := r.reader.Read(p)
	if n > 0 {
		r.stall.Reset(downloadStallTimeout)
		r.read += int64(n)
		if r.read-r.reported >= 1<<20 {
			r.reported = r.read
			r.report(r.read, r.total)
		}
	}
	return n, e
}

type HTTPDoer interface {
	Do(*http.Request) (*http.Response, error)
}
type releaseAsset struct {
	Name                 string
	State                string
	Browser_download_url string
}
type githubRelease struct {
	Tag_name   string
	Draft      bool
	Prerelease bool
	Assets     []releaseAsset
}
type release struct {
	Version string
	ZipURL  string
	SumURL  string
}

// Sem limite total: um download lento continua enquanto os dados chegam. A conexao, o
// cabecalho e cada intervalo sem dados (downloadStallTimeout) tem prazo proprio.
var downloadStallTimeout = 60 * time.Second

const latestCacheTTL = 10 * time.Minute

func newHTTPClient() *http.Client {
	transport := http.DefaultTransport.(*http.Transport).Clone()
	transport.DialContext = (&net.Dialer{Timeout: 10 * time.Second, KeepAlive: 30 * time.Second}).DialContext
	transport.TLSHandshakeTimeout = 15 * time.Second
	transport.ResponseHeaderTimeout = 30 * time.Second
	client := &http.Client{Transport: transport}
	client.CheckRedirect = func(req *http.Request, via []*http.Request) error {
		if len(via) > 5 {
			return errors.New("redirecionamentos em excesso")
		}
		return allowedNetwork(req.URL, false)
	}
	return client
}
func allowedNetwork(u *url.URL, initialAsset bool) error {
	if u.Scheme != "https" || u.User != nil || (u.Port() != "" && u.Port() != "443") {
		return errors.New("download sem HTTPS recusado")
	}
	switch u.Hostname() {
	case "api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com":
	default:
		return errors.New("destino de download recusado")
	}
	if initialAsset && (u.Hostname() != "github.com" || !strings.HasPrefix(u.Path, "/"+repository+"/releases/download/")) {
		return errors.New("arquivo fora das releases oficiais")
	}
	return nil
}
func (m *Manager) get(ctx context.Context, address string, asset bool) (*http.Response, error) {
	u, e := url.Parse(address)
	if e != nil {
		return nil, e
	}
	// A origem de teste so pode ser injetada por testes Go, nao por argumentos do executavel.
	testURL, _ := url.Parse(m.api)
	if testURL.Scheme == "http" && testURL.Hostname() == "127.0.0.1" {
		if u.Scheme != "http" || u.Host != testURL.Host {
			return nil, errors.New("origem de teste invalida")
		}
	} else if e = allowedNetwork(u, asset); e != nil {
		return nil, e
	}
	req, e := http.NewRequestWithContext(ctx, http.MethodGet, address, nil)
	if e != nil {
		return nil, e
	}
	req.Header.Set("User-Agent", "InPETU-Hub-Desktop/0.4")
	response, e := m.client.Do(req)
	if e != nil {
		return nil, fmt.Errorf("%w: %v", errNetwork, e)
	}
	if response.StatusCode != http.StatusOK {
		response.Body.Close()
		return nil, errNetwork
	}
	return response, nil
}
func limited(body io.Reader, limit int64) ([]byte, error) {
	data, e := io.ReadAll(io.LimitReader(body, limit+1))
	if e != nil {
		return nil, e
	}
	if int64(len(data)) > limit {
		return nil, errors.New("resposta maior que o limite")
	}
	return data, nil
}

// Abrir e iniciar consultam a lista em seguida; reutilizar o resultado poupa o limite da API sem login.
func (m *Manager) latest() (*release, error) {
	m.mu.RLock()
	cached, at := m.latestCache, m.latestAt
	m.mu.RUnlock()
	if cached != nil && time.Since(at) < latestCacheTTL {
		snapshot := *cached
		return &snapshot, nil
	}
	best, e := m.queryLatest()
	if e == nil && best != nil {
		m.mu.Lock()
		m.latestCache = best
		m.latestAt = time.Now()
		m.mu.Unlock()
	}
	return best, e
}
func (m *Manager) queryLatest() (*release, error) {
	var best *release
	for page := 1; page <= 10; page++ {
		ctx, cancel := context.WithTimeout(context.Background(), 8*time.Second)
		response, e := m.get(ctx, fmt.Sprintf("%s?per_page=100&page=%d", m.api, page), false)
		if e != nil {
			cancel()
			return nil, e
		}
		data, e := limited(response.Body, 8<<20)
		response.Body.Close()
		cancel()
		if e != nil {
			return nil, e
		}
		var list []githubRelease
		if e = json.Unmarshal(data, &list); e != nil {
			return nil, e
		}
		for _, item := range list {
			if item.Draft || item.Prerelease {
				continue
			}
			if _, e = versionOrder(item.Tag_name); e != nil {
				continue
			}
			version := strings.TrimPrefix(item.Tag_name, "v")
			candidate := &release{Version: version}
			for _, asset := range item.Assets {
				if asset.State != "uploaded" {
					continue
				}
				if asset.Name == archiveName(version) {
					candidate.ZipURL = asset.Browser_download_url
				}
				if asset.Name == "SHA256SUMS.txt" {
					candidate.SumURL = asset.Browser_download_url
				}
			}
			if best == nil || newer(version, best.Version) {
				best = candidate
			}
		}
		if len(list) < 100 {
			return best, nil
		}
	}
	return nil, errors.New("lista de releases excedeu o limite de consulta")
}
func (m *Manager) downloadRelease(next *release, stage string) (string, error) {
	if next.ZipURL == "" || next.SumURL == "" {
		return "", errIncompleteRelease
	}
	sumsCtx, cancelSums := context.WithTimeout(context.Background(), time.Minute)
	defer cancelSums()
	response, e := m.get(sumsCtx, next.SumURL, true)
	if e != nil {
		return "", e
	}
	sums, e := limited(downloadReader{response.Body}, 65536)
	response.Body.Close()
	if e != nil {
		return "", e
	}
	expected := ""
	for _, line := range strings.Split(string(sums), "\n") {
		parts := strings.Fields(line)
		if len(parts) == 2 && parts[1] == archiveName(next.Version) {
			if expected != "" {
				return "", errors.New("checksum duplicado")
			}
			expected = parts[0]
		}
	}
	if !regexp.MustCompile("^[0-9a-f]{64}$").MatchString(expected) {
		return "", errors.New("checksum ausente ou invalido")
	}
	zipCtx, cancelZip := context.WithCancel(context.Background())
	defer cancelZip()
	response, e = m.get(zipCtx, next.ZipURL, true)
	if e != nil {
		return "", e
	}
	defer response.Body.Close()
	archive := filepath.Join(stage, "download.zip")
	out, e := os.OpenFile(archive, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
	if e != nil {
		return "", e
	}
	progress := &progressReader{reader: response.Body, total: response.ContentLength, stall: time.AfterFunc(downloadStallTimeout, cancelZip), report: func(read, total int64) {
		text := fmt.Sprintf("Baixando e conferindo a versao %s... %d MB", next.Version, read>>20)
		if total > 0 {
			text += fmt.Sprintf(" de %d MB", total>>20)
		}
		m.message("Instalando", text)
	}}
	defer progress.stall.Stop()
	n, e := io.Copy(out, io.LimitReader(downloadReader{progress}, (512<<20)+1))
	closeErr := out.Close()
	if e != nil {
		return "", e
	}
	if closeErr != nil {
		return "", closeErr
	}
	if n > 512<<20 {
		return "", errors.New("pacote maior que o limite")
	}
	actual, e := digest(archive)
	if e != nil {
		return "", e
	}
	if actual != expected {
		return "", errIntegrity
	}
	content := filepath.Join(stage, "content")
	if e = extractArchive(archive, content); e != nil {
		return "", e
	}
	root := content
	if runtime.GOOS == "darwin" {
		root = filepath.Join(content, "InPETU Hub.app", "Contents", "Resources")
	}
	b, e := readBundle(root)
	if e != nil {
		return "", e
	}
	if b.Version != next.Version {
		return "", errors.New("versao do pacote diferente da release")
	}
	return root, nil
}
func extractArchive(archive, dest string) error {
	reader, e := zip.OpenReader(archive)
	if e != nil {
		return e
	}
	defer reader.Close()
	if len(reader.File) > 10000 {
		return errors.New("pacote com arquivos em excesso")
	}
	if e = os.MkdirAll(dest, 0700); e != nil {
		return e
	}
	seen := make(map[string]bool)
	var total int64
	for _, file := range reader.File {
		name := strings.TrimSuffix(file.Name, "/")
		if e = safeRelative(name); e != nil {
			return e
		}
		portable := strings.ToLower(name)
		if seen[portable] {
			return errors.New("caminho duplicado no pacote")
		}
		seen[portable] = true
		if file.Mode()&os.ModeSymlink != 0 {
			return errors.New("atalho recusado no pacote")
		}
		path := filepath.Join(dest, filepath.FromSlash(name))
		if file.FileInfo().IsDir() {
			if e = os.MkdirAll(path, 0755); e != nil {
				return e
			}
			continue
		}
		if !file.Mode().IsRegular() {
			return errors.New("arquivo especial recusado")
		}
		if file.UncompressedSize64 > 256<<20 {
			return errors.New("arquivo maior que o limite")
		}
		if e = os.MkdirAll(filepath.Dir(path), 0755); e != nil {
			return e
		}
		in, e := file.Open()
		if e != nil {
			return e
		}
		mode := file.Mode().Perm() & 0777
		if mode == 0 {
			mode = 0644
		}
		out, e := os.OpenFile(path, os.O_CREATE|os.O_EXCL|os.O_WRONLY, mode)
		if e != nil {
			in.Close()
			return e
		}
		n, copyErr := io.Copy(out, io.LimitReader(in, (256<<20)+1))
		in.Close()
		closeErr := out.Close()
		total += n
		if copyErr != nil {
			return copyErr
		}
		if closeErr != nil {
			return closeErr
		}
		if n > 256<<20 || total > 1536<<20 {
			return errors.New("pacote descompactado maior que o limite")
		}
	}
	return nil
}
func embeddedBundle() (string, error) {
	exe, e := os.Executable()
	if e != nil {
		return "", e
	}
	root := filepath.Dir(exe)
	if _, e = readBundle(root); e == nil {
		return root, nil
	}
	if runtime.GOOS == "darwin" {
		root = filepath.Clean(filepath.Join(root, "..", "Resources"))
		if _, e = readBundle(root); e == nil {
			return root, nil
		}
	}
	return "", errors.New("iniciador sem pacote embarcado")
}
func (m *Manager) installEmbedded(root string) (string, Bundle, error) {
	b, e := readBundle(root)
	if e != nil {
		return "", b, e
	}
	target := filepath.Join(m.home, "installed", b.Version)
	if existing, e := readBundle(target); e == nil {
		return target, existing, nil
	}
	stage, e := os.MkdirTemp(filepath.Join(m.home, "installed"), ".stage-")
	if e != nil {
		return "", b, e
	}
	defer os.RemoveAll(stage)
	content := filepath.Join(stage, "bundle")
	if e = copyTree(root, content, false); e != nil {
		return "", b, e
	}
	if _, e = readBundle(content); e != nil {
		return "", b, e
	}
	if e = m.preserveDamaged(target, b.Version); e != nil {
		return "", b, e
	}
	if e = os.Rename(content, target); e != nil {
		return "", b, e
	}
	return target, b, nil
}

// Uma pasta de versao que existe mas nao confere (ex.: arquivo removido por antivirus) nunca e
// apagada nem usada: recebe outro nome para conferencia posterior e a versao e instalada de novo.
func (m *Manager) preserveDamaged(target, version string) error {
	if _, e := os.Lstat(target); os.IsNotExist(e) {
		return nil
	} else if e != nil {
		return e
	}
	if _, e := readBundle(target); e == nil {
		return errors.New("a versao " + version + " ja esta instalada")
	}
	kept := filepath.Join(filepath.Dir(target), ".danificada-"+version+"-"+time.Now().UTC().Format("20060102-150405")+"-"+randomText(4))
	if e := os.Rename(target, kept); e != nil {
		return e
	}
	m.record("WARN", "DAMAGED_VERSION_PRESERVED", map[string]any{"result": "RENAMED"})
	return nil
}
func updateFailureCode(err error) string {
	for _, known := range []struct {
		err  error
		code string
	}{{errNetwork, "NETWORK"}, {errIncompleteRelease, "INCOMPLETE_RELEASE"}, {errIntegrity, "INTEGRITY"}, {errDataInUse, "DATA_IN_USE"}, {errOrphanRunning, "ORPHAN_STILL_RUNNING"}, {errJavaCheck, "JAVA_CHECK_FAILED"}} {
		if errors.Is(err, known.err) {
			return known.code
		}
	}
	return "PACKAGE_REJECTED"
}

// Com uma versao instalada integra, a falha da atualizacao nunca impede o uso: o pacote novo
// nao e ativado e a troca e tentada de novo na proxima abertura ou inicio.
func (m *Manager) keepInstalled(root string, b Bundle, next string, cause error) error {
	m.record("WARN", "UPDATE_SKIPPED", map[string]any{"phase": "UPDATE", "result": updateFailureCode(cause)})
	// Iniciar logo apos a abertura nao repete a mesma tentativa demorada; a proxima abertura tenta de novo.
	m.mu.Lock()
	if m.skippedUpdates == nil {
		m.skippedUpdates = map[string]bool{}
	}
	m.skippedUpdates[next] = true
	m.mu.Unlock()
	if e := m.ready(root, b); e != nil {
		return e
	}
	if errors.Is(cause, errNetwork) {
		m.message("Pronto", "Download indisponivel. Usando a versao instalada.")
		return nil
	}
	m.message("Pronto", "A atualizacao "+next+" nao foi instalada ("+cause.Error()+"). Usando a versao "+b.Version+"; nova tentativa na proxima abertura.")
	return nil
}

// Pastas temporarias de download/copia so existem durante uma instalacao; com o lock do iniciador,
// as restantes sobraram de uma execucao interrompida e podem ocupar mais de 1 GB.
func (m *Manager) removeStaleStages() {
	entries, _ := os.ReadDir(filepath.Join(m.home, "installed"))
	for _, entry := range entries {
		name := entry.Name()
		if entry.IsDir() && (strings.HasPrefix(name, ".download-") || strings.HasPrefix(name, ".stage-")) {
			if os.RemoveAll(filepath.Join(m.home, "installed", name)) == nil {
				m.record("INFO", "STALE_DOWNLOAD_REMOVED", nil)
			}
		}
	}
}
func (m *Manager) installLatest() error {
	if m.state().Running {
		return errors.New("pare o sistema antes de atualizar")
	}
	m.removeStaleStages()
	root, current, e := m.current()
	if e != nil {
		if embedded, err := embeddedBundle(); err == nil {
			root, current, e = m.installEmbedded(embedded)
		}
	}
	hasCurrent := e == nil
	m.message("Verificando", "Verificando atualizacoes...")
	if m.offline {
		if !hasCurrent {
			return errors.New("sem pacote local; conecte a internet para a primeira instalacao")
		}
		return m.ready(root, current)
	}
	next, e := m.latest()
	if e != nil || next == nil {
		if !hasCurrent {
			return errNoPackage
		}
		if e = m.ready(root, current); e != nil {
			return e
		}
		m.message("Pronto", "Usando a versao instalada; nenhuma atualizacao disponivel ou consulta offline.")
		return nil
	}
	if hasCurrent && !newer(next.Version, current.Version) {
		return m.ready(root, current)
	}
	m.mu.RLock()
	skipped := m.skippedUpdates[next.Version]
	m.mu.RUnlock()
	if hasCurrent && skipped {
		if e = m.ready(root, current); e != nil {
			return e
		}
		m.message("Pronto", "Usando a versao "+current.Version+"; a atualizacao "+next.Version+" sera tentada na proxima abertura.")
		return nil
	}
	// Sem versao instalada, qualquer falha continua interrompendo; nunca instalar parcialmente.
	fail := func(cause error) error {
		if hasCurrent {
			return m.keepInstalled(root, current, next.Version, cause)
		}
		if errors.Is(cause, errNetwork) {
			return errNoPackage
		}
		return fmt.Errorf("atualizacao nao instalada: %w", cause)
	}
	existing := filepath.Join(m.home, "installed", next.Version)
	if b, e := readBundle(existing); e == nil {
		if hasCurrent {
			if e = m.backup(root, current); e != nil {
				return fail(e)
			}
		}
		return m.ready(existing, b)
	}
	if e = m.preserveDamaged(existing, next.Version); e != nil {
		return fail(e)
	}
	m.message("Instalando", "Baixando e conferindo a versao "+next.Version+"...")
	stage, e := os.MkdirTemp(filepath.Join(m.home, "installed"), ".download-")
	if e != nil {
		return fail(e)
	}
	defer os.RemoveAll(stage)
	content, e := m.downloadRelease(next, stage)
	if e != nil {
		return fail(e)
	}
	if _, e = os.Stat(existing); e == nil {
		return fail(errors.New("a pasta da nova versao reapareceu durante o download"))
	}
	if hasCurrent {
		if e = m.backup(root, current); e != nil {
			return fail(e)
		}
	}
	b, e := readBundle(content)
	if e != nil {
		return fail(e)
	}
	if e = os.Rename(content, existing); e != nil {
		return fail(e)
	}
	return m.ready(existing, b)
}
