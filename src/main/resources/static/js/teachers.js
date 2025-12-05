class TeacherManagement {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.currentSortBy = 'userId';
        this.currentSortDir = 'asc';
        this.currentSearch = '';
        this.currentDepartment = 'ALL';
        this.currentTeacherId = null;
        
        this.init();
    }

    async init() {
        await this.testConnection();
        await this.loadStatistics();
        await this.loadDepartments();
        await this.loadTeachers();
        this.setupEventListeners();
    }

    async testConnection() {
        try {
            const response = await fetch('/admin/teachers/api/test');
            const data = await response.json();
            console.log('Teachers API test result:', data);
        } catch (error) {
            console.error('Teachers API test failed:', error);
        }
    }

    setupEventListeners() {
        // Search input
        const searchInput = document.getElementById('searchInput');
        let searchTimeout;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                this.currentSearch = e.target.value;
                this.currentPage = 0;
                this.loadTeachers();
            }, 500);
        });

        // Department filter
        document.getElementById('departmentFilter').addEventListener('change', (e) => {
            this.currentDepartment = e.target.value;
            this.currentPage = 0;
            this.loadTeachers();
        });

        // Sort controls
        document.getElementById('sortBy').addEventListener('change', (e) => {
            this.currentSortBy = e.target.value;
            this.loadTeachers();
        });
    }

    async loadStatistics() {
        try {
            const response = await fetch('/admin/teachers/api/statistics');
            if (response.ok) {
                const stats = await response.json();
                document.getElementById('totalTeachers').textContent = stats.total || 0;
                document.getElementById('activeTeachers').textContent = stats.active || 0;
                document.getElementById('inactiveTeachers').textContent = stats.inactive || 0;
            } else {
                console.error('Failed to load statistics:', response.status);
                document.getElementById('totalTeachers').textContent = '-';
                document.getElementById('activeTeachers').textContent = '-';
                document.getElementById('inactiveTeachers').textContent = '-';
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
            document.getElementById('totalTeachers').textContent = '-';
            document.getElementById('activeTeachers').textContent = '-';
            document.getElementById('inactiveTeachers').textContent = '-';
        }
    }

    async loadDepartments() {
        try {
            const response = await fetch('/admin/teachers/api/departments');
            if (response.ok) {
                const departments = await response.json();
                const select = document.getElementById('departmentFilter');
                select.innerHTML = '<option value="ALL">All Departments</option>';
                departments.forEach(dept => {
                    const option = document.createElement('option');
                    option.value = dept;
                    option.textContent = dept;
                    select.appendChild(option);
                });
            }
        } catch (error) {
            console.error('Error loading departments:', error);
        }
    }

    async loadTeachers() {
        try {
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.currentSortBy,
                sortDir: 'asc',
                search: this.currentSearch,
                department: this.currentDepartment
            });

            const response = await fetch(`/admin/teachers/api?${params}`);
            
            if (response.ok) {
                const data = await response.json();
                this.renderTeachersTable(data.teachers);
                this.renderPagination(data);
            } else {
                const errorData = await response.json().catch(() => ({}));
                console.error('Failed to load teachers:', errorData);
                this.showError(`Failed to load teachers: ${errorData.message || 'Unknown error'}`);
            }
        } catch (error) {
            console.error('Error loading teachers:', error);
            this.showError('Error loading teachers');
        }
    }

    renderTeachersTable(teachers) {
        const tbody = document.getElementById('teachersTableBody');
        
        if (teachers.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="no-data">
                        <i class="fas fa-user-slash"></i>
                        <br>
                        No teachers found
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = teachers.map(teacher => `
            <tr>
                <td>
                    <div class="teacher-info">
                        <div class="teacher-avatar">
                            ${this.getTeacherInitials(teacher.user.firstName, teacher.user.lastName)}
                        </div>
                        <div class="teacher-details">
                            <h6>${teacher.user.firstName} ${teacher.user.lastName}</h6>
                            <small>@${teacher.user.username}</small>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="contact-info">
                        <div><i class="fas fa-envelope"></i>${teacher.user.email}</div>
                        ${teacher.user.phone ? `<div><i class="fas fa-phone"></i>${teacher.user.phone}</div>` : ''}
                    </div>
                </td>
                <td>
                    <span class="badge badge-department">${teacher.department || 'Not specified'}</span>
                </td>
                <td>
                    <span class="badge badge-specialization">${teacher.specialization || 'Not specified'}</span>
                </td>
                <td>
                    ${this.formatDate(teacher.hireDate)}
                </td>
                <td>
                    <button class="btn-action btn-edit" onclick="teacherManagement.editTeacher(${teacher.userId})" title="Edit">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn-action btn-delete" onclick="teacherManagement.deleteTeacher(${teacher.userId}, '${teacher.user.firstName} ${teacher.user.lastName}')" title="Delete">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `).join('');
    }

    renderPagination(data) {
        const pagination = document.getElementById('pagination');
        const paginationInfo = document.getElementById('paginationInfo');
        
        // Update pagination info
        const startItem = data.currentPage * data.pageSize + 1;
        const endItem = Math.min((data.currentPage + 1) * data.pageSize, data.totalElements);
        paginationInfo.textContent = `Showing ${startItem}-${endItem} of ${data.totalElements} teachers`;
        
        // Clear existing pagination
        pagination.innerHTML = '';
        
        if (data.totalPages <= 1) {
            return;
        }

        // Previous button
        const prevButton = `
            <li class="page-item ${!data.hasPrevious ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="teacherManagement.goToPage(${data.currentPage - 1})" ${!data.hasPrevious ? 'tabindex="-1" aria-disabled="true"' : ''}>
                    <i class="fas fa-chevron-left"></i>
                </a>
            </li>
        `;
        pagination.insertAdjacentHTML('beforeend', prevButton);

        // Page numbers
        const startPage = Math.max(0, data.currentPage - 2);
        const endPage = Math.min(data.totalPages - 1, data.currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            const pageItem = `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="teacherManagement.goToPage(${i})">
                        ${i + 1}
                    </a>
                </li>
            `;
            pagination.insertAdjacentHTML('beforeend', pageItem);
        }

        // Next button
        const nextButton = `
            <li class="page-item ${!data.hasNext ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="teacherManagement.goToPage(${data.currentPage + 1})" ${!data.hasNext ? 'tabindex="-1" aria-disabled="true"' : ''}>
                    <i class="fas fa-chevron-right"></i>
                </a>
            </li>
        `;
        pagination.insertAdjacentHTML('beforeend', nextButton);
    }

    goToPage(page) {
        if (page >= 0) {
            this.currentPage = page;
            this.loadTeachers();
        }
    }

    getTeacherInitials(firstName, lastName) {
        return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
    }

    formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    openAddTeacherModal() {
        this.currentTeacherId = null;
        document.getElementById('teacherModalLabel').textContent = 'Add New Teacher';
        document.getElementById('teacherForm').reset();
        document.getElementById('password').required = true;
        
        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('teacherModal'));
        modal.show();
    }

    async editTeacher(teacherId) {
        try {
            const response = await fetch(`/admin/teachers/api/${teacherId}`);
            if (response.ok) {
                const teacher = await response.json();
                this.currentTeacherId = teacherId;
                
                const populateForm = () => {
                    document.getElementById('teacherModalLabel').textContent = 'Edit Teacher';
                    document.getElementById('teacherId').value = teacher.userId;
                    const f = document.getElementById('firstName');
                    const l = document.getElementById('lastName');
                    const u = document.getElementById('username');
                    const e = document.getElementById('email');
                    const p = document.getElementById('phone');
                    const a = document.getElementById('address');
                    const s = document.getElementById('specialization');
                    const d = document.getElementById('department');

                    f.value = teacher.user.firstName || '';
                    l.value = teacher.user.lastName || '';
                    u.value = teacher.user.username || '';
                    e.value = teacher.user.email || '';
                    p.value = teacher.user.phone || '';
                    a.value = teacher.user.address || '';
                    s.value = teacher.specialization || '';
                    d.value = teacher.department || '';

                    // trigger input events to ensure UI updates
                    [f,l,u,e,p,a,s,d].forEach(inp => inp && inp.dispatchEvent(new Event('input')));

                    document.getElementById('password').required = false;
                };

                // Populate immediately (in case modal is already mounted)
                populateForm();

                // Show modal then re-populate after it's fully shown to avoid rendering glitches
                const modalEl = document.getElementById('teacherModal');
                const modal = new bootstrap.Modal(modalEl);
                modal.show();
                const onceShown = () => {
                    populateForm();
                    modalEl.removeEventListener('shown.bs.modal', onceShown);
                };
                modalEl.addEventListener('shown.bs.modal', onceShown);
            } else {
                this.showError('Failed to load teacher details');
            }
        } catch (error) {
            console.error('Error loading teacher:', error);
            this.showError('Error loading teacher details');
        }
    }

    async saveTeacher() {
        const form = document.getElementById('teacherForm');
        const formData = new FormData(form);
        
        const teacherData = {
            firstName: formData.get('firstName'),
            lastName: formData.get('lastName'),
            username: formData.get('username'),
            email: formData.get('email'),
            phone: formData.get('phone'),
            address: formData.get('address'),
            role: { name: 'TEACHER' },
            // Teacher-specific fields
            specialization: formData.get('specialization'),
            department: formData.get('department')
        };
        
        // Add password only if it's provided
        const password = formData.get('password');
        if (password && password.trim() !== '') {
            teacherData.password = password;
        }
        
        try {
            const url = this.currentTeacherId ? 
                `/admin/users/api/${this.currentTeacherId}` : 
                '/admin/users/api';
            const method = this.currentTeacherId ? 'PUT' : 'POST';
            
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(teacherData)
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('teacherModal')).hide();
                this.loadTeachers();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error saving teacher:', error);
            this.showError('Error saving teacher');
        }
    }

    deleteTeacher(teacherId, teacherName) {
        this.currentTeacherId = teacherId;
        document.getElementById('deleteTeacherInfo').innerHTML = `
            <strong>Teacher:</strong> ${teacherName}<br>
            <strong>ID:</strong> ${teacherId}
        `;
        
        // Show delete modal
        const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
        modal.show();
    }

    async confirmDelete() {
        try {
            const response = await fetch(`/admin/users/api/${this.currentTeacherId}`, {
                method: 'DELETE'
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
                this.loadTeachers();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error deleting teacher:', error);
            this.showError('Error deleting teacher');
        }
    }

    showSuccess(message) {
        this.showAlert(message, 'success');
    }

    showError(message) {
        this.showAlert(message, 'danger');
    }

    showAlert(message, type) {
        // Create alert element
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        alertDiv.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(alertDiv);
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.parentNode.removeChild(alertDiv);
            }
        }, 5000);
    }
}

// Global functions for onclick handlers
function openAddTeacherModal() {
    teacherManagement.openAddTeacherModal();
}

function saveTeacher() {
    teacherManagement.saveTeacher();
}

function confirmDelete() {
    teacherManagement.confirmDelete();
}

// Initialize when DOM is loaded
let teacherManagement;
document.addEventListener('DOMContentLoaded', function() {
    teacherManagement = new TeacherManagement();
});
