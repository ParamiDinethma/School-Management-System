package com.wsims.service;

import com.parami.wsims.entity.Message;
import com.parami.wsims.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for message operations
 */
public interface MessageService {

    /**
     * Send a message from sender to recipient
     */
    Message sendMessage(User sender, User recipient, String content, Message.MessageType messageType);

    /**
     * Send a text message from sender to recipient
     */
    Message sendMessage(User sender, User recipient, String content);

    /**
     * Get conversation between two users
     */
    List<Message> getConversation(Long userId1, Long userId2);

    /**
     * Get conversation between two users with pagination
     */
    Page<Message> getConversation(Long userId1, Long userId2, Pageable pageable);

    /**
     * Get all conversations for a user (recent messages)
     */
    List<Message> getRecentConversations(Long userId);

    /**
     * Get all conversations for a user with pagination
     */
    Page<Message> getRecentConversations(Long userId, Pageable pageable);


    /**
     * Delete a message (only by sender or system admin)
     */
    void deleteMessage(Long messageId, Long requestingUserId);

    /**
     * Get messages sent by a user
     */
    Page<Message> getSentMessages(Long userId, Pageable pageable);

    /**
     * Get messages received by a user
     */
    Page<Message> getReceivedMessages(Long userId, Pageable pageable);

    /**
     * Search messages by content for a user
     */
    Page<Message> searchMessages(Long userId, String searchTerm, Pageable pageable);

    /**
     * Get system messages for a user
     */
    Page<Message> getSystemMessages(Long userId, Pageable pageable);

    /**
     * Send system message to multiple recipients
     */
    void sendSystemMessage(List<User> recipients, String content);

    /**
     * Get new messages in a conversation after a specific message ID
     */
    List<Message> getNewMessages(Long userId1, Long userId2, Long afterMessageId);
}
