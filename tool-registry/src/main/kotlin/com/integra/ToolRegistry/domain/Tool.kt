package com.integra.ToolRegistry.domain

data class Tool(
    val id: String? = null,
    val name: String,
    val description: String,
    val parameters: Parameter,
)

data class ToolCreateDTO(
    val name: String,
    val description: String,
    val parameters: Parameter,
)

fun ToolCreateDTO.toEntity(): Tool {
    return Tool(
        name = this.name,
        description = this.description,
        parameters = this.parameters
    )
}