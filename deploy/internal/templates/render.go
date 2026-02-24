package templates

import (
	"bytes"
	"fmt"
	"text/template"
)

// RenderTemplate loads and renders a template from the embedded FS
func RenderTemplate(name string, data interface{}) (string, error) {
	content, err := FS.ReadFile(name)
	if err != nil {
		return "", fmt.Errorf("failed to read template %s: %w", name, err)
	}

	tmpl, err := template.New(name).Parse(string(content))
	if err != nil {
		return "", fmt.Errorf("failed to parse template %s: %w", name, err)
	}

	var buf bytes.Buffer
	if err := tmpl.Execute(&buf, data); err != nil {
		return "", fmt.Errorf("failed to execute template %s: %w", name, err)
	}

	return buf.String(), nil
}

// MustRenderTemplate renders a template and panics on error (useful for development)
func MustRenderTemplate(name string, data interface{}) string {
	result, err := RenderTemplate(name, data)
	if err != nil {
		panic(err)
	}
	return result
}
