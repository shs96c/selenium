package drivers

import (
	"fmt"
	"github.com/SeleniumHQ/selenium/golang/internal/platforms"
	ver "github.com/hashicorp/go-version"
	log "github.com/sirupsen/logrus"
	"net/http"
	"net/url"
	"strings"
)

type GeckoDriver struct {
}

const driverUrl = "https://github.com/mozilla/geckodriver/releases/"

func (d GeckoDriver) GetDriverName() string {
	return "geckodriver"
}

func (d GeckoDriver) GetVersion(os *platforms.Os, requestedVersion string) (*ver.Version, error) {
	log.Tracef("Attempting to get version for %s and %v", requestedVersion, os)
	if "latest" != requestedVersion {
		log.Tracef("Assuming user-supplied version (%s) is correct", requestedVersion)
		return ver.NewVersion(requestedVersion)
	}

	client := http.DefaultClient

	request, err := http.NewRequest("GET", driverUrl+"latest", nil)
	if err != nil {
		return nil, err
	}

	log.Debugf("Attempting to determine version from %v", request.URL)
	response, err := client.Do(request)
	defer response.Body.Close()
	if err != nil {
		return nil, err
	}

	responseOk := response.StatusCode >= 200 && response.StatusCode < 300
	if !responseOk {
		return nil, fmt.Errorf("Unable to determine version by reading URL. %s", response.StatusCode)
	}

	log.Debugf("Response URL is %v", response.Request.URL)
	path := response.Request.URL.Path

	// The version is in the last segment of the path
	parts := strings.Split(path, "/")
	lastSegment := parts[len(parts)-1]

	log.Debugf("Attempting to determine version from %s", lastSegment)
	parsed, err := ver.NewVersion(lastSegment)
	if err != nil {
		return nil, err
	}

	log.Debugf("Returning version %s", parsed.String())
	return parsed, nil
}

func (d GeckoDriver) GetUrl(os *platforms.Os, version *ver.Version) (*url.URL, error) {
	var fileSuffix string

	if os.Name == "linux" {
		switch os.Arch {
		case "arm64":
			fileSuffix = "linux-aarch64.tar.gz"
			break

		case "x86":
			fileSuffix = "linux32.tar.gz"
			break

		case "x86_64":
			fileSuffix = "linux64.tar.gz"
			break

		default:
			return nil, fmt.Errorf("unrecognised platform: %s/%s", os.Name, os.Arch)
		}
	} else if os.Name == "mac" {
		switch os.Arch {
		case "arm64":
			fileSuffix = "macos-aarch64.tar.gz"
			break

		case "x86_64":
			fileSuffix = "macos.tar.gz"
			break

		default:
			return nil, fmt.Errorf("unrecognised platform: %s/%s", os.Name, os.Arch)
		}
	} else if os.Name == "windows" {
		switch os.Arch {
		case "arm64":
			fileSuffix = "win-aarch64.tar.gz"
			break

		case "x86":
			fileSuffix = "win32.tar.gz"
			break

		case "x86_64":
			fileSuffix = "win64.tar.gz"
			break

		default:
			return nil, fmt.Errorf("unrecognised platform: %s/%s", os.Name, os.Arch)
		}
	} else {
		return nil, fmt.Errorf("unrecognised platform: %s/%s", os.Name, os.Arch)
	}
	log.Tracef("File suffix for downloaded file is %s", fileSuffix)

	rawUrl := fmt.Sprintf(
		"%sdownload/v%s/%s-v%s-%s",
		driverUrl,
		version.String(),
		d.GetDriverName(),
		version.String(),
		fileSuffix,
	)
	log.Tracef("Raw URL to download %s", rawUrl)

	toReturn, err := url.Parse(rawUrl)
	if err != nil {
		return nil, err
	}

	log.Tracef("Successfully parsed URL: %v", toReturn)
	return toReturn, nil
}

func (d GeckoDriver) GetDriverPathFromArchive() string {
	return "geckodriver"
}

func (d GeckoDriver) GetBinaryName(os platforms.Os) string {
	if os.Name == "windows" {
		return "geckodriver.exe"
	}
	return "geckodriver"
}
