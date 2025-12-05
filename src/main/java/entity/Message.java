package com.wsims.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Entity class representing messages in the school management system.
 * Stores communications between users including text messages, system notifications, etc.
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Message {

    /**
     * Message types available in the system
     */
    public enum MessageType {
        TEXT("TEXT"),
        IMAGE("IMAGE"),
        FILE("FILE"),
        SYSTEM("SYSTEM");

        private final String value;

        MessageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Unique identifier for the message
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * User who sent the message
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * User who received the message
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /**
     * Content of the message
     */
    @Column(name = "message_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String messageContent;

    /**
     * Type of the message (TEXT, IMAGE, FILE, SYSTEM)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 50)
    private MessageType messageType = MessageType.TEXT;


    /**
     * Timestamp when the message was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the message was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Convenience constructor for creating a new message
     */
    public Message(User sender, User recipient, String messageContent, MessageType messageType) {
        this.sender = sender;
        this.recipient = recipient;
        this.messageContent = messageContent;
        this.messageType = messageType;
    }

    /**
     * Convenience constructor for creating a text message
     */
    public Message(User sender, User recipient, String messageContent) {
        this(sender, recipient, messageContent, MessageType.TEXT);
    }


    /**
     * Check if the message is from a specific user
     */
    public boolean isFrom(User user) {
        return this.sender != null && this.sender.getId().equals(user.getId());
    }

    /**
     * Check if the message is to a specific user
     */
    public boolean isTo(User user) {
        return this.recipient != null && this.recipient.getId().equals(user.getId());
    }

    /**
     * Check if the message is a system message
     */
    public boolean isSystemMessage() {
        return MessageType.SYSTEM.equals(this.messageType);
    }

    /**
     * Check if the message contains any content
     */
    public boolean hasContent() {
        return this.messageContent != null && !this.messageContent.trim().isEmpty();
    }

    /**
     * Get a truncated version of the message content for display
     */
    public String getTruncatedContent(int maxLength) {
        if (this.messageContent == null) {
            return "";
        }
        if (this.messageContent.length() <= maxLength) {
            return this.messageContent;
        }
        return this.messageContent.substring(0, maxLength) + "...";
    }

    /**
     * Get a short preview of the message content (50 characters)
     */
    public String getPreview() {
        return getTruncatedContent(50);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", sender=" + (sender != null ? sender.getId() : null) +
                ", recipient=" + (recipient != null ? recipient.getId() : null) +
                ", messageType=" + messageType +
                ", createdAt=" + createdAt +
                '}';
    }
}
