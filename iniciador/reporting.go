package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

const reportingConfigURL = "https://raw.githubusercontent.com/" + repository + "/main/Testes/configuracao.json"

type reportingConfig struct {
	Enabled  bool   `json:"enabled"`
	Endpoint string `json:"endpoint"`
}
type reportReceipt struct {
	ID     string `json:"id"`
	Folder string `json:"folder"`
	URL    string `json:"url"`
}

func reportingHTTPClient() *http.Client {
	return &http.Client{Timeout: 7 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error {
		return errors.New("redirecionamento de relatorio recusado")
	}}
}
func validReportingEndpoint(address string) bool {
	u, e := url.Parse(address)
	return e == nil && u.Scheme == "https" && u.User == nil && u.RawQuery == "" && u.Fragment == "" && u.Port() == "" && strings.Contains(u.Hostname(), ".") && u.Path == "/v1/reports" && !regexp.MustCompile(`^[0-9.:]+$`).MatchString(u.Hostname())
}
func loadReportingConfig(home string, client *http.Client) (reportingConfig, error) {
	var config reportingConfig
	// Sobreposição local de manutenção; o destino padrão é consultado no repositório oficial.
	path := filepath.Join(home, ".secrets", "reporting.json")
	if fi, e := os.Lstat(path); e == nil {
		if !fi.Mode().IsRegular() || fi.Size() > 4096 {
			return config, errors.New("configuracao privada de envio invalida")
		}
		data, e := os.ReadFile(path)
		if e != nil {
			return config, e
		}
		if e = json.Unmarshal(data, &config); e != nil {
			return config, e
		}
	} else if !os.IsNotExist(e) {
		return config, e
	} else {
		req, _ := http.NewRequest("GET", reportingConfigURL, nil)
		req.Header.Set("User-Agent", "InPETU-Hub/"+buildVersion)
		response, e := client.Do(req)
		if e != nil {
			return config, e
		}
		defer response.Body.Close()
		if response.StatusCode != 200 {
			return config, errors.New("configuracao publica de envio indisponivel")
		}
		var public struct {
			Enabled  bool   `json:"enabled"`
			Endpoint string `json:"endpoint"`
		}
		decoder := json.NewDecoder(io.LimitReader(response.Body, 4097))
		decoder.DisallowUnknownFields()
		if e = decoder.Decode(&public); e != nil {
			return config, e
		}
		config.Enabled = public.Enabled
		config.Endpoint = public.Endpoint
	}
	if config.Enabled && !validReportingEndpoint(config.Endpoint) {
		return config, errors.New("destino de envio invalido")
	}
	return config, nil
}
func cleanReport(report SessionReport) (SessionReport, error) {
	fail := func() (SessionReport, error) { return SessionReport{}, errors.New("relatorio invalido") }
	if report.Schema != 1 || !sessionIDPattern.MatchString(report.ID) || report.Sequence < 1 || report.Sequence > 999999999 {
		return fail()
	}
	if report.Outcome != "CONCLUIDO" && report.Outcome != "FALHA" && report.Outcome != "INTERROMPIDO" {
		return fail()
	}
	switch report.OS {
	case "windows", "darwin", "linux":
	default:
		return fail()
	}
	if report.Arch != "amd64" && report.Arch != "arm64" {
		return fail()
	}
	if report.Dropped < 0 || report.Dropped > 1_000_000_000 || len(report.Events) < 1 || len(report.Events) > maxSessionEvents {
		return fail()
	}
	started, e := time.Parse(time.RFC3339Nano, report.StartedAt)
	if e != nil || started.Year() < 2020 || started.Year() > 2100 {
		return fail()
	}
	ended, e := time.Parse(time.RFC3339Nano, report.EndedAt)
	if e != nil || ended.Before(started) || ended.Year() > 2100 {
		return fail()
	}
	if len(report.LauncherVersions) < 1 || len(report.LauncherVersions) > 20 || len(report.AppVersions) > 20 {
		return fail()
	}
	for _, v := range report.LauncherVersions {
		if _, e = versionOrder(v); e != nil {
			return fail()
		}
	}
	for _, v := range report.AppVersions {
		if safeVersion(v) != v {
			return fail()
		}
	}
	for i, event := range report.Events {
		if event.SessionID != report.ID || !codePattern.MatchString(event.Event) || safeVersion(event.AppVersion) != event.AppVersion {
			return fail()
		}
		if _, e = versionOrder(event.LauncherVersion); e != nil {
			return fail()
		}
		if event.Component != "launcher" && event.Component != "app" {
			return fail()
		}
		if event.Level != "INFO" && event.Level != "WARN" && event.Level != "ERROR" {
			return fail()
		}
		if _, e = time.Parse(time.RFC3339Nano, event.Time); e != nil {
			return fail()
		}
		report.Events[i].Fields = safeFields(event.Fields)
	}
	return report, nil
}
func sendPendingReports(home string, config reportingConfig, client *http.Client) (int, error) {
	root := filepath.Join(home, "logs", "sessoes")
	entries, e := os.ReadDir(root)
	if os.IsNotExist(e) {
		return 0, nil
	}
	if e != nil {
		return 0, e
	}
	sent := 0
	attempts := 0
	for _, entry := range entries {
		if !entry.IsDir() {
			continue
		}
		folder := filepath.Join(root, entry.Name())
		if fi, e := os.Lstat(filepath.Join(folder, "enviado.json")); e == nil && fi.Mode().IsRegular() {
			continue
		}
		file := filepath.Join(folder, "relatorio.json")
		fi, e := os.Lstat(file)
		if e != nil {
			continue
		}
		if !fi.Mode().IsRegular() || fi.Size() > 3<<20 {
			continue
		}
		data, e := os.ReadFile(file)
		if e != nil {
			return sent, e
		}
		var report SessionReport
		if json.Unmarshal(data, &report) != nil {
			continue
		}
		var meta SessionReport
		metaData, metaErr := os.ReadFile(filepath.Join(folder, "sessao.json"))
		logInfo, logErr := os.Lstat(filepath.Join(folder, "log.txt"))
		if metaErr != nil || json.Unmarshal(metaData, &meta) != nil || meta.ID != report.ID ||
			meta.Outcome == "EM_ANDAMENTO" || meta.Outcome != report.Outcome ||
			meta.EndedAt != report.EndedAt || logErr != nil || !logInfo.Mode().IsRegular() {
			continue // Nunca enviar uma finalização ainda incompleta.
		}
		report, e = cleanReport(report)
		if e != nil {
			continue
		}
		data, e = json.Marshal(report)
		if e != nil {
			return sent, e
		}
		req, e := http.NewRequest("POST", config.Endpoint, bytes.NewReader(data))
		if e != nil {
			return sent, e
		}
		req.Header.Set("Content-Type", "application/json")
		req.Header.Set("User-Agent", "InPETU-Hub/"+buildVersion)
		req.Header.Set("X-Inpetu-Project", "inpetu-hub-v04")
		attempts++
		response, e := client.Do(req)
		if e != nil {
			return sent, e
		}
		var receipt reportReceipt
		decodeErr := json.NewDecoder(io.LimitReader(response.Body, 8192)).Decode(&receipt)
		response.Body.Close()
		if response.StatusCode == 202 {
			if attempts >= 2 {
				break
			}
			continue
		}
		if (response.StatusCode != 200 && response.StatusCode != 201) || decodeErr != nil || receipt.ID != report.ID || !regexp.MustCompile(`^Testes/teste[0-9]+-[0-9_-]+-[a-f0-9]{8}$`).MatchString(receipt.Folder) || receipt.URL != "https://github.com/"+repository+"/tree/main/"+receipt.Folder {
			return sent, errors.New("envio nao confirmado pelo receptor")
		}
		if e = atomicJSON(filepath.Join(folder, "enviado.json"), receipt); e != nil {
			return sent, e
		}
		sent++
		// Cada passagem e curta; a fila continua no proximo ciclo ou abertura.
		if attempts >= 2 {
			break
		}
	}
	return sent, nil
}
func pendingReportCount(home string) int {
	entries, _ := os.ReadDir(filepath.Join(home, "logs", "sessoes"))
	count := 0
	for _, entry := range entries {
		if !entry.IsDir() {
			continue
		}
		folder := filepath.Join(home, "logs", "sessoes", entry.Name())
		report, e := os.Lstat(filepath.Join(folder, "relatorio.json"))
		if e != nil || !report.Mode().IsRegular() {
			continue
		}
		if _, e = os.Lstat(filepath.Join(folder, "enviado.json")); os.IsNotExist(e) {
			count++
		}
	}
	return count
}

func (m *Manager) reportingStatus(text string) {
	m.mu.Lock()
	m.status.ReportStatus = text
	m.mu.Unlock()
}
func (m *Manager) flushReports() {
	m.reportMu.Lock()
	defer m.reportMu.Unlock()
	if m.offline {
		m.reportingStatus("Registros salvos neste computador (offline).")
		return
	}
	client := reportingHTTPClient()
	config, e := loadReportingConfig(m.home, client)
	if e != nil {
		// Sem internet a tentativa se repete a cada minuto; registrar so a mudanca de estado.
		if !m.reportFailing {
			m.record("WARN", "REPORT_CONFIG_FAILED", map[string]any{"phase": "CONFIG", "result": "NEGADO"})
		}
		m.reportFailing = true
		m.reportingStatus("Envio pendente; registros preservados neste computador.")
		return
	}
	if !config.Enabled {
		m.reportFailing = false
		m.reportingStatus("Registros locais ativos. Envio aguardando configuração do responsável.")
		return
	}
	count, e := sendPendingReports(m.home, config, client)
	if e != nil {
		if !m.reportFailing {
			m.record("WARN", "REPORT_UPLOAD_FAILED", map[string]any{"phase": "UPLOAD", "result": "NEGADO"})
		}
		m.reportFailing = true
		m.reportingStatus("Envio pendente; nova tentativa automática durante o uso ou na próxima abertura.")
		return
	}
	m.reportFailing = false
	if pendingReportCount(m.home) > 0 {
		m.reportingStatus("Relatórios aguardando confirmação de publicação no GitHub; cópias locais preservadas.")
	} else if count > 0 {
		m.record("INFO", "REPORTS_CONFIRMED", map[string]any{"result": "OK"})
		m.reportingStatus("Relatórios enviados ao GitHub; registros locais preservados.")
	} else {
		m.reportingStatus("Registros ativos. A sessão será enviada ao encerrar; a publicação no GitHub ocorre em segundo plano.")
	}
}
