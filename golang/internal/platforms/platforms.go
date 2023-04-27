package platforms

import (
	"fmt"
	flag "github.com/spf13/pflag"
	"runtime"
)

type Os struct {
	Name string
	Arch string
}

func (o Os) String() string {
	return fmt.Sprintf("%s/%s", o.Name, o.Arch)
}

func FromFlags(flags *flag.FlagSet) (*Os, error) {
	os, err := flags.GetString("os")
	if err != nil {
		return nil, err
	}

	arch, err := flags.GetString("arch")
	if err != nil {
		return nil, err
	}

	toReturn := &Os{
		Name: os,
		Arch: arch,
	}

	err = validate(toReturn)
	if err != nil {
		return nil, err
	}

	return toReturn, nil
}

func validate(os *Os) error {
	switch os.Name {
	case "":
		switch runtime.GOOS {
		case "darwin":
			os.Name = "mac"

		case "linx":
		case "windows":
			os.Name = runtime.GOOS

		default:
			return fmt.Errorf("OS must be one of linux, mac, or windows")
		}

	case "mac":
	case "linux":
	case "windows":
		break

	default:
		return fmt.Errorf("OS must be one of linux, mac, or windows")
	}

	switch os.Arch {
	case "":
		switch runtime.GOARCH {
		case "arm64":
			os.Arch = "arm64"
			break

		case "386":
			os.Arch = "x86"
			break

		case "amd64":
			os.Arch = "x86_64"
			break

		default:
			return fmt.Errorf("architecture must be one of arm64, x86, or x86_64")
		}

	case "arm64":
	case "x86":
	case "x86_64":
		break

	default:
		return fmt.Errorf("architecture must be one of arm64, x86, or x86_64")
	}

	return nil
}
