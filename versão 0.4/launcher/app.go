package main

import (
 "errors"
 "fmt"
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
func (m *Manager) checkStopped(root string,b Bundle) error {
 if m.stoppedCheck!=nil{return m.stoppedCheck(root,b)}
 command:=exec.Command(filepath.Join(root,filepath.FromSlash(b.Java)),"-jar",filepath.Join(root,b.Jar),"--verificar-parado")
 command.Dir=m.home;quietCommand(command)
 // O comando de conferencia nao inicializa Spring e nao imprime dados privados.
 if e:=command.Run();e!=nil{return errors.New("outra execucao esta usando os dados; pare-a antes de atualizar")}
 return nil
}
func (m *Manager) backup(root string,b Bundle) error {
 if e:=m.checkStopped(root,b);e!=nil{return e}
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
 if e:=m.checkStopped(root,b);e!=nil{return e}
 if e:=m.prepareConfig();e!=nil{return e}
 listener,e:=net.Listen("tcp4","127.0.0.1:0");if e!=nil{return e}
 port:=listener.Addr().(*net.TCPAddr).Port;listener.Close()
 _=os.Remove(filepath.Join(m.home,".secrets","launcher.stop"))
 logPath:=filepath.Join(m.home,"logs","launcher-java.log")
 if fi,e:=os.Stat(logPath);e==nil && fi.Size()>10<<20{
  _=os.Remove(logPath+".1");_ = os.Rename(logPath,logPath+".1")
 }
 log,e:=os.OpenFile(logPath,os.O_CREATE|os.O_APPEND|os.O_WRONLY,0600);if e!=nil{return e}
 command:=exec.Command(filepath.Join(root,filepath.FromSlash(b.Java)),"-jar",filepath.Join(root,b.Jar),
  "--spring.profiles.active=local","--server.address=127.0.0.1",fmt.Sprintf("--server.port=%d",port),
  "--app.launcher.enabled=true","--app.public-demo-accounts=true","--app.seed-demo=false")
 command.Dir=m.home
 // Configuracoes de outro projeto nao devem direcionar o aplicativo para outro banco.
 command.Env=cleanEnvironment(os.Environ())
 command.Stdout=log;command.Stderr=log;quietCommand(command)
 if e=command.Start();e!=nil{log.Close();return e}
 process:=&runningProcess{cmd:processHandle{command.Process},done:make(chan struct{})}
 appURL:=fmt.Sprintf("http://127.0.0.1:%d",port)
 m.mu.Lock();m.process=process;m.status.Running=true;m.status.AppURL=appURL;m.mu.Unlock()
 m.message("Iniciando","Abrindo o sistema...")
 go func(){
  _=command.Wait();_ = log.Close()
  m.mu.Lock()
  if m.process==process {m.process=nil;m.status.Running=false;m.status.AppURL="";if !m.status.Busy{m.status.Phase="Parado";m.status.Message="Sistema encerrado. Os dados foram preservados."}}
  m.mu.Unlock()
  close(process.done)
 }()
 probe:=&http.Client{Timeout:2*time.Second}
 for i:=0;i<150;i++{
  select{case <-process.done:return errors.New("o aplicativo nao iniciou; consulte os logs locais");default:}
  response,e:=probe.Get(appURL+"/login")
  if e==nil{response.Body.Close();if response.StatusCode==200{
   m.message("Em execucao","Sistema pronto. Use Abrir sistema para acessar.")
   if !m.headless{openBrowser(appURL)}
   return nil
  }}
  time.Sleep(time.Second)
 }
 _=m.stop()
 return errors.New("tempo de inicializacao excedido; consulte os logs locais")
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
 quietCommand(command);_ = command.Start()
}
