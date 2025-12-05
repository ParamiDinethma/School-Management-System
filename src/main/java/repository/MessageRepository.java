package com.wsims.repository;

import com.parami.wsims.entity.Message;
import com.parami.wsims.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Message entity operations
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find all messages between two users
     */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId1 AND m.recipient.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.recipient.id = :userId1) " +
           "ORDER BY m.createdAt DESC")
    List<Message> findConversationBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Find all messages between two users with pagination
     */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId1 AND m.recipient.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.recipient.id = :userId1) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findConversationBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);

    /**
     * Find all messages sent by a specific user
     */
    @Query("SELECT m FROM Message m WHERE m.sender.id = :senderId ORDER BY m.createdAt DESC")
    Page<Message> findBySenderId(@Param("senderId") Long senderId, Pageable pageable);

    /**
     * Find all messages received by a specific user
     */
    @Query("SELECT m FROM Message m WHERE m.recipient.id = :recipientId ORDER BY m.createdAt DESC")
    Page<Message> findByRecipientId(@Param("recipientId") Long recipientId, Pageable pageable);


    /**
     * Find all messages between two users within a date range
     */
    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender.id = :userId1 AND m.recipient.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.recipient.id = :userId1)) AND " +
           "m.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY m.createdAt DESC")
    List<Message> findConversationBetweenUsersInDateRange(
            @Param("userId1") Long userId1, 
            @Param("userId2") Long userId2,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find all system messages for a specific user
     */
    @Query("SELECT m FROM Message m WHERE m.recipient.id = :recipientId AND m.messageType = 'SYSTEM' ORDER BY m.createdAt DESC")
    Page<Message> findSystemMessagesByRecipientId(@Param("recipientId") Long recipientId, Pageable pageable);

    /**
     * Find recent conversations for a user (last message from each conversation)
     */
    @Query("SELECT DISTINCT m FROM Message m WHERE " +
           "(m.sender.id = :userId OR m.recipient.id = :userId) AND " +
           "m.id IN (" +
           "    SELECT MAX(m2.id) FROM Message m2 WHERE " +
           "    (m2.sender.id = :userId OR m2.recipient.id = :userId) " +
           "    GROUP BY " +
           "    CASE " +
           "        WHEN m2.sender.id = :userId THEN m2.recipient.id " +
           "        ELSE m2.sender.id " +
           "    END" +
           ") " +
           "ORDER BY m.createdAt DESC")
    List<Message> findRecentConversationsForUser(@Param("userId") Long userId);

    /**
     * Find recent conversations for a user with pagination
     */
    @Query("SELECT DISTINCT m FROM Message m WHERE " +
           "(m.sender.id = :userId OR m.recipient.id = :userId) AND " +
           "m.id IN (" +
           "    SELECT MAX(m2.id) FROM Message m2 WHERE " +
           "    (m2.sender.id = :userId OR m2.recipient.id = :userId) " +
           "    GROUP BY " +
           "    CASE " +
           "        WHEN m2.sender.id = :userId THEN m2.recipient.id " +
           "        ELSE m2.sender.id " +
           "    END" +
           ") " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findRecentConversationsForUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find messages by content (search functionality)
     */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId OR m.recipient.id = :userId) AND " +
           "LOWER(m.messageContent) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> searchMessagesByContent(@Param("userId") Long userId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find messages by type for a specific user
     */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId OR m.recipient.id = :userId) AND " +
           "m.messageType = :messageType " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findMessagesByTypeAndUser(@Param("userId") Long userId, @Param("messageType") Message.MessageType messageType, Pageable pageable);

    /**
     * Delete messages older than a specific date
     */
    @Query("DELETE FROM Message m WHERE m.createdAt < :cutoffDate")
    void deleteMessagesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find messages between two users after a specific message ID
     */
    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender.id = :userId1 AND m.recipient.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.recipient.id = :userId1)) AND " +
           "m.id > :afterMessageId " +
           "ORDER BY m.createdAt ASC")
    List<Message> findNewMessagesInConversation(
            @Param("userId1") Long userId1, 
            @Param("userId2") Long userId2,
            @Param("afterMessageId") Long afterMessageId);

    /**
     * Find the last message in a conversation between two users
     */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId1 AND m.recipient.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.recipient.id = :userId1) " +
           "ORDER BY m.createdAt DESC")
    List<Message> findLastMessageInConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);
}
