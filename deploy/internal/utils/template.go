package utils

import (
	"bytes"
	"fmt"
	"text/template"
)

// RenderTemplate renders a template with the given data
func RenderTemplate(tmplStr string, data interface{}) (string, error) {
	tmpl, err := template.New("template").Parse(tmplStr)
	if err != nil {
		return "", fmt.Errorf("failed to parse template: %w", err)
	}

	var buf bytes.Buffer
	if err := tmpl.Execute(&buf, data); err != nil {
		return "", fmt.Errorf("failed to execute template: %w", err)
	}

	return buf.String(), nil
}

// TemplateData holds common template variables
type TemplateData struct {
	User         string
	Group        string
	ProjectDir   string
	RAM          string
	Port         int
	ViewDistance int
	MaxPlayers   int
	JVMArgs      string
	SwapSize     string
}
