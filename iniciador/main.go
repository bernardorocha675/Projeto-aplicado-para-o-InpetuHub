package main

import (
 "context"
 _ "embed"
 "encoding/json"
 "errors"
 "flag"
 "fmt"
 "net"
 "net/http"
 "net/url"
 "os"
 "os/exec"
 "os/signal"
 "path/filepath"
 "runtime"
 "strings"
 "syscall"
 "time"
)

//go:embed ui.html
var page string

type controller struct {
 manager *Manager
 token string
 base string
 server *http.Server
 handoff string
}

func (c *controller) authorized(r *http.Request) bool{
 if r.Host!=strings.TrimPrefix(c.base,"http://"){return false}
 if origin:=r.Header.Get("Origin");origin!="" && origin!=c.base{return false}
 return r.Header.Get("Authorization")=="Bearer "+c.token
}
func (c *controller) handle(w http.ResponseWriter,r *http.Request){
 w.Header().Set("Cache-Control","no-store")
 w.Header().Set("X-Content-Type-Options","nosniff")
 w.Header().Set("Referrer-Policy","no-referrer")
 if r.Host!=strings.TrimPrefix(c.base,"http://"){http.Error(w,"Origem recusada",403);return}
 if r.URL.Path=="/" && r.Method=="GET"{
  nonce:=randomText(18)
  w.Header().Set("Content-Type","text/html; charset=utf-8")
  w.Header().Set("Content-Security-Policy","default-src 'none'; script-src 'nonce-"+nonce+"'; style-src 'nonce-"+nonce+"'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'")
  _,_=w.Write([]byte(strings.ReplaceAll(page,"__NONCE__",nonce)));return
 }
 if !c.authorized(r){http.Error(w,"Acesso recusado",403);return}
 w.Header().Set("Content-Type","application/json")
 if r.URL.Path=="/api/status" && r.Method=="GET"{_ = json.NewEncoder(w).Encode(c.manager.state());return}
 if r.Method!="POST"{http.Error(w,"Metodo recusado",405);return}
 switch r.URL.Path{
 case "/api/install","/api/start","/api/stop","/api/close":
 default:http.NotFound(w,r);return
 }
 select{case c.manager.operation<-struct{}{}:default:http.Error(w,"Aguarde a operacao atual",409);return}
 w.WriteHeader(http.StatusAccepted);_,_=w.Write([]byte("{}"))
 go c.perform(r.URL.Path)
}
func (c *controller) perform(action string){
 m:=c.manager
 m.mu.Lock();m.status.Busy=true;m.mu.Unlock()
 var err error
 operation:=strings.ToUpper(strings.TrimPrefix(action,"/api/"))
 m.record("INFO","ACTION_STARTED",map[string]any{"action":operation})
 switch action{
 case "/api/install":err=m.installLatest()
 case "/api/start":err=m.start()
 case "/api/stop":err=m.stop()
 case "/api/close":err=m.stop()
 }
 m.mu.Lock();m.status.Busy=false;m.mu.Unlock()
 <-m.operation
 if err!=nil{m.record("ERROR","ACTION_FAILED",map[string]any{"action":operation,"errorType":fmt.Sprintf("%T",err),"result":failureCode(err)});m.message("Precisa de atencao",err.Error())}else{m.record("INFO","ACTION_FINISHED",map[string]any{"action":operation})}
 if action=="/api/close"&&err==nil{go c.server.Shutdown(context.Background())}
}
// Codigo estavel do motivo, sem texto livre, para distinguir falhas nos relatorios.
func failureCode(err error) string {
 for _,known:=range []struct{err error;code string}{
  {errDataInUse,"DATA_IN_USE"},{errOrphanRunning,"ORPHAN_STILL_RUNNING"},{errJavaCheck,"JAVA_CHECK_FAILED"},
  {errAppNotStarted,"APP_NOT_STARTED"},{errStartTimeout,"APP_START_TIMEOUT"},{errPrivateConfigMissing,"PRIVATE_CONFIG_MISSING"},
  {errNoPackage,"NO_PACKAGE"},{errNetwork,"NETWORK"},
 }{if errors.Is(err,known.err){return known.code}}
 return "OTHER"
}
func safeLocalURL(address string)bool{
 u,e:=url.Parse(address);return e==nil && u.Scheme=="http" && u.Hostname()=="127.0.0.1" && u.Port()!=""
}
func run() (runErr error){
 var home,resume string;var offline,headless,start bool
 flag.StringVar(&home,"home","","Pasta de dados (padrao: perfil do usuario)")
 flag.BoolVar(&offline,"offline",false,"Usar somente pacote local ja completo")
 flag.BoolVar(&headless,"headless",false,"Nao abrir navegador automaticamente")
 flag.BoolVar(&start,"start",false,"Iniciar apos preparar")
 flag.StringVar(&resume,"resume-session","","Continuidade interna de uma sessao apos atualizar")
 flag.Parse()
 headlessRun=headless
 if flag.NArg()!=0{return errors.New("argumento desconhecido")}
 if home==""{var e error;home,e=defaultHome();if e!=nil{return e}}
 m,e:=newManager(home,offline,headless);if e!=nil{return e}
 lock,e:=acquireLock(filepath.Join(m.home,"launcher.lock"))
 if e!=nil{
  data,err:=os.ReadFile(filepath.Join(m.home,"running.json"));var running struct{URL string}
  if err==nil && json.Unmarshal(data,&running)==nil && safeLocalURL(running.URL){if !headless{openBrowser(running.URL)};return nil}
  return errors.New("outra instancia esta aberta; aguarde e tente novamente")
 }
 defer lock.Close()
 ownsRunning:=true
 defer func(){if ownsRunning{_ = os.Remove(filepath.Join(m.home,"running.json"))}}()
 session,e:=beginSession(m.home,resume);if e!=nil{return e};m.session=session
 handedOff:=false
 defer func(){
  if value:=recover();value!=nil{
   frames:=[]string{};pcs:=make([]uintptr,20);n:=runtime.Callers(3,pcs);iterator:=runtime.CallersFrames(pcs[:n])
   for{frame,more:=iterator.Next();name:=frame.Function;if pos:=strings.LastIndex(name,"/");pos>=0{name=name[pos+1:]};frames=append(frames,fmt.Sprintf("%s:%d",name,frame.Line));if !more{break}}
   session.record("ERROR","launcher","LAUNCHER_PANIC",map[string]any{"errorType":fmt.Sprintf("%T",value),"frames":frames});_ = session.finish("FALHA");m.flushReports();panic(value)
  }
  if !handedOff{outcome:="CONCLUIDO";if runErr!=nil{outcome="FALHA";session.record("ERROR","launcher","LAUNCHER_FAILED",map[string]any{"errorType":fmt.Sprintf("%T",runErr)})};_ = session.finish(outcome);m.flushReports()}
 }()
 reportDone:=make(chan struct{});defer close(reportDone)
 go func(){m.flushReports();ticker:=time.NewTicker(time.Minute);defer ticker.Stop();for{select{case <-reportDone:return;case <-ticker.C:m.flushReports()}}}()
 listener,e:=net.Listen("tcp4","127.0.0.1:0");if e!=nil{return e}
 c:=&controller{manager:m,token:randomText(32),base:"http://"+listener.Addr().String()}
 c.server=&http.Server{Handler:http.HandlerFunc(c.handle),ReadHeaderTimeout:5*time.Second,ReadTimeout:10*time.Second,WriteTimeout:10*time.Second,IdleTimeout:30*time.Second}
 address:=c.base+"/#"+c.token
 if e=atomicJSON(filepath.Join(m.home,"running.json"),struct{URL string}{address});e!=nil{listener.Close();return e}
 m.onHandoff=func(path string)error{
  m.mu.Lock();c.handoff=path;m.mu.Unlock()
  m.message("Atualizado","Abrindo o iniciador da nova versao...")
  go func(){time.Sleep(800*time.Millisecond);_ = c.server.Shutdown(context.Background())}()
  return nil
 }
 if !headless{openBrowser(address)}
 m.operation<-struct{}{}
 go func(){if start{c.perform("/api/start")}else{c.perform("/api/install")}}()
 // Logout, desligamento ou Ctrl+C: parar o Java e finalizar a sessao, sem deixar processo orfao.
 signals:=make(chan os.Signal,1);signal.Notify(signals,os.Interrupt,syscall.SIGTERM);defer signal.Stop(signals)
 go func(){
  select{case <-signals:case <-reportDone:return}
  m.record("WARN","LAUNCHER_SIGNAL",nil)
  select{case m.operation<-struct{}{}:case <-time.After(45*time.Second):}
  _=m.stop();_ = c.server.Shutdown(context.Background())
 }()
 e=c.server.Serve(listener)
 if e!=nil && !errors.Is(e,http.ErrServerClosed){return e}
 _=m.stop()
 m.mu.RLock();handoff:=c.handoff;m.mu.RUnlock()
 if handoff!=""{
  // Libere o lock antes de abrir o iniciador novo, sem sobrescrever o executavel atual.
  _=lock.Close();_ = os.Remove(filepath.Join(m.home,"running.json"));ownsRunning=false
  session.record("INFO","launcher","LAUNCHER_HANDOFF",nil);session.detach()
  args:=[]string{"--home",m.home,"--start","--resume-session",session.report.ID};if headless{args=append(args,"--headless")};if offline{args=append(args,"--offline")}
  command:=exec.Command(handoff,args...);quietCommand(command)
  if e=command.Start();e!=nil{return e}
  handedOff=true
 }
 return nil
}
// Sem terminal (Windows, app do macOS, gerenciador de arquivos), a falha precisa de um aviso visivel.
var headlessRun bool
var showFatal = nativeAlert
func reportFatal(e error){
 fmt.Fprintln(os.Stderr,"InPETU Hub:",e)
 if !headlessRun{showFatal("InPETU Hub","Nao foi possivel abrir o InPETU Hub: "+e.Error())}
}
func main(){if e:=run();e!=nil{reportFatal(e);os.Exit(1)}}
