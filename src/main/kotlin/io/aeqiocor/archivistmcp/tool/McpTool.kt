package io.aeqiocor.archivistmcp.tool

import io.modelcontextprotocol.kotlin.sdk.server.Server

interface McpTool {
    fun register(server: Server)
}
