package main

import (
	"github.com/SeleniumHQ/selenium/golang/cmd/selenium-manager/app/cmd/download"
	"github.com/SeleniumHQ/selenium/golang/cmd/selenium-manager/app/cmd/version"
	"github.com/SeleniumHQ/selenium/golang/internal"
	log "github.com/sirupsen/logrus"
	"github.com/spf13/cobra"
	"os"
)

func main() {
	var rootCmd = &cobra.Command{
		Use:          "selenium-manager",
		Short:        "Automated driver management for Selenium",
		Version:      "0.0.1",
		SilenceUsage: true,
		RunE: func(cmd *cobra.Command, args []string) error {
			return cmd.Usage()
		},
		PersistentPreRunE: preflight,
	}

	rootCmd.PersistentFlags().String("os", "", "Operating system to download browser or driver for")
	rootCmd.PersistentFlags().String("arch", "", "Architecture to download browser or driver for")
	rootCmd.PersistentFlags().Bool("debug", false, "Display `debug` messages")
	rootCmd.PersistentFlags().Bool("trace", false, "Display `trace` messages")

	rootCmd.AddCommand(download.NewDownloadCommand())
	rootCmd.AddCommand(version.NewVersionCommand())

	err := rootCmd.Execute()
	if err != nil {
		os.Exit(1)
	}
	os.Exit(0)
}

func preflight(cmd *cobra.Command, args []string) error {
	flags := cmd.Flags()
	debug, err := flags.GetBool("debug")
	if err != nil {
		return err
	}
	trace, err := flags.GetBool("trace")
	if err != nil {
		return err
	}

	var level = log.InfoLevel
	if debug {
		level = log.DebugLevel
	} else if trace {
		level = log.TraceLevel
	}

	internal.InitLogs(os.Stderr, level)

	return nil
}
