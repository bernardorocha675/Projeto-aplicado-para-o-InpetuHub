"""Release separada, imutável, prerelease, sem mudar a release desktop mais recente."""
from pathlib import Path
import os,subprocess,hashlib,json,zipfile
root=Path(__file__).resolve().parents[2];os.chdir(root)
version=(root/'android/VERSION').read_text().strip();tag='android-v'+version
sha=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
assert os.environ.get('GITHUB_SHA')==sha and os.environ.get('GITHUB_REF')=='refs/heads/main'
run=lambda args:subprocess.run(args,check=True)
existing=subprocess.run(['gh','release','view',tag,'--repo',os.environ['GITHUB_REPOSITORY']],capture_output=True)
if existing.returncode==0:raise SystemExit('Release já existe; não será substituída.')
out=root/'android/dist';folder=out/'public';folder.mkdir(exist_ok=True)
import shutil
shutil.copy2(out/('InPETU-Hub-Android-'+version+'.apk'),folder)
backup=out/'assinatura-android-backup.p7m'
if backup.exists():shutil.copy2(backup,folder)
shutil.copy2(out/'TESTE-ANDROID.txt',folder)
run(['git','archive','--format=zip','--output',str(folder/('InPETU-Hub-Android-codigo-'+version+'.zip')),sha,'--','android','.github/workflows/android-experimental.yml','docs/ANDROID.md','docs/HANDOVER-ANDROID.md'])
files=sorted(folder.iterdir());checks=folder/'SHA256SUMS.txt';checks.write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in files))
run(['gh','release','create',tag,'--repo',os.environ['GITHUB_REPOSITORY'],'--target',sha,'--title','InPETU Hub Android '+version+' — EXPERIMENTAL','--prerelease','--latest=false','--notes-file','android/RELEASE-NOTES.md',*[str(p) for p in folder.iterdir()]])
