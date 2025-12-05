class StudentDashboard {
    constructor() {
        this.init();
    }

    init() {
        this.loadStudentProfile();
        this.loadStatistics();
        this.loadTopStudents();
        this.setupEventListeners();
    }

    async loadStudentProfile() {
        try {
            // Try to load current user profile
            const response = await fetch('/api/user/current');
            if (response.ok) {
                const user = await response.json();
                this.updateStudentProfile(user);
            } else {
                // Fallback to default values
                this.updateStudentProfile({
                    firstName: 'Student',
                    lastName: 'Name',
                    email: 'student@example.com',
                    phone: '(555) 123-4567',
                    address: '123 University St',
                    username: 'student123'
                });
            }
        } catch (error) {
            console.error('Error loading student profile:', error);
            // Use default values
            this.updateStudentProfile({
                firstName: 'Student',
                lastName: 'Name',
                email: 'student@example.com',
                phone: '(555) 123-4567',
                address: '123 University St',
                username: 'student123'
            });
        }
    }

    updateStudentProfile(user) {
        const fullName = `${user.firstName || 'Student'} ${user.lastName || 'Name'}`;
        const initials = this.getInitials(user.firstName, user.lastName);
        
        document.getElementById('studentName').textContent = fullName;
        document.getElementById('summaryStudentName').textContent = fullName;
        document.getElementById('studentAvatar').textContent = initials;
        document.getElementById('studentId').textContent = user.username || '2024-0001';
        document.getElementById('studentPhone').textContent = user.phone || '(555) 123-4567';
        document.getElementById('studentEmail').textContent = user.email || 'student@example.com';
        document.getElementById('studentAddress').textContent = user.address || '123 University St';
    }

    getInitials(firstName, lastName) {
        const first = firstName ? firstName.charAt(0).toUpperCase() : 'S';
        const last = lastName ? lastName.charAt(0).toUpperCase() : 'N';
        return first + last;
    }

    async loadStatistics() {
        try {
            // Use existing endpoint that returns current student's attendance stats
            const attRes = await fetch('/student/attendance/api/my-statistics');
            const attendanceStats = attRes.ok ? await attRes.json() : null;
            this.updateAttendanceStats({ attendanceStats, classDays: null });
        } catch (error) {
            console.error('Error loading statistics:', error);
            this.updateAttendanceStats({ attendanceStats: null, classDays: null });
        }
    }

    updateAttendanceStats({ attendanceStats, classDays }) {
        // Expected attendanceStats shape (preferred):
        // { total, present, late, undertime, absent, presentPercentage }
        // Fallbacks use 0 when missing.
        // Map from backend keys (see AttendanceServiceImpl.getAttendanceStatistics)
        const totalAttendance = attendanceStats?.totalDays ?? attendanceStats?.total ?? 0;
        const lateAttendance = attendanceStats?.lateCount ?? attendanceStats?.late ?? 0;
        const undertimeAttendance = attendanceStats?.excusedCount ?? attendanceStats?.undertime ?? 0; // fallback
        const totalAbsent = attendanceStats?.absentCount ?? attendanceStats?.absent ?? 0;
        const presentPercentage = attendanceStats?.attendancePercentage ?? attendanceStats?.presentPercentage ?? 0;

        const classDaysValue = classDays ?? attendanceStats?.totalDays ?? 0;

        // Overview cards
        const el = (id) => document.getElementById(id);
        if (el('totalAttendance')) el('totalAttendance').textContent = `${totalAttendance} Days`;
        if (el('lateAttendance')) el('lateAttendance').textContent = `${lateAttendance} Days`;
        if (el('undertimeAttendance')) el('undertimeAttendance').textContent = `${undertimeAttendance} Days`;
        if (el('totalAbsent')) el('totalAbsent').textContent = `${totalAbsent} Days`;

        // Class days
        if (el('classDays')) el('classDays').textContent = `${classDaysValue} Days`;

        // Rate number and donut CSS variable
        if (el('attendanceRate')) el('attendanceRate').textContent = `${presentPercentage}%`;
        const donut = document.getElementById('attendanceDonut');
        if (donut) donut.style.setProperty('--p', presentPercentage);

        // Summary bars
        if (el('summaryAttendance')) el('summaryAttendance').textContent = String(totalAttendance).padStart(2, '0');
        if (el('summaryLate')) el('summaryLate').textContent = String(lateAttendance).padStart(2, '0');
        if (el('summaryUndertime')) el('summaryUndertime').textContent = String(undertimeAttendance).padStart(2, '0');
        if (el('summaryAbsent')) el('summaryAbsent').textContent = String(totalAbsent).padStart(2, '0');
    }

    async loadTopStudents() {
        try {
            // Mock top students data
            const topStudents = [
                { number: 1, name: 'Alice Johnson', id: '2024-0001', progress: '95%' },
                { number: 2, name: 'Bob Smith', id: '2024-0002', progress: '92%' },
                { number: 3, name: 'Carol Davis', id: '2024-0003', progress: '89%' },
                { number: 4, name: 'David Wilson', id: '2024-0004', progress: '87%' },
                { number: 5, name: 'Eva Brown', id: '2024-0005', progress: '85%' }
            ];

            this.renderTopStudents(topStudents);
        } catch (error) {
            console.error('Error loading top students:', error);
            this.renderTopStudents([]);
        }
    }

    renderTopStudents(students) {
        const tbody = document.getElementById('topStudentsTableBody');
        
        if (students.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" style="text-align: center; color: #64748b; padding: 20px;">
                        No student data available
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = students.map(student => `
            <tr>
                <td>${student.number}</td>
                <td>${student.name}</td>
                <td>${student.id}</td>
                <td>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <div style="flex: 1; height: 8px; background: #e2e8f0; border-radius: 4px; overflow: hidden;">
                            <div style="height: 100%; background: #3b82f6; width: ${student.progress}; transition: width 0.3s ease;"></div>
                        </div>
                        <span style="font-size: 12px; color: #64748b; min-width: 30px;">${student.progress}</span>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    setupEventListeners() {
        // Rate toggle buttons
        const rateToggleBtns = document.querySelectorAll('.rate-toggle-btn');
        rateToggleBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                rateToggleBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                
                // Update attendance rate based on selection
                const isThisYear = btn.textContent === 'This Year';
                document.getElementById('attendanceRate').textContent = isThisYear ? '56%' : '78%';
            });
        });

        // Theme toggle
        const themeToggle = document.querySelector('.theme-toggle');
        if (themeToggle) {
            themeToggle.addEventListener('click', () => {
                document.body.classList.toggle('dark-theme');
            });
        }

        // Period selector
        const periodSelector = document.querySelector('.period-selector');
        if (periodSelector) {
            periodSelector.addEventListener('change', (e) => {
                console.log('Period changed to:', e.target.value);
                // Update data based on period selection
            });
        }

        // Download button
        const downloadBtn = document.querySelector('.download-btn');
        if (downloadBtn) {
            downloadBtn.addEventListener('click', () => {
                // Implement download functionality
                console.log('Download requested');
            });
        }

        // Search functionality
        const searchInput = document.querySelector('.search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                // Implement search functionality
                console.log('Search query:', e.target.value);
            });
        }
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    new StudentDashboard();
});
