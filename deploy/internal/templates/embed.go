package templates

import (
	"embed"
)

//go:embed *.tmpl
var FS embed.FS

const (
	ServerProperties = "server.properties.tmpl"
	MinecraftService = "minecraft.service.tmpl"
	StartServer      = "start-server.sh.tmpl"
	MCServer         = "mcserver.sh.tmpl"
	MCRedeploy       = "mc-redeploy.sh.tmpl"
)
