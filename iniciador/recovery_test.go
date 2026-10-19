package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"testing"
	"time"
)

// O binario de teste tambem faz o papel do Java do pacote nas conferencias de parada.
func TestMain(m *testing.M) {
	if mode := os.Getenv("INPETU_FAKE_JAVA"); mode != "" {
		os.Exit(fakeJava(mode))
	}
	os.Exit(m.Run())
}

func fakeJava(mode string) int {
	switch mode {
	case "legacy-lock":
		fmt.Fprintln(os.Stderr, "Exception in thread \"main\" java.lang.IllegalStateException: Outra execucao ou backup esta usando esta instalacao.")
		fmt.Fprintln(os.Stderr, "\tat br.com.inpetu.hub.InpetuApplication.main(InpetuApplication.java:27)")
		return 1
	case "jvm-failure":
		fmt.Fprintln(os.Stderr, "Error: A JNI error has occurred, please check your installation and try again")
		return 1
	case "environment":
		if os.Getenv("JAVA_TOOL_OPTIONS") != "" {
			return 9
		}
		if runtime.GOOS == "linux" && !strings.Contains(strings.ToLower(os.Getenv("LC_CTYPE")), "utf-8") {
			return 8
		}
		return 0
	}
	code, e := strconv.Atoi(mode)
	if e != nil {
		return 2
	}
	return code
}

func fakeJavaBundle(t *testing.T, m *Manager) (string, Bundle) {
	t.Helper()
	self, e := os.Executable()
	if e != nil {
		t.Fatal(e)
	}
	root := filepath.Join(m.home, "installed", "0.4.0")
	java := "runtime/bin/java"
	if runtime.GOOS == "windows" {
		java += ".exe"
	}
	target := filepath.Join(root, filepath.FromSlash(java))
	if e = os.MkdirAll(filepath.Dir(target), 0700); e != nil {
		t.Fatal(e)
	}
	in, e := os.Open(self)
	if e != nil {
		t.Fatal(e)
	}
	defer in.Close()
	out, e := os.OpenFile(target, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0700)
	if e != nil {
		t.Fatal(e)
	}
	if _, e = io.Copy(out, in); e != nil {
		t.Fatal(e)
	}
	if e = out.Close(); e != nil {
		t.Fatal(e)
	}
	return root, Bundle{Version: "0.4.0", Jar: "app.jar", Java: java}
}

func TestStoppedCheckDistinguishesDataInUseFromJavaFailure(t *testing.T) {
	m := testManager(t)
	root, b := fakeJavaBundle(t, m)
	for _, test := range []struct {
		mode string
		want error
	}{{"0", nil}, {"75", errDataInUse}, {"legacy-lock", errDataInUse}, {"jvm-failure", errJavaCheck}, {"1", errJavaCheck}} {
		t.Setenv("INPETU_FAKE_JAVA", test.mode)
		if e := m.checkStopped(root, b); !errors.Is(e, test.want) || (test.want == nil && e != nil) {
			t.Fatalf("modo %s: %v, esperado %v", test.mode, e, test.want)
		}
	}
}

func TestStoppedCheckUsesCleanUTF8Environment(t *testing.T) {
	m := testManager(t)
	root, b := fakeJavaBundle(t, m)
	t.Setenv("INPETU_FAKE_JAVA", "environment")
	t.Setenv("JAVA_TOOL_OPTIONS", "-javaagent:/caminho/inexistente/agente.jar")
	t.Setenv("LC_ALL", "")
	t.Setenv("LC_CTYPE", "")
	t.Setenv("LANG", "")
	if e := m.checkStopped(root, b); e != nil {
		t.Fatal("opcoes Java ou locale de outro programa chegaram a conferencia:", e)
	}
}

func TestJavaEnvironmentForcesUTF8OnlyWhenNeeded(t *testing.T) {
	get := func(env []string, key string) (string, bool) {
		for _, entry := range env {
			if pair := strings.SplitN(entry, "=", 2); pair[0] == key {
				return pair[1], true
			}
		}
		return "", false
	}
	posix := javaEnvironment([]string{"PATH=/bin", "LANG=", "LC_ALL=POSIX", "JAVA_TOOL_OPTIONS=x"}, "linux")
	if v, _ := get(posix, "LC_CTYPE"); v != "C.UTF-8" {
		t.Fatal("POSIX sem UTF-8", posix)
	}
	if _, ok := get(posix, "LC_ALL"); ok {
		t.Fatal("LC_ALL nao UTF-8 mantido", posix)
	}
	if _, ok := get(posix, "JAVA_TOOL_OPTIONS"); ok {
		t.Fatal("opcoes Java herdadas", posix)
	}
	brazil := javaEnvironment([]string{"LANG=pt_BR.UTF-8"}, "linux")
	if len(brazil) != 1 || brazil[0] != "LANG=pt_BR.UTF-8" {
		t.Fatal("locale UTF-8 existente alterado", brazil)
	}
	for _, goos := range []string{"windows", "darwin"} {
		if other := javaEnvironment([]string{"LANG="}, goos); len(other) != 1 {
			t.Fatal("locale alterado fora do Linux", goos, other)
		}
	}
}

func TestOrphanAppIsStoppedThroughLocalRequest(t *testing.T) {
	m := testManager(t)
	orphanStopTimeout = 5 * time.Second
	t.Cleanup(func() { orphanStopTimeout = 30 * time.Second })
	request := filepath.Join(m.home, ".secrets", "launcher.stop")
	stopped := false
	m.stoppedCheck = func(string, Bundle) error {
		if _, e := os.Stat(request); e == nil {
			stopped = true
		}
		if stopped {
			return nil
		}
		return errDataInUse
	}
	if e := m.ensureStopped("", Bundle{}); e != nil {
		t.Fatal("aplicativo deixado pela execucao anterior nao foi encerrado:", e)
	}
	if _, e := os.Stat(request); !os.IsNotExist(e) {
		t.Fatal("pedido de parada mantido depois da recuperacao", e)
	}
}

func TestOrphanThatKeepsRunningIsReportedAndRequestRemoved(t *testing.T) {
	m := testManager(t)
	orphanStopTimeout = 1500 * time.Millisecond
	t.Cleanup(func() { orphanStopTimeout = 30 * time.Second })
	m.stoppedCheck = func(string, Bundle) error { return errDataInUse }
	if e := m.ensureStopped("", Bundle{}); !errors.Is(e, errOrphanRunning) || failureCode(e) != "ORPHAN_STILL_RUNNING" {
		t.Fatal(e)
	}
	if _, e := os.Stat(filepath.Join(m.home, ".secrets", "launcher.stop")); !os.IsNotExist(e) {
		t.Fatal("pedido de parada esquecido", e)
	}
	m.stoppedCheck = func(string, Bundle) error { return errJavaCheck }
	if e := m.ensureStopped("", Bundle{}); !errors.Is(e, errJavaCheck) {
		t.Fatal("falha do Java tratada como processo em uso", e)
	}
}

func TestOwnRunningAppIsNeverStoppedByRecovery(t *testing.T) {
	m := testManager(t)
	m.process = &runningProcess{done: make(chan struct{})}
	m.stoppedCheck = func(string, Bundle) error { return errDataInUse }
	if e := m.ensureStopped("", Bundle{}); !errors.Is(e, errDataInUse) {
		t.Fatal(e)
	}
	if _, e := os.Stat(filepath.Join(m.home, ".secrets", "launcher.stop")); !os.IsNotExist(e) {
		t.Fatal("pedido de parada enviado ao proprio processo")
	}
}

func installedFixture(t *testing.T, m *Manager) {
	t.Helper()
	old := filepath.Join(m.home, "installed", "0.4.0")
	if e := m.ready(old, fixtureBundle(t, old, "0.4.0")); e != nil {
		t.Fatal(e)
	}
}

func TestDamagedNewVersionFolderIsPreservedAndReinstalled(t *testing.T) {
	m := testManager(t)
	m.stoppedCheck = func(string, Bundle) error { return nil }
	installedFixture(t, m)
	next := filepath.Join(m.home, "installed", "0.4.1")
	fixtureBundle(t, next, "0.4.1")
	if e := os.WriteFile(filepath.Join(next, "app.jar"), []byte("arquivo danificado"), 0600); e != nil {
		t.Fatal(e)
	}
	server := releaseServer(t, m, "0.4.1", false)
	defer server.Close()
	if e := m.installLatest(); e != nil {
		t.Fatal("pasta danificada bloqueou a atualizacao:", e)
	}
	if _, b, e := m.current(); e != nil || b.Version != "0.4.1" {
		t.Fatal("versao reinstalada nao selecionada", b, e)
	}
	kept, _ := filepath.Glob(filepath.Join(m.home, "installed", ".danificada-0.4.1-*"))
	if len(kept) != 1 {
		t.Fatal("pasta danificada nao preservada", kept)
	}
	if data, _ := os.ReadFile(filepath.Join(kept[0], "app.jar")); string(data) != "arquivo danificado" {
		t.Fatal("conteudo preservado alterado")
	}
	if _, e := readBundle(filepath.Join(m.home, "installed", "0.4.0")); e != nil {
		t.Fatal("versao anterior removida", e)
	}
}

func TestDamagedFolderIsPreservedWhenDownloadIsUnavailable(t *testing.T) {
	m := testManager(t)
	installedFixture(t, m)
	next := filepath.Join(m.home, "installed", "0.4.1")
	if e := os.MkdirAll(next, 0700); e != nil {
		t.Fatal(e)
	}
	server := releaseServer(t, m, "0.4.1", true)
	defer server.Close()
	if e := m.installLatest(); e != nil || m.state().Version != "0.4.0" || !m.state().Ready {
		t.Fatal("versao instalada bloqueada", e, m.state())
	}
	if kept, _ := filepath.Glob(filepath.Join(m.home, "installed", ".danificada-0.4.1-*")); len(kept) != 1 {
		t.Fatal("pasta danificada nao preservada", kept)
	}
}

func TestRejectedUpdateKeepsInstalledVersionUsable(t *testing.T) {
	for _, test := range []struct {
		name string
		sums string
		zip  bool
	}{{"release sem pacote da plataforma", "", false}, {"checksum divergente", strings.Repeat("0", 64), true}} {
		t.Run(test.name, func(t *testing.T) {
			m := testManager(t)
			installedFixture(t, m)
			var server *httptest.Server
			server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				switch r.URL.Path {
				case "/releases":
					assets := []releaseAsset{{Name: "SHA256SUMS.txt", State: "uploaded", Browser_download_url: server.URL + "/sums"}}
					if test.zip {
						assets = append(assets, releaseAsset{Name: archiveName("0.4.1"), State: "uploaded", Browser_download_url: server.URL + "/zip"})
					}
					_ = json.NewEncoder(w).Encode([]githubRelease{{Tag_name: "v0.4.1", Assets: assets}})
				case "/sums":
					fmt.Fprintf(w, "%s  %s\n", test.sums, archiveName("0.4.1"))
				default:
					_, _ = w.Write([]byte("pacote incorreto"))
				}
			}))
			defer server.Close()
			m.api = server.URL + "/releases"
			m.client = server.Client()
			if e := m.installLatest(); e != nil {
				t.Fatal("versao instalada integra bloqueada:", e)
			}
			if m.state().Version != "0.4.0" || !m.state().Ready {
				t.Fatal(m.state())
			}
			if _, e := os.Stat(filepath.Join(m.home, "installed", "0.4.1")); !os.IsNotExist(e) {
				t.Fatal("pacote rejeitado instalado", e)
			}
		})
	}
}

func TestFirstInstallStillRefusesRejectedPackage(t *testing.T) {
	m := testManager(t)
	var server *httptest.Server
	server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/releases" {
			_ = json.NewEncoder(w).Encode([]githubRelease{{Tag_name: "v0.4.1", Assets: []releaseAsset{{Name: archiveName("0.4.1"), State: "uploaded", Browser_download_url: server.URL + "/zip"}, {Name: "SHA256SUMS.txt", State: "uploaded", Browser_download_url: server.URL + "/sums"}}}})
			return
		}
		if r.URL.Path == "/sums" {
			fmt.Fprintf(w, "%s  %s\n", strings.Repeat("0", 64), archiveName("0.4.1"))
			return
		}
		_, _ = w.Write([]byte("pacote incorreto"))
	}))
	defer server.Close()
	m.api = server.URL + "/releases"
	m.client = server.Client()
	if e := m.installLatest(); !errors.Is(e, errIntegrity) {
		t.Fatal("primeira instalacao aceitou pacote invalido", e)
	}
	if entries, _ := os.ReadDir(filepath.Join(m.home, "installed")); len(entries) != 0 {
		t.Fatal("instalacao parcial", entries)
	}
}

func TestFailureCodesIdentifyCauseWithoutFreeText(t *testing.T) {
	for err, want := range map[error]string{
		fmt.Errorf("contexto: %w", errDataInUse): "DATA_IN_USE",
		errJavaCheck:                             "JAVA_CHECK_FAILED",
		errAppNotStarted:                         "APP_NOT_STARTED",
		errStartTimeout:                          "APP_START_TIMEOUT",
		errPrivateConfigMissing:                  "PRIVATE_CONFIG_MISSING",
		errNoPackage:                             "NO_PACKAGE",
		errors.New("texto livre qualquer"):       "OTHER",
	} {
		if got := failureCode(err); got != want || !codePattern.MatchString(got) {
			t.Fatalf("%v: %s, esperado %s", err, got, want)
		}
	}
}
