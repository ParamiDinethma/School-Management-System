// Students Management JavaScript
class StudentsManagement {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.currentSort = 'id';
        this.currentSortDir = 'asc';
        this.currentSearch = '';
        this.currentGradeLevel = 'ALL';
        this.currentStudentId = null;
        
        this.init();
    }

    async init() {
        await this.testConnection();
        await this.loadStatistics();
        await this.loadStudents();
        this.setupEventListeners();
    }

    async testConnection() {
        try {
            const response = await fetch('/admin/students/api/test');
            const data = await response.json();
            console.log('Students API test result:', data);
        } catch (error) {
            console.error('Students API test failed:', error);
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
                this.loadStudents();
            }, 500);
        });

        // Grade level filter
        document.getElementById('gradeLevelFilter').addEventListener('change', (e) => {
            this.currentGradeLevel = e.target.value;
            this.currentPage = 0;
            this.loadStudents();
        });
    }

    async loadStatistics() {
        try {
            const response = await fetch('/admin/students/api/statistics');
            if (response.ok) {
                const stats = await response.json();
                document.getElementById('totalStudents').textContent = stats.total || 0;
                document.getElementById('activeStudents').textContent = stats.active || 0;
                document.getElementById('inactiveStudents').textContent = stats.inactive || 0;
            } else {
                console.error('Failed to load statistics:', response.status);
                document.getElementById('totalStudents').textContent = '-';
                document.getElementById('activeStudents').textContent = '-';
                document.getElementById('inactiveStudents').textContent = '-';
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
            document.getElementById('totalStudents').textContent = '-';
            document.getElementById('activeStudents').textContent = '-';
            document.getElementById('inactiveStudents').textContent = '-';
        }
    }

    async loadStudents() {
        try {
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.currentSort,
                sortDir: this.currentSortDir,
                search: this.currentSearch,
                gradeLevel: this.currentGradeLevel
            });

            const response = await fetch(`/admin/students/api?${params}`);
            
            if (response.ok) {
                const data = await response.json();
                this.renderStudentsTable(data.students);
                this.renderPagination(data);
            } else {
                const errorData = await response.json().catch(() => ({}));
                console.error('Failed to load students:', errorData);
                this.showError(`Failed to load students: ${errorData.message || 'Unknown error'}`);
            }
        } catch (error) {
            console.error('Error loading students:', error);
            this.showError('Error loading students');
        }
    }

    renderStudentsTable(students) {
        const tbody = document.getElementById('studentsTableBody');
        
        if (students.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="no-data">
                        <i class="fas fa-user-graduate"></i>
                        <p>No students found</p>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = students.map(student => `
            <tr>
                <td>
                    <div class="student-info">
                        <div class="student-avatar">
                            ${this.getStudentInitials(student.user.firstName, student.user.lastName)}
                        </div>
                        <div class="student-details">
                            <h6>${student.user.firstName} ${student.user.lastName}</h6>
                            <p>@${student.user.username}</p>
                        </div>
                    </div>
                </td>
                <td>${student.user.email || '-'}</td>
                <td>
                    <span class="grade-badge">
                        ${student.gradeLevel || '-'}
                    </span>
                </td>
                <td>${this.formatDate(student.enrollmentDate)}</td>
                <td>
                    <span class="status-badge ${this.getStatusClass(student.enrollmentDate)}">
                        ${this.getStatus(student.enrollmentDate)}
                    </span>
                </td>
                <td>${student.emergencyContactPhone || '-'}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn-action btn-edit" onclick="studentsManagement.editStudent(${student.userId})">
                            <i class="fas fa-edit"></i>
                            Edit
                        </button>
                        <button class="btn-action btn-delete" onclick="studentsManagement.deleteStudent(${student.userId}, '${student.user.firstName} ${student.user.lastName}')">
                            <i class="fas fa-trash"></i>
                            Delete
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    renderPagination(data) {
        const paginationInfo = document.getElementById('paginationInfo');
        const pagination = document.getElementById('pagination');
        
        // Update pagination info
        const startItem = (data.currentPage * data.pageSize) + 1;
        const endItem = Math.min((data.currentPage + 1) * data.pageSize, data.totalItems);
        paginationInfo.textContent = `Showing ${startItem}-${endItem} of ${data.totalItems} students`;
        
        // Clear existing pagination
        pagination.innerHTML = '';
        
        // Previous button
        const prevButton = document.createElement('li');
        prevButton.innerHTML = `
            <a class="page-link ${!data.hasPrevious ? 'disabled' : ''}" 
               href="#" onclick="studentsManagement.goToPage(${data.currentPage - 1})">
                <i class="fas fa-chevron-left"></i>
            </a>
        `;
        pagination.appendChild(prevButton);
        
        // Page numbers
        const startPage = Math.max(0, data.currentPage - 2);
        const endPage = Math.min(data.totalPages - 1, data.currentPage + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            const pageButton = document.createElement('li');
            pageButton.innerHTML = `
                <a class="page-link ${i === data.currentPage ? 'active' : ''}" 
                   href="#" onclick="studentsManagement.goToPage(${i})">
                    ${i + 1}
                </a>
            `;
            pagination.appendChild(pageButton);
        }
        
        // Next button
        const nextButton = document.createElement('li');
        nextButton.innerHTML = `
            <a class="page-link ${!data.hasNext ? 'disabled' : ''}" 
               href="#" onclick="studentsManagement.goToPage(${data.currentPage + 1})">
                <i class="fas fa-chevron-right"></i>
            </a>
        `;
        pagination.appendChild(nextButton);
    }

    goToPage(page) {
        if (page >= 0) {
            this.currentPage = page;
            this.loadStudents();
        }
    }

    getStudentInitials(firstName, lastName) {
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

    getStatus(enrollmentDate) {
        if (!enrollmentDate) return 'Inactive';
        
        const enrollment = new Date(enrollmentDate);
        const oneYearAgo = new Date();
        oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
        
        return enrollment >= oneYearAgo ? 'Active' : 'Inactive';
    }

    getStatusClass(enrollmentDate) {
        return this.getStatus(enrollmentDate) === 'Active' ? 'status-active' : 'status-inactive';
    }

    openAddStudentModal() {
        this.currentStudentId = null;
        document.getElementById('studentModalLabel').textContent = 'Add New Student';
        document.getElementById('studentForm').reset();
        
        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('studentModal'));
        modal.show();
    }

    async editStudent(studentId) {
        try {
            const response = await fetch(`/admin/students/api/${studentId}`);
            if (response.ok) {
                const student = await response.json();
                this.currentStudentId = studentId;
                
                // Populate form
                document.getElementById('studentModalLabel').textContent = 'Edit Student';
                document.getElementById('studentId').value = student.userId;
                document.getElementById('firstName').value = student.user.firstName;
                document.getElementById('lastName').value = student.user.lastName;
                document.getElementById('username').value = student.user.username;
                document.getElementById('email').value = student.user.email;
                document.getElementById('phone').value = student.user.phone || '';
                document.getElementById('address').value = student.user.address || '';
                document.getElementById('gradeLevel').value = student.gradeLevel || '';
                document.getElementById('emergencyContactPhone').value = student.emergencyContactPhone || '';
                
                // Hide password field for edit
                document.getElementById('password').parentElement.style.display = 'none';
                
                // Show modal
                const modal = new bootstrap.Modal(document.getElementById('studentModal'));
                modal.show();
            } else {
                this.showError('Failed to load student details');
            }
        } catch (error) {
            console.error('Error loading student:', error);
            this.showError('Error loading student details');
        }
    }

    async saveStudent() {
        const form = document.getElementById('studentForm');
        const formData = new FormData(form);
        
        const studentData = {
            firstName: formData.get('firstName'),
            lastName: formData.get('lastName'),
            username: formData.get('username'),
            email: formData.get('email'),
            phone: formData.get('phone'),
            address: formData.get('address'),
            gradeLevel: formData.get('gradeLevel'),
            emergencyContactPhone: formData.get('emergencyContactPhone')
        };
        
        // Add password only if it's provided (for new students)
        const password = formData.get('password');
        if (password && password.trim() !== '') {
            studentData.password = password;
        }
        
        try {
            const url = this.currentStudentId ? 
                `/admin/students/api/${this.currentStudentId}` : 
                '/admin/students/api';
            const method = this.currentStudentId ? 'PUT' : 'POST';
            
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(studentData)
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('studentModal')).hide();
                this.loadStudents();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error saving student:', error);
            this.showError('Error saving student');
        }
    }

    deleteStudent(studentId, studentName) {
        this.currentStudentId = studentId;
        document.getElementById('deleteStudentInfo').innerHTML = `
            <strong>Student:</strong> ${studentName}<br>
            <strong>ID:</strong> ${studentId}
        `;
        
        const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
        modal.show();
    }

    async confirmDelete() {
        try {
            const response = await fetch(`/admin/students/api/${this.currentStudentId}`, {
                method: 'DELETE'
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
                this.loadStudents();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error deleting student:', error);
            this.showError('Error deleting student');
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
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
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
function openAddStudentModal() {
    studentsManagement.openAddStudentModal();
}

function saveStudent() {
    studentsManagement.saveStudent();
}

function confirmDelete() {
    studentsManagement.confirmDelete();
}

// Initialize when DOM is loaded
let studentsManagement;
document.addEventListener('DOMContentLoaded', function() {
    studentsManagement = new StudentsManagement();
});
