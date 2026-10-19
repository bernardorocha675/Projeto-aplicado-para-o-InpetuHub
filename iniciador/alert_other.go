//go:build !windows && !darwin

package main

import "os/exec"

// Usa o primeiro aviso grafico disponivel; sem nenhum, a mensagem fica so na saida de erro.
func nativeAlert(title,text string){
 for _,candidate:=range [][]string{{"zenity","--error","--no-markup","--title",title,"--text",text},{"kdialog","--title",title,"--error",text},{"xmessage","-center",title+": "+text}}{
  if _,e:=exec.LookPath(candidate[0]);e==nil{_ = exec.Command(candidate[0],candidate[1:]...).Run();return}
 }
}
