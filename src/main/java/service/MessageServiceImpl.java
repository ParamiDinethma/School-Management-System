package com.wsims.service;

import com.parami.wsims.entity.Message;
import com.parami.wsims.entity.User;
import com.parami.wsims.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of MessageService
 */
@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;

    @Autowired
    public MessageServiceImpl(MessageRepository messageRepository, UserService userService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    @Override
    public Message sendMessage(User sender, User recipient, String content, Message.MessageType messageType) {
        if (sender == null || recipient == null) {
            throw new IllegalArgumentException("Sender and recipient cannot be null");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        Message message = new Message(sender, recipient, content.trim(), messageType);
        return messageRepository.save(message);
    }

    @Override
    public Message sendMessage(User sender, User recipient, String content) {
        return sendMessage(sender, recipient, content, Message.MessageType.TEXT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversation(Long userId1, Long userId2) {
        return messageRepository.findConversationBetweenUsers(userId1, userId2);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getConversation(Long userId1, Long userId2, Pageable pageable) {
        return messageRepository.findConversationBetweenUsers(userId1, userId2, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getRecentConversations(Long userId) {
        return messageRepository.findRecentConversationsForUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getRecentConversations(Long userId, Pageable pageable) {
        return messageRepository.findRecentConversationsForUser(userId, pageable);
    }


    @Override
    public void deleteMessage(Long messageId, Long requestingUserId) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isPresent()) {
            Message message = messageOpt.get();
            
            // Only sender can delete their message, or system admin
            User requestingUser = userService.findById(requestingUserId).orElse(null);
            if (requestingUser != null && 
                (message.isFrom(requestingUser) || isSystemAdmin(requestingUser))) {
                messageRepository.delete(message);
            } else {
                throw new IllegalArgumentException("You can only delete your own messages");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getSentMessages(Long userId, Pageable pageable) {
        return messageRepository.findBySenderId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getReceivedMessages(Long userId, Pageable pageable) {
        return messageRepository.findByRecipientId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> searchMessages(Long userId, String searchTerm, Pageable pageable) {
        return messageRepository.searchMessagesByContent(userId, searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getSystemMessages(Long userId, Pageable pageable) {
        return messageRepository.findSystemMessagesByRecipientId(userId, pageable);
    }

    @Override
    public void sendSystemMessage(List<User> recipients, String content) {
        for (User recipient : recipients) {
            User systemUser = getSystemUser();
            if (systemUser != null) {
                sendMessage(systemUser, recipient, content, Message.MessageType.SYSTEM);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getNewMessages(Long userId1, Long userId2, Long afterMessageId) {
        return messageRepository.findNewMessagesInConversation(userId1, userId2, afterMessageId);
    }

    /**
     * Check if user is system admin
     */
    private boolean isSystemAdmin(User user) {
        return user.getRole() != null && 
               ("IT_ADMIN".equals(user.getRole().getName()) || 
                "PRINCIPAL".equals(user.getRole().getName()));
    }

    /**
     * Get system user for sending system messages
     */
    private User getSystemUser() {
        // Return the first admin user as system user
        // In a real system, you might want to create a dedicated system user
        return userService.findByRoleName("IT_ADMIN").stream().findFirst().orElse(null);
    }
}
