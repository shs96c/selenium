package download

import (
	"fmt"
	"github.com/SeleniumHQ/selenium/golang/internal/cache"
	"github.com/SeleniumHQ/selenium/golang/internal/download"
	"github.com/SeleniumHQ/selenium/golang/internal/platforms"
	"github.com/SeleniumHQ/selenium/golang/pkg/drivers"
	log "github.com/sirupsen/logrus"
	"github.com/spf13/cobra"
)

func NewDownloadCommand() *cobra.Command {
	var driverName string
	var driverVersion string

	cmd := &cobra.Command{
		Use:   "download",
		Short: "Download drivers and browsers",
		RunE: func(cmd *cobra.Command, args []string) error {
			driver, err := createDriver(driverName)
			if err != nil {
				return err
			}

			if driver == nil {
				return fmt.Errorf("unable to find driver implementation for %s", driverName)
			}

			os, err := platforms.FromFlags(cmd.Flags())
			if err != nil {
				return err
			}

			log.Tracef("Checking cache for driver %s at version %s", driverName, driverVersion)
			c, err := cache.CreateCache()
			if err != nil {
				return err
			}

			ver, err := driver.GetVersion(os, driverVersion)
			if err != nil {
				return err
			}

			binary := c.GetDriver(driver, os, ver)
			if binary != nil {
				log.Tracef("Found driver (%s) in cache: %s", driver.GetDriverName(), binary.Name())
				fmt.Printf(binary.Name())
				return nil
			}

			log.Infof(
				"Downloading %s at version %s for %s/%s",
				driver.GetDriverName(),
				driverVersion,
				os.Name,
				os.Arch)

			downloaded, err := download.DownloadDriver(driver, os, driverVersion)
			if err != nil {
				return err
			}

			log.Tracef("Downloaded %s", downloaded.Name())
			cachedFile, err := c.AddDriver(downloaded, driver, os, ver)
			if err != nil {
				return err
			}
			fmt.Printf(cachedFile.Name())

			return nil
		},
	}

	cmd.Flags().StringVar(&driverName, "driver", "", "Driver name (chromedriver, geckodriver, msedgedriver, IEDriverServer, or safaridriver)")
	cmd.Flags().StringVar(&driverVersion, "driver-version", "latest", "Driver version (e.g., 106.0.5249.61, 0.31.0, etc.)")

	return cmd
}

func createDriver(driverName string) (drivers.Driver, error) {
	if "geckodriver" == driverName {
		return &drivers.GeckoDriver{}, nil
	}

	return nil, fmt.Errorf("unknown driver %s", driverName)
}
