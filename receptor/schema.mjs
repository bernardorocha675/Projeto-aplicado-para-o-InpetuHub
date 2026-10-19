import {createHash} from 'node:crypto';
export const REPOSITORY='bernardorocha675/Projeto-aplicado-para-o-InpetuHub';
export const REPOSITORY_ID='1419952796';
export const AUDIENCE='api://inpetu-report-collector';
export const WORKFLOW=REPOSITORY+'/.github/workflows/collect-test-reports.yml@refs/heads/main';
export const MAX_BODY=3*1024*1024;
const version=/^0\.(4|5)\.(0|[1-9][0-9]*)$/;
const validVersion=value=>typeof value==='string'&&version.test(value);
const code=/^[A-Z][A-Z0-9_]{0,63}$/;
const symbol=/^[a-zA-Z_$*][a-zA-Z0-9_.$*]{0,159}$/;
const frame=/^[a-zA-Z_$][a-zA-Z0-9_.$<>]*:[0-9]{1,7}$/;
export const validID=value=>typeof value==='string'&&/^[a-f0-9]{32}$/.test(value);
export const validFolder=value=>typeof value==='string'&&/^Testes\/teste[0-9]+-[0-9_-]+-[a-f0-9]{8}$/.test(value);
const validTime=value=>typeof value==='string'&&/^20[0-9]{2}-[0-9TZ:.,+-]+$/.test(value)&&Number.isFinite(Date.parse(value));
const object=value=>value!==null&&typeof value==='object'&&!Array.isArray(value);
function ensure(value){if(!value)throw new Error('REPORT_INVALID');}
export function cleanFields(fields){
 const out={};if(!object(fields))return out;
 for(const key of Object.keys(fields).sort()){
  const value=fields[key];
  if(['status','durationMs','actorId','targetId','exitCode','pid'].includes(key)&&Number.isSafeInteger(value)&&value>=-1&&value<=1e9)out[key]=value;
  if(['action','result','phase'].includes(key)&&typeof value==='string'&&code.test(value))out[key]=value;
  if(key==='method'&&['GET','POST','PUT','PATCH','DELETE','HEAD','OPTIONS','OTHER'].includes(value))out[key]=value;
  if(key==='target'&&typeof value==='string'&&/^[a-z_]{1,40}$/.test(value))out[key]=value;
  if(['logger','errorType'].includes(key)&&typeof value==='string'&&symbol.test(value))out[key]=value;
  if(key==='requestId'&&typeof value==='string'&&/^[a-f0-9-]{36}$/.test(value))out[key]=value;
  if(key==='route'&&typeof value==='string'&&(value==='OTHER'||value==='ASSET'||/^\/[a-zA-Z_/{}/-]{0,120}$/.test(value)))out[key]=value;
  if(key==='frames'&&Array.isArray(value)){const frames=value.filter(s=>typeof s==='string'&&frame.test(s)).slice(0,20);if(frames.length)out[key]=frames;}
 }
 return out;
}
export function normalizeReport(input){
 ensure(object(input));
 const keys=['schema','id','sequence','startedAt','endedAt','outcome','os','arch','launcherVersions','appVersions','dropped','events'];
 ensure(Object.keys(input).every(k=>keys.includes(k))&&input.schema===1&&validID(input.id));
 ensure(Number.isSafeInteger(input.sequence)&&input.sequence>=1&&input.sequence<=999999999);
 ensure(validTime(input.startedAt)&&validTime(input.endedAt)&&Date.parse(input.endedAt)>=Date.parse(input.startedAt));
 ensure(['CONCLUIDO','FALHA','INTERROMPIDO'].includes(input.outcome)&&['windows','darwin','linux'].includes(input.os)&&['amd64','arm64'].includes(input.arch));
 ensure(Array.isArray(input.launcherVersions)&&input.launcherVersions.length>=1&&input.launcherVersions.length<=20&&input.launcherVersions.every(v=>validVersion(v)));
 ensure(Array.isArray(input.appVersions)&&input.appVersions.length<=20&&input.appVersions.every(v=>validVersion(v)||['unknown','dev'].includes(v)));
 ensure(Number.isSafeInteger(input.dropped)&&input.dropped>=0&&input.dropped<=1e9);
 ensure(Array.isArray(input.events)&&input.events.length>=1&&input.events.length<=5000);
 const events=input.events.map(e=>{
  ensure(object(e)&&Object.keys(e).every(k=>['time','level','component','event','appVersion','launcherVersion','sessionId','fields'].includes(k)));
  ensure(validTime(e.time)&&['INFO','WARN','ERROR'].includes(e.level)&&['app','launcher'].includes(e.component)&&typeof e.event==='string'&&code.test(e.event));
  ensure((validVersion(e.appVersion)||['unknown','dev'].includes(e.appVersion))&&validVersion(e.launcherVersion)&&e.sessionId===input.id);
  return {time:e.time,level:e.level,component:e.component,event:e.event,appVersion:e.appVersion,launcherVersion:e.launcherVersion,sessionId:e.sessionId,fields:cleanFields(e.fields)};
 });
 const out={schema:1,id:input.id,sequence:input.sequence,startedAt:input.startedAt,endedAt:input.endedAt,outcome:input.outcome,os:input.os,arch:input.arch,launcherVersions:[...new Set(input.launcherVersions)],appVersions:[...new Set(input.appVersions)],dropped:input.dropped,events};
 ensure(Buffer.byteLength(JSON.stringify(out))<=MAX_BODY);
 return out;
}
export const reportDigest=report=>createHash('sha256').update(JSON.stringify(normalizeReport(report))).digest('hex');
export function renderLog(report){
 report=normalizeReport(report);
 return `InPETU Hub — sessão ${report.sequence}\nID: ${report.id}\nVersões do aplicativo: ${report.appVersions.join(', ')}\nVersões do iniciador: ${report.launcherVersions.join(', ')}\nSistema: ${report.os}/${report.arch}\nInício: ${report.startedAt}\nFim: ${report.endedAt}\nResultado: ${report.outcome}\nEventos omitidos por limite: ${report.dropped}\nOrigem: relatório de execução; identificação do dispositivo não atestada.\n\n`+report.events.map(e=>`${e.time} [${e.level}] app=${e.appVersion} iniciador=${e.launcherVersion} ${e.component} ${e.event} ${JSON.stringify(e.fields)}`).join('\n')+'\n';
}
export function summarize(report){
 report=normalizeReport(report);const {events,...meta}=report;
 return {...meta,eventCount:events.length,errorCount:events.filter(e=>e.level==='ERROR').length,warningCount:events.filter(e=>e.level==='WARN').length,sha256:reportDigest(report)};
}
