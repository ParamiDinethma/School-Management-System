class StudentEnrollmentManager {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.sortBy = 'enrollmentDate';
        this.sortDir = 'desc';
        this.currentEnrollmentId = null;
        this.currentCourseId = null;
        
        this.init();
    }

    init() {
        this.loadStatistics();
        this.loadMyEnrollments();
        this.loadAvailableCourses();
        
        // Set up event listeners
        document.getElementById('searchInput')?.addEventListener('input', this.debounce(() => {
            this.searchTerm = document.getElementById('searchInput').value;
            this.currentPage = 0;
            this.loadMyEnrollments();
        }, 300));

        // Tab switching
        document.getElementById('my-enrollments-tab').addEventListener('click', () => {
            this.loadMyEnrollments();
        });

        document.getElementById('available-courses-tab').addEventListener('click', () => {
            this.loadAvailableCourses();
        });
    }

    async loadStatistics() {
        try {
            const response = await fetch('/student/enrollment/api/statistics');
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

    async loadMyEnrollments() {
        try {
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.sortBy,
                sortDir: this.sortDir
            });

            const response = await fetch(`/student/enrollment/api/my-enrollments?${params}`);
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

    async loadAvailableCourses() {
        try {
            const response = await fetch('/student/enrollment/api/available-courses');
            if (response.ok) {
                const courses = await response.json();
                this.renderAvailableCourses(courses);
            } else {
                document.getElementById('availableCoursesGrid').innerHTML = `
                    <div class="no-data">
                        <i class="fas fa-exclamation-triangle"></i>
                        <br>
                        Failed to load available courses
                    </div>
                `;
            }
        } catch (error) {
            console.error('Error loading available courses:', error);
            document.getElementById('availableCoursesGrid').innerHTML = `
                <div class="no-data">
                    <i class="fas fa-exclamation-triangle"></i>
                    <br>
                    Failed to load available courses
                </div>
            `;
        }
    }

    renderEnrollmentsTable(enrollments) {
        const tbody = document.getElementById('enrollmentsTableBody');

        if (enrollments.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="no-data">
                        <i class="fas fa-book-open"></i>
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
                    ${enrollment.status === 'ACTIVE' ? `
                        <button class="btn-action btn-unenroll" onclick="studentEnrollment.unenrollFromCourse(${enrollment.courseId}, '${enrollment.courseName}')" title="Unenroll">
                            <i class="fas fa-times"></i> Unenroll
                        </button>
                    ` : ''}
                </td>
            </tr>
        `).join('');
    }

    renderAvailableCourses(courses) {
        const grid = document.getElementById('availableCoursesGrid');

        if (courses.length === 0) {
            grid.innerHTML = `
                <div class="no-data">
                    <i class="fas fa-book-open"></i>
                    <br>
                    No available courses to enroll
                </div>
            `;
            return;
        }

        grid.innerHTML = courses.map(course => `
            <div class="course-card">
                <div class="course-card-header">
                    <h4 class="course-card-title">${course.courseName}</h4>
                    <p class="course-card-code">${course.courseCode}</p>
                </div>
                <div class="course-card-body">
                    <p class="course-description">${course.description || 'No description available'}</p>
                    <div class="course-meta">
                        <span><i class="fas fa-clock"></i> ${course.durationMonths || 'N/A'} months</span>
                        <span><i class="fas fa-credit-card"></i> ${course.creditHours || 'N/A'} credits</span>
                    </div>
                    <button class="btn-enroll-course" onclick="studentEnrollment.openEnrollModal(${course.id}, '${course.courseName}')">
                        <i class="fas fa-plus"></i> Enroll Now
                    </button>
                </div>
            </div>
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
                    <a class="page-link" href="#" onclick="studentEnrollment.goToPage(${data.currentPage - 1})">Previous</a>
                </li>`;
            }

            for (let i = Math.max(0, data.currentPage - 2); i <= Math.min(data.totalPages - 1, data.currentPage + 2); i++) {
                paginationHTML += `<li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="studentEnrollment.goToPage(${i})">${i + 1}</a>
                </li>`;
            }

            if (data.hasNext) {
                paginationHTML += `<li class="page-item">
                    <a class="page-link" href="#" onclick="studentEnrollment.goToPage(${data.currentPage + 1})">Next</a>
                </li>`;
            }

            pagination.innerHTML = paginationHTML;
        }
    }

    openEnrollModal(courseId, courseName) {
        this.currentCourseId = courseId;
        document.getElementById('enrollCourseId').value = courseId;
        document.getElementById('enrollRemarks').value = '';
        document.getElementById('enrollModalLabel').textContent = `Enroll in ${courseName}`;
        
        const modal = new bootstrap.Modal(document.getElementById('enrollModal'));
        modal.show();
    }

    async enrollInCourse() {
        const courseId = document.getElementById('enrollCourseId').value;
        const remarks = document.getElementById('enrollRemarks').value;

        if (!courseId) {
            this.showError('Please select a course');
            return;
        }

        try {
            const response = await fetch('/student/enrollment/api/enroll', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    courseId: parseInt(courseId),
                    remarks: remarks
                })
            });

            const result = await response.json();

            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('enrollModal')).hide();
                this.loadMyEnrollments();
                this.loadAvailableCourses();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error enrolling in course:', error);
            this.showError('Error enrolling in course');
        }
    }

    unenrollFromCourse(courseId, courseName) {
        this.currentCourseId = courseId;
        document.getElementById('unenrollCourseName').textContent = courseName;
        
        const modal = new bootstrap.Modal(document.getElementById('unenrollModal'));
        modal.show();
    }

    async confirmUnenroll() {
        if (!this.currentCourseId) return;

        try {
            const response = await fetch(`/student/enrollment/api/unenroll/${this.currentCourseId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const result = await response.json();

            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('unenrollModal')).hide();
                this.loadMyEnrollments();
                this.loadAvailableCourses();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error unenrolling from course:', error);
            this.showError('Error unenrolling from course');
        }

        this.currentCourseId = null;
    }

    showAvailableCourses() {
        const availableTab = new bootstrap.Tab(document.getElementById('available-courses-tab'));
        availableTab.show();
        this.loadAvailableCourses();
    }

    goToPage(page) {
        this.currentPage = page;
        this.loadMyEnrollments();
    }

    getCourseInitials(courseName) {
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
function enrollInCourse() {
    studentEnrollment.enrollInCourse();
}

function confirmUnenroll() {
    studentEnrollment.confirmUnenroll();
}

function showAvailableCourses() {
    studentEnrollment.showAvailableCourses();
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.studentEnrollment = new StudentEnrollmentManager();
});
