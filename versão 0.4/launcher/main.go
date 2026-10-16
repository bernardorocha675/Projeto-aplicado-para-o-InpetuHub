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
 "path/filepath"
 "strings"
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
 switch action{
 case "/api/install":err=m.installLatest()
 case "/api/start":err=m.start()
 case "/api/stop":err=m.stop()
 case "/api/close":err=m.stop();if err==nil{go c.server.Shutdown(context.Background())}
 }
 m.mu.Lock();m.status.Busy=false;m.mu.Unlock()
 <-m.operation
 if err!=nil{m.message("Precisa de atencao",err.Error())}
}
func safeLocalURL(address string)bool{
 u,e:=url.Parse(address);return e==nil && u.Scheme=="http" && u.Hostname()=="127.0.0.1" && u.Port()!=""
}
func run() error{
 var home string;var offline,headless,start bool
 flag.StringVar(&home,"home","","Pasta de dados (padrao: perfil do usuario)")
 flag.BoolVar(&offline,"offline",false,"Usar somente pacote local ja completo")
 flag.BoolVar(&headless,"headless",false,"Nao abrir navegador automaticamente")
 flag.BoolVar(&start,"start",false,"Iniciar apos preparar")
 flag.Parse()
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
 listener,e:=net.Listen("tcp4","127.0.0.1:0");if e!=nil{return e}
 c:=&controller{manager:m,token:randomText(32),base:"http://"+listener.Addr().String()}
 c.server=&http.Server{Handler:http.HandlerFunc(c.handle),ReadHeaderTimeout:5*time.Second,ReadTimeout:10*time.Second,WriteTimeout:10*time.Second,IdleTimeout:30*time.Second}
 address:=c.base+"/#"+c.token
 if e=atomicJSON(filepath.Join(m.home,"running.json"),struct{URL string}{address});e!=nil{listener.Close();return e}
 ownsRunning:=true
 defer func(){if ownsRunning{_ = os.Remove(filepath.Join(m.home,"running.json"))}}()
 m.onHandoff=func(path string)error{
  m.mu.Lock();c.handoff=path;m.mu.Unlock()
  m.message("Atualizado","Abrindo o iniciador da nova versao...")
  go func(){time.Sleep(800*time.Millisecond);_ = c.server.Shutdown(context.Background())}()
  return nil
 }
 if !headless{openBrowser(address)}
 m.operation<-struct{}{}
 go func(){if start{c.perform("/api/start")}else{c.perform("/api/install")}}()
 e=c.server.Serve(listener)
 if e!=nil && !errors.Is(e,http.ErrServerClosed){return e}
 _=m.stop()
 m.mu.RLock();handoff:=c.handoff;m.mu.RUnlock()
 if handoff!=""{
  // Libere o lock antes de abrir o iniciador novo, sem sobrescrever o executavel atual.
  _=lock.Close();_ = os.Remove(filepath.Join(m.home,"running.json"));ownsRunning=false
  args:=[]string{"--home",m.home,"--start"};if headless{args=append(args,"--headless")};if offline{args=append(args,"--offline")}
  command:=exec.Command(handoff,args...);quietCommand(command)
  if e=command.Start();e!=nil{return e}
 }
 return nil
}
func main(){if e:=run();e!=nil{fmt.Fprintln(os.Stderr,"InPETU Hub:",e);os.Exit(1)}}
