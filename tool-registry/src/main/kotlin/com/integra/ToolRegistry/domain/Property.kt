package com.integra.ToolRegistry.domain

data class Property(
    val id: String? = null,
    val type: String,
    val expression: Expression
)