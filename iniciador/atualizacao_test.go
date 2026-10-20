package main

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"sync/atomic"
	"testing"
)

func TestConfigureRetriesUpdateSkippedEarlier(t *testing.T) {
	shortStall(t)
	m := testManager(t)
	installedFixture(t, m)
	m.stoppedCheck = func(string, Bundle) error { return nil }
	server := releaseServer(t, m, "0.4.1", false)
	defer server.Close()
	m.client = &slowDownloadClient{Client: server.Client(), body: func(r *http.Request, inner io.ReadCloser) io.ReadCloser {
		inner.Close()
		return stalledBody{r}
	}}
	if e := m.installLatest(); e != nil || m.state().Version != "0.4.0" {
		t.Fatal("falha do download deveria manter a versao instalada", e, m.state())
	}
	m.client = server.Client()
	c := &controller{manager: m}
	m.operation <- struct{}{}
	c.perform("/api/install")
	if m.state().Version != "0.4.1" {
		t.Fatal("Configurar/atualizar nao tentou de novo", m.state())
	}
}

func TestNewerPackageOfOpenedExecutableIsInstalledWithoutDownload(t *testing.T) {
	m := testManager(t)
	installedFixture(t, m)
	m.stoppedCheck = func(string, Bundle) error { return nil }
	opened := t.TempDir()
	fixtureBundle(t, opened, "0.5.2")
	previous := findEmbedded
	findEmbedded = func() (string, error) { return opened, nil }
	defer func() { findEmbedded = previous }()
	if e := os.WriteFile(filepath.Join(m.home, "data", "banco.mv.db"), []byte("dados"), 0600); e != nil {
		t.Fatal(e)
	}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { http.Error(w, "offline", 503) }))
	defer server.Close()
	m.api = server.URL
	m.client = server.Client()
	if e := m.installLatest(); e != nil || m.state().Version != "0.5.2" {
		t.Fatal("pacote do executavel aberto nao foi usado", e, m.state())
	}
	if _, e := readBundle(filepath.Join(m.home, "installed", "0.4.0")); e != nil {
		t.Fatal("versao anterior removida", e)
	}
	if data, e := os.ReadFile(filepath.Join(m.home, "data", "banco.mv.db")); e != nil || string(data) != "dados" {
		t.Fatal("dados alterados", e)
	}
	backups, _ := os.ReadDir(filepath.Join(m.home, "backups"))
	if len(backups) != 1 {
		t.Fatal("troca sem backup", len(backups))
	}
	// Um pacote igual ou mais antigo nunca troca a versao em uso.
	fixtureBundle(t, opened, "0.4.0")
	if e := m.installLatest(); e != nil || m.state().Version != "0.5.2" {
		t.Fatal("pacote antigo substituiu a versao em uso", e, m.state())
	}
}

func oldLauncher(t *testing.T, status Status) (*httptest.Server, *atomic.Int32) {
	t.Helper()
	closed := &atomic.Int32{}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("Authorization") != "Bearer token" {
			http.Error(w, "Acesso recusado", 403)
			return
		}
		switch {
		case r.Method == "GET" && r.URL.Path == "/api/status":
			_ = json.NewEncoder(w).Encode(status)
		case r.Method == "POST" && r.URL.Path == "/api/close":
			closed.Add(1)
			w.WriteHeader(http.StatusAccepted)
		default:
			http.NotFound(w, r)
		}
	}))
	t.Cleanup(server.Close)
	return server, closed
}

func TestOlderIdleLauncherGivesWayToNewerExecutable(t *testing.T) {
	previous := buildVersion
	buildVersion = "0.5.2"
	defer func() { buildVersion = previous }()
	server, closed := oldLauncher(t, Status{Version: "0.4.17", Ready: true})
	if !replaceOlderLauncher(server.URL+"/#token") || closed.Load() != 1 {
		t.Fatal("iniciador antigo parado nao foi fechado")
	}
	for _, status := range []Status{{Version: "0.4.17", Running: true}, {Version: "0.4.17", Busy: true}, {Version: "0.5.2"}} {
		server, closed := oldLauncher(t, status)
		if replaceOlderLauncher(server.URL+"/#token") || closed.Load() != 0 {
			t.Fatal("iniciador em uso ou da mesma versao foi fechado", status)
		}
	}
}
