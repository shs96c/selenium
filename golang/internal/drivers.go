package internal

import (
	"errors"
)

type Driver string

const (
	Nulldriver  Driver = ""
	Geckodriver Driver = "geckodriver"
)

func (d *Driver) String() string {
	return string(*d)
}

func (d *Driver) Set(v string) error {
	switch v {
	case "geckodriver":
		*d = Driver(v)
		return nil
	default:
		return errors.New(`must be one of "geckodriver"`)
	}
}

// Type is only used in help text
func (d *Driver) Type() string {
	return "Driver"
}
