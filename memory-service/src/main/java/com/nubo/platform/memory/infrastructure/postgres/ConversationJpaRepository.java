package com.nubo.platform.memory.infrastructure.postgres;

import com.nubo.platform.memory.domain.ConversationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, UUID> {
}
