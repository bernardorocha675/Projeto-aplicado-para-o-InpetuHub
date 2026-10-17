import {test} from 'node:test';
import assert from 'node:assert/strict';
import {generateKeyPairSync,sign} from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {createReceiver,verifyClaims} from './server.mjs';
import {AUDIENCE,REPOSITORY,REPOSITORY_ID,WORKFLOW,normalizeReport,renderLog,reportDigest} from './schema.mjs';
import {storeReports,ciReport} from '../.github/scripts/collect-reports.mjs';

function report(id='a'.repeat(32)){
 return {schema:1,id,sequence:1,startedAt:'2026-10-16T21:00:00Z',endedAt:'2026-10-16T21:01:00Z',outcome:'CONCLUIDO',os:'linux',arch:'amd64',launcherVersions:['0.4.2'],appVersions:['0.4.2'],dropped:0,
  events:[{time:'2026-10-16T21:00:30Z',level:'ERROR',component:'app',event:'HTTP',appVersion:'0.4.2',launcherVersion:'0.4.2',sessionId:id,fields:{route:'/login',method:'POST',status:403,errorType:'java.lang.IllegalStateException',frames:['example.Controller.login:42'],password:'synthetic-private',email:'person@example.com',message:'token=private'}}]};
}
test('normaliza relatórios, preserva versões e remove conteúdo livre',()=>{
 const result=normalizeReport(report());assert.equal(result.events[0].appVersion,'0.4.2');
 const log=renderLog(result);assert.match(log,/app=0.4.2 iniciador=0.4.2/);assert.match(log,/IllegalStateException/);
 for(const secret of ['synthetic-private','person@example.com','token=private'])assert.ok(!log.includes(secret));
 assert.equal(reportDigest(report()),reportDigest(result));
});
test('recusa estrutura livre, travessia de caminhos, versões falsas e limites excedidos',()=>{
 for(const change of [{id:'../outside'},{schema:2},{launcherVersions:['password-secret']},{sequence:-1},{endedAt:'2026-01-01T00:00:00Z'},{password:'private'},{events:Array(5001).fill(report().events[0])}])assert.throws(()=>normalizeReport({...report(),...change}));
 const raw=report();raw.events[0].fields.frames=['/home/user/private.key:1','Type.method:2'];
 assert.deepEqual(normalizeReport(raw).events[0].fields.frames,['Type.method:2']);
});
test('o resumo publicado preserva os nomes dos nove testes da CI, inclusive Linux',()=>{
 const names=['ubuntu-24.04','ubuntu-24.04-arm','macos-15','macos-15-intel','windows-2025','windows-11-arm','source','dependencies','codeql'];
 const jobs=names.map(name=>({name,status:'completed',conclusion:'success',started_at:'2026-10-16T21:00:00Z',completed_at:'2026-10-16T21:01:00Z'}));
 const normalized=normalizeReport(ciReport('0.4.2',jobs));
 assert.equal(new Set(normalized.events.map(e=>e.fields.action)).size,9);
 assert.deepEqual(normalized.events.slice(0,2).map(e=>e.fields.action),['UBUNTU_24_04','UBUNTU_24_04_ARM']);
 assert.throws(()=>ciReport('0.4.2',jobs.slice(1)));
});
test('cria pasta numerada com data, mantém versão e evita duplicação',()=>{
 const root=fs.mkdtempSync(path.join(os.tmpdir(),'inpetu-ledger-'));
 try{
  const first=storeReports(root,[report(),report('b'.repeat(32))]);assert.equal(first.receipts.length,2);
  assert.match(first.receipts[0].folder,/^Testes\/teste01-2026-10-16_18-00-00-/);
  assert.match(first.receipts[1].folder,/^Testes\/teste02-/);
  const duplicate=storeReports(root,[report()]);assert.equal(duplicate.files.length,0);assert.deepEqual(duplicate.receipts[0],first.receipts[0]);
  const summary=JSON.parse(fs.readFileSync(path.join(root,first.receipts[0].folder,'resumo.json')));assert.equal(summary.errorCount,1);assert.deepEqual(summary.appVersions,['0.4.2']);
  const conflict=report();conflict.outcome='FALHA';assert.throws(()=>storeReports(root,[conflict]));
 }finally{fs.rmSync(root,{recursive:true,force:true});}
});

const pair=generateKeyPairSync('rsa',{modulusLength:2048});
const jwk={...pair.publicKey.export({format:'jwk'}),kid:'test-key',alg:'RS256'};
const now=Math.floor(Date.now()/1000);
const claims={iss:'https://token.actions.githubusercontent.com',aud:AUDIENCE,repository:REPOSITORY,repository_id:REPOSITORY_ID,workflow_ref:WORKFLOW,ref:'refs/heads/main',event_name:'schedule',iat:now,nbf:now-1,exp:now+300};
function jwt(body=claims,header={alg:'RS256',kid:'test-key'}){const data=[header,body].map(v=>Buffer.from(JSON.stringify(v)).toString('base64url')).join('.');return data+'.'+sign('RSA-SHA256',Buffer.from(data),pair.privateKey).toString('base64url');}
test('aceita somente identidade assinada do workflow oficial e audiência esperada',()=>{
 assert.equal(verifyClaims(jwt(),[jwk],now).repository,REPOSITORY);
 for(const change of [{aud:'wrong'},{repository_id:'1'},{ref:'refs/heads/fork'},{workflow_ref:'other/repo/workflow'},{event_name:'pull_request'},{exp:now-1},{iat:now+120}])assert.throws(()=>verifyClaims(jwt({...claims,...change}),[jwk],now));
 assert.throws(()=>verifyClaims(jwt(claims,{alg:'none',kid:'test-key'}),[jwk],now));
 const token=jwt().split('.');token[1]=Buffer.from(JSON.stringify({...claims,repository_id:'1'})).toString('base64url');assert.throws(()=>verifyClaims(token.join('.'),[jwk],now));
});
test('recebimento é uma fila; só confirma envio depois da publicação autorizada',async()=>{
 const server=createReceiver({fetcher:async()=>new Response('{}',{status:404}),authorize:req=>{if(req.headers.authorization!=='Bearer test-only')throw new Error('AUTH_DENIED');}});
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const base='http://127.0.0.1:'+server.address().port;
 const submit=body=>fetch(base+'/v1/reports',{method:'POST',headers:{'Content-Type':'application/json','X-Inpetu-Project':'inpetu-hub-v04'},body:JSON.stringify(body)});
 try{
  assert.equal((await submit(report())).status,202);
  assert.equal((await fetch(base+'/v1/pending')).status,403);
  const pending=await (await fetch(base+'/v1/pending',{headers:{Authorization:'Bearer test-only'}})).json();assert.equal(pending.reports.length,1);
  assert.ok(!JSON.stringify(pending).includes('synthetic-private'));
  const receipt={id:report().id,digest:reportDigest(report()),folder:'Testes/teste01-2026-10-16_18-00-00-aaaaaaaa'};
  assert.equal((await fetch(base+'/v1/ack',{method:'POST',headers:{Authorization:'Bearer test-only','Content-Type':'application/json'},body:JSON.stringify({receipts:[receipt]})})).status,200);
  const confirmed=await submit(report());assert.equal(confirmed.status,200);assert.equal((await confirmed.json()).folder,receipt.folder);
  const conflict=report();conflict.outcome='FALHA';assert.equal((await submit(conflict)).status,409);
  assert.equal((await submit({...report(),password:'must-not-be-accepted'})).status,400);
 }finally{server.closeAllConnections();await new Promise(resolve=>server.close(resolve));}
});
test('fila e tamanho têm limites; entrada não dá acesso à leitura ou a caminhos livres',async()=>{
 const server=createReceiver({fetcher:async()=>new Response('{}',{status:404})});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const base='http://127.0.0.1:'+server.address().port;
 try{
  for(let i=0;i<20;i++){const result=await fetch(base+'/v1/reports',{method:'POST',headers:{'Content-Type':'application/json','X-Inpetu-Project':'inpetu-hub-v04'},body:JSON.stringify(report(i.toString(16).padStart(32,'0')))});assert.equal(result.status,202);}
  assert.equal((await fetch(base+'/v1/reports',{method:'POST',headers:{'Content-Type':'application/json','X-Inpetu-Project':'inpetu-hub-v04'},body:JSON.stringify(report('f'.repeat(32)))})).status,429);
  assert.equal((await fetch(base+'/v1/pending')).status,403);
  assert.equal((await fetch(base+'/v1/reports',{method:'POST',headers:{Origin:'https://external.invalid'}})).status,403);
 }finally{server.closeAllConnections();await new Promise(resolve=>server.close(resolve));}
});
