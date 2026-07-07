package com.integra.ToolRegistry.controller

import com.integra.ToolRegistry.domain.Tool
import com.integra.ToolRegistry.domain.ToolCreateDTO
import com.integra.ToolRegistry.service.ToolRegistryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tools")
class ToolRegistryController(private val service: ToolRegistryService) {
    @PostMapping
    fun publishTool(@RequestBody tool: ToolCreateDTO): ResponseEntity<Tool> {
        return ResponseEntity.ok(service.registerTool(tool))
    }

    @GetMapping
    fun listAll(): List<Tool> = service.findAll()
}