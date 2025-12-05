// User Management JavaScript
class UserManagement {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.currentSort = 'id';
        this.currentSortDir = 'asc';
        this.currentSearch = '';
        this.currentRole = 'ALL';
        this.currentUserId = null;
        this.roles = [];
        
        this.init();
    }

    async init() {
        await this.loadRoles();
        await this.loadUsers();
        this.setupEventListeners();
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
                this.loadUsers();
            }, 500);
        });

        // Role filter
        document.getElementById('roleFilter').addEventListener('change', (e) => {
            this.currentRole = e.target.value;
            this.currentPage = 0;
            this.loadUsers();
        });

        // Table header clicks for sorting
        document.querySelectorAll('.users-table th[data-sort]').forEach(th => {
            th.addEventListener('click', () => {
                const sortField = th.getAttribute('data-sort');
                if (this.currentSort === sortField) {
                    this.currentSortDir = this.currentSortDir === 'asc' ? 'desc' : 'asc';
                } else {
                    this.currentSort = sortField;
                    this.currentSortDir = 'asc';
                }
                this.currentPage = 0;
                this.loadUsers();
            });
        });
    }

    async loadRoles() {
        try {
            const response = await fetch('/admin/users/api/roles');
            if (response.ok) {
                this.roles = await response.json();
                this.populateRoleSelect();
            }
        } catch (error) {
            console.error('Error loading roles:', error);
        }
    }

    populateRoleSelect() {
        const roleSelect = document.getElementById('role');
        roleSelect.innerHTML = '<option value="">Select a role</option>';
        
        this.roles.forEach(role => {
            const option = document.createElement('option');
            option.value = role.name;
            option.textContent = role.name;
            roleSelect.appendChild(option);
        });
    }

    async loadUsers() {
        try {
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.currentSort,
                sortDir: this.currentSortDir,
                search: this.currentSearch,
                role: this.currentRole
            });

            const response = await fetch(`/admin/users/api?${params}`);
            
            if (response.ok) {
                const data = await response.json();
                this.renderUsersTable(data.users);
                this.renderPagination(data);
            } else {
                this.showError('Failed to load users');
            }
        } catch (error) {
            console.error('Error loading users:', error);
            this.showError('Error loading users');
        }
    }

    renderUsersTable(users) {
        const tbody = document.getElementById('usersTableBody');
        
        if (users.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="no-data">
                        <i class="fas fa-users"></i>
                        <p>No users found</p>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = users.map(user => `
            <tr>
                <td>
                    <div class="user-info">
                        <div class="user-avatar">
                            ${this.getUserInitials(user.firstName, user.lastName)}
                        </div>
                        <div class="user-details">
                            <h6>${user.firstName} ${user.lastName}</h6>
                            <p>@${user.username}</p>
                        </div>
                    </div>
                </td>
                <td>${user.email || '-'}</td>
                <td>${user.phone || '-'}</td>
                <td>
                    <span class="role-badge role-${user.role.name.toLowerCase().replace('_', '-')}">
                        ${user.role.name}
                    </span>
                </td>
                <td>${this.formatDate(user.createdAt)}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn-action btn-edit" onclick="userManagement.editUser(${user.id})">
                            <i class="fas fa-edit"></i>
                            Edit
                        </button>
                        <button class="btn-action btn-delete" onclick="userManagement.deleteUser(${user.id}, '${user.firstName} ${user.lastName}')">
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
        paginationInfo.textContent = `Showing ${startItem}-${endItem} of ${data.totalItems} users`;
        
        // Clear existing pagination
        pagination.innerHTML = '';
        
        // Previous button
        const prevButton = document.createElement('li');
        prevButton.innerHTML = `
            <a class="page-link ${!data.hasPrevious ? 'disabled' : ''}" 
               href="#" onclick="userManagement.goToPage(${data.currentPage - 1})">
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
                   href="#" onclick="userManagement.goToPage(${i})">
                    ${i + 1}
                </a>
            `;
            pagination.appendChild(pageButton);
        }
        
        // Next button
        const nextButton = document.createElement('li');
        nextButton.innerHTML = `
            <a class="page-link ${!data.hasNext ? 'disabled' : ''}" 
               href="#" onclick="userManagement.goToPage(${data.currentPage + 1})">
                <i class="fas fa-chevron-right"></i>
            </a>
        `;
        pagination.appendChild(nextButton);
    }

    goToPage(page) {
        if (page >= 0) {
            this.currentPage = page;
            this.loadUsers();
        }
    }

    getUserInitials(firstName, lastName) {
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

    openAddUserModal() {
        this.currentUserId = null;
        document.getElementById('userModalLabel').textContent = 'Add New User';
        document.getElementById('userForm').reset();
        document.getElementById('password').required = true;
        
        // Hide all role-specific fields
        this.hideAllRoleSpecificFields();
        
        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('userModal'));
        modal.show();
    }

    async editUser(userId) {
        try {
            const response = await fetch(`/admin/users/api/${userId}`);
            if (response.ok) {
                const user = await response.json();
                this.currentUserId = userId;
                
                // Populate form
                document.getElementById('userModalLabel').textContent = 'Edit User';
                document.getElementById('userId').value = user.id;
                document.getElementById('firstName').value = user.firstName;
                document.getElementById('lastName').value = user.lastName;
                document.getElementById('username').value = user.username;
                document.getElementById('email').value = user.email;
                document.getElementById('phone').value = user.phone || '';
                document.getElementById('address').value = user.address || '';
                document.getElementById('role').value = user.role.name;
                document.getElementById('password').required = false;
                
                // Trigger role-specific fields toggle
                this.toggleRoleSpecificFields();
                
                // Show modal
                const modal = new bootstrap.Modal(document.getElementById('userModal'));
                modal.show();
            } else {
                this.showError('Failed to load user details');
            }
        } catch (error) {
            console.error('Error loading user:', error);
            this.showError('Error loading user details');
        }
    }

    async saveUser() {
        const form = document.getElementById('userForm');
        const formData = new FormData(form);
        
        const userData = {
            firstName: formData.get('firstName'),
            lastName: formData.get('lastName'),
            username: formData.get('username'),
            email: formData.get('email'),
            phone: formData.get('phone'),
            address: formData.get('address'),
            role: { name: formData.get('role') },
            // Role-specific fields
            gradeLevel: formData.get('gradeLevel'),
            emergencyContactPhone: formData.get('emergencyContactPhone'),
            specialization: formData.get('specialization'),
            department: formData.get('department'),
            occupation: formData.get('occupation')
        };
        
        // Add password only if it's provided
        const password = formData.get('password');
        if (password && password.trim() !== '') {
            userData.password = password;
        }
        
        try {
            const url = this.currentUserId ? 
                `/admin/users/api/${this.currentUserId}` : 
                '/admin/users/api';
            const method = this.currentUserId ? 'PUT' : 'POST';
            
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userData)
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('userModal')).hide();
                this.loadUsers();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error saving user:', error);
            this.showError('Error saving user');
        }
    }

    deleteUser(userId, userName) {
        this.currentUserId = userId;
        document.getElementById('deleteUserInfo').innerHTML = `
            <strong>User:</strong> ${userName}<br>
            <strong>ID:</strong> ${userId}
        `;
        
        const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
        modal.show();
    }

    async confirmDelete() {
        try {
            const response = await fetch(`/admin/users/api/${this.currentUserId}`, {
                method: 'DELETE'
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
                this.loadUsers();
            } else {
                this.showError(result.message);
            }
        } catch (error) {
            console.error('Error deleting user:', error);
            this.showError('Error deleting user');
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

    hideAllRoleSpecificFields() {
        document.getElementById('studentFields').style.display = 'none';
        document.getElementById('teacherFields').style.display = 'none';
        document.getElementById('parentFields').style.display = 'none';
    }

    toggleRoleSpecificFields() {
        const roleSelect = document.getElementById('role');
        const selectedRole = roleSelect.value;
        
        // Hide all role-specific fields first
        this.hideAllRoleSpecificFields();
        
        // Show relevant fields based on role
        switch(selectedRole) {
            case 'STUDENT':
                document.getElementById('studentFields').style.display = 'block';
                break;
            case 'TEACHER':
                document.getElementById('teacherFields').style.display = 'block';
                break;
            case 'PARENT':
                document.getElementById('parentFields').style.display = 'block';
                break;
            default:
                // For PRINCIPAL, STAFF, IT_ADMIN - no additional fields
                break;
        }
    }
}

// Global functions for onclick handlers
function openAddUserModal() {
    userManagement.openAddUserModal();
}

function saveUser() {
    userManagement.saveUser();
}

function toggleRoleSpecificFields() {
    userManagement.toggleRoleSpecificFields();
}

// Expose delete confirmation to modal button
function confirmDelete() {
    userManagement.confirmDelete();
}

// Initialize when DOM is loaded
let userManagement;
document.addEventListener('DOMContentLoaded', function() {
    userManagement = new UserManagement();
});
