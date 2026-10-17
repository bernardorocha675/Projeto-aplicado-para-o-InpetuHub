package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func finishedSession(t *testing.T, home string) *Session {
	t.Helper()
	s, e := beginSession(home, "")
	if e != nil {
		t.Fatal(e)
	}
	s.setAppVersion("0.4.2")
	s.record("ERROR", "app", "HTTP", map[string]any{"method": "POST", "route": "/login", "status": 403, "password": "synthetic-private", "email": "person@example.com", "errorType": "java.lang.IllegalStateException", "frames": []string{"example.Controller.login:42", "/home/private/key:42"}})
	if e = s.finish("CONCLUIDO"); e != nil {
		t.Fatal(e)
	}
	return s
}
func readSessionReport(t *testing.T, s *Session) SessionReport {
	t.Helper()
	data, e := os.ReadFile(filepath.Join(s.folder, "relatorio.json"))
	if e != nil {
		t.Fatal(e)
	}
	var report SessionReport
	if e = json.Unmarshal(data, &report); e != nil {
		t.Fatal(e)
	}
	return report
}
func TestSessionRecordsVersionsAndNeverCopiesFreeText(t *testing.T) {
	s := finishedSession(t, t.TempDir())
	report := readSessionReport(t, s)
	if len(report.Events) < 3 || report.LauncherVersions[0] != buildVersion || report.AppVersions[0] != "0.4.2" {
		t.Fatal("versoes ausentes", report)
	}
	data, _ := os.ReadFile(filepath.Join(s.folder, "log.txt"))
	text := string(data)
	for _, secret := range []string{"synthetic-private", "person@example.com", "/home/private"} {
		if strings.Contains(text, secret) {
			t.Fatal("dado privado no relatorio")
		}
	}
	if !strings.Contains(text, "app=0.4.2") || !strings.Contains(text, "IllegalStateException") {
		t.Fatal("diagnostico ausente")
	}
	for _, event := range report.Events {
		if event.LauncherVersion != buildVersion || event.SessionID != report.ID {
			t.Fatal("evento sem versao ou sessao")
		}
	}
}
func TestInterruptedSessionIsRecoveredOnNextOpening(t *testing.T) {
	home := t.TempDir()
	old, e := beginSession(home, "")
	if e != nil {
		t.Fatal(e)
	}
	old.setAppVersion("0.4.2")
	old.record("INFO", "launcher", "ACTION_STARTED", map[string]any{"action": "START"})
	old.detach()
	next, e := beginSession(home, "")
	if e != nil {
		t.Fatal(e)
	}
	defer next.finish("CONCLUIDO")
	recovered := readSessionReport(t, old)
	if recovered.Outcome != "INTERROMPIDO" || next.report.Sequence != 2 || next.report.ID == old.report.ID {
		t.Fatal("sessao interrompida perdida")
	}
}
func TestLauncherHandoffResumesTheSameSession(t *testing.T) {
	home := t.TempDir()
	old, e := beginSession(home, "")
	if e != nil {
		t.Fatal(e)
	}
	id := old.report.ID
	old.setAppVersion("0.4.0")
	old.detach()
	resumed, e := beginSession(home, id)
	if e != nil {
		t.Fatal(e)
	}
	resumed.setAppVersion("0.4.2")
	if e = resumed.finish("CONCLUIDO"); e != nil {
		t.Fatal(e)
	}
	report := readSessionReport(t, resumed)
	if report.ID != id || report.Sequence != 1 || len(report.AppVersions) != 2 || report.AppVersions[0] != "0.4.0" || report.AppVersions[1] != "0.4.2" {
		t.Fatal("continuidade perdida", report.AppVersions)
	}
	entries, _ := os.ReadDir(filepath.Dir(old.folder))
	if len(entries) != 1 {
		t.Fatal("handoff criou sessao duplicada")
	}
}
func TestJavaOutputIsParsedAcrossChunksAndSanitized(t *testing.T) {
	s, e := beginSession(t.TempDir(), "")
	if e != nil {
		t.Fatal(e)
	}
	s.setAppVersion("0.4.2")
	sink := &javaSessionSink{session: s}
	line := `{"level":"ERROR","logger":"br.com.inpetu.hub.observability.OperationalEvents","message":"event=HTTP method=POST route=/login status=403 durationMs=8 token=private-secret","exceptionType":"java.lang.IllegalArgumentException","frames":["example.Login.check:12"]}` + "\n"
	_, _ = sink.Write([]byte("unstructured password=do-not-copy\n"))
	_, _ = sink.Write([]byte(line[:25]))
	_, _ = sink.Write([]byte(line[25:]))
	if e = s.finish("CONCLUIDO"); e != nil {
		t.Fatal(e)
	}
	report := readSessionReport(t, s)
	found := false
	for _, event := range report.Events {
		if event.Event == "HTTP" {
			found = true
			if event.Fields["route"] != "/login" || event.AppVersion != "0.4.2" {
				t.Fatal("acao ausente")
			}
		}
	}
	data, _ := os.ReadFile(filepath.Join(s.folder, "relatorio.json"))
	if !found || strings.Contains(string(data), "private-secret") || strings.Contains(string(data), "do-not-copy") {
		t.Fatal("saida Java nao foi filtrada")
	}
}
func TestQueuedReportIsRetainedUntilGitHubReceiptAndIsNotSentTwice(t *testing.T) {
	home := t.TempDir()
	s := finishedSession(t, home)
	calls := 0
	server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		calls++
		var report SessionReport
		if e := json.NewDecoder(r.Body).Decode(&report); e != nil {
			t.Error(e)
		}
		if report.ID != s.report.ID || r.Header.Get("X-Inpetu-Project") != "inpetu-hub-v04" {
			t.Error("relatorio incorreto")
		}
		if calls == 1 {
			w.WriteHeader(202)
			_, _ = w.Write([]byte(`{"state":"QUEUED"}`))
			return
		}
		folder := "Testes/teste01-2026-10-16_21-00-00-" + report.ID[:8]
		_ = json.NewEncoder(w).Encode(reportReceipt{ID: report.ID, Folder: folder, URL: "https://github.com/" + repository + "/tree/main/" + folder})
	}))
	defer server.Close()
	config := reportingConfig{Enabled: true, Endpoint: server.URL}
	if count, e := sendPendingReports(home, config, server.Client()); e != nil || count != 0 {
		t.Fatal(count, e)
	}
	if _, e := os.Stat(filepath.Join(s.folder, "enviado.json")); !os.IsNotExist(e) {
		t.Fatal("fila confundida com publicacao")
	}
	if count, e := sendPendingReports(home, config, server.Client()); e != nil || count != 1 {
		t.Fatal(count, e)
	}
	if count, e := sendPendingReports(home, config, server.Client()); e != nil || count != 0 || calls != 2 {
		t.Fatal("duplicacao", count, e, calls)
	}
	if _, e := os.Stat(filepath.Join(s.folder, "relatorio.json")); e != nil {
		t.Fatal("copia local removida")
	}
}
func TestFailedUploadKeepsTheLocalReport(t *testing.T) {
	home := t.TempDir()
	s := finishedSession(t, home)
	server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "private-response-must-not-be-logged", 503)
	}))
	defer server.Close()
	if _, e := sendPendingReports(home, reportingConfig{Enabled: true, Endpoint: server.URL}, server.Client()); e == nil {
		t.Fatal("envio falso confirmado")
	}
	if _, e := os.Stat(filepath.Join(s.folder, "enviado.json")); !os.IsNotExist(e) {
		t.Fatal("falha marcada como envio")
	}
	data, _ := os.ReadFile(filepath.Join(s.folder, "log.txt"))
	if strings.Contains(string(data), "private-response") {
		t.Fatal("resposta livre copiada")
	}
}
func TestReceiptCannotRedirectOrChooseAnArbitraryRepositoryPath(t *testing.T) {
	home := t.TempDir()
	s := finishedSession(t, home)
	server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_ = json.NewEncoder(w).Encode(reportReceipt{ID: s.report.ID, Folder: "../outside", URL: "https://external.invalid"})
	}))
	defer server.Close()
	if _, e := sendPendingReports(home, reportingConfig{Enabled: true, Endpoint: server.URL}, server.Client()); e == nil {
		t.Fatal("recibo arbitrario aceito")
	}
}
func TestReportingEndpointsRequireHTTPSAndRejectCredentialsAndQueries(t *testing.T) {
	if !validReportingEndpoint("https://example.onrender.com/v1/reports") {
		t.Fatal("HTTPS valido recusado")
	}
	for _, address := range []string{"http://example.com/v1/reports", "https://user:secret@example.com/v1/reports", "https://example.com:443/v1/reports", "https://example.com/v1/reports?token=x", "https://example.com/v1/reports#x", "https://127.0.0.1/v1/reports"} {
		if validReportingEndpoint(address) {
			t.Fatal("destino inseguro", address)
		}
	}
}
func TestSessionLimitIsExplicitAndDoesNotCreateInvalidJSON(t *testing.T) {
	s, e := beginSession(t.TempDir(), "")
	if e != nil {
		t.Fatal(e)
	}
	s.setAppVersion("0.4.2")
	for i := 0; i < maxSessionEvents+10; i++ {
		s.record("INFO", "app", "HTTP", map[string]any{"status": 200, "route": "/agenda"})
	}
	if e = s.finish("CONCLUIDO"); e != nil {
		t.Fatal(e)
	}
	report := readSessionReport(t, s)
	if report.Dropped == 0 || len(report.Events) > maxSessionEvents {
		t.Fatal("limites nao registrados")
	}
	if _, e = cleanReport(report); e != nil {
		t.Fatal("relatorio invalido apos limite", e)
	}
}
