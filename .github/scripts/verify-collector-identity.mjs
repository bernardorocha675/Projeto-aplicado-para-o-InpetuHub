import {verifyClaims} from '../../versão 0.4/receiver/server.mjs';
import {AUDIENCE} from '../../versão 0.4/receiver/schema.mjs';

async function main(){
 const url=new URL(process.env.ACTIONS_ID_TOKEN_REQUEST_URL);url.searchParams.set('audience',AUDIENCE);
 const tokenResponse=await fetch(url,{headers:{Authorization:'Bearer '+process.env.ACTIONS_ID_TOKEN_REQUEST_TOKEN},redirect:'error',signal:AbortSignal.timeout(20000)});
 if(!tokenResponse.ok)throw new Error();
 const token=(await tokenResponse.json()).value;
 const keysResponse=await fetch('https://token.actions.githubusercontent.com/.well-known/jwks',{redirect:'error',signal:AbortSignal.timeout(20000)});
 if(!keysResponse.ok)throw new Error();
 const keys=(await keysResponse.json()).keys;
 verifyClaims(token,keys);
 console.log('Identidade temporária do coletor oficial confirmada; nenhum token foi gravado.');
}
main().catch(()=>{console.error('Identidade do coletor não confirmada. A leitura da fila permanece bloqueada.');process.exitCode=1;});
