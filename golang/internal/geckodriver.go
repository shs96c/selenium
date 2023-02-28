package internal

import (
	"fmt"
	"io"
	"net/http"
)

const (
	driverUrl     = "https://github.com/mozilla/geckodriver/releases/"
	latestRelease = "latest"
)

func GetDriverUrl() (string, error) {
	url := fmt.Sprintf("%s%s", driverUrl, latestRelease)

	c := http.Client{}

	r, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return "", err
	}

	res, err := c.Do(r)
	defer res.Body.Close()
	if err != nil {
		return "", err
	}

	bytes, err := io.ReadAll(res.Body)
	if err != nil {
		return "", err
	}

	return string(bytes), nil
}
