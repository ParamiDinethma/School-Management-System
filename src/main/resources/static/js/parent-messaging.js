/**
 * Parent Messaging JavaScript
 */
const parentMessaging = {
    selectedConversation: null,
    currentTeacherId: null,
    pollingInterval: null,
    
    init() {
        this.loadConversations();
        this.setupEventListeners();
        this.startPolling();
    },
    
    setupEventListeners() {
        // Message input auto-resize
        const messageInput = document.getElementById('messageInput');
        if (messageInput) {
            messageInput.addEventListener('input', this.autoResizeTextarea);
            messageInput.addEventListener('keydown', this.handleKeyDown);
        }
        
        // Search functionality
        const searchInput = document.getElementById('conversationSearch');
        if (searchInput) {
            searchInput.addEventListener('input', this.filterConversations);
        }
    },
    
    autoResizeTextarea(event) {
        const textarea = event.target;
        textarea.style.height = 'auto';
        textarea.style.height = Math.min(textarea.scrollHeight, 100) + 'px';
    },
    
    handleKeyDown(event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.sendMessage(event);
        }
    },
    
    async loadConversations() {
        try {
            const response = await fetch('/parent/messages/api/conversations?page=0&size=50');
            const data = await response.json();
            
            if (data.success) {
                this.renderConversations(data.conversations);
            } else {
                this.showAlert('Error loading conversations: ' + data.message, 'danger');
            }
        } catch (error) {
            console.error('Error loading conversations:', error);
            this.showAlert('Error loading conversations. Please try again.', 'danger');
        }
    },
    
    renderConversations(conversations) {
        const conversationsList = document.getElementById('conversationsList');
        if (!conversationsList) return;
        
        if (!conversations || conversations.length === 0) {
            conversationsList.innerHTML = `
                <div style="padding: 40px; text-align: center; color: #6c757d;">
                    <i class="fas fa-comments" style="font-size: 3rem; margin-bottom: 15px; opacity: 0.5;"></i>
                    <h4>No conversations yet</h4>
                    <p>Teachers will appear here when they send you messages.</p>
                </div>
            `;
            return;
        }
        
        conversationsList.innerHTML = conversations.map(conversation => {
            const teacherName = this.getTeacherName(conversation);
            const timestamp = this.formatTimestamp(conversation.createdAt);
            const preview = this.truncateText(conversation.messageContent, 60);
            
            return `
                <div class="conversation-item" 
                     onclick="parentMessaging.selectConversation(${conversation.id}, ${this.getTeacherId(conversation)}, '${teacherName}')">
                    <div class="conversation-header-info">
                        <span class="conversation-teacher">${teacherName}</span>
                        <span class="conversation-time">${timestamp}</span>
                    </div>
                    <p class="conversation-preview">${this.escapeHtml(preview)}</p>
                </div>
            `;
        }).join('');
    },
    
    getTeacherName(conversation) {
        // Determine if current user is sender or recipient
        const currentUserId = this.getCurrentUserId();
        if (conversation.sender.id === currentUserId) {
            return conversation.recipient.firstName + ' ' + conversation.recipient.lastName;
        } else {
            return conversation.sender.firstName + ' ' + conversation.sender.lastName;
        }
    },
    
    getTeacherId(conversation) {
        // Return the ID of the teacher (not the current user)
        const currentUserId = this.getCurrentUserId();
        if (conversation.sender.id === currentUserId) {
            return conversation.recipient.id;
        } else {
            return conversation.sender.id;
        }
    },
    
    async selectConversation(conversationId, teacherId, teacherName) {
        // Update UI
        document.querySelectorAll('.conversation-item').forEach(item => item.classList.remove('selected'));
        event.target.closest('.conversation-item').classList.add('selected');
        
        this.selectedConversation = conversationId;
        this.currentTeacherId = teacherId;
        
        // Load conversation
        await this.loadConversation(teacherId);
    },
    
    async loadConversation(teacherId) {
        if (!teacherId) return;
        
        try {
            // Show conversation view
            document.getElementById('emptyState').style.display = 'none';
            document.getElementById('conversationView').style.display = 'flex';
            
            // Update conversation title
            const teacherName = document.querySelector('.conversation-item.selected .conversation-teacher').textContent;
            document.getElementById('conversationTitle').innerHTML = `
                <i class="fas fa-user"></i>
                Conversation with ${teacherName}
            `;
            
            // Show loading in messages container
            const messagesContainer = document.getElementById('messagesContainer');
            messagesContainer.innerHTML = `
                <div class="loading">
                    <div class="spinner-border" role="status">
                        <span class="sr-only">Loading...</span>
                    </div>
                </div>
            `;
            
            const response = await fetch(`/parent/messages/api/conversation/${teacherId}?page=0&size=50`);
            const data = await response.json();
            
            if (data.success) {
                this.renderMessages(data.conversation);
                
            } else {
                messagesContainer.innerHTML = '<p style="color: #6c757d; text-align: center; padding: 20px;">Error loading conversation.</p>';
                this.showAlert('Error loading conversation: ' + data.message, 'danger');
            }
        } catch (error) {
            console.error('Error loading conversation:', error);
            this.showAlert('Error loading conversation. Please try again.', 'danger');
        }
    },
    
    renderMessages(messages) {
        const messagesContainer = document.getElementById('messagesContainer');
        if (!messagesContainer) return;
        
        if (!messages || messages.length === 0) {
            messagesContainer.innerHTML = `
                <div class="empty-state" style="height: auto; padding: 40px;">
                    <i class="fas fa-comments"></i>
                    <h4>No messages yet</h4>
                    <p>Start a conversation by sending a message.</p>
                </div>
            `;
            return;
        }
        
        // Reverse messages to show oldest first
        const sortedMessages = messages.reverse();
        
        messagesContainer.innerHTML = sortedMessages.map(message => {
            const isSent = message.sender.id === this.getCurrentUserId();
            const senderName = message.sender.firstName + ' ' + message.sender.lastName;
            const timestamp = this.formatTimestamp(message.createdAt);
            
            return `
                <div class="message ${isSent ? 'sent' : 'received'}">
                    <div class="message-avatar">
                        ${senderName.charAt(0).toUpperCase()}
                    </div>
                    <div class="message-content">
                        <p class="message-text">${this.escapeHtml(message.messageContent)}</p>
                        <div class="message-time">${timestamp}</div>
                    </div>
                </div>
            `;
        }).join('');
        
        // Scroll to bottom
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    },
    
    async sendMessage(event) {
        event.preventDefault();
        
        if (!this.currentTeacherId) {
            this.showAlert('Please select a conversation first.', 'danger');
            return;
        }
        
        const messageInput = document.getElementById('messageInput');
        const content = messageInput.value.trim();
        
        if (!content) {
            this.showAlert('Please enter a message.', 'danger');
            return;
        }
        
        try {
            const response = await fetch('/parent/messages/api/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    recipientId: this.currentTeacherId,
                    content: content
                })
            });
            
            const data = await response.json();
            
            if (data.success) {
                // Clear input
                messageInput.value = '';
                messageInput.style.height = 'auto';
                
                // Reload conversation to show new message
                await this.loadConversation(this.currentTeacherId);
                
                // Reload conversations list to update preview
                await this.loadConversations();
                
                this.showAlert('Message sent successfully!', 'success');
            } else {
                this.showAlert('Error sending message: ' + data.message, 'danger');
            }
        } catch (error) {
            console.error('Error sending message:', error);
            this.showAlert('Error sending message. Please try again.', 'danger');
        }
    },
    
    
    async refreshConversations() {
        await this.loadConversations();
        this.showAlert('Conversations refreshed!', 'success');
    },
    
    formatTimestamp(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
        
        if (diffDays === 0) {
            return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } else if (diffDays === 1) {
            return 'Yesterday ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } else if (diffDays < 7) {
            return date.toLocaleDateString([], { weekday: 'short', hour: '2-digit', minute: '2-digit' });
        } else {
            return date.toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
        }
    },
    
    truncateText(text, maxLength) {
        if (!text) return '';
        if (text.length <= maxLength) return text;
        return text.substring(0, maxLength) + '...';
    },
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },
    
    getCurrentUserId() {
        // This would typically come from the server or be stored in a global variable
        // For now, we'll assume it's available in a data attribute or global variable
        return window.currentUserId || null;
    },
    
    showAlert(message, type) {
        const alertContainer = document.getElementById('alertContainer');
        if (!alertContainer) return;
        
        const alertId = 'alert-' + Date.now();
        const alertHtml = `
            <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" onclick="document.getElementById('${alertId}').remove()"></button>
            </div>
        `;
        
        alertContainer.innerHTML = alertHtml;
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            const alertElement = document.getElementById(alertId);
            if (alertElement) {
                alertElement.remove();
            }
        }, 5000);
    },
    
    startPolling() {
        // Poll for new messages every 30 seconds
        this.pollingInterval = setInterval(() => {
            this.loadConversations();
        }, 30000);
    },
    
    stopPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
    },
    
    filterConversations(event) {
        const searchTerm = event.target.value.toLowerCase();
        const conversationItems = document.querySelectorAll('.conversation-item');
        
        conversationItems.forEach(item => {
            const text = item.textContent.toLowerCase();
            item.style.display = text.includes(searchTerm) ? 'block' : 'none';
        });
    }
};

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    parentMessaging.init();
});

// Clean up polling when page is unloaded
window.addEventListener('beforeunload', () => {
    parentMessaging.stopPolling();
});
