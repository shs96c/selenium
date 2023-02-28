package cmd

import (
	"fmt"
	"os"
)

func Usage() {
	fmt.Fprintf(os.Stderr, `selenium-manager [flags]"`)
}
