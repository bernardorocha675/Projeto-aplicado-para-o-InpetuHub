package main

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
)

var buildVersion = "0.4.14"

const repository = "bernardorocha675/Projeto-aplicado-para-o-InpetuHub"
const family = "0.4"

var versionPattern = regexp.MustCompile("^v?0[.]4[.](0|[1-9][0-9]*)$")

type Bundle struct {
	Version    string
	GOOS       string
	GOARCH     string
	Jar        string
	Java       string
	Launcher   string
	JarSHA256  string
	JavaSHA256 string
}
type Status struct {
	Phase        string
	Message      string
	Version      string
	Ready        bool
	Running      bool
	Busy         bool
	AppURL       string
	LogPath      string
	ReportStatus string
}
type Manager struct {
	home         string
	api          string
	client       HTTPDoer
	offline      bool
	headless     bool
	mu           sync.RWMutex
	status       Status
	operation    chan struct{}
	process      *runningProcess
	onHandoff    func(string) error
	stoppedCheck func(string, Bundle) error
	session      *Session
	reportMu     sync.Mutex
	// Consulta recente das releases e versoes cuja instalacao ja falhou nesta execucao.
	latestCache    *release
	latestAt       time.Time
	skippedUpdates map[string]bool
	reportFailing  bool
}
type runningProcess struct {
	cmd  commandProcess
	done chan struct{}
}
type commandProcess interface{ Kill() error }
type installState struct{ Version string }

func patch(version string) (int, error) {
	m := versionPattern.FindStringSubmatch(version)
	if m == nil {
		return 0, errors.New("versao fora da linha 0.4.x")
	}
	return strconv.Atoi(m[1])
}
func newer(a, b string) bool {
	x, e := patch(a)
	y, e2 := patch(b)
	return e == nil && (e2 != nil || x > y)
}
func randomText(size int) string {
	b := make([]byte, size)
	if _, e := rand.Read(b); e != nil {
		panic(e)
	}
	return base64.RawURLEncoding.EncodeToString(b)
}
func digest(path string) (string, error) {
	file, e := os.Open(path)
	if e != nil {
		return "", e
	}
	defer file.Close()
	h := sha256.New()
	if _, e = io.Copy(h, file); e != nil {
		return "", e
	}
	return hex.EncodeToString(h.Sum(nil)), nil
}
func (m *Manager) message(phase, text string) {
	m.mu.Lock()
	m.status.Phase = phase
	m.status.Message = text
	m.mu.Unlock()
}
func (m *Manager) state() Status { m.mu.RLock(); defer m.mu.RUnlock(); return m.status }
func privateDir(path string) error {
	if fi, e := os.Lstat(path); e == nil && fi.Mode()&os.ModeSymlink != 0 {
		return errors.New("pasta privada nao pode ser um atalho")
	}
	if e := os.MkdirAll(path, 0700); e != nil {
		return e
	}
	return os.Chmod(path, 0700)
}
func atomicJSON(path string, value any) error {
	data, e := json.Marshal(value)
	if e != nil {
		return e
	}
	return atomicWrite(path, data, 0600)
}
func atomicWrite(path string, data []byte, mode os.FileMode) error {
	file, e := os.CreateTemp(filepath.Dir(path), ".inpetu-")
	if e != nil {
		return e
	}
	tmp := file.Name()
	defer os.Remove(tmp)
	if e = file.Chmod(mode); e == nil {
		_, e = file.Write(data)
	}
	if e == nil {
		e = file.Sync()
	}
	closeErr := file.Close()
	if e != nil {
		return e
	}
	if closeErr != nil {
		return closeErr
	}
	e = os.Rename(tmp, path)
	// No Windows, antivirus ou indexacao podem segurar o destino por instantes: tentar de novo antes
	// do ultimo recurso de remover o arquivo antigo, que deixa um intervalo sem o arquivo.
	for attempt := 0; e != nil && runtime.GOOS == "windows" && attempt < 10; attempt++ {
		time.Sleep(100 * time.Millisecond)
		e = os.Rename(tmp, path)
	}
	if e != nil && runtime.GOOS == "windows" {
		if fi, err := os.Lstat(path); err == nil && fi.Mode().IsRegular() {
			if err = os.Remove(path); err != nil {
				return err
			}
			e = os.Rename(tmp, path)
		}
	}
	return e
}
func copyTree(source, dest string, private bool) error {
	fi, e := os.Lstat(source)
	if e != nil {
		return e
	}
	if fi.Mode()&os.ModeSymlink != 0 {
		return errors.New("atalho recusado na copia")
	}
	if fi.IsDir() {
		mode := os.FileMode(0755)
		if private {
			mode = 0700
		}
		if e = os.MkdirAll(dest, mode); e != nil {
			return e
		}
		entries, e := os.ReadDir(source)
		if e != nil {
			return e
		}
		for _, entry := range entries {
			if private && entry.Name() == "runtime.lock" {
				continue
			}
			if e = copyTree(filepath.Join(source, entry.Name()), filepath.Join(dest, entry.Name()), private); e != nil {
				return e
			}
		}
		return nil
	}
	if !fi.Mode().IsRegular() {
		return errors.New("arquivo especial recusado")
	}
	if e = os.MkdirAll(filepath.Dir(dest), 0700); e != nil {
		return e
	}
	mode := fi.Mode().Perm()
	if private {
		mode = 0600
	}
	in, e := os.Open(source)
	if e != nil {
		return e
	}
	defer in.Close()
	out, e := os.OpenFile(dest, os.O_CREATE|os.O_EXCL|os.O_WRONLY, mode)
	if e != nil {
		return e
	}
	_, e = io.Copy(out, in)
	closeErr := out.Close()
	if e != nil {
		return e
	}
	return closeErr
}
func safeRelative(name string) error {
	if name == "" || strings.ContainsAny(name, "\\:\x00") || !filepath.IsLocal(name) {
		return errors.New("caminho invalido no pacote")
	}
	parts := strings.Split(name, "/")
	for _, part := range parts {
		lower := strings.ToLower(part)
		if part == "" || part == "." || part == ".." || strings.HasSuffix(part, ".") || strings.HasSuffix(part, " ") {
			return errors.New("caminho invalido no pacote")
		}
		switch lower {
		case ".secrets", "data", "backups", ".env", ".env.local", "logs", ".git":
			return errors.New("arquivo privado recusado no pacote")
		}
	}
	return nil
}
func readBundle(root string) (Bundle, error) {
	var b Bundle
	data, e := os.ReadFile(filepath.Join(root, "bundle.json"))
	if e != nil {
		return b, e
	}
	if e = json.Unmarshal(data, &b); e != nil {
		return b, e
	}
	if _, e = patch(b.Version); e != nil {
		return b, e
	}
	if b.GOOS != runtime.GOOS || b.GOARCH != runtime.GOARCH {
		return b, errors.New("pacote de outro sistema ou arquitetura")
	}
	for _, name := range []string{b.Jar, b.Java, b.Launcher} {
		if e = safeRelative(name); e != nil {
			return b, e
		}
		if fi, err := os.Stat(filepath.Join(root, filepath.FromSlash(name))); err != nil || !fi.Mode().IsRegular() {
			return b, errors.New("pacote incompleto")
		}
	}
	jar, e := digest(filepath.Join(root, b.Jar))
	if e != nil || jar != b.JarSHA256 {
		return b, errors.New("arquivo do aplicativo corrompido")
	}
	java, e := digest(filepath.Join(root, filepath.FromSlash(b.Java)))
	if e != nil || java != b.JavaSHA256 {
		return b, errors.New("runtime corrompido")
	}
	return b, nil
}
func defaultHome() (string, error) {
	var base string
	if runtime.GOOS == "windows" {
		base = os.Getenv("LOCALAPPDATA")
		if base == "" {
			base = os.Getenv("APPDATA")
		}
	}
	if base == "" {
		var e error
		base, e = os.UserConfigDir()
		if e != nil {
			return "", e
		}
	}
	return filepath.Join(base, "InPETU-Hub", "0.4"), nil
}
func newManager(home string, offline, headless bool) (*Manager, error) {
	home, e := filepath.Abs(home)
	if e != nil {
		return nil, e
	}
	for _, name := range []string{"", ".secrets", "data", "logs", "installed", "backups"} {
		if e = privateDir(filepath.Join(home, name)); e != nil {
			return nil, e
		}
	}
	m := &Manager{home: home, api: "https://api.github.com/repos/" + repository + "/releases", offline: offline, headless: headless, operation: make(chan struct{}, 1)}
	m.client = newHTTPClient()
	m.status = Status{Phase: "Preparando", Message: "Preparando o iniciador...", LogPath: filepath.Join(home, "logs", "launcher-java.log")}
	return m, nil
}
func (m *Manager) current() (string, Bundle, error) {
	var saved installState
	data, e := os.ReadFile(filepath.Join(m.home, "current.json"))
	if e == nil && json.Unmarshal(data, &saved) == nil {
		if _, e = patch(saved.Version); e == nil {
			root := filepath.Join(m.home, "installed", saved.Version)
			if b, e := readBundle(root); e == nil {
				return root, b, nil
			}
		}
	}
	entries, e := os.ReadDir(filepath.Join(m.home, "installed"))
	if e != nil {
		return "", Bundle{}, e
	}
	sort.Slice(entries, func(i, j int) bool { return newer(entries[i].Name(), entries[j].Name()) })
	for _, entry := range entries {
		if !entry.IsDir() {
			continue
		}
		root := filepath.Join(m.home, "installed", entry.Name())
		if b, e := readBundle(root); e == nil {
			return root, b, nil
		}
	}
	return "", Bundle{}, errors.New("primeira instalacao ainda nao concluida")
}

var errPrivateConfigMissing = errors.New("configuracao privada ausente com dados existentes; preserve a instalacao e restaure o backup completo")

func (m *Manager) prepareConfig() error {
	// Um banco existente depende da chave e do ambiente originais. Nunca reparar
	// sua ausencia gerando material novo: a recuperacao exige o backup completo.
	entries, e := os.ReadDir(filepath.Join(m.home, "data"))
	if e != nil {
		return e
	}
	for _, name := range []string{".secrets/mfa.key", ".env.local"} {
		fi, err := os.Lstat(filepath.Join(m.home, filepath.FromSlash(name)))
		if err != nil && !os.IsNotExist(err) {
			return err
		}
		if err == nil && !fi.Mode().IsRegular() {
			return errors.New("configuracao privada deve ser arquivo regular, sem atalhos")
		}
		if os.IsNotExist(err) && len(entries) > 0 {
			return errPrivateConfigMissing
		}
	}
	secret := filepath.Join(m.home, ".secrets", "mfa.key")
	if _, e := os.Stat(secret); os.IsNotExist(e) {
		if e = atomicWrite(secret, []byte(randomText(32)+"\n"), 0600); e != nil {
			return e
		}
	}
	env := filepath.Join(m.home, ".env.local")
	if _, e := os.Stat(env); os.IsNotExist(e) {
		return atomicWrite(env, []byte("LOCAL_DB_PASSWORD="+randomText(32)+"\nSEED_DEMO=false\nMAIL_USERNAME=\nMAIL_PASSWORD=\n"), 0600)
	}
	return nil
}
func (m *Manager) ready(root string, b Bundle) error {
	if e := m.prepareConfig(); e != nil {
		return e
	}
	if e := atomicJSON(filepath.Join(m.home, "current.json"), installState{b.Version}); e != nil {
		return e
	}
	m.mu.Lock()
	m.status.Ready = true
	m.status.Version = b.Version
	m.mu.Unlock()
	if m.session != nil {
		m.session.setAppVersion(b.Version)
	}
	m.record("INFO", "PACKAGE_READY", nil)
	m.message("Pronto", "Tudo configurado. Clique em Iniciar.")
	return nil
}
func platformName() string {
	name := map[string]string{"windows": "Windows", "darwin": "macOS", "linux": "Linux"}[runtime.GOOS]
	arch := map[string]string{"amd64": "x64", "arm64": "ARM64"}[runtime.GOARCH]
	return name + "-" + arch
}
func archiveName(version string) string {
	return fmt.Sprintf("InPETU-Hub-%s-v%s.zip", platformName(), strings.TrimPrefix(version, "v"))
}
