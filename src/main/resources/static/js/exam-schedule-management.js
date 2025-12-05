class ExamScheduleManagement {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.sortBy = 'examName';
        this.sortDir = 'asc';
        this.searchTerm = '';
        this.statusFilter = '';
        this.selectedExams = new Set();
        this.isEditMode = false;
        this.init();
    }

    init() {
        this.loadStatistics();
        this.loadExamSchedules();
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Search input
        document.getElementById('searchInput')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.searchExams();
            }
        });

        // Status filter
        document.getElementById('statusFilter')?.addEventListener('change', (e) => {
            this.statusFilter = e.target.value;
            this.currentPage = 0;
            this.loadExamSchedules();
        });

        // Date validation
        document.getElementById('startDate')?.addEventListener('change', (e) => {
            this.validateDateRange();
        });

        document.getElementById('endDate')?.addEventListener('change', (e) => {
            this.validateDateRange();
        });

        // Form reset on modal close
        document.getElementById('examScheduleModal')?.addEventListener('hidden.bs.modal', () => {
            this.resetForm();
        });
    }

    async loadStatistics() {
        try {
            const response = await fetch('/admin/exam-schedules/api/statistics');
            if (response.ok) {
                const data = await response.json();
                this.updateStatistics(data);
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    updateStatistics(data) {
        const stats = data.statistics || {};
        
        document.getElementById('totalExamsCount').textContent = 
            (stats.ACTIVE || 0) + (stats.INACTIVE || 0) + (stats.COMPLETED || 0);
        document.getElementById('upcomingExamsCount').textContent = data.upcomingCount || 0;
        document.getElementById('ongoingExamsCount').textContent = data.ongoingCount || 0;
        document.getElementById('completedExamsCount').textContent = stats.COMPLETED || 0;
    }

    async loadExamSchedules() {
        try {
            let url = `/admin/exam-schedules/api?page=${this.currentPage}&size=${this.pageSize}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;
            
            if (this.searchTerm) {
                url += `&search=${encodeURIComponent(this.searchTerm)}`;
            }

            const response = await fetch(url);
            if (response.ok) {
                const data = await response.json();
                this.renderExamScheduleTable(data);
                this.updatePagination(data);
            } else {
                this.showError('Failed to load exam schedules');
            }
        } catch (error) {
            console.error('Error loading exam schedules:', error);
            this.showError('Error loading exam schedules: ' + error.message);
        }
    }

    renderExamScheduleTable(data) {
        const tbody = document.getElementById('examScheduleTableBody');
        
        if (!data.content || data.content.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="empty-state">
                        <i class="fas fa-calendar-times"></i>
                        <h5>No Exam Schedules Found</h5>
                        <p>No exam schedules match your current search criteria.</p>
                        <button type="button" class="btn btn-primary" onclick="examScheduleManagement.openAddModal()">
                            <i class="fas fa-plus"></i>
                            Add First Exam Schedule
                        </button>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = data.content.map(exam => `
            <tr>
                <td>
                    <input type="checkbox" class="exam-checkbox" value="${exam.id}" 
                           onchange="examScheduleManagement.toggleExamSelection(${exam.id})">
                </td>
                <td>
                    <div class="exam-info">
                        <strong>${this.escapeHtml(exam.examName)}</strong>
                        ${exam.description ? `<br><small class="text-muted">${this.escapeHtml(exam.description)}</small>` : ''}
                    </div>
                </td>
                <td>${this.formatDate(exam.startDate)}</td>
                <td>${this.formatDate(exam.endDate)}</td>
                <td>${exam.academicYear || '-'}</td>
                <td>${exam.semester || '-'}</td>
                <td>
                    <span class="status-badge status-${exam.status.toLowerCase()}">
                        ${this.formatStatus(exam.status)}
                    </span>
                </td>
                <td>
                    <div class="action-buttons">
                        <button type="button" class="btn btn-sm btn-primary" 
                                onclick="examScheduleManagement.openEditModal(${exam.id})" 
                                title="Edit Exam Schedule">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button type="button" class="btn btn-sm btn-secondary" 
                                onclick="examScheduleManagement.updateStatus(${exam.id}, '${exam.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}')" 
                                title="${exam.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}">
                            <i class="fas fa-${exam.status === 'ACTIVE' ? 'pause' : 'play'}"></i>
                        </button>
                        <button type="button" class="btn btn-sm btn-danger" 
                                onclick="examScheduleManagement.deleteExamSchedule(${exam.id})" 
                                title="Delete Exam Schedule">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    updatePagination(data) {
        const pagination = document.getElementById('examSchedulePagination');
        
        if (data.totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        let paginationHTML = '';
        
        // Previous button
        paginationHTML += `
            <li class="page-item ${data.first ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="examScheduleManagement.goToPage(${data.currentPage - 1})">
                    <i class="fas fa-chevron-left"></i>
                </a>
            </li>
        `;

        // Page numbers
        const startPage = Math.max(0, data.currentPage - 2);
        const endPage = Math.min(data.totalPages - 1, data.currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="examScheduleManagement.goToPage(${i})">${i + 1}</a>
                </li>
            `;
        }

        // Next button
        paginationHTML += `
            <li class="page-item ${data.last ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="examScheduleManagement.goToPage(${data.currentPage + 1})">
                    <i class="fas fa-chevron-right"></i>
                </a>
            </li>
        `;

        pagination.innerHTML = paginationHTML;
    }

    goToPage(page) {
        if (page >= 0) {
            this.currentPage = page;
            this.loadExamSchedules();
        }
    }

    sortTable(column) {
        if (this.sortBy === column) {
            this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
        } else {
            this.sortBy = column;
            this.sortDir = 'asc';
        }
        this.currentPage = 0;
        this.loadExamSchedules();
    }

    searchExams() {
        this.searchTerm = document.getElementById('searchInput').value.trim();
        this.currentPage = 0;
        this.loadExamSchedules();
    }

    openAddModal() {
        this.isEditMode = false;
        document.getElementById('examScheduleModalLabel').innerHTML = '<i class="fas fa-calendar-plus"></i> Add New Exam Schedule';
        document.querySelector('#examScheduleModal .btn-primary').innerHTML = '<i class="fas fa-save"></i> Save Exam Schedule';
        this.resetForm();
        
        const modal = new bootstrap.Modal(document.getElementById('examScheduleModal'));
        modal.show();
    }

    async openEditModal(id) {
        try {
            const response = await fetch(`/admin/exam-schedules/api/${id}`);
            if (response.ok) {
                const data = await response.json();
                const exam = data.examSchedule;
                
                this.isEditMode = true;
                document.getElementById('examScheduleModalLabel').innerHTML = '<i class="fas fa-edit"></i> Edit Exam Schedule';
                document.querySelector('#examScheduleModal .btn-primary').innerHTML = '<i class="fas fa-save"></i> Update Exam Schedule';
                
                // Populate form
                document.getElementById('examScheduleId').value = exam.id;
                document.getElementById('examName').value = exam.examName;
                document.getElementById('description').value = exam.description || '';
                document.getElementById('startDate').value = exam.startDate;
                document.getElementById('endDate').value = exam.endDate;
                document.getElementById('academicYear').value = exam.academicYear || '';
                document.getElementById('semester').value = exam.semester || '';
                document.getElementById('status').value = exam.status;
                
                const modal = new bootstrap.Modal(document.getElementById('examScheduleModal'));
                modal.show();
            } else {
                this.showError('Failed to load exam schedule details');
            }
        } catch (error) {
            console.error('Error loading exam schedule:', error);
            this.showError('Error loading exam schedule: ' + error.message);
        }
    }

    async saveExamSchedule() {
        if (!this.validateForm()) {
            return;
        }

        const formData = {
            examName: document.getElementById('examName').value.trim(),
            description: document.getElementById('description').value.trim(),
            startDate: document.getElementById('startDate').value,
            endDate: document.getElementById('endDate').value,
            academicYear: document.getElementById('academicYear').value,
            semester: document.getElementById('semester').value,
            status: document.getElementById('status').value
        };

        try {
            const url = this.isEditMode 
                ? `/admin/exam-schedules/api/${document.getElementById('examScheduleId').value}`
                : '/admin/exam-schedules/api';
            
            const method = this.isEditMode ? 'PUT' : 'POST';

            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                const data = await response.json();
                this.showSuccess(data.message || 'Exam schedule saved successfully');
                
                // Close modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('examScheduleModal'));
                modal.hide();
                
                // Refresh data
                this.loadExamSchedules();
                this.loadStatistics();
            } else {
                const errorData = await response.json();
                this.showError(errorData.error || 'Failed to save exam schedule');
            }
        } catch (error) {
            console.error('Error saving exam schedule:', error);
            this.showError('Error saving exam schedule: ' + error.message);
        }
    }

    async deleteExamSchedule(id) {
        if (!confirm('Are you sure you want to delete this exam schedule? This action cannot be undone.')) {
            return;
        }

        try {
            const response = await fetch(`/admin/exam-schedules/api/${id}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                const data = await response.json();
                this.showSuccess(data.message || 'Exam schedule deleted successfully');
                this.loadExamSchedules();
                this.loadStatistics();
            } else {
                const errorData = await response.json();
                this.showError(errorData.error || 'Failed to delete exam schedule');
            }
        } catch (error) {
            console.error('Error deleting exam schedule:', error);
            this.showError('Error deleting exam schedule: ' + error.message);
        }
    }

    async updateStatus(id, newStatus) {
        try {
            const response = await fetch(`/admin/exam-schedules/api/${id}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ status: newStatus })
            });

            if (response.ok) {
                const data = await response.json();
                this.showSuccess(data.message || 'Exam schedule status updated successfully');
                this.loadExamSchedules();
                this.loadStatistics();
            } else {
                const errorData = await response.json();
                this.showError(errorData.error || 'Failed to update exam schedule status');
            }
        } catch (error) {
            console.error('Error updating status:', error);
            this.showError('Error updating exam schedule status: ' + error.message);
        }
    }

    validateForm() {
        const requiredFields = ['examName', 'startDate', 'endDate', 'academicYear', 'semester'];
        let isValid = true;

        requiredFields.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            const value = field.value.trim();
            
            if (!value) {
                field.classList.add('is-invalid');
                isValid = false;
            } else {
                field.classList.remove('is-invalid');
            }
        });

        // Validate date range
        if (!this.validateDateRange()) {
            isValid = false;
        }

        return isValid;
    }

    validateDateRange() {
        const startDate = document.getElementById('startDate').value;
        const endDate = document.getElementById('endDate').value;
        
        if (startDate && endDate) {
            const start = new Date(startDate);
            const end = new Date(endDate);
            
            if (end < start) {
                document.getElementById('endDate').classList.add('is-invalid');
                this.showError('End date must be after or equal to start date');
                return false;
            } else {
                document.getElementById('endDate').classList.remove('is-invalid');
                return true;
            }
        }
        
        return true;
    }

    resetForm() {
        document.getElementById('examScheduleForm').reset();
        document.getElementById('examScheduleId').value = '';
        
        // Remove validation classes
        document.querySelectorAll('.is-invalid').forEach(el => {
            el.classList.remove('is-invalid');
        });
        
        this.selectedExams.clear();
        this.updateSelectAllCheckbox();
    }

    toggleSelectAll() {
        const selectAll = document.getElementById('selectAll');
        const checkboxes = document.querySelectorAll('.exam-checkbox');
        
        checkboxes.forEach(checkbox => {
            checkbox.checked = selectAll.checked;
            if (selectAll.checked) {
                this.selectedExams.add(parseInt(checkbox.value));
            } else {
                this.selectedExams.delete(parseInt(checkbox.value));
            }
        });
    }

    toggleExamSelection(id) {
        if (this.selectedExams.has(id)) {
            this.selectedExams.delete(id);
        } else {
            this.selectedExams.add(id);
        }
        
        this.updateSelectAllCheckbox();
    }

    updateSelectAllCheckbox() {
        const selectAll = document.getElementById('selectAll');
        const checkboxes = document.querySelectorAll('.exam-checkbox');
        
        if (checkboxes.length === 0) {
            selectAll.checked = false;
            selectAll.indeterminate = false;
            return;
        }
        
        const checkedCount = Array.from(checkboxes).filter(cb => cb.checked).length;
        
        if (checkedCount === 0) {
            selectAll.checked = false;
            selectAll.indeterminate = false;
        } else if (checkedCount === checkboxes.length) {
            selectAll.checked = true;
            selectAll.indeterminate = false;
        } else {
            selectAll.checked = false;
            selectAll.indeterminate = true;
        }
    }

    async refreshData() {
        this.loadStatistics();
        this.loadExamSchedules();
        this.showInfo('Data refreshed successfully');
    }

    // Helper methods
    formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    formatStatus(status) {
        const statusMap = {
            'ACTIVE': 'Active',
            'INACTIVE': 'Inactive',
            'COMPLETED': 'Completed'
        };
        return statusMap[status] || status;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Alert methods
    showAlert(message, type = 'info') {
        const alertContainer = document.getElementById('alertContainer');
        const alertId = 'alert-' + Date.now();
        
        const alertHTML = `
            <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
        
        alertContainer.insertAdjacentHTML('beforeend', alertHTML);
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            const alertElement = document.getElementById(alertId);
            if (alertElement) {
                alertElement.remove();
            }
        }, 5000);
    }

    showSuccess(message) {
        this.showAlert(message, 'success');
    }

    showError(message) {
        this.showAlert(message, 'danger');
    }

    showWarning(message) {
        this.showAlert(message, 'warning');
    }

    showInfo(message) {
        this.showAlert(message, 'info');
    }
}

// Initialize when DOM is loaded
let examScheduleManagement;
document.addEventListener('DOMContentLoaded', function() {
    examScheduleManagement = new ExamScheduleManagement();
});

