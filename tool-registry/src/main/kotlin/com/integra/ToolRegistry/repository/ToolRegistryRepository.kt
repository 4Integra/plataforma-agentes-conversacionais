package com.integra.ToolRegistry.repository

import com.integra.ToolRegistry.domain.Tool
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ToolRegistryRepository: MongoRepository<Tool, String>