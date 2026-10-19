import http from 'node:http';
import {createPublicKey,verify} from 'node:crypto';
import {pathToFileURL} from 'node:url';
import {REPOSITORY,REPOSITORY_ID,AUDIENCE,WORKFLOW,MAX_BODY,validID,validFolder,normalizeReport,reportDigest} from './schema.mjs';

const ISSUER='https://token.actions.githubusercontent.com';
export function verifyClaims(token,keys,now=Math.floor(Date.now()/1000)){
 if(typeof token!=='string'||token.length>16384)throw new Error('AUTH_DENIED');
 const parts=token.split('.');if(parts.length!==3||parts.some(p=>!/^[A-Za-z0-9_-]+$/.test(p)))throw new Error('AUTH_DENIED');
 const header=JSON.parse(Buffer.from(parts[0],'base64url'));const claims=JSON.parse(Buffer.from(parts[1],'base64url'));
 if(header.alg!=='RS256'||typeof header.kid!=='string')throw new Error('AUTH_DENIED');
 const jwk=keys.find(k=>k.kid===header.kid&&k.kty==='RSA'&&k.alg==='RS256');
 if(!jwk||!verify('RSA-SHA256',Buffer.from(parts[0]+'.'+parts[1]),createPublicKey({key:jwk,format:'jwk'}),Buffer.from(parts[2],'base64url')))throw new Error('AUTH_DENIED');
 if(claims.iss!==ISSUER||claims.aud!==AUDIENCE||claims.repository!==REPOSITORY||String(claims.repository_id)!==REPOSITORY_ID||claims.ref!=='refs/heads/main'||claims.workflow_ref!==WORKFLOW||!['schedule','workflow_dispatch','push'].includes(claims.event_name)||!Number.isSafeInteger(claims.exp)||claims.exp<=now||claims.exp>now+900||!Number.isSafeInteger(claims.iat)||claims.iat>now+60||claims.iat<now-900||(claims.nbf!==undefined&&(!Number.isSafeInteger(claims.nbf)||claims.nbf>now+60)))throw new Error('AUTH_DENIED');
 return claims;
}
async function boundedJSON(response,max){
 if(!response.ok)throw new Error('REMOTE_UNAVAILABLE');
 const reader=response.body.getReader();let bytes=0;const chunks=[];
 try{while(true){const {done,value}=await reader.read();if(done)break;bytes+=value.length;if(bytes>max)throw new Error('REMOTE_TOO_LARGE');chunks.push(Buffer.from(value));}}finally{reader.releaseLock();}
 return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}
export function createReceiver({fetcher=fetch,now=()=>Date.now(),authorize}={}){
 const pending=new Map(),receipts=new Map(),rates=new Map();let day='',receivedToday=0,queuedBytes=0,keys=[],keysAt=0,receiptAt=0,receiptRefresh=null;
 async function authenticate(req){
  if(authorize)return authorize(req);
  const value=req.headers.authorization||'';if(!value.startsWith('Bearer '))throw new Error('AUTH_DENIED');
  if(now()-keysAt>300000||!keys.length){const result=await boundedJSON(await fetcher(ISSUER+'/.well-known/jwks',{signal:AbortSignal.timeout(7000),redirect:'error'}),65536);keys=result.keys;keysAt=now();}
  return verifyClaims(value.slice(7),keys,Math.floor(now()/1000));
 }
 async function refreshReceipts(){
  if(now()-receiptAt<60000)return;if(receiptRefresh)return receiptRefresh;
  receiptRefresh=(async()=>{
   try{const index=await boundedJSON(await fetcher('https://raw.githubusercontent.com/'+REPOSITORY+'/main/Testes/recebidos.json',{signal:AbortSignal.timeout(7000),redirect:'error'}),2<<20);
    if(index.schema===1&&index.receipts&&typeof index.receipts==='object')for(const [id,r] of Object.entries(index.receipts).slice(-5000)){if(validID(id)&&validFolder(r.folder)&&/^[a-f0-9]{64}$/.test(r.digest)){receipts.set(id,r);const old=pending.get(id);if(old&&old.digest===r.digest){queuedBytes-=old.bytes;pending.delete(id);}}}
   }catch{}finally{receiptAt=now();receiptRefresh=null;}
  })();return receiptRefresh;
 }
 function respond(res,status,value){res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(JSON.stringify(value));}
 async function body(req){
  let size=0;const parts=[];for await(const part of req){size+=part.length;if(size>MAX_BODY)throw new Error('BODY_TOO_LARGE');parts.push(part);}return JSON.parse(Buffer.concat(parts).toString('utf8'));
 }
 let active=0;
 const server=http.createServer(async(req,res)=>{
  if(active>=4){respond(res,503,{error:"BUSY"});return;}active++;
  try{
   if(req.method==='GET'&&req.url==='/healthz'){respond(res,200,{status:'ok',version:'0.4.17',pending:pending.size,publisher:'github-actions'});return;}
   if(req.headers.origin){respond(res,403,{error:'ORIGIN_DENIED'});return;}
   if(req.method==='POST'&&req.url==='/v1/reports'){
    if(req.headers['x-inpetu-project']!=='inpetu-hub-v04'||!/^application\/json(?:;|$)/.test(req.headers['content-type']||'')){respond(res,415,{error:'FORMAT_DENIED'});return;}
    const today=new Date(now()).toISOString().slice(0,10);if(today!==day){day=today;receivedToday=0;rates.clear();}
    // Limites globais e por origem: esta entrada recebe apenas diagnósticos sem dados livres.
    const ip=req.socket.remoteAddress||'unknown';if(!rates.has(ip)&&rates.size>=1000){respond(res,429,{error:'RATE_LIMITED'});return;}const rate=rates.get(ip)||{start:now(),count:0};if(now()-rate.start>900000){rate.start=now();rate.count=0;}rate.count++;rates.set(ip,rate);
    if(rate.count>120||rates.size>1000){respond(res,429,{error:'RATE_LIMITED'});return;}
    const report=normalizeReport(await body(req));const digest=reportDigest(report);
    const receipt=receipts.get(report.id);if(receipt){if(receipt.digest!==digest){respond(res,409,{error:'ID_CONFLICT'});return;}respond(res,200,{id:report.id,folder:receipt.folder,url:'https://github.com/'+REPOSITORY+'/tree/main/'+receipt.folder});return;}
    const previous=pending.get(report.id);if(previous&&previous.digest!==digest){respond(res,409,{error:'ID_CONFLICT'});return;}
    if(!previous){
     const bytes=Buffer.byteLength(JSON.stringify(report));if(receivedToday>=50||pending.size>=20||queuedBytes+bytes>(40<<20)){respond(res,429,{error:'QUEUE_FULL'});return;}
     pending.set(report.id,{report,digest,bytes});queuedBytes+=bytes;receivedToday++;
    }
    respond(res,202,{id:report.id,state:'QUEUED'});void refreshReceipts();return;
   }
   if(req.method==='GET'&&req.url==='/v1/pending'){
    await authenticate(req);respond(res,200,{reports:[...pending.values()].slice(0,3).map(r=>r.report)});return;
   }
   if(req.method==='POST'&&req.url==='/v1/ack'){
    await authenticate(req);const data=await body(req);if(!Array.isArray(data.receipts)||data.receipts.length>3)throw new Error('REPORT_INVALID');
    for(const r of data.receipts){if(!validID(r.id)||!validFolder(r.folder)||!/^[a-f0-9]{64}$/.test(r.digest))throw new Error('REPORT_INVALID');const old=pending.get(r.id);if(old&&old.digest!==r.digest)throw new Error('REPORT_INVALID');receipts.set(r.id,{folder:r.folder,digest:r.digest});if(old){queuedBytes-=old.bytes;pending.delete(r.id);}}
    respond(res,200,{accepted:true});return;
   }
   respond(res,404,{error:'NOT_FOUND'});
  }catch(error){const code=error?.message==='AUTH_DENIED'?'AUTH_DENIED':error?.message==='BODY_TOO_LARGE'?'BODY_TOO_LARGE':'REQUEST_REJECTED';respond(res,code==='AUTH_DENIED'?403:code==='BODY_TOO_LARGE'?413:400,{error:code});}finally{active--;}
 });
 server.requestTimeout=30000;server.headersTimeout=10000;server.keepAliveTimeout=5000;
 return server;
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href){const server=createReceiver();server.listen(Number(process.env.PORT||10000),'0.0.0.0',()=>console.log('RECEIVER_READY version=0.4.10'));}
