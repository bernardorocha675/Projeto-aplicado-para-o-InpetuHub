document.querySelectorAll('.voltar').forEach(b=>b.addEventListener('click',()=>history.back()));
document.querySelectorAll('.confirm-delete').forEach(f=>f.addEventListener('submit',e=>{if(!confirm('Excluir este registro? Cadastros vinculados a reservas devem ser desativados.'))e.preventDefault();}));
document.querySelectorAll('.sidebar nav a').forEach(a=>{if(location.pathname===new URL(a.href).pathname||(a.pathname!='/admin'&&location.pathname.startsWith(a.pathname)))a.classList.add('current');});
document.querySelectorAll('form').forEach(f=>{if(f.method.toLowerCase()==='post')f.addEventListener('submit',()=>{if(!f.checkValidity())return;setTimeout(()=>f.querySelectorAll('button[type=submit],button:not([type])').forEach(b=>b.disabled=true),0);});});
