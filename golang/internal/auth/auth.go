package auth

import (
	"fmt"
	"github.com/bgentry/go-netrc/netrc"
	"net/http"
	"os"
	"path/filepath"
)

func AddCredentials(req *http.Request) error {
	if req.Host == "" {
		return fmt.Errorf("No host on request %v", req)
	}

	home, err := os.UserHomeDir()
	if err != nil {
		return nil
	}

	netrcFile := filepath.Join(home, ".netrc")

	stat, err := os.Stat(netrcFile)
	if err != nil {
		if os.IsNotExist(err) {
			// Fine. We're not going to authenticate
			return nil
		}
		return err
	}

	if stat.IsDir() {
		return nil
	}

	machine, err := netrc.FindMachine(netrcFile, req.Host)
	if err != nil {
		return err
	}
	if machine.IsDefault() {
		return nil
	}

	req.SetBasicAuth(machine.Login, machine.Password)
	return nil
}
