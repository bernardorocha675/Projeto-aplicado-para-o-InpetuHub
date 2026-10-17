"""Primeira assinatura privada + backup CMS para o detentor da chave de recuperação."""
from pathlib import Path
import base64, os, secrets, subprocess, json, zipfile, tempfile
root=Path(__file__).resolve().parents[1];out=root/'dist';out.mkdir(exist_ok=True)
key=out/'signing.p12'
provided=os.environ.get('ANDROID_KEYSTORE_B64','')
password=os.environ.get('ANDROID_KEYSTORE_PASSWORD','')
if provided:
 if not password:raise SystemExit('Configure também ANDROID_KEYSTORE_PASSWORD.')
 key.write_bytes(base64.b64decode(provided,validate=True))
else:
 # Nunca criar outra identidade silenciosamente para uma aplicação já publicada.
 result=subprocess.run(['gh','release','list','--repo',os.environ['GITHUB_REPOSITORY'],'--json','tagName','--limit','100'],check=True,capture_output=True,text=True)
 if any(r['tagName'].startswith('android-v') for r in json.loads(result.stdout)):
  raise SystemExit('Assinatura Android existente: restaure o backup e configure os secrets antes de outra release.')
 password=secrets.token_urlsafe(32)
 print('::add-mask::'+password,flush=True)
 env=dict(os.environ,ANDROID_KEYSTORE_PASSWORD=password)
 subprocess.run(['keytool','-genkeypair','-keystore',str(key),'-storetype','PKCS12','-storepass:env','ANDROID_KEYSTORE_PASSWORD','-keypass:env','ANDROID_KEYSTORE_PASSWORD','-alias','inpetu','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=InPETU Android Experimental'],env=env,check=True)
 with tempfile.TemporaryDirectory() as temp:
  archive=Path(temp)/'signing.zip'
  with zipfile.ZipFile(archive,'w') as z:z.write(key,'signing.p12');z.writestr('password.txt',password)
  subprocess.run(['openssl','cms','-encrypt','-binary','-aes-256-cbc','-in',str(archive),'-outform','DER','-out',str(out/'assinatura-android-backup.p7m'),str(root/'signing-recovery.crt')],check=True)
key.chmod(0o600)
with open(os.environ['GITHUB_ENV'],'a') as f:f.write('ANDROID_KEYSTORE='+str(key.resolve())+'\nANDROID_KEYSTORE_PASSWORD='+password+'\n')
