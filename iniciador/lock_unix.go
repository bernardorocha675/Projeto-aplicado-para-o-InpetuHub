//go:build !windows

package main

import (
	"os"
	"os/exec"
	"syscall"
)

func acquireLock(path string) (*os.File, error) {
	file, e := os.OpenFile(path, os.O_CREATE|os.O_RDWR, 0600)
	if e != nil {
		return nil, e
	}
	if e = syscall.Flock(int(file.Fd()), syscall.LOCK_EX|syscall.LOCK_NB); e != nil {
		file.Close()
		return nil, e
	}
	return file, nil
}
func setQuiet(command *exec.Cmd) {}
