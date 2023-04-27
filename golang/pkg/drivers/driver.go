package drivers

import (
	"github.com/SeleniumHQ/selenium/golang/internal/platforms"
	"github.com/hashicorp/go-version"
	"net/url"
)

type Driver interface {
	GetDriverName() string

	GetVersion(os *platforms.Os, requestedVersion string) (*version.Version, error)

	GetUrl(os *platforms.Os, version *version.Version) (*url.URL, error)

	GetDriverPathFromArchive() string

	GetBinaryName(os platforms.Os) string
}
