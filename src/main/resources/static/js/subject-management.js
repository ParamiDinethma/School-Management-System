class SubjectManagement {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.sortBy = 'id';
        this.sortDir = 'asc';
        this.searchTerm = '';
        this.currentSubjectId = null;
        this.subjectToDelete = null;
        
        this.init();
    }

    init() {
        this.loadSubjects();
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
                this.loadSubjects();
            });
        }

        // Sort functionality
        const sortSelect = document.getElementById('sortBy');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                this.sortBy = e.target.value;
                this.currentPage = 0;
                this.loadSubjects();
            });
        }
    }

    async loadSubjects() {
        try {
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.sortBy,
                sortDir: this.sortDir,
                search: this.searchTerm
            });

            console.log('Loading subjects with params:', params.toString());
            const response = await fetch(`/admin/subjects/api?${params}`);
            console.log('Response status:', response.status);
            
            const data = await response.json();
            console.log('Response data:', data);

            if (response.ok) {
                console.log('Full API response:', data);
                console.log('Subjects received:', data.subjects);
                console.log('Subjects type:', typeof data.subjects);
                console.log('Subjects length:', data.subjects ? data.subjects.length : 'undefined');
                console.log('Total items:', data.totalItems);
                
                // Check if subjects is an array
                if (Array.isArray(data.subjects)) {
                    this.renderSubjectsTable(data.subjects);
                } else {
                    console.error('Subjects is not an array:', data.subjects);
                    this.showError('Invalid data format received from server');
                }
                
                this.renderPagination(data);
            } else {
                console.error('API Error:', data);
                this.showError('Failed to load subjects: ' + data.message);
            }
        } catch (error) {
            console.error('Error loading subjects:', error);
            this.showError('Failed to load subjects');
        }
    }

    async loadStatistics() {
        try {
            const response = await fetch('/admin/subjects/api/statistics');
            const stats = await response.json();

            if (response.ok) {
                document.getElementById('totalSubjects').textContent = stats.total;
                document.getElementById('activeSubjects').textContent = stats.active;
                document.getElementById('inactiveSubjects').textContent = stats.inactive;
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    renderSubjectsTable(subjects) {
        const tbody = document.getElementById('subjectsTableBody');

        if (subjects.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="no-data">
                        <i class="fas fa-book-open"></i>
                        <br>
                        No subjects found
                    </td>
                </tr>
            `;
            return;
        }

        let tableHTML = '';
        subjects.forEach((subject, index) => {
            console.log(`Processing subject ${index + 1}:`, subject);
            
            // Escape subject name for onclick handler
            const escapedSubjectName = subject.subjectName.replace(/'/g, "\\'");
            
            tableHTML += `
                <tr>
                    <td>
                        <div class="subject-info">
                            <div class="subject-avatar">
                                ${this.getSubjectInitials(subject.subjectName)}
                            </div>
                            <div class="subject-details">
                                <h6>${subject.subjectName}</h6>
                            </div>
                        </div>
                    </td>
                    <td>
                        <span class="badge badge-secondary">${subject.subjectCode}</span>
                    </td>
                    <td>
                        <span>${subject.description || 'No description'}</span>
                    </td>
                    <td>
                        <span class="badge ${subject.isActive ? 'badge-active' : 'badge-inactive'}">
                            ${subject.isActive ? 'Active' : 'Inactive'}
                        </span>
                    </td>
                    <td>
                        <button class="btn-action btn-edit" onclick="subjectManagement.editSubject(${subject.id})" title="Edit">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                        <button class="btn-action btn-delete" onclick="subjectManagement.deleteSubject(${subject.id}, '${escapedSubjectName}')" title="Delete">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </td>
                </tr>
            `;
        });
        
        console.log('Generated table HTML:', tableHTML);
        tbody.innerHTML = tableHTML;
    }

    renderPagination(data) {
        const paginationInfo = document.getElementById('paginationInfo');
        const pagination = document.getElementById('pagination');

        // Update pagination info
        const startItem = this.currentPage * this.pageSize + 1;
        const endItem = Math.min((this.currentPage + 1) * this.pageSize, data.totalItems);
        paginationInfo.textContent = `Showing ${startItem}-${endItem} of ${data.totalItems} subjects`;

        // Generate pagination buttons
        let paginationHTML = '';

        // Previous button
        paginationHTML += `
            <li class="page-item ${!data.hasPrevious ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="subjectManagement.goToPage(${this.currentPage - 1})">Previous</a>
            </li>
        `;

        // Page numbers
        const totalPages = data.totalPages;
        const startPage = Math.max(0, this.currentPage - 2);
        const endPage = Math.min(totalPages - 1, this.currentPage + 2);

        if (startPage > 0) {
            paginationHTML += `<li class="page-item"><a class="page-link" href="#" onclick="subjectManagement.goToPage(0)">1</a></li>`;
            if (startPage > 1) {
                paginationHTML += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `
                <li class="page-item ${i === this.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="subjectManagement.goToPage(${i})">${i + 1}</a>
                </li>
            `;
        }

        if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) {
                paginationHTML += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
            paginationHTML += `<li class="page-item"><a class="page-link" href="#" onclick="subjectManagement.goToPage(${totalPages - 1})">${totalPages}</a></li>`;
        }

        // Next button
        paginationHTML += `
            <li class="page-item ${!data.hasNext ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="subjectManagement.goToPage(${this.currentPage + 1})">Next</a>
            </li>
        `;

        pagination.innerHTML = paginationHTML;
    }

    goToPage(page) {
        this.currentPage = page;
        this.loadSubjects();
    }

    getSubjectInitials(subjectName) {
        return subjectName.split(' ')
            .map(word => word.charAt(0))
            .join('')
            .toUpperCase()
            .substring(0, 2);
    }

    openAddSubjectModal() {
        this.currentSubjectId = null;
        document.getElementById('subjectModalLabel').textContent = 'Add New Subject';
        document.getElementById('subjectForm').reset();
        
        // Set default values
        document.getElementById('isActive').value = 'true';
        
        const modal = new bootstrap.Modal(document.getElementById('subjectModal'));
        modal.show();
    }

    editSubject(subjectId) {
        this.currentSubjectId = subjectId;
        
        // Fetch subject data
        fetch(`/admin/subjects/api/${subjectId}`)
            .then(response => response.json())
            .then(subject => {
                document.getElementById('subjectModalLabel').textContent = 'Edit Subject';
                document.getElementById('subjectName').value = subject.subjectName;
                document.getElementById('subjectCode').value = subject.subjectCode;
                document.getElementById('description').value = subject.description || '';
                document.getElementById('isActive').value = subject.isActive ? 'true' : 'false';
                
                const modal = new bootstrap.Modal(document.getElementById('subjectModal'));
                modal.show();
            })
            .catch(error => {
                console.error('Error fetching subject:', error);
                this.showError('Failed to load subject data');
            });
    }

    async saveSubject() {
        const form = document.getElementById('subjectForm');
        const formData = new FormData(form);

        const subjectData = {
            subjectName: formData.get('subjectName'),
            subjectCode: formData.get('subjectCode'),
            description: formData.get('description'),
            isActive: formData.get('isActive') === 'true'
        };

        try {
            const url = this.currentSubjectId ? 
                `/admin/subjects/api/${this.currentSubjectId}` : 
                '/admin/subjects/api';
            const method = this.currentSubjectId ? 'PUT' : 'POST';

            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(subjectData)
            });

            const result = await response.json();

            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('subjectModal')).hide();
                this.loadSubjects();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error saving subject:', error);
            this.showError('Error saving subject');
        }
    }

    deleteSubject(subjectId, subjectName) {
        this.subjectToDelete = { id: subjectId, name: subjectName };
        document.getElementById('deleteSubjectName').textContent = subjectName;
        
        const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
        modal.show();
    }

    async confirmDelete() {
        if (!this.subjectToDelete) return;

        try {
            console.log('Attempting to delete subject with ID:', this.subjectToDelete.id);
            
            const response = await fetch(`/admin/subjects/api/${this.subjectToDelete.id}`, {
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
                this.loadSubjects();
                this.loadStatistics();
            } else {
                this.showError(result.message || 'Failed to delete subject');
            }
        } catch (error) {
            console.error('Error deleting subject:', error);
            this.showError('Error deleting subject: ' + error.message);
        }

        this.subjectToDelete = null;
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

// Initialize subject management when page loads
let subjectManagement;
document.addEventListener('DOMContentLoaded', function() {
    subjectManagement = new SubjectManagement();
});

// Global functions for onclick handlers
function openAddSubjectModal() {
    subjectManagement.openAddSubjectModal();
}

function saveSubject() {
    subjectManagement.saveSubject();
}

function confirmDelete() {
    subjectManagement.confirmDelete();
}
