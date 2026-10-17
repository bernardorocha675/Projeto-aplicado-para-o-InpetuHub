//go:build windows

package main

import (
 "os"
 "os/exec"
 "syscall"
 "unsafe"
)

func acquireLock(path string)(*os.File,error){
 file,e:=os.OpenFile(path,os.O_CREATE|os.O_RDWR,0600);if e!=nil{return nil,e}
 proc:=syscall.NewLazyDLL("kernel32.dll").NewProc("LockFileEx")
 overlapped:=new(syscall.Overlapped)
 ok,_,err:=proc.Call(file.Fd(),3,0,1,0,uintptr(unsafe.Pointer(overlapped)))
 if ok==0{file.Close();return nil,err}
 return file,nil
}
func setQuiet(command *exec.Cmd){command.SysProcAttr=&syscall.SysProcAttr{CreationFlags:0x08000000}}
