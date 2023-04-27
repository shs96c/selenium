package download

import (
	"fmt"
	"github.com/SeleniumHQ/selenium/golang/internal"
	"github.com/SeleniumHQ/selenium/golang/internal/platforms"
	"github.com/SeleniumHQ/selenium/golang/pkg/drivers"
	"github.com/schollz/progressbar/v3"
	log "github.com/sirupsen/logrus"
	"io"
	"net/http"
	"net/url"
	"os"
	"path"
	"path/filepath"
)

func DownloadDriver(d drivers.Driver, os *platforms.Os, ver string) (*os.File, error) {
	log.Tracef("Determing version from %s", ver)
	version, err := d.GetVersion(os, ver)
	if err != nil {
		return nil, err
	}

	log.Tracef("Have version %s. Finding URL to download from", version.String())
	toDownload, err := d.GetUrl(os, version)
	if err != nil {
		return nil, err
	}

	log.Tracef("%s %s (%s) -> %s", d.GetDriverName(), version.String(), os.String(), toDownload.String())
	archive, err := doDownload(toDownload)
	if err != nil {
		return nil, err
	}

	log.Tracef("Unpacking %s", archive.Name())
	unpackedDir, err := unpack(archive)
	if err != nil {
		return nil, err
	}

	log.Tracef("Unpacked to %s", unpackedDir)
	driverBinary, err := getDriverFromUnpacked(d, unpackedDir)
	if err != nil {
		return nil, err
	}

	log.Tracef("Have located driver binary: %s", driverBinary.Name())
	return driverBinary, nil
}

func doDownload(url *url.URL) (*os.File, error) {
	response, err := http.Get(url.String())
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()

	responseOk := response.StatusCode >= 200 && response.StatusCode < 300
	if !responseOk {
		return nil, fmt.Errorf("unable to download %s (%s)", url.String(), response.StatusCode)
	}

	bar := progressbar.NewOptions64(
		response.ContentLength,
		progressbar.OptionClearOnFinish(),
		progressbar.OptionSetWriter(os.Stderr),
		progressbar.OptionSetDescription("Downloading"),
		progressbar.OptionSetTheme(progressbar.Theme{
			Saucer:        "=",
			SaucerHead:    ">",
			SaucerPadding: " ",
			BarStart:      "[",
			BarEnd:        "]",
		}),
	)

	tempDir, err := os.MkdirTemp("", "selenium-manager-download")
	if err != nil {
		return nil, err
	}

	temp, err := os.Create(filepath.Join(tempDir, path.Base(url.Path)))
	log.Tracef("Created temp file %s", temp.Name())
	if err != nil {
		return nil, err
	}

	_, err = io.Copy(io.MultiWriter(temp, bar), response.Body)
	if err != nil {
		return nil, err
	}

	log.Tracef("Returning temp file %s", temp.Name())
	return temp, nil
}

func unpack(archive *os.File) (string, error) {
	temp, err := os.MkdirTemp("", "selenium-manager-unpack")
	if err != nil {
		return "", err
	}

	err = internal.Unpack(archive, temp)
	if err != nil {
		return "", err
	}

	return temp, nil
}

func getDriverFromUnpacked(d drivers.Driver, dir string) (*os.File, error) {
	binaryLocation := filepath.Join(dir, d.GetDriverPathFromArchive())

	return os.Open(binaryLocation)
}
