package internal

import (
	"archive/tar"
	"compress/gzip"
	"fmt"
	log "github.com/sirupsen/logrus"
	"io"
	"os"
	"path/filepath"
	"strings"
)

func Unpack(source *os.File, target string) error {
	name := source.Name()

	if strings.HasSuffix(name, ".tar.gz") || strings.HasSuffix(name, ".tgz") {
		return unpackTarGz(source, target)
	}

	return fmt.Errorf("unknown file kind %s", name)
}

func unpackTarGz(source *os.File, target string) error {
	// Ensure we're at the start of the file
	source.Seek(0, io.SeekStart)

	log.Tracef("Creating gzip reader for %s", source.Name())
	gzReader, err := gzip.NewReader(source)
	if err != nil {
		log.Tracef("Unable to open gzip stream")
		return err
	}
	defer gzReader.Close()

	log.Tracef("Created gzip reader for %s", source.Name())

	tarReader := tar.NewReader(gzReader)

	log.Tracef("Created tar reader from gzip reader for %v", source)

	for true {
		header, err := tarReader.Next()

		if err == io.EOF {
			break
		} else if err != nil {
			return err
		}

		out := filepath.Join(target, header.Name)

		switch header.Typeflag {
		case tar.TypeDir:
			log.Tracef("Creating directory %s", out)
			err := os.Mkdir(out, 0755)
			if err != nil {
				return err
			}

			break

		case tar.TypeReg:
			log.Tracef("Writing: %s", out)
			out, err := os.Create(out)
			if err != nil {
				return err
			}
			_, err = io.Copy(out, tarReader)
			out.Close() // Ignore the error
			if err != nil {
				return err
			}
			break

		default:
			return fmt.Errorf("unable to unpack file from tarball %v", header)
		}
	}

	if err != nil {
		return err
	}
	return nil
}
