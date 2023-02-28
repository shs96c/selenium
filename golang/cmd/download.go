package cmd

import (
	"fmt"
	"github.com/SeleniumHQ/selenium/golang/internal/auth"
	"net/http"
)

func Download([]string) error {

	req, err := http.NewRequest("GET", "https://github.com/mozilla/geckodriver/releases/latest", nil)
	if err != nil {
		return err
	}
	err = auth.AddCredentials(req)
	if err != nil {
		return err
	}

	client := http.Client{}
	res, err := client.Do(req)
	if err != nil {
		return err
	}
	defer res.Body.Close()

	location := res.Request.URL

	fmt.Printf("Location is: %s\n", location)

	return nil
}
