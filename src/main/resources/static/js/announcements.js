// Announcements Management JavaScript

let deleteAnnouncementId = null;
let deleteModal = null;

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    loadAnnouncements();
    initializeForm();
    initializeDeleteModal();
});

/**
 * Initialize the announcement form
 */
function initializeForm() {
    const form = document.getElementById('announcementForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            postAnnouncement();
        });
    }
}

/**
 * Initialize the delete modal
 */
function initializeDeleteModal() {
    const modalElement = document.getElementById('deleteModal');
    if (modalElement) {
        deleteModal = new bootstrap.Modal(modalElement);
    }
}

/**
 * Load all announcements from the server
 */
function loadAnnouncements() {
    fetch('/admin/announcements/api/all')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                displayAnnouncements(data.announcements);
            } else {
                showError(data.message || 'Failed to load announcements');
                displayNoData();
            }
        })
        .catch(error => {
            console.error('Error loading announcements:', error);
            showError('Failed to load announcements. Please try again.');
            displayNoData();
        });
}

/**
 * Display announcements in the table
 */
function displayAnnouncements(announcements) {
    const tbody = document.getElementById('announcementsTableBody');
    
    if (!announcements || announcements.length === 0) {
        displayNoData();
        return;
    }
    
    let html = '';
    announcements.forEach(announcement => {
        html += `
            <tr>
                <td><strong>${escapeHtml(announcement.title)}</strong></td>
                <td>
                    <div class="announcement-content" title="${escapeHtml(announcement.content)}">
                        ${escapeHtml(announcement.content)}
                    </div>
                </td>
                <td>
                    <span class="audience-badge ${getAudienceBadgeClass(announcement.targetAudience)}">
                        ${formatTargetAudience(announcement.targetAudience)}
                    </span>
                </td>
                <td>${formatDate(announcement.createdAt)}</td>
                <td>${escapeHtml(announcement.createdBy)}</td>
                <td>
                    <button class="btn-delete" onclick="deleteAnnouncement(${announcement.id}, '${escapeHtml(announcement.title)}')">
                        <i class="fas fa-trash"></i>
                        Delete
                    </button>
                </td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

/**
 * Display "no data" message
 */
function displayNoData() {
    const tbody = document.getElementById('announcementsTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="6" class="no-data">
                <i class="fas fa-inbox"></i>
                <p>No announcements found. Create your first announcement above!</p>
            </td>
        </tr>
    `;
}

/**
 * Post a new announcement
 */
function postAnnouncement() {
    const title = document.getElementById('title').value.trim();
    const content = document.getElementById('content').value.trim();
    const targetAudience = document.getElementById('targetAudience').value;
    
    // Validate
    if (!title || !content || !targetAudience) {
        showError('Please fill in all required fields');
        return;
    }
    
    // Disable submit button
    const submitBtn = document.querySelector('.btn-post');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Posting...';
    
    // Send request
    fetch('/admin/announcements/api/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            title: title,
            content: content,
            targetAudience: targetAudience
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showSuccess(data.message || 'Announcement posted successfully!');
            document.getElementById('announcementForm').reset();
            loadAnnouncements();
        } else {
            showError(data.message || 'Failed to post announcement');
        }
    })
    .catch(error => {
        console.error('Error posting announcement:', error);
        showError('Failed to post announcement. Please try again.');
    })
    .finally(() => {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    });
}

/**
 * Delete an announcement (show confirmation modal)
 */
function deleteAnnouncement(id, title) {
    deleteAnnouncementId = id;
    
    // Update modal content
    const infoDiv = document.getElementById('deleteAnnouncementInfo');
    infoDiv.innerHTML = `<strong>Title:</strong> ${escapeHtml(title)}`;
    
    // Show modal
    if (deleteModal) {
        deleteModal.show();
    }
}

/**
 * Confirm and execute the delete
 */
function confirmDelete() {
    if (!deleteAnnouncementId) {
        return;
    }
    
    // Send delete request
    fetch(`/admin/announcements/api/delete/${deleteAnnouncementId}`, {
        method: 'DELETE'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showSuccess(data.message || 'Announcement deleted successfully!');
            loadAnnouncements();
        } else {
            showError(data.message || 'Failed to delete announcement');
        }
    })
    .catch(error => {
        console.error('Error deleting announcement:', error);
        showError('Failed to delete announcement. Please try again.');
    })
    .finally(() => {
        deleteAnnouncementId = null;
        if (deleteModal) {
            deleteModal.hide();
        }
    });
}

/**
 * Show success message
 */
function showSuccess(message) {
    hideAllAlerts();
    const alert = document.getElementById('successAlert');
    alert.textContent = message;
    alert.style.display = 'block';
    
    // Auto-hide after 5 seconds
    setTimeout(() => {
        alert.style.display = 'none';
    }, 5000);
    
    // Scroll to top
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

/**
 * Show error message
 */
function showError(message) {
    hideAllAlerts();
    const alert = document.getElementById('errorAlert');
    alert.textContent = message;
    alert.style.display = 'block';
    
    // Auto-hide after 7 seconds
    setTimeout(() => {
        alert.style.display = 'none';
    }, 7000);
    
    // Scroll to top
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

/**
 * Hide all alert messages
 */
function hideAllAlerts() {
    document.getElementById('successAlert').style.display = 'none';
    document.getElementById('errorAlert').style.display = 'none';
}

/**
 * Format target audience for display
 */
function formatTargetAudience(audience) {
    switch(audience) {
        case 'ALL_USERS':
            return 'All Users';
        case 'ALL_STUDENTS':
            return 'All Students';
        case 'ALL_TEACHERS':
            return 'All Teachers';
        case 'ALL_PARENTS':
            return 'All Parents';
        default:
            return audience;
    }
}

/**
 * Get CSS class for audience badge
 */
function getAudienceBadgeClass(audience) {
    switch(audience) {
        case 'ALL_USERS':
            return 'audience-all-users';
        case 'ALL_STUDENTS':
            return 'audience-all-students';
        case 'ALL_TEACHERS':
            return 'audience-all-teachers';
        case 'ALL_PARENTS':
            return 'audience-all-parents';
        default:
            return 'audience-all-users';
    }
}

/**
 * Format date to readable string
 */
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    
    const date = new Date(dateString);
    
    // Format: Jan 15, 2024 at 10:30 AM
    const options = {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    };
    
    return date.toLocaleDateString('en-US', options);
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}


