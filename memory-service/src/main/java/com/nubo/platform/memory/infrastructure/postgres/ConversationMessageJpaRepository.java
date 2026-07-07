package com.nubo.platform.memory.infrastructure.postgres;

import com.nubo.platform.memory.domain.ConversationMessageEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMessageJpaRepository extends JpaRepository<ConversationMessageEntity, UUID> {

    @Query("""
            select message
            from ConversationMessageEntity message
            where message.conversation.id = :conversationId
              and (:before is null or message.createdAt < :before)
            order by message.createdAt desc, message.id desc
            """)
    List<ConversationMessageEntity> findMessages(
            @Param("conversationId") UUID conversationId,
            @Param("before") Instant before,
            Pageable pageable);
}
