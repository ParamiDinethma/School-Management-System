/**
 * Teacher Messaging JavaScript
 */
const teacherMessaging = {
    selectedStudent: null,
    selectedParent: null,
    currentConversation: null,
    
    init() {
        this.loadStudents();
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
    
    async loadStudents() {
        try {
            const response = await fetch('/teacher/messages/api/students');
            const data = await response.json();
            
            if (data.success) {
                this.renderStudents(data.students);
            } else {
                this.showAlert('Error loading students: ' + data.message, 'danger');
            }
        } catch (error) {
            console.error('Error loading students:', error);
            this.showAlert('Error loading students. Please try again.', 'danger');
        }
    },
    
    renderStudents(students) {
        const studentList = document.getElementById('studentList');
        if (!studentList) return;
        
        if (!students || students.length === 0) {
            studentList.innerHTML = '<p style="color: #6c757d; text-align: center; padding: 20px;">No students found for this teacher.</p>';
            return;
        }
        
        studentList.innerHTML = students.map(student => `
            <div class="student-item" onclick="teacherMessaging.selectStudent(${student.id}, '${student.firstName} ${student.lastName}')">
                <div class="student-name">${student.firstName} ${student.lastName}</div>
                <div class="student-class">${student.email || 'No class info'}</div>
            </div>
        `).join('');
    },
    
    async selectStudent(studentId, studentName) {
        // Update UI
        document.querySelectorAll('.student-item').forEach(item => item.classList.remove('selected'));
        event.target.closest('.student-item').classList.add('selected');
        
        this.selectedStudent = { id: studentId, name: studentName };
        
        // Load parents for this student
        await this.loadParents(studentId);
    },
    
    async loadParents(studentId) {
        try {
            const response = await fetch(`/teacher/messages/api/students/${studentId}/parents`);
            const data = await response.json();
            
            if (data.success) {
                this.renderParents(data.parents, studentId);
                document.getElementById('parentList').style.display = 'block';
            } else {
                this.showAlert('Error loading parents: ' + data.message, 'danger');
                document.getElementById('parentList').style.display = 'none';
            }
        } catch (error) {
            console.error('Error loading parents:', error);
            this.showAlert('Error loading parents. Please try again.', 'danger');
            document.getElementById('parentList').style.display = 'none';
        }
    },
    
    renderParents(parents, studentId) {
        const parentsContainer = document.getElementById('parentsContainer');
        if (!parentsContainer) return;
        
        if (!parents || parents.length === 0) {
            parentsContainer.innerHTML = '<p style="color: #6c757d; text-align: center; padding: 10px; font-size: 0.9rem;">No parents linked to this student.</p>';
            return;
        }
        
        parentsContainer.innerHTML = parents.map(parent => `
            <div class="parent-item" onclick="teacherMessaging.selectParent(${parent.id}, '${parent.firstName} ${parent.lastName}', ${studentId})">
                <div class="parent-name">${parent.firstName} ${parent.lastName}</div>
                <div class="parent-email">${parent.email || 'No email'}</div>
            </div>
        `).join('');
    },
    
    async selectParent(parentId, parentName, studentId) {
        // Update UI
        document.querySelectorAll('.parent-item').forEach(item => item.classList.remove('selected'));
        event.target.closest('.parent-item').classList.add('selected');
        
        this.selectedParent = { id: parentId, name: parentName };
        
        // Load conversation
        await this.loadConversation(parentId);
    },
    
    async loadConversation(parentId) {
        if (!parentId) return;
        
        try {
            // Show conversation view
            document.getElementById('emptyState').style.display = 'none';
            document.getElementById('conversationView').style.display = 'flex';
            
            // Update conversation title
            document.getElementById('conversationTitle').innerHTML = `
                <i class="fas fa-user"></i>
                Conversation with ${this.selectedParent.name}
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
            
            const response = await fetch(`/teacher/messages/api/conversation/${parentId}?page=0&size=50`);
            const data = await response.json();
            
            if (data.success) {
                this.renderMessages(data.conversation);
                this.currentConversation = parentId;
                
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
        
        if (!this.selectedParent) {
            this.showAlert('Please select a parent first.', 'danger');
            return;
        }
        
        const messageInput = document.getElementById('messageInput');
        const content = messageInput.value.trim();
        
        if (!content) {
            this.showAlert('Please enter a message.', 'danger');
            return;
        }
        
        try {
            const response = await fetch('/teacher/messages/api/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    recipientId: this.selectedParent.id,
                    content: content
                })
            });
            
            const data = await response.json();
            
            if (data.success) {
                // Clear input
                messageInput.value = '';
                messageInput.style.height = 'auto';
                
                // Reload conversation to show new message
                await this.loadConversation(this.selectedParent.id);
                
                this.showAlert('Message sent successfully!', 'success');
            } else {
                this.showAlert('Error sending message: ' + data.message, 'danger');
            }
        } catch (error) {
            console.error('Error sending message:', error);
            this.showAlert('Error sending message. Please try again.', 'danger');
        }
    },
    
    
    async deleteMessage(messageId) {
        if (!confirm('Are you sure you want to delete this message?')) {
            return;
        }
        
        try {
            const response = await fetch(`/teacher/messages/api/message/${messageId}`, {
                method: 'DELETE'
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.showAlert('Message deleted successfully!', 'success');
                // Reload conversation
                if (this.selectedParent) {
                    await this.loadConversation(this.selectedParent.id);
                }
            } else {
                this.showAlert('Error deleting message: ' + data.message, 'danger');
            }
        } catch (error) {
            console.error('Error deleting message:', error);
            this.showAlert('Error deleting message. Please try again.', 'danger');
        }
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
        setInterval(() => {
            if (this.selectedParent) {
                this.loadConversation(this.selectedParent.id);
            }
        }, 30000);
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
    teacherMessaging.init();
});
