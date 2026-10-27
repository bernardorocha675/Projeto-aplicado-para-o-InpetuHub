package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

// Gerente com versao 0.4.0 instalada e sessao gravando eventos, para conferir tela e log.
func checkManager(t *testing.T) (*Manager, *Session) {
	t.Helper()
	m := testManager(t)
	installedFixture(t, m)
	m.stoppedCheck = func(string, Bundle) error { return nil }
	s, e := beginSession(t.TempDir(), "")
	if e != nil {
		t.Fatal(e)
	}
	m.session = s
	// Fecha o eventos.jsonl antes de apagar a pasta temporaria; no Windows, arquivo aberto impede a limpeza.
	t.Cleanup(func() { _ = s.finish("CONCLUIDO") })
	return m, s
}

func launcherEvents(t *testing.T, s *Session) map[string]map[string]any {
	t.Helper()
	if e := s.finish("CONCLUIDO"); e != nil {
		t.Fatal(e)
	}
	out := map[string]map[string]any{}
	for _, event := range readSessionReport(t, s).Events {
		if event.Component == "launcher" {
			out[event.Event] = event.Fields
		}
	}
	return out
}

// Nunca mais as duas situacoes juntas na mesma frase.
func assertClearMessage(t *testing.T, m *Manager, want string) {
	t.Helper()
	got := m.state().Message
	if !strings.Contains(got, want) || strings.Contains(got, "ou consulta offline") || strings.Contains(got, "nenhuma atualizacao disponivel") {
		t.Fatalf("mensagem %q, esperava conter %q", got, want)
	}
	if m.state().Version != "0.4.0" {
		t.Fatal("a versao instalada deveria continuar em uso", m.state())
	}
}

func TestUpdateCheckFailureIsRecordedWithAFixedCode(t *testing.T) {
	for _, c := range []struct {
		status  int
		code    string
		message string
	}{
		{403, "RATE_LIMITED", "limite de consultas do GitHub"},
		{429, "RATE_LIMITED", "limite de consultas do GitHub"},
		{500, "HTTP_STATUS", "o GitHub respondeu com erro"},
	} {
		t.Run(fmt.Sprint(c.status), func(t *testing.T) {
			m, s := checkManager(t)
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.Header().Set("X-Secret-Header", "token-privado")
				http.Error(w, "corpo-privado https://api.github.com/x", c.status)
			}))
			defer server.Close()
			m.api, m.client = server.URL, server.Client()
			if e := m.installLatest(); e != nil {
				t.Fatal(e)
			}
			assertClearMessage(t, m, c.message)
			assertClearMessage(t, m, "Usando a versao 0.4.0")
			failed := launcherEvents(t, s)["UPDATE_CHECK_FAILED"]
			if failed == nil || failed["result"] != c.code || fmt.Sprint(failed["status"]) != fmt.Sprint(c.status) {
				t.Fatalf("evento ausente ou errado: %v", failed)
			}
			data, _ := json.Marshal(failed)
			for _, secret := range []string{"token-privado", "corpo-privado", "127.0.0.1", "api.github.com"} {
				if strings.Contains(string(data), secret) {
					t.Fatalf("vazou %q: %s", secret, data)
				}
			}
		})
	}
}

func TestUpdateCheckTimeoutAndNoConnectionAreTold(t *testing.T) {
	t.Run("TIMEOUT", func(t *testing.T) {
		m, s := checkManager(t)
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			time.Sleep(500 * time.Millisecond)
		}))
		defer server.Close()
		m.api = server.URL
		m.client = &http.Client{Timeout: 100 * time.Millisecond}
		if e := m.installLatest(); e != nil {
			t.Fatal(e)
		}
		assertClearMessage(t, m, "sem conexao com o GitHub")
		if failed := launcherEvents(t, s)["UPDATE_CHECK_FAILED"]; failed == nil || failed["result"] != "TIMEOUT" {
			t.Fatalf("evento ausente ou errado: %v", failed)
		}
	})
	t.Run("NETWORK", func(t *testing.T) {
		m, s := checkManager(t)
		server := httptest.NewServer(http.NotFoundHandler())
		m.api = server.URL
		m.client = server.Client()
		server.Close() // conexao recusada
		if e := m.installLatest(); e != nil {
			t.Fatal(e)
		}
		assertClearMessage(t, m, "sem conexao com o GitHub")
		if failed := launcherEvents(t, s)["UPDATE_CHECK_FAILED"]; failed == nil || failed["result"] != "NETWORK" {
			t.Fatalf("evento ausente ou errado: %v", failed)
		}
	})
}

func TestAlreadyOnTheLatestVersionSaysSo(t *testing.T) {
	m, s := checkManager(t)
	server := releaseServer(t, m, "0.4.0", false)
	defer server.Close()
	if e := m.installLatest(); e != nil {
		t.Fatal(e)
	}
	assertClearMessage(t, m, "Voce ja esta na versao mais recente (0.4.0)")
	if events := launcherEvents(t, s); events["UPDATE_CHECK_FAILED"] != nil {
		t.Fatal("consulta bem-sucedida registrada como falha", events)
	}
}

func TestIncompleteNewerReleaseFallsBackToTheLastCompleteOne(t *testing.T) {
	for _, c := range []struct {
		name, code string
		assets     func(base, version string) []releaseAsset
	}{
		{"sem pacote", "MISSING_PACKAGE", func(base, version string) []releaseAsset {
			return []releaseAsset{{Name: "SHA256SUMS.txt", State: "uploaded", Browser_download_url: base + "/sums"}}
		}},
		{"sem SHA256SUMS", "MISSING_SUMS", func(base, version string) []releaseAsset {
			return []releaseAsset{{Name: archiveName(version), State: "uploaded", Browser_download_url: base + "/zip"}}
		}},
		{"pacote ainda enviando", "MISSING_PACKAGE", func(base, version string) []releaseAsset {
			return []releaseAsset{{Name: archiveName(version), State: "starter", Browser_download_url: base + "/zip"}, {Name: "SHA256SUMS.txt", State: "uploaded", Browser_download_url: base + "/sums"}}
		}},
	} {
		t.Run(c.name, func(t *testing.T) {
			m, s := checkManager(t)
			var server *httptest.Server
			server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				complete := githubRelease{Tag_name: "v0.4.1", Assets: []releaseAsset{{Name: archiveName("0.4.1"), State: "uploaded", Browser_download_url: server.URL + "/zip"}, {Name: "SHA256SUMS.txt", State: "uploaded", Browser_download_url: server.URL + "/sums"}}}
				_ = json.NewEncoder(w).Encode([]githubRelease{{Tag_name: "v0.4.2", Assets: c.assets(server.URL, "0.4.2")}, complete})
			}))
			defer server.Close()
			m.api, m.client = server.URL, server.Client()
			latest, e := m.latest()
			if e != nil || latest == nil || latest.Version != "0.4.1" || latest.ZipURL == "" || latest.SumURL == "" {
				t.Fatalf("deveria cair para a 0.4.1 completa: %v %v", latest, e)
			}
			incomplete := launcherEvents(t, s)["UPDATE_RELEASE_INCOMPLETE"]
			if incomplete == nil || incomplete["result"] != c.code {
				t.Fatalf("evento da release incompleta ausente ou errado: %v", incomplete)
			}
		})
	}
}

func TestIncompleteReleaseIsNotInstalledAndTheCompleteOneIs(t *testing.T) {
	m, _ := checkManager(t)
	server := releaseServer(t, m, "0.4.1", false)
	defer server.Close()
	inner := server.Config.Handler
	server.Config.Handler = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/releases" {
			var list []githubRelease
			rec := httptest.NewRecorder()
			inner.ServeHTTP(rec, r)
			_ = json.Unmarshal(rec.Body.Bytes(), &list)
			list = append([]githubRelease{{Tag_name: "v0.4.2", Assets: []releaseAsset{{Name: "SHA256SUMS.txt", State: "uploaded", Browser_download_url: server.URL + "/sums"}}}}, list...)
			_ = json.NewEncoder(w).Encode(list)
			return
		}
		inner.ServeHTTP(w, r)
	})
	if e := m.installLatest(); e != nil || m.state().Version != "0.4.1" {
		t.Fatal("deveria instalar a 0.4.1 completa", e, m.state())
	}
}
