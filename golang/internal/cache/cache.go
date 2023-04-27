package cache

import (
	"fmt"
	"github.com/SeleniumHQ/selenium/golang/internal/platforms"
	"github.com/SeleniumHQ/selenium/golang/pkg/drivers"
	"github.com/hashicorp/go-version"
	log "github.com/sirupsen/logrus"
	"io"
	_os "os"
	"path/filepath"
)

type cache struct {
	dir string
}

func CreateCache() (*cache, error) {
	dir, err := getDir()
	if err != nil {
		return nil, err
	}

	return &cache{
		dir: dir,
	}, nil
}

func (c cache) GetDriver(d drivers.Driver, os *platforms.Os, version *version.Version) *_os.File {
	path := getDriverPath(c.dir, d, os, version)

	log.Tracef("Looking in cache for driver with path: %s", path)

	stat, err := _os.Stat(path)
	if err != nil {
		log.Tracef("%s did not exist", path)
		return nil
	}

	if stat.IsDir() {
		log.Tracef("%s was a directory", path)
		return nil
	}

	file, err := _os.Open(path)
	if err != nil {
		log.Warnf("Unable to open %s", path)
		return nil
	}
	return file
}

func (c cache) AddDriver(binary *_os.File, d drivers.Driver, os *platforms.Os, version *version.Version) (*_os.File, error) {
	path := getDriverPath(c.dir, d, os, version)
	log.Debugf("Adding %s (%s) to cache at %s", d.GetDriverName(), binary.Name(), path)

	log.Tracef("Creating parent directories, if necessary")
	err := _os.MkdirAll(filepath.Dir(path), 0755)
	if err != nil {
		return nil, err
	}

	out, err := _os.Create(path)
	if err != nil {
		return nil, err
	}
	defer out.Close()

	log.Tracef("Copying to %s", path)
	_, err = io.Copy(out, binary)
	if err != nil {
		return nil, err
	}

	log.Tracef("Ensuring %s is executable", path)
	err = _os.Chmod(path, 0755)
	return out, err
}

func getDir() (string, error) {
	cacheDir := _os.Getenv("XDG_CACHE_HOME")

	if "" == cacheDir {
		home, err := _os.UserHomeDir()
		if err != nil {
			return "", err
		}

		cacheDir = filepath.Join(home, ".cache")
		log.Tracef("Setting cache dir to sensible default: %s", cacheDir)
	} else {
		log.Tracef("Found cache dir from `XDG_CACHE_HOME` value: %s", cacheDir)
	}

	return filepath.Join(cacheDir, "selenium"), nil
}

func getDriverPath(dir string, d drivers.Driver, os *platforms.Os, version *version.Version) string {
	return filepath.Join(
		dir,
		d.GetDriverName(),
		fmt.Sprintf("%s-%s", os.Name, os.Arch),
		version.String(),
		d.GetBinaryName(*os),
	)
}
