//go:build windows

package main

import (
 "syscall"
 "unsafe"
)

// O executavel usa o subsistema grafico; sem esta janela, erros anteriores a tela ficariam invisiveis.
func nativeAlert(title,text string){
 caption,e:=syscall.UTF16PtrFromString(title);if e!=nil{return}
 body,e:=syscall.UTF16PtrFromString(text);if e!=nil{return}
 // MB_OK | MB_ICONERROR | MB_SETFOREGROUND
 _,_,_=syscall.NewLazyDLL("user32.dll").NewProc("MessageBoxW").Call(0,uintptr(unsafe.Pointer(body)),uintptr(unsafe.Pointer(caption)),0x10|0x10000)
}
