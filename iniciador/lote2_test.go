package main

import (
	"errors"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"
)

// Entrega o pacote em pedacos pequenos, como uma conexao lenta mas ativa.
type slowDownloadClient struct {
	*http.Client
	mu     sync.Mutex
	counts map[string]int
	body   func(*http.Request, io.ReadCloser) io.ReadCloser
}

func (c *slowDownloadClient) Do(r *http.Request) (*http.Response, error) {
	c.mu.Lock()
	if c.counts == nil {
		c.counts = map[string]int{}
	}
	c.counts[r.URL.Path]++
	c.mu.Unlock()
	response, e := c.Client.Do(r)
	if e == nil && r.URL.Path == "/zip" && c.body != nil {
		response.Body = c.body(r, response.Body)
	}
	return response, e
}
func (c *slowDownloadClient) count(path string) int {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.counts[path]
}

type trickleBody struct{ inner io.ReadCloser }

func (b trickleBody) Read(p []byte) (int, error) {
	time.Sleep(40 * time.Millisecond)
	if len(p) > 16 {
		p = p[:16]
	}
	return b.inner.Read(p)
}
func (b trickleBody) Close() error { return b.inner.Close() }

// Para de enviar dados, mas mantem a conexao aberta ate o cancelamento do pedido.
type stalledBody struct{ request *http.Request }

func (b stalledBody) Read([]byte) (int, error) {
	<-b.request.Context().Done()
	return 0, b.request.Context().Err()
}
func (stalledBody) Close() error { return nil }

func shortStall(t *testing.T) {
	downloadStallTimeout = 300 * time.Millisecond
	t.Cleanup(func() { downloadStallTimeout = 60 * time.Second })
}

func TestDownloadClientHasNoTotalTimeout(t *testing.T) {
	client := newHTTPClient()
	transport := client.Transport.(*http.Transport)
	if client.Timeout != 0 || transport.ResponseHeaderTimeout <= 0 || transport.TLSHandshakeTimeout <= 0 {
		t.Fatal("download lento seria interrompido por prazo total", client.Timeout)
	}
}

func TestSlowButActiveDownloadCompletes(t *testing.T) {
	shortStall(t)
	m := testManager(t)
	m.stoppedCheck = func(string, Bundle) error { return nil }
	installedFixture(t, m)
	server := releaseServer(t, m, "0.4.1", false)
	defer server.Close()
	client := &slowDownloadClient{Client: server.Client(), body: func(_ *http.Request, inner io.ReadCloser) io.ReadCloser { return trickleBody{inner} }}
	m.client = client
	started := time.Now()
	if e := m.installLatest(); e != nil {
		t.Fatal(e)
	}
	if _, b, e := m.current(); e != nil || b.Version != "0.4.1" {
		t.Fatal("download lento nao instalado", b, e)
	}
	if time.Since(started) < 3*downloadStallTimeout {
		t.Fatal("teste nao simulou transferencia mais longa que o prazo de travamento")
	}
}

func TestStalledDownloadKeepsInstalledVersionAndIsNotRepeatedOnStart(t *testing.T) {
	shortStall(t)
	m := testManager(t)
	installedFixture(t, m)
	server := releaseServer(t, m, "0.4.1", false)
	defer server.Close()
	client := &slowDownloadClient{Client: server.Client(), body: func(r *http.Request, inner io.ReadCloser) io.ReadCloser {
		inner.Close()
		return stalledBody{r}
	}}
	m.client = client
	for i := 0; i < 2; i++ {
		if e := m.installLatest(); e != nil || m.state().Version != "0.4.0" || !m.state().Ready {
			t.Fatal("versao instalada bloqueada", e, m.state())
		}
	}
	if client.count("/zip") != 1 || client.count("/releases") != 1 {
		t.Fatal("abrir e iniciar repetiram a consulta/download", client.counts)
	}
	if _, e := os.Stat(filepath.Join(m.home, "installed", "0.4.1")); !os.IsNotExist(e) {
		t.Fatal("pacote parcial instalado", e)
	}
}

func TestStaleDownloadFoldersAreRemovedButDamagedFoldersKept(t *testing.T) {
	m := testManager(t)
	m.offline = true
	installedFixture(t, m)
	for _, name := range []string{".download-123", ".stage-456", ".danificada-0.4.1-x"} {
		if e := os.MkdirAll(filepath.Join(m.home, "installed", name, "runtime"), 0700); e != nil {
			t.Fatal(e)
		}
	}
	if e := m.installLatest(); e != nil {
		t.Fatal(e)
	}
	for name, kept := range map[string]bool{".download-123": false, ".stage-456": false, ".danificada-0.4.1-x": true, "0.4.0": true} {
		if _, e := os.Stat(filepath.Join(m.home, "installed", name)); (e == nil) != kept {
			t.Fatal(name, kept, e)
		}
	}
}

func TestRepeatedReportFailureIsRecordedOnce(t *testing.T) {
	home := t.TempDir()
	session, e := beginSession(home, "")
	if e != nil {
		t.Fatal(e)
	}
	m := &Manager{home: home, session: session}
	if e = privateDir(filepath.Join(home, ".secrets")); e != nil {
		t.Fatal(e)
	}
	if e = os.WriteFile(filepath.Join(home, ".secrets", "reporting.json"), []byte("{invalido"), 0600); e != nil {
		t.Fatal(e)
	}
	for i := 0; i < 3; i++ {
		m.flushReports()
	}
	if e = session.finish("CONCLUIDO"); e != nil {
		t.Fatal(e)
	}
	data, _ := os.ReadFile(filepath.Join(session.folder, "eventos.jsonl"))
	if n := strings.Count(string(data), "REPORT_CONFIG_FAILED"); n != 1 {
		t.Fatal("falha repetida registrada", n, "vezes")
	}
}

func TestFatalErrorIsShownOnlyWithInterface(t *testing.T) {
	shown := []string{}
	previous := showFatal
	showFatal = func(title, text string) { shown = append(shown, text) }
	t.Cleanup(func() { showFatal = previous; headlessRun = false })
	headlessRun = true
	reportFatal(errors.New("teste sem tela"))
	headlessRun = false
	reportFatal(errDataInUse)
	if len(shown) != 1 || !strings.Contains(shown[0], errDataInUse.Error()) {
		t.Fatal("aviso grafico incorreto", shown)
	}
}
