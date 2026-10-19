package main

import (
 "errors"
 "fmt"
 "io"
 "net"
 "net/http"
 "os"
 "os/exec"
 "path/filepath"
 "runtime"
 "strings"
 "time"
)

type processHandle struct { process *os.Process }
func (h processHandle) Kill()error{return h.process.Kill()}
func quietCommand(cmd *exec.Cmd){setQuiet(cmd)}

// Saida do aplicativo quando outro processo mantem o lock dos dados (InpetuApplication.DATA_IN_USE_EXIT).
const dataInUseExit = 75
var errDataInUse = errors.New("outra execucao esta usando os dados; pare-a antes de continuar")
var errJavaCheck = errors.New("o Java do pacote nao conseguiu conferir os dados; consulte os logs locais")
var orphanStopTimeout = 30*time.Second

func (m *Manager) checkStopped(root string,b Bundle) error {
 if m.stoppedCheck!=nil{return m.stoppedCheck(root,b)}
 command:=exec.Command(filepath.Join(root,filepath.FromSlash(b.Java)),"-jar",filepath.Join(root,b.Jar),"--verificar-parado")
 command.Dir=m.home;command.Env=javaEnvironment(os.Environ(),runtime.GOOS);quietCommand(command)
 // O comando de conferencia nao inicializa Spring e nao imprime dados privados.
 output:=&limitedBuffer{limit:16<<10};command.Stdout=output;command.Stderr=output
 if log,e:=os.OpenFile(filepath.Join(m.home,"logs","launcher-java.log"),os.O_CREATE|os.O_APPEND|os.O_WRONLY,0600);e==nil{defer log.Close();command.Stdout=io.MultiWriter(log,output);command.Stderr=command.Stdout}
 e:=command.Run();if e==nil{return nil}
 var exit *exec.ExitError
 if errors.As(e,&exit) && exit.ExitCode()==dataInUseExit{return errDataInUse}
 // Pacotes ate 0.4.10 encerram com a excecao do lock em vez do codigo dedicado.
 if errors.As(e,&exit) && strings.Contains(output.String(),"IllegalStateException") && strings.Contains(output.String(),"InpetuApplication.main"){return errDataInUse}
 return errJavaCheck
}
// Com o lock do iniciador, um aplicativo ainda ativo so pode ter sobrado de uma execucao
// encerrada sem parar o Java. O pedido local de parada fecha o Spring normalmente.
func (m *Manager) ensureStopped(root string,b Bundle) error {
 e:=m.checkStopped(root,b)
 if !errors.Is(e,errDataInUse){return e}
 m.mu.RLock();own:=m.process!=nil;m.mu.RUnlock()
 if own{return e}
 m.record("WARN","ORPHAN_APP_FOUND",nil)
 m.message("Recuperando","Encerrando o sistema deixado aberto pela execucao anterior...")
 request:=filepath.Join(m.home,".secrets","launcher.stop")
 if e:=atomicWrite(request,[]byte("stop\n"),0600);e!=nil{return e}
 defer os.Remove(request)
 deadline:=time.Now().Add(orphanStopTimeout)
 for time.Now().Before(deadline){
  time.Sleep(time.Second)
  if e=m.checkStopped(root,b);!errors.Is(e,errDataInUse){
   if e==nil{m.record("INFO","ORPHAN_APP_STOPPED",nil)}
   return e
  }
 }
 m.record("WARN","ORPHAN_APP_STOP_FAILED",nil)
 return errOrphanRunning
}
var errOrphanRunning = errors.New("o sistema da execucao anterior continua usando os dados; feche-o ou reinicie o computador e tente novamente")
type limitedBuffer struct { limit int; data []byte }
func (b *limitedBuffer) Write(p []byte)(int,error){if room:=b.limit-len(b.data);room>0{if len(p)<room{room=len(p)};b.data=append(b.data,p[:room]...)};return len(p),nil}
func (b *limitedBuffer) String() string {return string(b.data)}
func (m *Manager) backup(root string,b Bundle) error {
 if e:=m.ensureStopped(root,b);e!=nil{return e}
 folder:=filepath.Join(m.home,"backups",time.Now().UTC().Format("20060102-150405")+"-"+randomText(6))
 if e:=privateDir(folder);e!=nil{return e}
 for _,name:=range []string{"data",".secrets",".env.local",".env"}{
  source:=filepath.Join(m.home,name)
  if _,e:=os.Lstat(source);os.IsNotExist(e){continue}
  if e:=copyTree(source,filepath.Join(folder,name),true);e!=nil{return e}
 }
 return atomicJSON(filepath.Join(folder,"version.json"),installState{b.Version})
}
func (m *Manager) start() error {
 if m.state().Running{return nil}
 if e:=m.installLatest();e!=nil{return e}
 root,b,e:=m.current();if e!=nil{return e}
 // Cada versao instalada traz seu iniciador. Nao substituir EXE em execucao no Windows.
 if newer(b.Version,buildVersion) && m.onHandoff!=nil{
  launcher:=filepath.Join(root,filepath.FromSlash(b.Launcher))
  return m.onHandoff(launcher)
 }
 return m.startBundle(root,b)
}
func (m *Manager) startBundle(root string,b Bundle) error {
 if e:=m.ensureStopped(root,b);e!=nil{return e}
 if e:=m.prepareConfig();e!=nil{return e}
 listener,e:=net.Listen("tcp4","127.0.0.1:0");if e!=nil{return e}
 port:=listener.Addr().(*net.TCPAddr).Port;listener.Close()
 _=os.Remove(filepath.Join(m.home,".secrets","launcher.stop"))
 logPath:=filepath.Join(m.home,"logs","launcher-java.log")
 if fi,e:=os.Stat(logPath);e==nil && fi.Size()>10<<20{
  _=os.Remove(logPath+".1");_ = os.Rename(logPath,logPath+".1")
 }
 log,e:=os.OpenFile(logPath,os.O_CREATE|os.O_APPEND|os.O_WRONLY,0600);if e!=nil{return e}
 args:=[]string{}
 if m.session!=nil{
  m.session.setAppVersion(b.Version)
  args=append(args,"-Dinpetu.app.version="+b.Version,"-Dinpetu.launcher.version="+buildVersion,"-Dinpetu.session.id="+m.session.report.ID)
 }
 args=append(args,"-jar",filepath.Join(root,b.Jar),
  "--spring.profiles.active=local","--server.address=127.0.0.1",fmt.Sprintf("--server.port=%d",port),
  "--app.launcher.enabled=true","--app.public-demo-accounts=true","--app.seed-demo=false")
 command:=exec.Command(filepath.Join(root,filepath.FromSlash(b.Java)),args...)
 command.Dir=m.home
 // Configuracoes de outro projeto nao devem direcionar o aplicativo para outro banco.
 command.Env=javaEnvironment(os.Environ(),runtime.GOOS)
 command.Stdout=log;command.Stderr=log;quietCommand(command)
 if m.session!=nil{sink:=&javaSessionSink{session:m.session};command.Stdout=io.MultiWriter(log,sink);command.Stderr=command.Stdout}
 if e=command.Start();e!=nil{log.Close();return e}
 m.record("INFO","APP_PROCESS_STARTED",nil)
 process:=&runningProcess{cmd:processHandle{command.Process},done:make(chan struct{})}
 appURL:=fmt.Sprintf("http://127.0.0.1:%d",port)
 m.mu.Lock();m.process=process;m.status.Running=true;m.status.AppURL=appURL;m.mu.Unlock()
 m.message("Iniciando","Abrindo o sistema...")
 go func(){
  result:=command.Wait();_ = log.Close()
  if result!=nil{m.record("ERROR","APP_PROCESS_FAILED",map[string]any{"errorType":fmt.Sprintf("%T",result),"exitCode":command.ProcessState.ExitCode()})}else{m.record("INFO","APP_PROCESS_STOPPED",map[string]any{"exitCode":0})}
  m.mu.Lock()
  if m.process==process {m.process=nil;m.status.Running=false;m.status.AppURL="";if !m.status.Busy{m.status.Phase="Parado";m.status.Message="Sistema encerrado. Os dados foram preservados."}}
  m.mu.Unlock()
  close(process.done)
 }()
 probe:=&http.Client{Timeout:2*time.Second}
 for i:=0;i<150;i++{
  select{case <-process.done:return errAppNotStarted;default:}
  response,e:=probe.Get(appURL+"/login")
  if e==nil{response.Body.Close();if response.StatusCode==200{
   m.record("INFO","APP_READY",nil)
   m.message("Em execucao","Sistema pronto. Use Abrir sistema para acessar.")
   if !m.headless{openBrowser(appURL)}
   return nil
  }}
  time.Sleep(time.Second)
 }
 _=m.stop()
 return errStartTimeout
}
var errAppNotStarted = errors.New("o aplicativo nao iniciou; consulte os logs locais")
var errStartTimeout = errors.New("tempo de inicializacao excedido; consulte os logs locais")
// No Linux, o Java privado so le caminhos com acentos quando LC_CTYPE usa UTF-8.
// Sessoes sem locale (POSIX) recebem C.UTF-8; locales UTF-8 existentes sao mantidos.
func javaEnvironment(input []string,goos string) []string {
 result:=cleanEnvironment(input)
 if goos!="linux"{return result}
 values:=map[string]string{}
 for _,entry:=range result{pair:=strings.SplitN(entry,"=",2);if len(pair)==2{values[pair[0]]=pair[1]}}
 ctype:=values["LC_ALL"];if ctype==""{ctype=values["LC_CTYPE"]};if ctype==""{ctype=values["LANG"]}
 lower:=strings.ToLower(ctype)
 if strings.Contains(lower,"utf-8") || strings.Contains(lower,"utf8"){return result}
 adjusted:=[]string{}
 for _,entry:=range result{key:=strings.SplitN(entry,"=",2)[0];if key=="LC_ALL" || key=="LC_CTYPE"{continue};adjusted=append(adjusted,entry)}
 return append(adjusted,"LC_CTYPE=C.UTF-8")
}
func cleanEnvironment(input []string) []string {
 result:=[]string{}
 for _,entry:=range input{
  key:=strings.ToUpper(strings.SplitN(entry,"=",2)[0])
  if strings.HasPrefix(key,"SPRING_") || strings.HasPrefix(key,"DB_") || strings.HasPrefix(key,"APP_") || strings.HasPrefix(key,"MAIL_") || strings.HasPrefix(key,"TLS_") || strings.HasPrefix(key,"LOGGING_") || strings.HasPrefix(key,"LOGBACK_"){continue}
  switch key {case "JAVA_TOOL_OPTIONS","JDK_JAVA_OPTIONS","_JAVA_OPTIONS","LOCAL_DB_PASSWORD","MFA_KEY_FILE","ACCOUNTS_FILE","PUBLIC_DEMO_ACCOUNTS","SEED_DEMO","PORT","SERVER_ADDRESS":continue}
  result=append(result,entry)
 }
 return result
}
func (m *Manager) stop() error {
 m.mu.RLock();process:=m.process;m.mu.RUnlock()
 if process==nil{return nil}
 m.message("Encerrando","Salvando e encerrando o sistema...")
 if e:=atomicWrite(filepath.Join(m.home,".secrets","launcher.stop"),[]byte("stop\n"),0600);e!=nil{return e}
 select{
 case <-process.done:
 case <-time.After(20*time.Second):
  m.record("WARN","APP_FORCE_STOP",nil)
  _=process.cmd.Kill()
  select{case <-process.done:case <-time.After(5*time.Second):return errors.New("nao foi possivel encerrar o processo")}
 }
 m.message("Parado","Sistema encerrado. Os dados foram preservados.")
 return nil
}
func openBrowser(address string) {
 var command *exec.Cmd
 switch runtime.GOOS{
 case "windows":command=exec.Command("rundll32","url.dll,FileProtocolHandler",address)
 case "darwin":command=exec.Command("open",address)
 default:command=exec.Command("xdg-open",address)
 }
 quietCommand(command)
 // Aguardar o auxiliar evita processos encerrados acumulados enquanto o iniciador fica aberto.
 if command.Start()==nil{go func(){_ = command.Wait()}()}
}
