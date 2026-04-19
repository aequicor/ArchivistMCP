# McpTool Interface Documentation

## Overview

The `McpTool` interface serves as the foundational contract for all tools in the ArchivistMCP system. It defines the standard lifecycle and registration pattern that each tool must implement.

## Location

`src/main/kotlin/io/aeqiocor/archivistmcp/tool/McpTool.kt`

## KDoc

```kotlin
/**
 * Base interface for MCP tools.
 *
 * All tools must implement this interface to be registered with the MCP server.
 * This provides a standardized way for tools to define their capabilities and
 * behavior when invoked through the Model Context Protocol.
 *
 * The MCP (Model Context Protocol) is a protocol that allows models to request
 * tool execution. This interface ensures all tools follow a consistent registration
 * pattern and lifecycle.
 *
 * @see io.modelcontextprotocol.kotlin.sdk.server.Server
 * @see AddDocumentTool
 * @see SemanticSearchTool
 * @see SmartSearchTool
 */
interface McpTool {
    /**
     * Registers this tool with the MCP server.
     *
     * This method is called during server initialization to register the tool
     * and make it available for invocation by clients. Implementations should
     * use [Server.addTool] to register tool functionality, including:
     * - Tool name and description
     * - Input schema (parameter definitions with types and constraints)
     * - Tool handler logic (the actual tool behavior)
     *
     * The registration process makes the tool discoverable and invocable through
     * the MCP protocol. Clients can query available tools and invoke them with
     * appropriate parameters.
     *
     * ## Registration Pattern
     *
     * Typical implementation pattern:
     * ```kotlin
     * override fun register(server: Server) {
     *     server.addTool(
     *         name = "unique_tool_name",
     *         description = "Clear description of what tool does",
     *         inputSchema = ToolSchema(
     *             properties = buildJsonObject {
     *                 putJsonObject("param_name") {
     *                     put("type", "string")
     *                     put("description", "Parameter description")
     *                 }
     *             },
     *             required = listOf("param_name")
     *         ),
     *     ) { request ->
     *         // Tool implementation
     *         // Access parameters via request.arguments
     *         // Return CallToolResult
     *     }
     * }
     * ```
     *
     * ## Parameter Extraction
     *
     * Parameters are accessed through the request object:
     * ```kotlin
     * val paramValue = request.arguments?.get("paramName")?.jsonPrimitive?.contentOrNull
     * ```
     *
     * ## Response Format
     *
     * Tools must return a [CallToolResult] object:
     * ```kotlin
     * CallToolResult(
     *     content = listOf(TextContent(responseJson)),
     *     isError = false  // true for error conditions
     * )
     * ```
     *
     * @param server The MCP server instance to register with
     * @throws Exception Any exceptions during registration will prevent server startup
     *
     * @see Server.addTool
     * @see CallToolResult
     * @see TextContent
     * @see ToolSchema
     */
    fun register(server: Server)
}
```

## Implementation Requirements

When implementing `McpTool`, you must:

1. **Define Tool Metadata**
   - Unique tool name (snake_case)
   - Clear, concise description
   - Input schema with parameter definitions

2. **Validate Inputs**
   - Check for null/empty required parameters
   - Return error responses for invalid input
   - Include descriptive error messages

3. **Handle Execution**
   - Implement business logic
   - Catch and handle exceptions gracefully
   - Return consistent response format

4. **Return Results**
   - Use `CallToolResult` for success and error cases
   - Format responses as JSON strings in `TextContent`
   - Set `isError` flag appropriately

## Implementation Example

```kotlin
class MyCustomTool(private val indexer: Indexer) : McpTool {
    override fun register(server: Server) {
        server.addTool(
            name = "my_custom_tool",
            description = "Does something useful with documents",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("input_param") {
                        put("type", "string")
                        put("description", "Input parameter")
                    }
                },
                required = listOf("input_param"),
            ),
        ) { request ->
            val param = request.arguments?.get("input_param")?.jsonPrimitive?.contentOrNull
            
            if (param == null) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("""{"error": "input_param is required"}""")),
                    isError = true,
                )
            }
            
            // Perform work
            val result = performAction(param)
            
            CallToolResult(
                content = listOf(TextContent("""{"status": "ok", "result": "$result"}""")),
            )
        }
    }
    
    private fun performAction(param: String): String {
        // Implementation
        return "completed"
    }
}
```

## Dependency Injection Pattern

The ArchivistMCP project uses constructor injection for tool dependencies:

```kotlin
class MyTool(
    private val indexer: Indexer,  // Injected dependency
    private val logger: Logger      // Another dependency
) : McpTool {
    override fun register(server: Server) {
        // Implementation
    }
}
```

This pattern allows tools to be unit tested and keeps them loosely coupled.

## Lifecycle

1. **Tool Creation** - Tool instance is created with dependencies
2. **Server Startup** - Server calls `register()` on each tool
3. **Registration** - Tool uses `server.addTool()` to register itself
4. **Client Requests** - Clients invoke registered tools
5. **Tool Execution** - Handler lambda is called with request
6. **Response** - Tool returns `CallToolResult`

## Best Practices

1. **Naming Convention** - Use snake_case for tool names (e.g., `add_document`, `semantic_search`)

2. **Validation First** - Validate all inputs before processing
   ```kotlin
   if (param == null) {
       return@addTool CallToolResult(
           content = listOf(TextContent("""{"error": "param is required"}""")),
           isError = true,
       )
   }
   ```

3. **Consistent Response Format** - Always return JSON responses
   ```kotlin
   // Success
   """{"status": "ok", "data": ...}"""
   
   // Error
   """{"error": "descriptive error message"}"""
   ```

4. **Clear Descriptions** - Tool descriptions should clearly state:
   - What the tool does
   - What inputs it accepts
   - What outputs it produces

5. **Input Schema Definition** - Define schemas with:
   - Type information (string, number, object, etc.)
   - Descriptions for each parameter
   - Required fields list

## Common Error Patterns

### Missing Required Parameter

```kotlin
if (query == null) {
    CallToolResult(
        content = listOf(TextContent("""{"error": "query is required"}""")),
        isError = true,
    )
}
```

### Invalid Parameter Type

```kotlin
val score = request.arguments?.get("score")?.jsonPrimitive?.doubleOrNull
if (score == null || score < 0.0 || score > 1.0) {
    CallToolResult(
        content = listOf(TextContent("""{"error": "score must be between 0 and 1"}""")),
        isError = true,
    )
}
```

### Unexpected Exception

```kotlin
try {
    // Tool logic
} catch (e: Exception) {
    CallToolResult(
        content = listOf(TextContent("""{"error": "Internal error: ${e.message}"}""")),
        isError = true,
    )
}
```

## Testing

To test a tool implementation:

```kotlin
@Test
fun testMyToolRegistration() {
    val indexer = mockk<Indexer>()
    val tool = MyCustomTool(indexer)
    val server = mockk<Server>()
    
    tool.register(server)
    
    verify { server.addTool(any(), any(), any(), any()) }
}
```

## See Also

- [AddDocumentTool Documentation](AddDocumentTool.md)
- [SemanticSearchTool Documentation](SemanticSearchTool.md)
- [SmartSearchTool Documentation](SmartSearchTool.md)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
