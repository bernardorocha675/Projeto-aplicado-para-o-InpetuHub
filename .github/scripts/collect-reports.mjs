import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {execFileSync} from 'node:child_process';
import {pathToFileURL} from 'node:url';
import {REPOSITORY,AUDIENCE,normalizeReport,reportDigest,renderLog,summarize,validID,validFolder} from '../../versão 0.4/receiver/schema.mjs';

export function storeReports(root,input){
 const directory=path.join(root,'Testes');fs.mkdirSync(directory,{recursive:true});if(!fs.lstatSync(directory).isDirectory()||fs.lstatSync(directory).isSymbolicLink())throw new Error('Diretório Testes inválido.');
 const indexPath=path.join(directory,'recebidos.json');
 const index=fs.existsSync(indexPath)?JSON.parse(fs.readFileSync(indexPath,'utf8')):{schema:1,next:1,receipts:{}};
 if(index.schema!==1||!Number.isSafeInteger(index.next)||index.next<1||index.next>999999999||!index.receipts||Array.isArray(index.receipts)||typeof index.receipts!=='object'||Object.keys(index.receipts).length>5000)throw new Error('Índice de relatórios inválido.');
 const files=[],receipts=[];
 for(const raw of input){
  const report=normalizeReport(raw),digest=reportDigest(report);const old=index.receipts[report.id];
  if(old){if(old.digest!==digest||!validFolder(old.folder))throw new Error('ID de relatório conflitante.');receipts.push({id:report.id,...old});continue;}
  if(Object.keys(index.receipts).length>=5000)throw new Error('Arquivar o índice antes de receber mais relatórios.');
  const parts=Object.fromEntries(new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hourCycle:'h23'}).formatToParts(new Date(report.startedAt)).map(p=>[p.type,p.value]));
  const date=`${parts.year}-${parts.month}-${parts.day}_${parts.hour}-${parts.minute}-${parts.second}`;
  const folder=`Testes/teste${String(index.next++).padStart(2,'0')}-${date}-${report.id.slice(0,8)}`;
  if(!validFolder(folder)||fs.existsSync(path.join(root,folder)))throw new Error('Pasta de relatório já existente.');
  fs.mkdirSync(path.join(root,folder));
  for(const [name,content] of [['log.txt',renderLog(report)],['resumo.json',JSON.stringify(summarize(report),null,2)+'\n']]){const target=folder+'/'+name;fs.writeFileSync(path.join(root,target),content,{flag:'wx'});files.push(target);}
  const receipt={folder,digest};index.receipts[report.id]=receipt;receipts.push({id:report.id,...receipt});
 }
 if(files.length){fs.writeFileSync(indexPath+'.tmp',JSON.stringify(index,null,2)+'\n');fs.renameSync(indexPath+'.tmp',indexPath);files.push('Testes/recebidos.json');}
 return {files,receipts};
}
async function jsonRequest(url,options={},limit=10<<20){
 const response=await fetch(url,{...options,redirect:'error',signal:AbortSignal.timeout(90000)});
 if(!response.ok)throw new Error('Serviço indisponível: HTTP '+response.status);
 let total=0;const chunks=[];for await(const chunk of response.body){total+=chunk.length;if(total>limit)throw new Error('Resposta excede o limite.');chunks.push(Buffer.from(chunk));}
 return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}
function ciReport(version,jobs){
 const names=new Set(['ubuntu-24.04','ubuntu-24.04-arm','macos-15','macos-15-intel','windows-2025','windows-11-arm','source','dependencies','codeql']);
 const selected=jobs.filter(j=>names.has(j.name));if(selected.length!==9||selected.some(j=>j.status!=='completed'))throw new Error('Matriz da CI incompleta.');
 const id=createHash('sha256').update('ci:'+process.env.GITHUB_RUN_ID+':'+process.env.GITHUB_RUN_ATTEMPT).digest('hex').slice(0,32);
 const startedAt=selected.map(j=>j.started_at).sort()[0];const endedAt=new Date().toISOString();
 return {schema:1,id,sequence:Number(process.env.GITHUB_RUN_ATTEMPT||1),startedAt,endedAt,outcome:selected.every(j=>j.conclusion==='success')?'CONCLUIDO':'FALHA',os:'linux',arch:'amd64',launcherVersions:[version],appVersions:[version],dropped:0,
  events:selected.map(j=>({time:j.completed_at,level:j.conclusion==='success'?'INFO':'ERROR',component:'launcher',event:'CI_JOB_FINISHED',appVersion:version,launcherVersion:version,sessionId:id,fields:{action:j.name.toUpperCase().replaceAll('-','_'),result:j.conclusion==='success'?'SUCCESS':'FAILED'}}))};
}
function git(root,args,env=process.env){return execFileSync('git',args,{cwd:root,env,encoding:'utf8',stdio:['ignore','pipe','pipe']}).trim();}
export async function main(){
 if(process.env.GITHUB_ACTIONS!=='true'||process.env.GITHUB_REF!=='refs/heads/main'||process.env.GITHUB_REPOSITORY!==REPOSITORY)throw new Error('Publicação permitida somente no workflow oficial de main.');
 const root=git(process.cwd(),['rev-parse','--show-toplevel']);const version=fs.readFileSync(path.join(root,'VERSION'),'utf8').trim();if(!/^0\.4\.(0|[1-9][0-9]*)$/.test(version))throw new Error('Versão inválida.');
 const ci=process.argv.includes('--ci');let reports,endpoint,oidc;
 if(ci){
  const jobs=await jsonRequest(`https://api.github.com/repos/${REPOSITORY}/actions/runs/${Number(process.env.GITHUB_RUN_ID)}/jobs?per_page=100`,{headers:{Authorization:'Bearer '+process.env.GITHUB_TOKEN,'User-Agent':'InPETU-Reports'}});
  reports=[ciReport(version,jobs.jobs)];
 }else{
  const config=JSON.parse(fs.readFileSync(path.join(root,'Testes/configuracao.json'),'utf8'));
  if(!config.enabled){console.log('Receptor ainda não ativado; nenhum relatório local é descartado.');return;}
  const address=new URL(config.endpoint);if(address.protocol!=='https:'||address.username||address.password||address.search||address.hash||address.port||address.pathname!=='/v1/reports')throw new Error('Receptor inválido.');endpoint=address.origin;
  const tokenURL=new URL(process.env.ACTIONS_ID_TOKEN_REQUEST_URL);tokenURL.searchParams.set('audience',AUDIENCE);
  oidc=(await jsonRequest(tokenURL,{headers:{Authorization:'Bearer '+process.env.ACTIONS_ID_TOKEN_REQUEST_TOKEN}},65536)).value;
  reports=(await jsonRequest(endpoint+'/v1/pending',{headers:{Authorization:'Bearer '+oidc}})).reports;
  if(!Array.isArray(reports)||reports.length>3)throw new Error('Fila inválida.');
  if(!reports.length){console.log('Nenhum relatório pendente.');return;}
 }
 const auth={...process.env,GIT_CONFIG_COUNT:'1',GIT_CONFIG_KEY_0:'http.https://github.com/.extraheader',GIT_CONFIG_VALUE_0:'AUTHORIZATION: basic '+Buffer.from('x-access-token:'+process.env.GITHUB_TOKEN).toString('base64')};
 git(root,['pull','--ff-only','origin','main'],auth);
 const result=storeReports(root,reports);
 if(result.files.length){
  git(root,['add','--',...result.files]);git(root,['config','user.name','InPETU Reports']);git(root,['config','user.email','41898282+github-actions[bot]@users.noreply.github.com']);
  git(root,['commit','-m','test: registrar relatórios automáticos de execução']);git(root,['push','origin','HEAD:main'],auth);
 }
 if(!ci)await jsonRequest(endpoint+'/v1/ack',{method:'POST',headers:{Authorization:'Bearer '+oidc,'Content-Type':'application/json'},body:JSON.stringify({receipts:result.receipts})},8192);
 console.log('Relatórios confirmados no GitHub:',result.receipts.length);
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href)main().catch(()=>{console.error('Não foi possível concluir a publicação. Os relatórios ficam pendentes para nova tentativa.');process.exitCode=1;});
