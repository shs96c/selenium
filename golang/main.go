package main

import (
  "fmt"
  "github.com/SeleniumHQ/selenium/golang/cmd"
  "os"
)

func main() {
  args := os.Args

  if len(args) < 2 {
    fmt.Printf("Oh noes!")
    cmd.Usage()
    os.Exit(1)
  }

  switch args[1] {
  case "download":
    err := cmd.Download(args)
    if err != nil {
      fmt.Fprintf(os.Stderr, "%v\n", err)
      os.Exit(1)
    }
    os.Exit(0)
    break

  case "version":
    cmd.Version()
    os.Exit(0)
    break

  default:
    fmt.Fprintf(os.Stderr, "Unrecognized command: %s", args[0])
    cmd.Usage()
    os.Exit(2)
  }
}
