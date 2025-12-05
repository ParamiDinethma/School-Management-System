class CourseManagement {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.sortBy = 'id';
        this.sortDir = 'asc';
        this.searchTerm = '';
        this.currentCourseId = null;
        this.courseToDelete = null;
        this.availableSubjects = [];
        
        this.init();
    }

    init() {
        this.loadCourses();
        this.loadStatistics();
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Search functionality
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchTerm = e.target.value;
                this.currentPage = 0;
                this.loadCourses();
            });
        }

        // Sort functionality
        const sortSelect = document.getElementById('sortBy');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                this.sortBy = e.target.value;
                this.currentPage = 0;
                this.loadCourses();
            });
        }
    }

    async loadCourses() {
        try {
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.sortBy,
                sortDir: this.sortDir,
                search: this.searchTerm
            });

            const response = await fetch(`/admin/courses/api?${params}`);
            const data = await response.json();

            if (response.ok) {
                this.renderCoursesTable(data.courses);
                this.renderPagination(data);
            } else {
                this.showError('Failed to load courses: ' + data.message);
            }
        } catch (error) {
            console.error('Error loading courses:', error);
            this.showError('Failed to load courses');
        }
    }

    async loadStatistics() {
        try {
            const response = await fetch('/admin/courses/api/statistics');
            const stats = await response.json();

            if (response.ok) {
                document.getElementById('totalCourses').textContent = stats.total;
                document.getElementById('activeCourses').textContent = stats.active;
                document.getElementById('inactiveCourses').textContent = stats.inactive;
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    renderCoursesTable(courses) {
        const tbody = document.getElementById('coursesTableBody');

        if (courses.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="no-data">
                        <i class="fas fa-book-open"></i>
                        <br>
                        No courses found
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = courses.map(course => `
            <tr>
                <td>
                    <div class="course-info">
                        <div class="course-avatar">
                            ${this.getCourseInitials(course.courseName)}
                        </div>
                        <div class="course-details">
                            <h6>${course.courseName}</h6>
                            <small>${course.description || 'No description'}</small>
                        </div>
                    </div>
                </td>
                <td>
                    <span class="badge badge-secondary">${course.courseCode}</span>
                </td>
                <td>
                    <strong>${course.creditHours || 'N/A'}</strong>
                </td>
                <td>
                    ${course.durationMonths ? `${course.durationMonths} months` : 'N/A'}
                </td>
                <td>
                    <span class="badge ${course.isActive ? 'badge-active' : 'badge-inactive'}">
                        ${course.isActive ? 'Active' : 'Inactive'}
                    </span>
                </td>
                <td>
                    <button class="btn-action btn-edit" onclick="courseManagement.editCourse(${course.id})" title="Edit">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn-action btn-subjects" onclick="courseManagement.assignSubjects(${course.id}, '${course.courseName}')" title="Assign Subjects">
                        <i class="fas fa-plus"></i> Subjects
                    </button>
                    <button class="btn-action btn-delete" onclick="courseManagement.deleteCourse(${course.id}, '${course.courseName}')" title="Delete">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `).join('');
    }

    renderPagination(data) {
        const paginationInfo = document.getElementById('paginationInfo');
        const pagination = document.getElementById('pagination');

        // Update pagination info
        const startItem = this.currentPage * this.pageSize + 1;
        const endItem = Math.min((this.currentPage + 1) * this.pageSize, data.totalItems);
        paginationInfo.textContent = `Showing ${startItem}-${endItem} of ${data.totalItems} courses`;

        // Generate pagination buttons
        let paginationHTML = '';

        // Previous button
        paginationHTML += `
            <li class="page-item ${!data.hasPrevious ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="courseManagement.goToPage(${this.currentPage - 1})">Previous</a>
            </li>
        `;

        // Page numbers
        const totalPages = data.totalPages;
        const startPage = Math.max(0, this.currentPage - 2);
        const endPage = Math.min(totalPages - 1, this.currentPage + 2);

        if (startPage > 0) {
            paginationHTML += `<li class="page-item"><a class="page-link" href="#" onclick="courseManagement.goToPage(0)">1</a></li>`;
            if (startPage > 1) {
                paginationHTML += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `
                <li class="page-item ${i === this.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="courseManagement.goToPage(${i})">${i + 1}</a>
                </li>
            `;
        }

        if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) {
                paginationHTML += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
            paginationHTML += `<li class="page-item"><a class="page-link" href="#" onclick="courseManagement.goToPage(${totalPages - 1})">${totalPages}</a></li>`;
        }

        // Next button
        paginationHTML += `
            <li class="page-item ${!data.hasNext ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="courseManagement.goToPage(${this.currentPage + 1})">Next</a>
            </li>
        `;

        pagination.innerHTML = paginationHTML;
    }

    goToPage(page) {
        this.currentPage = page;
        this.loadCourses();
    }

    getCourseInitials(courseName) {
        return courseName.split(' ')
            .map(word => word.charAt(0))
            .join('')
            .toUpperCase()
            .substring(0, 2);
    }

    openAddCourseModal() {
        this.currentCourseId = null;
        document.getElementById('courseModalLabel').textContent = 'Add New Course';
        document.getElementById('courseForm').reset();
        
        // Set default values
        document.getElementById('isActive').value = 'true';
        
        const modal = new bootstrap.Modal(document.getElementById('courseModal'));
        modal.show();
    }

    editCourse(courseId) {
        this.currentCourseId = courseId;
        
        // Fetch course data
        fetch(`/admin/courses/api/${courseId}`)
            .then(response => response.json())
            .then(course => {
                document.getElementById('courseModalLabel').textContent = 'Edit Course';
                document.getElementById('courseName').value = course.courseName;
                document.getElementById('courseCode').value = course.courseCode;
                document.getElementById('description').value = course.description || '';
                document.getElementById('credits').value = course.creditHours || '';
                document.getElementById('durationMonths').value = course.durationMonths || '';
                document.getElementById('isActive').value = course.isActive ? 'true' : 'false';
                
                const modal = new bootstrap.Modal(document.getElementById('courseModal'));
                modal.show();
            })
            .catch(error => {
                console.error('Error fetching course:', error);
                this.showError('Failed to load course data');
            });
    }

    async saveCourse() {
        const form = document.getElementById('courseForm');
        const formData = new FormData(form);

        const courseData = {
            courseName: formData.get('courseName'),
            courseCode: formData.get('courseCode'),
            description: formData.get('description'),
            creditHours: formData.get('credits') ? parseInt(formData.get('credits')) : null,
            durationMonths: formData.get('durationMonths') ? parseInt(formData.get('durationMonths')) : null,
            isActive: formData.get('isActive') === 'true'
        };

        try {
            const url = this.currentCourseId ? 
                `/admin/courses/api/${this.currentCourseId}` : 
                '/admin/courses/api';
            const method = this.currentCourseId ? 'PUT' : 'POST';

            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(courseData)
            });

            const result = await response.json();

            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('courseModal')).hide();
                this.loadCourses();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error saving course:', error);
            this.showError('Error saving course');
        }
    }

    async assignSubjects(courseId, courseName) {
        this.currentCourseId = courseId;
        document.getElementById('assignSubjectsModalLabel').textContent = `Assign Subjects to ${courseName}`;
        
        // Load course subjects info
        try {
            const response = await fetch(`/admin/courses/api/${courseId}/subjects`);
            const subjects = await response.json();
            
            let subjectsHTML = '<h6>Current Subjects:</h6>';
            if (subjects.length > 0) {
                subjectsHTML += '<ul class="list-group mb-3">';
                subjects.forEach(subject => {
                    subjectsHTML += `<li class="list-group-item d-flex justify-content-between align-items-center">
                        ${subject.subjectName} (${subject.subjectCode})
                        <button class="btn btn-sm btn-outline-danger" onclick="courseManagement.removeSubjectFromCourse(${subject.id})">
                            <i class="fas fa-times"></i>
                        </button>
                    </li>`;
                });
                subjectsHTML += '</ul>';
            } else {
                subjectsHTML += '<p class="text-muted">No subjects assigned yet.</p>';
            }
            
            document.getElementById('courseSubjectsInfo').innerHTML = subjectsHTML;
            
            // Load available subjects
            await this.loadAvailableSubjects(courseId);
            
            const modal = new bootstrap.Modal(document.getElementById('assignSubjectsModal'));
            modal.show();
        } catch (error) {
            console.error('Error loading course subjects:', error);
            this.showError('Failed to load course subjects');
        }
    }

    async loadAvailableSubjects(courseId) {
        try {
            const response = await fetch(`/admin/courses/api/${courseId}/available-subjects`);
            const subjects = await response.json();
            
            this.availableSubjects = subjects;
            const select = document.getElementById('availableSubjects');
            
            select.innerHTML = subjects.map(subject => 
                `<option value="${subject.id}">${subject.subjectName} (${subject.subjectCode}) - ${subject.credits || 'N/A'} credits</option>`
            ).join('');
        } catch (error) {
            console.error('Error loading available subjects:', error);
            this.showError('Failed to load available subjects');
        }
    }

    async assignSubjectToCourse() {
        const subjectId = document.getElementById('availableSubjects').value;
        const isMandatory = document.getElementById('isMandatory').value === 'true';
        const gradeLevel = document.getElementById('gradeLevel').value;

        if (!subjectId) {
            this.showError('Please select a subject');
            return;
        }

        if (!gradeLevel) {
            this.showError('Please select a grade level');
            return;
        }

        try {
            const response = await fetch(`/admin/courses/api/${this.currentCourseId}/subjects`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    subjectId: parseInt(subjectId),
                    isMandatory: isMandatory,
                    gradeLevel: gradeLevel
                })
            });

            const result = await response.json();

            if (result.success) {
                this.showSuccess(result.message);
                // Reload the modal content
                await this.assignSubjects(this.currentCourseId, 'Course');
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error assigning subject:', error);
            this.showError('Error assigning subject');
        }
    }

    async removeSubjectFromCourse(subjectId) {
        if (confirm('Are you sure you want to remove this subject from the course?')) {
            try {
                const response = await fetch(`/admin/courses/api/${this.currentCourseId}/subjects/${subjectId}`, {
                    method: 'DELETE'
                });

                const result = await response.json();

                if (result.success) {
                    this.showSuccess(result.message);
                    // Reload the modal content
                    await this.assignSubjects(this.currentCourseId, 'Course');
                } else {
                    this.showError(result.message);
                }
            } catch (error) {
                console.error('Error removing subject:', error);
                this.showError('Error removing subject');
            }
        }
    }

    deleteCourse(courseId, courseName) {
        this.courseToDelete = { id: courseId, name: courseName };
        document.getElementById('deleteCourseName').textContent = courseName;
        
        const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
        modal.show();
    }

    async confirmDelete() {
        if (!this.courseToDelete) return;

        try {
            console.log('Attempting to delete course with ID:', this.courseToDelete.id);
            
            const response = await fetch(`/admin/courses/api/${this.courseToDelete.id}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            console.log('Delete response status:', response.status);
            
            const result = await response.json();
            console.log('Delete response result:', result);

            if (response.ok && result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
                this.loadCourses();
                this.loadStatistics();
            } else {
                this.showError(result.message || 'Failed to delete course');
            }
        } catch (error) {
            console.error('Error deleting course:', error);
            this.showError('Error deleting course: ' + error.message);
        }

        this.courseToDelete = null;
    }

    showSuccess(message) {
        // Create and show success toast/alert
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-success alert-dismissible fade show position-fixed';
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

    showError(message) {
        // Create and show error toast/alert
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-danger alert-dismissible fade show position-fixed';
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

// Initialize course management when page loads
let courseManagement;
document.addEventListener('DOMContentLoaded', function() {
    courseManagement = new CourseManagement();
});

// Global functions for onclick handlers
function openAddCourseModal() {
    courseManagement.openAddCourseModal();
}

function saveCourse() {
    courseManagement.saveCourse();
}

function assignSubjectToCourse() {
    courseManagement.assignSubjectToCourse();
}

function confirmDelete() {
    courseManagement.confirmDelete();
}
