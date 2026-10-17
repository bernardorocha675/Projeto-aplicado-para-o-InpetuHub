const cnpj=document.getElementById('cnpj');if(cnpj)cnpj.addEventListener('input',()=>{cnpj.value=cnpj.value.replace(/[^0-9]/g,'').slice(0,14);});
const data=document.getElementById('data');if(data){const now=new Date();const local=new Date(now.getTime()-now.getTimezoneOffset()*60000).toISOString().slice(0,10);data.min=local;}
