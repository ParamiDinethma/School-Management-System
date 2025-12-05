class StudentProfile {
    constructor() {
        this.init();
    }

    init() {
        this.loadProfile();
        this.loadStatistics();
        this.setupFormHandlers();
    }

    async loadProfile() {
        try {
            const response = await fetch('/api/user/profile');
            if (response.ok) {
                const user = await response.json();
                this.populateProfile(user);
            } else {
                // Fallback: load from authentication context
                this.loadProfileFromAuth();
            }
        } catch (error) {
            console.error('Error loading profile:', error);
            this.loadProfileFromAuth();
        }
    }

    async loadProfileFromAuth() {
        try {
            const response = await fetch('/api/user/current');
            if (response.ok) {
                const user = await response.json();
                this.populateProfile(user);
            }
        } catch (error) {
            console.error('Error loading profile from auth:', error);
            // Show default profile
            this.showDefaultProfile();
        }
    }

    populateProfile(user) {
        // Update profile card
        document.getElementById('profileName').textContent = `${user.firstName} ${user.lastName}`;
        document.getElementById('profileAvatar').innerHTML = this.getInitials(user.firstName, user.lastName);
        
        // Populate form
        document.getElementById('firstName').value = user.firstName || '';
        document.getElementById('lastName').value = user.lastName || '';
        document.getElementById('email').value = user.email || '';
        document.getElementById('phone').value = user.phone || '';
        document.getElementById('address').value = user.address || '';
        document.getElementById('username').value = user.username || '';
    }

    async loadStatistics() {
        try {
            const response = await fetch('/student/enrollment/api/statistics');
            if (response.ok) {
                const stats = await response.json();
                
                document.getElementById('totalEnrollments').textContent = stats.total || 0;
                document.getElementById('activeCourses').textContent = stats.active || 0;
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    setupFormHandlers() {
        const form = document.getElementById('profileForm');
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            this.saveProfile();
        });
    }

    async saveProfile() {
        const formData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            email: document.getElementById('email').value,
            phone: document.getElementById('phone').value,
            address: document.getElementById('address').value
        };

        try {
            const response = await fetch('/api/user/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                this.showSuccess('Profile updated successfully!');
                this.loadProfile(); // Reload to get updated data
            } else {
                const error = await response.json();
                this.showError(error.message || 'Failed to update profile');
            }
        } catch (error) {
            console.error('Error saving profile:', error);
            this.showError('Error updating profile');
        }
    }

    resetForm() {
        this.loadProfile(); // Reload original data
    }

    getInitials(firstName, lastName) {
        const first = firstName ? firstName.charAt(0).toUpperCase() : 'S';
        const last = lastName ? lastName.charAt(0).toUpperCase() : 'T';
        return first + last;
    }

    showDefaultProfile() {
        document.getElementById('profileName').textContent = 'Student User';
        document.getElementById('profileAvatar').innerHTML = '<i class="fas fa-user"></i>';
    }

    showSuccess(message) {
        // You can implement a toast notification here
        alert('Success: ' + message);
    }

    showError(message) {
        // You can implement a toast notification here
        alert('Error: ' + message);
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    new StudentProfile();
});
