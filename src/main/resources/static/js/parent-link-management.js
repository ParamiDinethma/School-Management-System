class ParentLinkManagement {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.searchTerm = '';
        this.searchType = '';
        this.deleteTarget = null;
        this.init();
    }

    init() {
        this.loadStatistics();
        this.loadLinks();
        this.loadStudentsAndParents();
        
        // Event listeners
        document.getElementById('searchInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.searchLinks();
            }
        });
    }

    async loadStatistics() {
        try {
            const response = await fetch('/admin/parent-links/api/statistics');
            if (response.ok) {
                const stats = await response.json();
                document.getElementById('totalLinksCount').textContent = stats.totalLinks || 0;
                document.getElementById('unlinkedStudentsCount').textContent = stats.unlinkedStudents || 0;
                document.getElementById('unlinkedParentsCount').textContent = stats.unlinkedParents || 0;
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    async loadLinks() {
        try {
            let url = `/admin/parent-links/api/links?page=${this.currentPage}&size=${this.pageSize}`;
            
            if (this.searchTerm) {
                url += `&search=${encodeURIComponent(this.searchTerm)}`;
            }
            if (this.searchType) {
                url += `&searchType=${this.searchType}`;
            }

            const response = await fetch(url);
            if (response.ok) {
                const data = await response.json();
                this.renderLinksTable(data);
                this.renderPagination(data);
            } else {
                this.showError('Failed to load parent links');
            }
        } catch (error) {
            console.error('Error loading links:', error);
            this.showError('Error loading parent links');
        }
    }

    renderLinksTable(data) {
        const tbody = document.getElementById('linksTableBody');
        
        if (!data.content || data.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No parent links found</td></tr>';
            return;
        }

        tbody.innerHTML = data.content.map(link => `
            <tr>
                <td>
                    <div class="d-flex align-items-center">
                        <div class="avatar-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-2">
                            ${link.student.user.firstName.charAt(0)}${link.student.user.lastName.charAt(0)}
                        </div>
                        <div>
                            <h6 class="mb-0">${link.student.user.firstName} ${link.student.user.lastName}</h6>
                            <small class="text-muted">${link.student.user.email}</small>
                        </div>
                    </div>
                </td>
                <td>${link.student.user.username}</td>
                <td>
                    <div class="d-flex align-items-center">
                        <div class="avatar-sm bg-success text-white rounded-circle d-flex align-items-center justify-content-center me-2">
                            ${link.parent.user.firstName.charAt(0)}${link.parent.user.lastName.charAt(0)}
                        </div>
                        <div>
                            <h6 class="mb-0">${link.parent.user.firstName} ${link.parent.user.lastName}</h6>
                            <small class="text-muted">${link.parent.user.email}</small>
                        </div>
                    </div>
                </td>
                <td>${link.parent.user.username}</td>
                <td>${new Date(link.createdAt).toLocaleDateString()}</td>
                <td>
                    <button class="btn btn-sm btn-outline-danger" 
                            onclick="parentLinkManagement.deleteLink(${link.student.userId}, ${link.parent.userId}, '${link.student.user.firstName} ${link.student.user.lastName}', '${link.parent.user.firstName} ${link.parent.user.lastName}')">
                        <i class="fas fa-unlink"></i> Unlink
                    </button>
                </td>
            </tr>
        `).join('');
    }

    renderPagination(data) {
        const pagination = document.getElementById('linksPagination');
        
        if (data.totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        let paginationHTML = '';
        
        // Previous button
        if (!data.first) {
            paginationHTML += `
                <li class="page-item">
                    <a class="page-link" href="#" onclick="parentLinkManagement.goToPage(${data.currentPage - 1})">Previous</a>
                </li>
            `;
        }

        // Page numbers
        const startPage = Math.max(0, data.currentPage - 2);
        const endPage = Math.min(data.totalPages - 1, data.currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="parentLinkManagement.goToPage(${i})">${i + 1}</a>
                </li>
            `;
        }

        // Next button
        if (!data.last) {
            paginationHTML += `
                <li class="page-item">
                    <a class="page-link" href="#" onclick="parentLinkManagement.goToPage(${data.currentPage + 1})">Next</a>
                </li>
            `;
        }

        pagination.innerHTML = paginationHTML;
    }

    async loadStudentsAndParents() {
        try {
            console.log('Loading students and parents...');
            
            // Load students
            const studentsResponse = await fetch('/admin/parent-links/api/students');
            console.log('Students response status:', studentsResponse.status);
            if (studentsResponse.ok) {
                const students = await studentsResponse.json();
                console.log('Students data:', students);
                const studentSelect = document.getElementById('studentSelect');
                studentSelect.innerHTML = '<option value="">Select a student</option>' +
                    students.map(student => 
                        `<option value="${student.id}">${student.firstName} ${student.lastName} (${student.username})</option>`
                    ).join('');
            } else {
                console.error('Failed to load students:', studentsResponse.status);
            }

            // Load parents
            const parentsResponse = await fetch('/admin/parent-links/api/parents');
            console.log('Parents response status:', parentsResponse.status);
            if (parentsResponse.ok) {
                const parents = await parentsResponse.json();
                console.log('Parents data:', parents);
                const parentSelect = document.getElementById('parentSelect');
                if (parents && parents.length > 0) {
                    parentSelect.innerHTML = '<option value="">Select a parent</option>' +
                        parents.map(parent => 
                            `<option value="${parent.id}">${parent.firstName} ${parent.lastName} (${parent.username})</option>`
                        ).join('');
                } else {
                    parentSelect.innerHTML = '<option value="">No parents found</option>';
                }
            } else {
                console.error('Failed to load parents:', parentsResponse.status);
                const parentSelect = document.getElementById('parentSelect');
                parentSelect.innerHTML = '<option value="">Error loading parents</option>';
            }
        } catch (error) {
            console.error('Error loading students and parents:', error);
            const parentSelect = document.getElementById('parentSelect');
            parentSelect.innerHTML = '<option value="">Error loading parents</option>';
        }
    }

    async saveLink() {
        const studentUserId = document.getElementById('studentSelect').value;
        const parentUserId = document.getElementById('parentSelect').value;

        if (!studentUserId || !parentUserId) {
            this.showError('Please select both a student and a parent');
            return;
        }

        try {
            const response = await fetch('/admin/parent-links/api/links', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    studentUserId: parseInt(studentUserId),
                    parentUserId: parseInt(parentUserId)
                })
            });

            const result = await response.json();
            
            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('addLinkModal')).hide();
                document.getElementById('addLinkForm').reset();
                this.loadLinks();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error saving link:', error);
            this.showError('Error creating parent link');
        }
    }

    deleteLink(studentUserId, parentUserId, studentName, parentName) {
        this.deleteTarget = { studentUserId, parentUserId };
        document.getElementById('deleteLinkInfo').innerHTML = `
            <strong>Student:</strong> ${studentName}<br>
            <strong>Parent:</strong> ${parentName}
        `;
        new bootstrap.Modal(document.getElementById('confirmDeleteModal')).show();
    }

    async confirmDelete() {
        if (!this.deleteTarget) return;

        try {
            const response = await fetch(`/admin/parent-links/api/links?studentUserId=${this.deleteTarget.studentUserId}&parentUserId=${this.deleteTarget.parentUserId}`, {
                method: 'DELETE'
            });

            const result = await response.json();
            
            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('confirmDeleteModal')).hide();
                this.loadLinks();
                this.loadStatistics();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error deleting link:', error);
            this.showError('Error deleting parent link');
        }

        this.deleteTarget = null;
    }

    searchLinks() {
        this.searchTerm = document.getElementById('searchInput').value.trim();
        this.searchType = document.getElementById('searchTypeSelect').value;
        this.currentPage = 0;
        this.loadLinks();
    }

    clearSearch() {
        document.getElementById('searchInput').value = '';
        document.getElementById('searchTypeSelect').value = '';
        this.searchTerm = '';
        this.searchType = '';
        this.currentPage = 0;
        this.loadLinks();
    }

    goToPage(page) {
        this.currentPage = page;
        this.loadLinks();
    }

    showAlert(message, type) {
        const alertContainer = document.getElementById('alertContainer');
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.role = 'alert';
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;
        alertContainer.appendChild(alertDiv);
        setTimeout(() => alertDiv.remove(), 5000);
    }

    showSuccess(message) { this.showAlert(message, 'success'); }
    showError(message) { this.showAlert(message, 'danger'); }
    showWarning(message) { this.showAlert(message, 'warning'); }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.parentLinkManagement = new ParentLinkManagement();
});
