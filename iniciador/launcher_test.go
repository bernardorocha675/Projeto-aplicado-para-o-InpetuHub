package main

import (
 "archive/zip"
 "bytes"
 "context"
 "encoding/json"
 "fmt"
 "errors"
 "io"
 "net/http"
 "net/http/httptest"
 "net/url"
 "os"
 "path/filepath"
 "runtime"
 "strings"
 "testing"
)

func testManager(t *testing.T)*Manager{
 t.Helper();m,e:=newManager(t.TempDir(),false,true);if e!=nil{t.Fatal(e)};return m
}
func fixtureBundle(t *testing.T,root,version string) Bundle{
 t.Helper();java:="runtime/bin/java";launcher:="InPETU-Hub";if runtime.GOOS=="windows"{java+=".exe";launcher+=".exe"}
 files:=map[string]string{"app.jar":"app fixture",java:"java fixture",launcher:"launcher fixture"}
 for name,data:=range files{p:=filepath.Join(root,filepath.FromSlash(name));if e:=os.MkdirAll(filepath.Dir(p),0700);e!=nil{t.Fatal(e)};if e:=os.WriteFile(p,[]byte(data),0755);e!=nil{t.Fatal(e)}}
 jar,_:=digest(filepath.Join(root,"app.jar"));jvm,_:=digest(filepath.Join(root,filepath.FromSlash(java)))
 b:=Bundle{Version:version,GOOS:runtime.GOOS,GOARCH:runtime.GOARCH,Jar:"app.jar",Java:java,Launcher:launcher,JarSHA256:jar,JavaSHA256:jvm}
 if e:=atomicJSON(filepath.Join(root,"bundle.json"),b);e!=nil{t.Fatal(e)};return b
}
func TestVersionFamilyAndNumericOrder(t *testing.T){
 if !newer("0.4.10","0.4.9"){t.Fatal("ordem lexicografica incorreta")}
 for _,v:=range []string{"0.3.99","0.5.0","0.4.1-beta","0.4.01","../../0.4.2"}{if _,e:=patch(v);e==nil{t.Fatal(v)}}
}
func TestLatestSkipsOtherFamiliesDraftsAndPrereleases(t *testing.T){
 m:=testManager(t)
 var server *httptest.Server
 server=httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter,r *http.Request){
  releases:=[]githubRelease{{Tag_name:"v0.5.0"},{Tag_name:"v0.4.50",Draft:true},{Tag_name:"v0.4.99",Prerelease:true},{Tag_name:"v0.4.9"},{Tag_name:"v0.4.10"}}
  for i:=range releases {releases[i].Assets=[]releaseAsset{{Name:archiveName(strings.TrimPrefix(releases[i].Tag_name,"v")),State:"uploaded",Browser_download_url:server.URL+"/package.zip"},{Name:"SHA256SUMS.txt",State:"uploaded",Browser_download_url:server.URL+"/SHA256SUMS.txt"}}}
  _=json.NewEncoder(w).Encode(releases)
 }));defer server.Close();m.api=server.URL;m.client=server.Client()
 latest,e:=m.latest();if e!=nil || latest.Version!="0.4.10" || latest.ZipURL==""{t.Fatalf("release inesperada: %v %v",latest,e)}
}
func TestLatestPaginates(t *testing.T){
 m:=testManager(t);pages:=0
 server:=httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter,r *http.Request){
  pages++
  if r.URL.Query().Get("page")=="1"{list:=make([]githubRelease,100);for i:=range list{list[i]=githubRelease{Tag_name:"v0.5.0"}};_ = json.NewEncoder(w).Encode(list)
  }else{_ = json.NewEncoder(w).Encode([]githubRelease{{Tag_name:"v0.4.12"}})}
 }));defer server.Close();m.api=server.URL;m.client=server.Client()
 result,e:=m.latest();if e!=nil || result.Version!="0.4.12" || pages!=2{t.Fatal(result,e,pages)}
}
func TestOfflineUsesInstalledBundleAndPreservesConfiguration(t *testing.T){
 m:=testManager(t);m.offline=true;root:=filepath.Join(m.home,"installed","0.4.0");fixtureBundle(t,root,"0.4.0")
 if e:=m.installLatest();e!=nil{t.Fatal(e)}
 key,e:=os.ReadFile(filepath.Join(m.home,".secrets","mfa.key"));if e!=nil{t.Fatal(e)}
 env,e:=os.ReadFile(filepath.Join(m.home,".env.local"));if e!=nil{t.Fatal(e)}
 if e=m.installLatest();e!=nil{t.Fatal(e)}
 again,_:=os.ReadFile(filepath.Join(m.home,".secrets","mfa.key"));againEnv,_:=os.ReadFile(filepath.Join(m.home,".env.local"))
 if !bytes.Equal(key,again)||!bytes.Equal(env,againEnv){t.Fatal("configuracao privada foi substituida")}
 if !m.state().Ready{t.Fatal("nao pronto")}
}
func TestBundleCorruptionIsRejected(t *testing.T){
 root:=t.TempDir();fixtureBundle(t,root,"0.4.0");if _,e:=readBundle(root);e!=nil{t.Fatal(e)}
 _=os.WriteFile(filepath.Join(root,"app.jar"),[]byte("changed"),0600)
 if _,e:=readBundle(root);e==nil{t.Fatal("arquivo adulterado aceito")}
}
func maliciousZIP(t *testing.T,name string)string{
 t.Helper();path:=filepath.Join(t.TempDir(),"package.zip");f,e:=os.Create(path);if e!=nil{t.Fatal(e)};w:=zip.NewWriter(f)
 entry,e:=w.Create(name);if e!=nil{t.Fatal(e)};_,_=entry.Write([]byte("fixture"));_ = w.Close();_ = f.Close();return path
}
func TestZipTraversalAndPrivatePathsAreRejected(t *testing.T){
 for _,name:=range []string{"../escape.txt","/absolute.txt","C:/escape.txt","folder/../escape.txt",".secrets/key","data/db","a\\b","x/.env.local"}{
  if e:=extractArchive(maliciousZIP(t,name),filepath.Join(t.TempDir(),"out"));e==nil{t.Fatal(name)}
 }
}
func TestZipDuplicateIsRejected(t *testing.T){
 path:=filepath.Join(t.TempDir(),"duplicate.zip");out,_:=os.Create(path);w:=zip.NewWriter(out)
 for i:=0;i<2;i++{entry,_:=w.Create("same.txt");_,_=entry.Write([]byte("fixture"))};_=w.Close();_=out.Close()
 if e:=extractArchive(path,filepath.Join(t.TempDir(),"out"));e==nil{t.Fatal("duplicata aceita")}
}
func TestChecksumFailureDoesNotInstallAnything(t *testing.T){
 m:=testManager(t)
 server:=httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter,r *http.Request){
  if r.URL.Path=="/sums"{fmt.Fprintf(w,"%s  %s\n",strings.Repeat("0",64),archiveName("0.4.1"))}else{_,_=w.Write([]byte("wrong archive"))}
 }));defer server.Close();m.api=server.URL;m.client=server.Client()
 if _,e:=m.downloadRelease(&release{Version:"0.4.1",ZipURL:server.URL+"/zip",SumURL:server.URL+"/sums"},t.TempDir());e==nil{t.Fatal("checksum aceito")}
 entries,_:=os.ReadDir(filepath.Join(m.home,"installed"));if len(entries)!=0{t.Fatal("instalacao alterada")}
}
func TestDownloadOriginsRequireOfficialHTTPS(t *testing.T){
 for _,address:=range []string{"http://github.com/x","https://example.com/x","https://github.com/other/repo/releases/download/v0.4.0/x"}{u,_:=url.Parse(address);if e:=allowedNetwork(u,true);e==nil{t.Fatal(address)}}
}
func TestControllerRejectsMissingTokenWrongOriginAndWrongHost(t *testing.T){
 c:=&controller{manager:testManager(t),token:"synthetic-controller-token",base:"http://127.0.0.1:12345"}
 for _,test:=range []struct{token,origin,host string;want int}{{"","","127.0.0.1:12345",403},{"synthetic-controller-token","http://example.com","127.0.0.1:12345",403},{"synthetic-controller-token","","attacker.example",403},{"synthetic-controller-token","http://127.0.0.1:12345","127.0.0.1:12345",200}}{
  request:=httptest.NewRequest("GET",c.base+"/api/status",nil);request.Host=test.host;request.Header.Set("Authorization","Bearer "+test.token);request.Header.Set("Origin",test.origin)
  response:=httptest.NewRecorder();c.handle(response,request);if response.Code!=test.want{t.Fatalf("status %d want %d",response.Code,test.want)}
 }
}
func TestLauncherLockPreventsTwoControllers(t *testing.T){
 path:=filepath.Join(t.TempDir(),"lock")
 first,e:=acquireLock(path);if e!=nil{t.Fatal(e)};defer first.Close()
 second,e:=acquireLock(path);if e==nil{second.Close();t.Fatal("segunda instancia aceita")}
}
func TestEnvironmentOfAnotherProjectIsNotInherited(t *testing.T){
 result:=cleanEnvironment([]string{"SPRING_DATASOURCE_URL=wrong","JAVA_TOOL_OPTIONS=wrong","LOCAL_DB_PASSWORD=wrong","PATH=keep"})
 if len(result)!=1||result[0]!="PATH=keep"{t.Fatal(result)}
}
func TestAPIUnavailableRetainsInstalledBundle(t *testing.T){
 m:=testManager(t);root:=filepath.Join(m.home,"installed","0.4.0");fixtureBundle(t,root,"0.4.0")
 server:=httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter,r *http.Request){http.Error(w,"offline",503)}));defer server.Close();m.api=server.URL;m.client=server.Client()
 if e:=m.installLatest();e!=nil || !m.state().Ready{t.Fatal(e)}
}
func TestUnconfiguredOfflineHasClearFailure(t *testing.T){
 m:=testManager(t);m.offline=true
 if e:=m.installLatest();e==nil{t.Fatal("primeira instalacao deveria exigir pacote")}
}
func TestGetRejectsCrossOriginTestInjection(t *testing.T){
 m:=testManager(t);m.api="http://127.0.0.1:12345"
 if _,e:=m.get(context.Background(),"http://example.com/file",true);e==nil{t.Fatal("origem externa aceita")}
}

func releaseServer(t *testing.T,m *Manager,version string,downloadFails bool)*httptest.Server{
 t.Helper()
 content:=t.TempDir();fixtureBundle(t,content,version)
 var buf bytes.Buffer;z:=zip.NewWriter(&buf)
 err:=filepath.Walk(content,func(path string,info os.FileInfo,e error)error{
  if e!=nil{return e};if info.IsDir(){return nil}
  name,_:=filepath.Rel(content,path);name=filepath.ToSlash(name)
  if runtime.GOOS=="darwin"{name="InPETU Hub.app/Contents/Resources/"+name}
  header,e:=zip.FileInfoHeader(info);if e!=nil{return e};header.Name=name
  entry,e:=z.CreateHeader(header);if e!=nil{return e};data,e:=os.ReadFile(path);if e!=nil{return e};_,e=entry.Write(data);return e
 });if err!=nil{t.Fatal(err)};_=z.Close()
 archive:=filepath.Join(t.TempDir(),"fixture.zip");_=os.WriteFile(archive,buf.Bytes(),0600);sha,_:=digest(archive)
 var server *httptest.Server
 server=httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter,r *http.Request){
  switch r.URL.Path{
  case "/releases":
   _=json.NewEncoder(w).Encode([]githubRelease{{Tag_name:"v"+version,Assets:[]releaseAsset{{Name:archiveName(version),State:"uploaded",Browser_download_url:server.URL+"/zip"},{Name:"SHA256SUMS.txt",State:"uploaded",Browser_download_url:server.URL+"/sums"}}}})
  case "/sums":fmt.Fprintf(w,"%s  %s\n",sha,archiveName(version))
  case "/zip":if downloadFails{http.Error(w,"offline",503)}else{_,_=w.Write(buf.Bytes())}
  default:http.NotFound(w,r)
  }
 }));m.api=server.URL+"/releases";m.client=server.Client();return server
}
func TestVerifiedUpdateBacksUpAndPreservesDataAndSecrets(t *testing.T){
 m:=testManager(t)
 old:=filepath.Join(m.home,"installed","0.4.0");b:=fixtureBundle(t,old,"0.4.0")
 if e:=m.ready(old,b);e!=nil{t.Fatal(e)}
 _=os.WriteFile(filepath.Join(m.home,"data","record.txt"),[]byte("private business fixture"),0600)
 key,_:=os.ReadFile(filepath.Join(m.home,".secrets","mfa.key"))
 env,_:=os.ReadFile(filepath.Join(m.home,".env.local"))
 m.stoppedCheck=func(string,Bundle)error{return nil}
 server:=releaseServer(t,m,"0.4.1",false);defer server.Close()
 if e:=m.installLatest();e!=nil{t.Fatal(e)}
 _,installed,e:=m.current();if e!=nil || installed.Version!="0.4.1"{t.Fatal(installed,e)}
 again,_:=os.ReadFile(filepath.Join(m.home,".secrets","mfa.key"));againEnv,_:=os.ReadFile(filepath.Join(m.home,".env.local"))
 if !bytes.Equal(key,again)||!bytes.Equal(env,againEnv){t.Fatal("segredos substituidos")}
 data,_:=os.ReadFile(filepath.Join(m.home,"data","record.txt"));if string(data)!="private business fixture"{t.Fatal("dados substituidos")}
 backups,_:=os.ReadDir(filepath.Join(m.home,"backups"));if len(backups)!=1{t.Fatal("backup ausente")}
 saved,_:=os.ReadFile(filepath.Join(m.home,"backups",backups[0].Name(),"data","record.txt"))
 if !bytes.Equal(data,saved){t.Fatal("backup divergente")}
 if _,e=readBundle(old);e!=nil{t.Fatal("versao anterior removida")}
}
func TestInterruptedDownloadKeepsInstalledVersion(t *testing.T){
 m:=testManager(t);old:=filepath.Join(m.home,"installed","0.4.0");b:=fixtureBundle(t,old,"0.4.0");if e:=m.ready(old,b);e!=nil{t.Fatal(e)}
 server:=releaseServer(t,m,"0.4.1",true);defer server.Close()
 if e:=m.installLatest();e!=nil{t.Fatal(e)}
 if m.state().Version!="0.4.0"||!m.state().Ready{t.Fatal(m.state())}
}

func TestNewInstalledLauncherIsSelectedBeforeStartingTheBackend(t *testing.T){
 m:=testManager(t);m.offline=true
 current,e:=patch(buildVersion);if e!=nil{t.Fatal(e)}
 next:=fmt.Sprintf("0.4.%d",current+1)
 root:=filepath.Join(m.home,"installed",next);b:=fixtureBundle(t,root,next);if e:=m.ready(root,b);e!=nil{t.Fatal(e)}
 called:=""
 m.onHandoff=func(path string)error{called=path;return nil}
 if e:=m.start();e!=nil{t.Fatal(e)}
 if called!=filepath.Join(root,b.Launcher)||m.state().Running{t.Fatal("iniciador novo nao selecionado",called)}
}

func TestMissingPrivateConfigurationNeverReplacesExistingInstallation(t *testing.T) {
	for _, missing := range []string{".secrets/mfa.key", ".env.local"} {
		t.Run(missing, func(t *testing.T) {
			m := testManager(t)
			if e := m.prepareConfig(); e != nil {
				t.Fatal(e)
			}
			db := filepath.Join(m.home, "data", "inpetu-v02.mv.db")
			original := []byte("existing database fixture")
			if e := os.WriteFile(db, original, 0600); e != nil {
				t.Fatal(e)
			}
			other := ".env.local"
			if missing == other {
				other = ".secrets/mfa.key"
			}
			preserved, e := os.ReadFile(filepath.Join(m.home, other))
			if e != nil {
				t.Fatal(e)
			}
			if e = os.Remove(filepath.Join(m.home, missing)); e != nil {
				t.Fatal(e)
			}
			if e = m.prepareConfig(); e == nil {
				t.Fatal("missing private configuration was silently regenerated")
			}
			if _, e = os.Lstat(filepath.Join(m.home, missing)); !os.IsNotExist(e) {
				t.Fatal("missing file was recreated", e)
			}
			data, _ := os.ReadFile(db)
			again, _ := os.ReadFile(filepath.Join(m.home, other))
			if !bytes.Equal(data, original) || !bytes.Equal(again, preserved) {
				t.Fatal("existing installation changed")
			}
		})
	}
}

func TestTruncatedDownloadKeepsInstalledVersion(t *testing.T) {
	m := testManager(t)
	old := filepath.Join(m.home, "installed", "0.4.0")
	b := fixtureBundle(t, old, "0.4.0")
	if e := m.ready(old, b); e != nil {
		t.Fatal(e)
	}
	server := releaseServer(t, m, "0.4.1", false)
	defer server.Close()
	m.client = truncatedDownloadClient{Client: server.Client()}
	if e := m.installLatest(); e != nil {
		t.Fatal("usable installed version blocked by interrupted transfer:", e)
	}
	if m.state().Version != "0.4.0" || !m.state().Ready {
		t.Fatal(m.state())
	}
	if _, e := os.Stat(filepath.Join(m.home, "installed", "0.4.1")); !os.IsNotExist(e) {
		t.Fatal("partial update installed", e)
	}
}

type truncatedDownloadClient struct{ *http.Client }

func (c truncatedDownloadClient) Do(r *http.Request) (*http.Response, error) {
	response, e := c.Client.Do(r)
	if e == nil && r.URL.Path == "/zip" {
		response.Body.Close()
		response.Body = io.NopCloser(&interruptedReader{})
	}
	return response, e
}

type interruptedReader struct{}

func (*interruptedReader) Read([]byte) (int, error) { return 0, io.ErrUnexpectedEOF }

func TestDownloadReaderDoesNotClassifyDiskFailureAsNetwork(t *testing.T) {
 diskError:=errors.New("simulated disk full")
 _,e:=io.Copy(failingDownloadWriter{diskError},downloadReader{strings.NewReader("package bytes")})
 if !errors.Is(e,diskError)||errors.Is(e,errNetwork){t.Fatal("disk error must block update",e)}
 _,e=io.ReadAll(downloadReader{&interruptedReader{}})
 if !errors.Is(e,errNetwork){t.Fatal("interrupted network read must allow installed version",e)}
}
type failingDownloadWriter struct { err error }
func (w failingDownloadWriter) Write([]byte)(int,error){return 0,w.err}
