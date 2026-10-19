//go:build darwin

package main

import "os/exec"

// O texto segue como argumento do script, nunca interpolado no codigo AppleScript.
func nativeAlert(title, text string) {
	_ = exec.Command("osascript", "-e", "on run argv", "-e", "display alert (item 1 of argv) message (item 2 of argv) as critical", "-e", "end run", title, text).Run()
}
