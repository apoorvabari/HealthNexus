package org.proj.repository;

import org.proj.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessageEntity, UUID> {

    @Query("SELECT m FROM ChatMessageEntity m " +
           "WHERE (m.sender.id = :user1 AND m.recipient.id = :user2) " +
           "   OR (m.sender.id = :user2 AND m.recipient.id = :user1) " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessageEntity> findConversation(@Param("user1") UUID user1, @Param("user2") UUID user2);

    @Query("SELECT COUNT(m) FROM ChatMessageEntity m " +
           "WHERE m.recipient.id = :recipientId AND m.isRead = false")
    long countUnreadMessages(@Param("recipientId") UUID recipientId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ChatMessageEntity m SET m.isRead = true " +
           "WHERE m.recipient.id = :currentUserId AND m.sender.id = :contactUserId AND m.isRead = false")
    int markConversationAsRead(@Param("currentUserId") UUID currentUserId, @Param("contactUserId") UUID contactUserId);
}
