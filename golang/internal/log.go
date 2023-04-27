package internal

import (
	log "github.com/sirupsen/logrus"
	"io"
)

func InitLogs(writer io.Writer, level log.Level) {
	log.SetOutput(writer)
	log.SetLevel(level)
}
