class EnrollmentManagement {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.sortBy = 'enrollmentDate';
        this.sortDir = 'desc';
        this.searchTerm = '';
        this.statusFilter = '';
        this.courseFilter = '';
        this.currentEnrollmentId = null;
        this.isEditMode = false;
        
        this.init();
    }

    init() {
        this.loadStatistics();
        this.loadEnrollments();
        this.loadCourses();
        this.loadStudents();
        
        // Set up event listeners
        document.getElementById('searchInput')?.addEventListener('input', this.debounce(() => {
            this.searchTerm = document.getElementById('searchInput').value;
            this.currentPage = 0;
            this.loadEnrollments();
        }, 300));

        document.getElementById('statusFilter')?.addEventListener('change', (e) => {
            this.statusFilter = e.target.value;
            this.currentPage = 0;
            this.loadEnrollments();
        });

        document.getElementById('courseFilter')?.addEventListener('change', (e) => {
            this.courseFilter = e.target.value;
            this.currentPage = 0;
            this.loadEnrollments();
        });

        document.getElementById('sortBy')?.addEventListener('change', (e) => {
            this.sortBy = e.target.value;
            this.loadEnrollments();
        });
    }

    async loadStatistics() {
        try {
            const response = await fetch('/admin/enrollments/api/statistics');
            if (response.ok) {
                const stats = await response.json();
                
                document.getElementById('totalEnrollments').textContent = stats.total || 0;
                document.getElementById('activeEnrollments').textContent = stats.active || 0;
                document.getElementById('completedEnrollments').textContent = stats.completed || 0;
                document.getElementById('droppedEnrollments').textContent = stats.dropped || 0;
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    async loadEnrollments() {
        try {
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.sortBy,
                sortDir: this.sortDir
            });

            if (this.searchTerm) params.append('search', this.searchTerm);
            if (this.statusFilter) params.append('status', this.statusFilter);
            if (this.courseFilter) params.append('courseId', this.courseFilter);

            const response = await fetch(`/admin/enrollments/api?${params}`);
            const data = await response.json();

            if (response.ok) {
                this.renderEnrollmentsTable(data.enrollments);
                this.renderPagination(data);
            } else {
                this.showError('Failed to load enrollments: ' + data.message);
            }
        } catch (error) {
            console.error('Error loading enrollments:', error);
            this.showError('Failed to load enrollments');
        }
    }

    async loadCourses() {
        try {
            const response = await fetch('/admin/enrollments/api/courses');
            if (response.ok) {
                const courses = await response.json();
                const courseFilter = document.getElementById('courseFilter');
                const courseSelect = document.getElementById('courseId');
                
                // Populate filter dropdown
                courseFilter.innerHTML = '<option value="">All Courses</option>' +
                    courses.map(course => `<option value="${course.id}">${course.courseName}</option>`).join('');
                
                // Populate modal dropdown
                if (courseSelect) {
                    courseSelect.innerHTML = '<option value="">Select Course</option>' +
                        courses.map(course => `<option value="${course.id}">${course.courseName}</option>`).join('');
                }
            }
        } catch (error) {
            console.error('Error loading courses:', error);
        }
    }

    async loadStudents() {
        try {
            const response = await fetch('/admin/enrollments/api/students');
            if (response.ok) {
                const students = await response.json();
                const studentSelect = document.getElementById('studentId');
                
                if (studentSelect) {
                    studentSelect.innerHTML = '<option value="">Select Student</option>' +
                        students.map(student => `<option value="${student.id}">${student.firstName} ${student.lastName} (${student.username})</option>`).join('');
                }
            }
        } catch (error) {
            console.error('Error loading students:', error);
        }
    }

    renderEnrollmentsTable(enrollments) {
        const tbody = document.getElementById('enrollmentsTableBody');

        if (enrollments.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="no-data">
                        <i class="fas fa-user-plus"></i>
                        <br>
                        No enrollments found
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = enrollments.map(enrollment => `
            <tr>
                <td>
                    <div class="student-info">
                        <div class="student-avatar">
                            ${this.getStudentInitials(enrollment.studentName)}
                        </div>
                        <div class="student-details">
                            <h6>${enrollment.studentName}</h6>
                            <small>${enrollment.studentUsername}</small>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="course-info">
                        <div class="course-avatar">
                            ${this.getCourseInitials(enrollment.courseName)}
                        </div>
                        <div class="course-details">
                            <h6>${enrollment.courseName}</h6>
                            <small>${enrollment.courseCode}</small>
                        </div>
                    </div>
                </td>
                <td>
                    <span>${this.formatDate(enrollment.enrollmentDate)}</span>
                </td>
                <td>
                    <span class="badge ${this.getStatusBadgeClass(enrollment.status)}">
                        ${enrollment.status}
                    </span>
                </td>
                <td>
                    ${enrollment.grade ? `<span class="grade-badge grade-${enrollment.grade.toLowerCase()}">${enrollment.grade}</span>` : '-'}
                </td>
                <td>
                    <button class="btn-action btn-edit" onclick="enrollmentManagement.editEnrollment(${enrollment.id})" title="Edit">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn-action btn-delete" onclick="enrollmentManagement.deleteEnrollment(${enrollment.id}, '${enrollment.studentName}', '${enrollment.courseName}')" title="Delete">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `).join('');
    }

    renderPagination(data) {
        const paginationInfo = document.getElementById('paginationInfo');
        const pagination = document.getElementById('pagination');

        if (paginationInfo) {
            const start = data.currentPage * data.pageSize + 1;
            const end = Math.min(start + data.pageSize - 1, data.totalItems);
            paginationInfo.textContent = `Showing ${start}-${end} of ${data.totalItems} enrollments`;
        }

        if (pagination) {
            let paginationHTML = '';
            
            if (data.hasPrevious) {
                paginationHTML += `<li class="page-item">
                    <a class="page-link" href="#" onclick="enrollmentManagement.goToPage(${data.currentPage - 1})">Previous</a>
                </li>`;
            }

            for (let i = Math.max(0, data.currentPage - 2); i <= Math.min(data.totalPages - 1, data.currentPage + 2); i++) {
                paginationHTML += `<li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="enrollmentManagement.goToPage(${i})">${i + 1}</a>
                </li>`;
            }

            if (data.hasNext) {
                paginationHTML += `<li class="page-item">
                    <a class="page-link" href="#" onclick="enrollmentManagement.goToPage(${data.currentPage + 1})">Next</a>
                </li>`;
            }

            pagination.innerHTML = paginationHTML;
        }
    }

    openAddEnrollmentModal() {
        this.isEditMode = false;
        this.currentEnrollmentId = null;
        
        document.getElementById('enrollmentModalLabel').textContent = 'Add New Enrollment';
        document.getElementById('enrollmentForm').reset();
        
        const modal = new bootstrap.Modal(document.getElementById('enrollmentModal'));
        modal.show();
    }

    async editEnrollment(enrollmentId) {
        try {
            const response = await fetch(`/admin/enrollments/api/${enrollmentId}`);
            if (response.ok) {
                const enrollment = await response.json();
                
                this.isEditMode = true;
                this.currentEnrollmentId = enrollmentId;
                
                document.getElementById('enrollmentModalLabel').textContent = 'Edit Enrollment';
                document.getElementById('studentId').value = enrollment.studentId;
                document.getElementById('courseId').value = enrollment.courseId;
                document.getElementById('status').value = enrollment.status;
                document.getElementById('grade').value = enrollment.grade || '';
                document.getElementById('remarks').value = enrollment.remarks || '';
                
                const modal = new bootstrap.Modal(document.getElementById('enrollmentModal'));
                modal.show();
            } else {
                this.showError('Failed to load enrollment details');
            }
        } catch (error) {
            console.error('Error loading enrollment:', error);
            this.showError('Failed to load enrollment details');
        }
    }

    async saveEnrollment() {
        const formData = {
            studentId: document.getElementById('studentId').value,
            courseId: document.getElementById('courseId').value,
            status: document.getElementById('status').value,
            grade: document.getElementById('grade').value,
            remarks: document.getElementById('remarks').value
        };

        if (!formData.studentId || !formData.courseId) {
            this.showError('Please fill in all required fields');
            return;
        }

        try {
            const url = this.isEditMode ? 
                `/admin/enrollments/api/${this.currentEnrollmentId}` : 
                '/admin/enrollments/api';
            
            const method = this.isEditMode ? 'PUT' : 'POST';

            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData)
            });

            const result = await response.json();

            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('enrollmentModal')).hide();
                this.loadEnrollments();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error saving enrollment:', error);
            this.showError('Error saving enrollment');
        }
    }

    deleteEnrollment(enrollmentId, studentName, courseName) {
        this.currentEnrollmentId = enrollmentId;
        document.getElementById('deleteEnrollmentInfo').textContent = `${studentName} - ${courseName}`;
        
        const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
        modal.show();
    }

    async confirmDelete() {
        if (!this.currentEnrollmentId) return;

        try {
            const response = await fetch(`/admin/enrollments/api/${this.currentEnrollmentId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const result = await response.json();

            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
                this.loadEnrollments();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error deleting enrollment:', error);
            this.showError('Error deleting enrollment');
        }

        this.currentEnrollmentId = null;
    }

    goToPage(page) {
        this.currentPage = page;
        this.loadEnrollments();
    }

    getStudentInitials(studentName) {
        if (!studentName) return '??';
        return studentName.split(' ').map(name => name.charAt(0)).join('').toUpperCase().substring(0, 2);
    }

    getCourseInitials(courseName) {
        if (!courseName) return '??';
        return courseName.split(' ').map(word => word.charAt(0)).join('').toUpperCase().substring(0, 2);
    }

    getStatusBadgeClass(status) {
        switch (status) {
            case 'ACTIVE': return 'badge-active';
            case 'COMPLETED': return 'badge-completed';
            case 'DROPPED': return 'badge-dropped';
            case 'SUSPENDED': return 'badge-suspended';
            default: return 'badge-secondary';
        }
    }

    formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString();
    }

    showSuccess(message) {
        // You can implement a toast notification here
        alert('Success: ' + message);
    }

    showError(message) {
        // You can implement a toast notification here
        alert('Error: ' + message);
    }

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
}

// Global functions for onclick handlers
function openAddEnrollmentModal() {
    enrollmentManagement.openAddEnrollmentModal();
}

function saveEnrollment() {
    enrollmentManagement.saveEnrollment();
}

function confirmDelete() {
    enrollmentManagement.confirmDelete();
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.enrollmentManagement = new EnrollmentManagement();
});
