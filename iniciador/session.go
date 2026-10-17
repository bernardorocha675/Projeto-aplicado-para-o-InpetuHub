package main

import (
	"bufio"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"
)

const maxSessionBytes = 2 << 20
const maxSessionEvents = 5000

var sessionIDPattern = regexp.MustCompile(`^[a-f0-9]{32}$`)
var codePattern = regexp.MustCompile(`^[A-Z][A-Z0-9_]{0,63}$`)
var symbolPattern = regexp.MustCompile(`^[a-zA-Z_$*][a-zA-Z0-9_.$*]{0,159}$`)
var framePattern = regexp.MustCompile(`^[a-zA-Z_$][a-zA-Z0-9_.$<>]*:[0-9]{1,7}$`)
var routePattern = regexp.MustCompile(`^/[a-zA-Z0-9_/{}/-]{0,120}$`)

type SessionEvent struct {
	Time            string         `json:"time"`
	Level           string         `json:"level"`
	Component       string         `json:"component"`
	Event           string         `json:"event"`
	AppVersion      string         `json:"appVersion"`
	LauncherVersion string         `json:"launcherVersion"`
	SessionID       string         `json:"sessionId"`
	Fields          map[string]any `json:"fields,omitempty"`
}
type SessionReport struct {
	Schema           int            `json:"schema"`
	ID               string         `json:"id"`
	Sequence         int            `json:"sequence"`
	StartedAt        string         `json:"startedAt"`
	EndedAt          string         `json:"endedAt,omitempty"`
	Outcome          string         `json:"outcome"`
	OS               string         `json:"os"`
	Arch             string         `json:"arch"`
	LauncherVersions []string       `json:"launcherVersions"`
	AppVersions      []string       `json:"appVersions"`
	Dropped          int            `json:"dropped"`
	Events           []SessionEvent `json:"events,omitempty"`
}
type Session struct {
	mu         sync.Mutex
	folder     string
	report     SessionReport
	file       *os.File
	size       int64
	count      int
	appVersion string
	closed     bool
}

func safeVersion(v string) string {
	if _, e := patch(v); e == nil {
		return strings.TrimPrefix(v, "v")
	}
	if v == "dev" {
		return v
	}
	return "unknown"
}
func addVersion(list []string, v string) []string {
	for _, old := range list {
		if old == v {
			return list
		}
	}
	return append(list, v)
}
func newSessionID() string {
	var b [16]byte
	if _, e := rand.Read(b[:]); e != nil {
		panic(e)
	}
	return hex.EncodeToString(b[:])
}
func safeFields(input map[string]any) map[string]any {
	out := map[string]any{}
	for key, value := range input {
		switch key {
		case "status", "durationMs", "actorId", "targetId", "exitCode", "pid":
			n, e := strconv.ParseInt(fmt.Sprint(value), 10, 64)
			if e == nil && n >= -1 && n <= 1_000_000_000 {
				out[key] = n
			}
		case "action", "result", "phase":
			if v, ok := value.(string); ok && codePattern.MatchString(v) {
				out[key] = v
			}
		case "method":
			if v, ok := value.(string); ok && regexp.MustCompile(`^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS|OTHER)$`).MatchString(v) {
				out[key] = v
			}
		case "target":
			if v, ok := value.(string); ok && regexp.MustCompile(`^[a-z_]{1,40}$`).MatchString(v) {
				out[key] = v
			}
		case "errorType", "logger":
			if v, ok := value.(string); ok && symbolPattern.MatchString(v) {
				out[key] = v
			}
		case "route":
			if v, ok := value.(string); ok && (v == "OTHER" || v == "ASSET" || routePattern.MatchString(v)) {
				out[key] = v
			}
		case "requestId":
			if v, ok := value.(string); ok && regexp.MustCompile(`^[a-f0-9-]{36}$`).MatchString(v) {
				out[key] = v
			}
		case "frames":
			var values []string
			switch v := value.(type) {
			case []string:
				values = v
			case []any:
				for _, item := range v {
					if s, ok := item.(string); ok {
						values = append(values, s)
					}
				}
			}
			clean := []string{}
			for _, s := range values {
				if framePattern.MatchString(s) && len(clean) < 20 {
					clean = append(clean, s)
				}
			}
			if len(clean) > 0 {
				out[key] = clean
			}
		}
	}
	return out
}
func openSessionFile(folder string, report SessionReport) (*Session, error) {
	if e := privateDir(folder); e != nil {
		return nil, e
	}
	path := filepath.Join(folder, "eventos.jsonl")
	if fi, e := os.Lstat(path); e == nil && !fi.Mode().IsRegular() {
		return nil, errors.New("arquivo de sessao invalido")
	}
	f, e := os.OpenFile(path, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0600)
	if e != nil {
		return nil, e
	}
	fi, e := f.Stat()
	if e != nil {
		f.Close()
		return nil, e
	}
	s := &Session{folder: folder, report: report, file: f, size: fi.Size(), appVersion: "unknown"}
	if len(report.AppVersions) > 0 {
		s.appVersion = report.AppVersions[len(report.AppVersions)-1]
	}
	// Cada registro ocupa ao menos 100 bytes; a leitura final tambem tem limites independentes.
	if s.size <= maxSessionBytes {
		if data, e := os.ReadFile(path); e == nil {
			s.count = strings.Count(string(data), "\n")
		}
	} else {
		s.count = maxSessionEvents
	}
	s.report.LauncherVersions = addVersion(s.report.LauncherVersions, safeVersion(buildVersion))
	return s, nil
}
func beginSession(home, resume string) (*Session, error) {
	root := filepath.Join(home, "logs", "sessoes")
	if e := privateDir(root); e != nil {
		return nil, e
	}
	entries, e := os.ReadDir(root)
	if e != nil {
		return nil, e
	}
	sequence := 0
	for _, entry := range entries {
		if !entry.IsDir() || !regexp.MustCompile(`^teste[0-9]+-[0-9_-]+-[a-f0-9]{32}$`).MatchString(entry.Name()) {
			continue
		}
		folder := filepath.Join(root, entry.Name())
		data, e := os.ReadFile(filepath.Join(folder, "sessao.json"))
		var old SessionReport
		if e != nil || len(data) > 32<<10 || json.Unmarshal(data, &old) != nil || !sessionIDPattern.MatchString(old.ID) {
			continue
		}
		if old.Sequence > sequence {
			sequence = old.Sequence
		}
		if old.Outcome == "EM_ANDAMENTO" {
			recovered, e := openSessionFile(folder, old)
			if e != nil {
				return nil, e
			}
			if resume != "" && old.ID == resume {
				recovered.record("INFO", "launcher", "SESSION_RESUMED", nil)
				return recovered, nil
			}
			recovered.record("WARN", "launcher", "SESSION_INTERRUPTED", nil)
			if e = recovered.finish("INTERROMPIDO"); e != nil {
				return nil, e
			}
		}
	}
	if sequence >= 999999999 {
		return nil, errors.New("limite de sessoes locais atingido")
	}
	now := time.Now()
	report := SessionReport{Schema: 1, ID: newSessionID(), Sequence: sequence + 1, StartedAt: now.UTC().Format(time.RFC3339Nano), Outcome: "EM_ANDAMENTO", OS: runtime.GOOS, Arch: runtime.GOARCH, LauncherVersions: []string{safeVersion(buildVersion)}, AppVersions: []string{}}
	folder := filepath.Join(root, fmt.Sprintf("teste%02d-%s-%s", report.Sequence, now.Format("2006-01-02_15-04-05"), report.ID))
	s, e := openSessionFile(folder, report)
	if e != nil {
		return nil, e
	}
	if e = atomicJSON(filepath.Join(folder, "sessao.json"), s.report); e != nil {
		s.file.Close()
		return nil, e
	}
	s.record("INFO", "launcher", "SESSION_STARTED", nil)
	return s, nil
}
func (s *Session) setAppVersion(version string) {
	if s == nil {
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	s.appVersion = safeVersion(version)
	s.report.AppVersions = addVersion(s.report.AppVersions, s.appVersion)
	_ = atomicJSON(filepath.Join(s.folder, "sessao.json"), s.report)
}
func (s *Session) record(level, component, event string, fields map[string]any) {
	if s == nil {
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.closed || s.file == nil {
		return
	}
	if level != "INFO" && level != "WARN" && level != "ERROR" {
		level = "INFO"
	}
	if component != "app" {
		component = "launcher"
	}
	if !codePattern.MatchString(event) {
		event = "OTHER"
	}
	e := SessionEvent{Time: time.Now().UTC().Format(time.RFC3339Nano), Level: level, Component: component, Event: event, AppVersion: s.appVersion, LauncherVersion: safeVersion(buildVersion), SessionID: s.report.ID, Fields: safeFields(fields)}
	data, err := json.Marshal(e)
	if err != nil {
		return
	}
	if s.size+int64(len(data)+1) > maxSessionBytes || s.count >= maxSessionEvents {
		s.report.Dropped++
		return
	}
	n, err := s.file.Write(append(data, '\n'))
	if err == nil {
		s.size += int64(n)
		s.count++
	}
}
func (s *Session) detach() {
	if s == nil {
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.file != nil {
		_ = s.file.Sync()
		_ = s.file.Close()
		s.file = nil
	}
	_ = atomicJSON(filepath.Join(s.folder, "sessao.json"), s.report)
}
func (s *Session) finish(outcome string) error {
	if s == nil {
		return nil
	}
	s.record("INFO", "launcher", "SESSION_FINISHED", map[string]any{"result": outcome})
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.closed {
		return nil
	}
	s.closed = true
	if s.file != nil {
		_ = s.file.Sync()
		_ = s.file.Close()
		s.file = nil
	}
	s.report.Outcome = outcome
	ended := time.Now().UTC()
	if began, e := time.Parse(time.RFC3339Nano, s.report.StartedAt); e == nil && ended.Before(began) {
		ended = began
	}
	s.report.EndedAt = ended.Format(time.RFC3339Nano)
	if e := atomicJSON(filepath.Join(s.folder, "sessao.json"), s.report); e != nil {
		return e
	}
	f, e := os.Open(filepath.Join(s.folder, "eventos.jsonl"))
	if e != nil {
		return e
	}
	defer f.Close()
	scanner := bufio.NewScanner(io.LimitReader(f, maxSessionBytes))
	scanner.Buffer(make([]byte, 4096), 32<<10)
	events := []SessionEvent{}
	for scanner.Scan() {
		var event SessionEvent
		if json.Unmarshal(scanner.Bytes(), &event) == nil && event.SessionID == s.report.ID && codePattern.MatchString(event.Event) && len(events) < maxSessionEvents {
			event.Fields = safeFields(event.Fields)
			events = append(events, event)
		}
	}
	if e = scanner.Err(); e != nil {
		return e
	}
	report := s.report
	report.Events = events
	if e = atomicJSON(filepath.Join(s.folder, "relatorio.json"), report); e != nil {
		return e
	}
	var text strings.Builder
	fmt.Fprintf(&text, "InPETU Hub — sessão %02d\nID: %s\nVersões do aplicativo: %s\nVersões do iniciador: %s\nSistema: %s/%s\nInício: %s\nFim: %s\nResultado: %s\nEventos omitidos por limite: %d\n\n", report.Sequence, report.ID, strings.Join(report.AppVersions, ", "), strings.Join(report.LauncherVersions, ", "), report.OS, report.Arch, report.StartedAt, report.EndedAt, report.Outcome, report.Dropped)
	for _, event := range events {
		fields, _ := json.Marshal(event.Fields)
		fmt.Fprintf(&text, "%s [%s] app=%s iniciador=%s %s %s %s\n", event.Time, event.Level, event.AppVersion, event.LauncherVersion, event.Component, event.Event, fields)
	}
	return atomicWrite(filepath.Join(s.folder, "log.txt"), []byte(text.String()), 0600)
}
func (m *Manager) record(level, event string, fields map[string]any) {
	if m.session != nil {
		m.session.record(level, "launcher", event, fields)
	}
}

// O processo Java ja usa JSON seguro; ainda assim, recusar mensagens e campos livres.
type javaSessionSink struct {
	mu      sync.Mutex
	session *Session
	pending []byte
	discard bool
}

func (w *javaSessionSink) Write(data []byte) (int, error) {
	w.mu.Lock()
	defer w.mu.Unlock()
	for _, b := range data {
		if b == '\n' {
			if !w.discard {
				w.line(w.pending)
			}
			w.pending = nil
			w.discard = false
			continue
		}
		if len(w.pending) >= 32<<10 {
			w.pending = nil
			w.discard = true
		}
		if !w.discard {
			w.pending = append(w.pending, b)
		}
	}
	return len(data), nil
}
func (w *javaSessionSink) line(line []byte) {
	var input struct {
		Level         string   `json:"level"`
		Logger        string   `json:"logger"`
		RequestID     string   `json:"requestId"`
		Message       string   `json:"message"`
		ExceptionType string   `json:"exceptionType"`
		Frames        []string `json:"frames"`
	}
	if json.Unmarshal(line, &input) != nil {
		return
	}
	fields := map[string]any{"logger": input.Logger, "requestId": input.RequestID, "errorType": input.ExceptionType, "frames": input.Frames}
	event := "FRAMEWORK_EVENT"
	if input.Logger == "br.com.inpetu.hub.observability.OperationalEvents" {
		for _, part := range strings.Fields(input.Message) {
			pair := strings.SplitN(part, "=", 2)
			if len(pair) != 2 {
				continue
			}
			if pair[0] == "event" && codePattern.MatchString(pair[1]) {
				event = pair[1]
			} else {
				fields[pair[0]] = pair[1]
			}
		}
	}
	w.session.record(input.Level, "app", event, fields)
}
