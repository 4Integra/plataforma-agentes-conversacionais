package com.integra.ToolRegistry.service

import com.integra.ToolRegistry.domain.Tool
import com.integra.ToolRegistry.domain.ToolCreateDTO
import com.integra.ToolRegistry.domain.toEntity
import com.integra.ToolRegistry.repository.ToolRegistryRepository
import org.springframework.stereotype.Service

@Service
class ToolRegistryService(
    private val repository: ToolRegistryRepository
) {
    fun findAll(): List<Tool> = repository.findAll()
    fun registerTool(tool: ToolCreateDTO) = repository.save(tool.toEntity())
}