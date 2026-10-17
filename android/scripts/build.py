"""Compila APK Java nativo com SDK oficial (aapt2, javac, D8, zipalign, apksigner)."""
from pathlib import Path
import os, subprocess, shutil, zipfile, sys, re
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'dist'; OUT.mkdir(exist_ok=True)
sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/usr/local/lib/android/sdk')))
bt=sdk/'build-tools/35.0.0'; platform=sdk/'platforms/android-35/android.jar'
java=Path(os.environ['JAVA_HOME'])/'bin'
version=(ROOT/'VERSION').read_text().strip()
assert re.fullmatch(r'0\.1\.[0-9]+-experimental\.[1-9][0-9]*',version)
assert 'android:versionName="'+version+'"' in (ROOT/'AndroidManifest.xml').read_text()
assert 'VERSION="'+version+'"' in (ROOT/'src/br/com/inpetu/android/Store.java').read_text()
keystore=Path(os.environ['ANDROID_KEYSTORE'])
def run(*args):subprocess.run([str(a) for a in args],check=True)

def compile_app(test=False):
 name='test' if test else 'app';work=OUT/name
 if work.exists():shutil.rmtree(work)
 for sub in ['classes','dex','gen']: (work/sub).mkdir(parents=True,exist_ok=True)
 unsigned=work/'unsigned.apk'
 args=[bt/'aapt2','link','-o',unsigned,'-I',platform,'--manifest',ROOT/('test/AndroidManifest.xml' if test else 'AndroidManifest.xml'),'--java',work/'gen']
 if not test:
  resources=work/'res.zip';run(bt/'aapt2','compile','--dir',ROOT/'res','-o',resources);args.append(resources)
 run(*args)
 sources=list((ROOT/('test' if test else 'src')).rglob('*.java'))+list((work/'gen').rglob('*.java'))
 cp=str(platform)+(os.pathsep+str(OUT/'app/classes') if test else '')
 run(java/'javac','-encoding','UTF-8','-source','8','-target','8','-bootclasspath',platform,'-classpath',cp,'-d',work/'classes',*sources)
 jar=work/'classes.jar'
 with zipfile.ZipFile(jar,'w') as z:
  for p in sorted((work/'classes').rglob('*.class')):z.write(p,p.relative_to(work/'classes'))
 d8=[java/'java','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--min-api','26','--lib',platform,'--output',work/'dex',jar]
 if test:d8+=['--classpath',OUT/'app/classes.jar']
 run(*d8)
 with zipfile.ZipFile(unsigned,'a') as z:
  for p in (work/'dex').glob('*.dex'):z.write(p,p.name)
 aligned=work/'aligned.apk';run(bt/'zipalign','-f','4',unsigned,aligned)
 target=OUT/('inpetu-android-test.apk' if test else 'InPETU-Hub-Android-'+version+'.apk')
 run(bt/'apksigner','sign','--ks',keystore,'--ks-key-alias','inpetu','--ks-pass','env:ANDROID_KEYSTORE_PASSWORD','--key-pass','env:ANDROID_KEYSTORE_PASSWORD','--out',target,aligned)
 run(bt/'apksigner','verify','--verbose','--print-certs',target)
 run(bt/'zipalign','-c','4',target)
 print('APK:',target)
 return target
compile_app();compile_app(True)
