class StudentAttendance {
    constructor() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.sortBy = 'attendanceDate';
        this.sortDir = 'desc';
        this.startDate = null;
        this.endDate = null;
        this.statusFilter = '';
        
        this.init();
    }

    init() {
        // Load initial data
        this.loadStatistics();
        this.loadAttendance();
        
        // Set up event listeners
        document.getElementById('startDate')?.addEventListener('change', (e) => {
            this.startDate = e.target.value;
        });
        
        document.getElementById('endDate')?.addEventListener('change', (e) => {
            this.endDate = e.target.value;
        });
        
        document.getElementById('statusFilter')?.addEventListener('change', (e) => {
            this.statusFilter = e.target.value;
            this.currentPage = 0;
            this.loadAttendance();
        });
    }

    async loadStatistics() {
        try {
            const response = await fetch('/student/attendance/api/my-statistics');
            if (response.ok) {
                const stats = await response.json();
                this.updateStatisticsDisplay(stats);
            } else {
                console.error('Failed to load statistics');
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    updateStatisticsDisplay(stats) {
        document.getElementById('totalDays').textContent = stats.totalDays || 0;
        document.getElementById('presentDays').textContent = stats.presentCount || 0;
        document.getElementById('absentDays').textContent = stats.absentCount || 0;
        document.getElementById('attendancePercentage').textContent = 
            (stats.attendancePercentage || 0) + '%';
    }

    async loadAttendance() {
        try {
            let url = `/student/attendance/api/my-attendance?page=${this.currentPage}&size=${this.pageSize}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;
            
            // Add filters if they exist
            if (this.startDate) {
                url += `&startDate=${this.startDate}`;
            }
            if (this.endDate) {
                url += `&endDate=${this.endDate}`;
            }
            if (this.statusFilter) {
                url += `&status=${this.statusFilter}`;
            }
            
            console.log('Loading attendance from URL:', url);
            const response = await fetch(url);
            console.log('Response status:', response.status);
            
            if (response.ok) {
                const data = await response.json();
                console.log('Received attendance data:', data);
                this.renderAttendanceTable(data);
                this.updatePagination(data);
            } else {
                const errorText = await response.text();
                console.error('Failed to load attendance records:', response.status, errorText);
                this.showError('Failed to load attendance records: ' + response.status);
            }
        } catch (error) {
            console.error('Error loading attendance:', error);
            this.showError('Error loading attendance records: ' + error.message);
        }
    }

    renderAttendanceTable(data) {
        const tbody = document.getElementById('attendanceTableBody');
        
        if (!data.content || data.content.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="no-data">
                        <i class="fas fa-calendar-times"></i>
                        <div>No attendance records found</div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = '';
        
        data.content.forEach(record => {
            const row = document.createElement('tr');
            
            // Format the date
            const date = new Date(record.attendanceDate);
            const formattedDate = date.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            });
            
            // Get status badge class
            const statusClass = this.getStatusBadgeClass(record.status);
            
            row.innerHTML = `
                <td>
                    <div class="subject-info">
                        <div class="subject-avatar">
                            ${record.subjectCode ? record.subjectCode.substring(0, 2).toUpperCase() : 'SU'}
                        </div>
                        <div class="subject-details">
                            <h6>${record.subjectName || 'Unknown Subject'}</h6>
                            <small>${record.subjectCode || ''}</small>
                        </div>
                    </div>
                </td>
                <td>${formattedDate}</td>
                <td>
                    <span class="badge ${statusClass}">
                        ${this.formatStatus(record.status)}
                    </span>
                </td>
                <td>${record.remarks || '-'}</td>
            `;
            
            tbody.appendChild(row);
        });
    }

    getStatusBadgeClass(status) {
        switch (status) {
            case 'PRESENT':
                return 'badge-present';
            case 'ABSENT':
                return 'badge-absent';
            case 'LATE':
                return 'badge-late';
            case 'EXCUSED':
                return 'badge-excused';
            default:
                return 'badge-absent';
        }
    }

    formatStatus(status) {
        switch (status) {
            case 'PRESENT':
                return 'Present';
            case 'ABSENT':
                return 'Absent';
            case 'LATE':
                return 'Late';
            case 'EXCUSED':
                return 'Excused';
            default:
                return 'Unknown';
        }
    }

    updatePagination(data) {
        // Update pagination info
        const startRecord = (data.currentPage * data.size) + 1;
        const endRecord = Math.min((data.currentPage + 1) * data.size, data.totalElements);
        document.getElementById('paginationInfo').textContent = 
            `Showing ${startRecord}-${endRecord} of ${data.totalElements} records`;
        
        // Generate pagination buttons
        const pagination = document.getElementById('pagination');
        pagination.innerHTML = '';
        
        if (data.totalPages <= 1) {
            return;
        }
        
        // Previous button
        const prevLi = document.createElement('li');
        prevLi.className = `page-item ${data.first ? 'disabled' : ''}`;
        prevLi.innerHTML = `
            <a class="page-link" href="#" onclick="studentAttendance.goToPage(${data.currentPage - 1})">
                Previous
            </a>
        `;
        pagination.appendChild(prevLi);
        
        // Page numbers
        const startPage = Math.max(0, data.currentPage - 2);
        const endPage = Math.min(data.totalPages - 1, data.currentPage + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            const pageLi = document.createElement('li');
            pageLi.className = `page-item ${i === data.currentPage ? 'active' : ''}`;
            pageLi.innerHTML = `
                <a class="page-link" href="#" onclick="studentAttendance.goToPage(${i})">
                    ${i + 1}
                </a>
            `;
            pagination.appendChild(pageLi);
        }
        
        // Next button
        const nextLi = document.createElement('li');
        nextLi.className = `page-item ${data.last ? 'disabled' : ''}`;
        nextLi.innerHTML = `
            <a class="page-link" href="#" onclick="studentAttendance.goToPage(${data.currentPage + 1})">
                Next
            </a>
        `;
        pagination.appendChild(nextLi);
    }

    goToPage(page) {
        this.currentPage = page;
        this.loadAttendance();
    }

    showError(message) {
        // You can implement a toast notification or alert here
        console.error(message);
        // For now, we'll just log to console
    }
}

// Initialize the attendance system when the page loads
let studentAttendance;
document.addEventListener('DOMContentLoaded', function() {
    studentAttendance = new StudentAttendance();
});
