class ChildDetail {
    constructor() {
        this.childUserId = window.childUserId;
        this.currentPage = 0;
        this.pageSize = 10;
        this.sortBy = 'attendanceDate';
        this.sortDir = 'desc';
        this.startDate = null;
        this.endDate = null;
        this.statusFilter = null;
        this.init();
    }

    init() {
        console.log('Initializing ChildDetail with childUserId:', this.childUserId);
        console.log('Type of childUserId:', typeof this.childUserId);
        console.log('Window.childUserId:', window.childUserId);
        
        if (!this.childUserId) {
            console.error('Child user ID not found');
            console.error('Available window properties:', Object.keys(window));
            this.showError('Child information not found');
            return;
        }

        this.loadStatistics();
        this.loadAttendance();
        this.loadGrades();
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Filter buttons
        document.getElementById('startDate')?.addEventListener('change', (e) => {
            this.startDate = e.target.value;
        });

        document.getElementById('endDate')?.addEventListener('change', (e) => {
            this.endDate = e.target.value;
        });

        document.getElementById('statusFilter')?.addEventListener('change', (e) => {
            this.statusFilter = e.target.value;
        });
    }

    async loadStatistics() {
        try {
            console.log('Loading statistics for child ID:', this.childUserId);
            const response = await fetch(`/parent/api/child/${this.childUserId}/attendance/statistics`);
            console.log('Statistics response status:', response.status);
            
            if (response.ok) {
                const data = await response.json();
                console.log('Received statistics data:', data);
                this.updateStatistics(data.statistics);
            } else {
                const errorText = await response.text();
                console.error('Failed to load statistics:', response.status, errorText);
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    updateStatistics(stats) {
        // Update statistics cards
        document.getElementById('totalDaysCount').textContent = stats.total || 0;
        document.getElementById('presentDaysCount').textContent = stats.present || 0;
        document.getElementById('absentDaysCount').textContent = stats.absent || 0;
        document.getElementById('attendancePercentage').textContent = `${stats.presentPercentage || 0}%`;
    }

    async loadAttendance() {
        try {
            console.log('Loading attendance for child ID:', this.childUserId);
            
            let url = `/parent/api/child/${this.childUserId}/attendance?page=${this.currentPage}&size=${this.pageSize}&sortBy=${this.sortBy}&sortDir=${this.sortDir}`;

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
                console.error('Failed to load attendance:', response.status, errorText);
                this.showError('Failed to load attendance: ' + response.status + ' - ' + errorText);
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
                    <td colspan="5" class="text-center py-4">
                        <div class="empty-state">
                            <i class="fas fa-calendar-times"></i>
                            <h5>No attendance records found</h5>
                            <p>No attendance records match your current filters.</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = data.content.map(attendance => {
            const statusClass = this.getStatusClass(attendance.status);
            const statusIcon = this.getStatusIcon(attendance.status);
            const subject = attendance.subject || {};
            const subjectName = subject.name || subject.subjectName || 'Subject';
            const subjectCode = subject.code || subject.subjectCode || '';
            
            return `
                <tr>
                    <td>${this.formatDate(attendance.attendanceDate)}</td>
                    <td>
                        <div class="subject-info">
                            <div class="subject-icon">
                                ${this.getSubjectIcon(subject)}
                            </div>
                            <div class="subject-details">
                                <h6>${subjectName}</h6>
                                <small>${subjectCode}</small>
                            </div>
                        </div>
                    </td>
                    <td>
                        <span class="status-badge ${statusClass}">
                            <i class="${statusIcon}"></i>
                            ${this.formatStatus(attendance.status)}
                        </span>
                    </td>
                    <td>${attendance.remarks || '-'}</td>
                    <td>
                        ${attendance.markedBy ? 
                            `${attendance.markedBy.firstName} ${attendance.markedBy.lastName}` : 
                            '-'
                        }
                    </td>
                </tr>
            `;
        }).join('');
    }

    updatePagination(data) {
        const pagination = document.getElementById('attendancePagination');
        
        if (data.totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        let paginationHTML = '';
        
        // Previous button
        paginationHTML += `
            <li class="page-item ${data.first ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="childDetail.goToPage(${data.currentPage - 1})">
                    <i class="fas fa-chevron-left"></i>
                </a>
            </li>
        `;

        // Page numbers
        const startPage = Math.max(0, data.currentPage - 2);
        const endPage = Math.min(data.totalPages - 1, data.currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="childDetail.goToPage(${i})">${i + 1}</a>
                </li>
            `;
        }

        // Next button
        paginationHTML += `
            <li class="page-item ${data.last ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="childDetail.goToPage(${data.currentPage + 1})">
                    <i class="fas fa-chevron-right"></i>
                </a>
            </li>
        `;

        pagination.innerHTML = paginationHTML;
    }

    goToPage(page) {
        if (page >= 0) {
            this.currentPage = page;
            this.loadAttendance();
        }
    }

    applyFilters() {
        this.currentPage = 0; // Reset to first page
        this.loadAttendance();
        this.showSuccess('Filters applied successfully');
    }

    clearFilters() {
        this.startDate = null;
        this.endDate = null;
        this.statusFilter = null;
        this.currentPage = 0;

        // Clear form inputs
        document.getElementById('startDate').value = '';
        document.getElementById('endDate').value = '';
        document.getElementById('statusFilter').value = '';

        this.loadAttendance();
        this.showInfo('Filters cleared');
    }

    toggleFilters() {
        const filtersCard = document.getElementById('filtersCard');
        if (filtersCard.style.display === 'none') {
            filtersCard.style.display = 'block';
        } else {
            filtersCard.style.display = 'none';
        }
    }

    exportAttendance() {
        // Simple CSV export functionality
        const table = document.querySelector('#attendanceTableBody');
        if (!table || table.children.length === 0) {
            this.showWarning('No data to export');
            return;
        }

        let csv = 'Date,Subject,Status,Remarks,Marked By\n';
        
        Array.from(table.children).forEach(row => {
            const cells = row.children;
            if (cells.length >= 5) {
                const date = cells[0].textContent.trim();
                const subject = cells[1].textContent.trim().replace(/\n/g, ' ');
                const status = cells[2].textContent.trim();
                const remarks = cells[3].textContent.trim();
                const markedBy = cells[4].textContent.trim();
                
                csv += `"${date}","${subject}","${status}","${remarks}","${markedBy}"\n`;
            }
        });

        // Download CSV
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `attendance_${this.childUserId}_${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        this.showSuccess('Attendance data exported successfully');
    }

    // Helper methods
    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    formatStatus(status) {
        const statusMap = {
            'PRESENT': 'Present',
            'ABSENT': 'Absent',
            'LATE': 'Late',
            'EXCUSED': 'Excused'
        };
        return statusMap[status] || status;
    }

    getStatusClass(status) {
        const classMap = {
            'PRESENT': 'status-present',
            'ABSENT': 'status-absent',
            'LATE': 'status-late',
            'EXCUSED': 'status-excused'
        };
        return classMap[status] || 'status-present';
    }

    getStatusIcon(status) {
        const iconMap = {
            'PRESENT': 'fas fa-check',
            'ABSENT': 'fas fa-times',
            'LATE': 'fas fa-clock',
            'EXCUSED': 'fas fa-user-check'
        };
        return iconMap[status] || 'fas fa-question';
    }

    getSubjectIcon(subject) {
        // Simple subject icon based on subject name
        const rawName = subject && (subject.name || subject.subjectName);
        if (!rawName) {
            return '<i class="fas fa-book text-secondary"></i>';
        }
        const name = String(rawName).toLowerCase();
        if (name.includes('math') || name.includes('mathematics')) {
            return '<i class="fas fa-calculator text-primary"></i>';
        } else if (name.includes('science')) {
            return '<i class="fas fa-flask text-success"></i>';
        } else if (name.includes('english') || name.includes('language')) {
            return '<i class="fas fa-book text-info"></i>';
        } else if (name.includes('computer') || name.includes('programming')) {
            return '<i class="fas fa-laptop-code text-warning"></i>';
        } else {
            return '<i class="fas fa-book text-secondary"></i>';
        }
    }

    // Alert methods
    showAlert(message, type = 'info') {
        const alertContainer = document.getElementById('alertContainer');
        const alertId = 'alert-' + Date.now();
        
        const alertHTML = `
            <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
        
        alertContainer.insertAdjacentHTML('beforeend', alertHTML);
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            const alertElement = document.getElementById(alertId);
            if (alertElement) {
                alertElement.remove();
            }
        }, 5000);
    }

    showSuccess(message) {
        this.showAlert(message, 'success');
    }

    showError(message) {
        this.showAlert(message, 'danger');
    }

    showWarning(message) {
        this.showAlert(message, 'warning');
    }

    showInfo(message) {
        this.showAlert(message, 'info');
    }

    // Grades functionality
    async loadGrades() {
        try {
            console.log('Loading grades for child ID:', this.childUserId);
            this.showGradesLoading();
            
            const response = await fetch(`/student/api/grades/${this.childUserId}`);
            console.log('Grades response status:', response.status);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            console.log('Grades response data:', data);
            
            if (data.success) {
                this.displayGrades(data.grades, data.gradesByExam, data.student);
            } else {
                throw new Error(data.message || 'Failed to load grades');
            }
        } catch (error) {
            console.error('Error loading grades:', error);
            this.showGradesError(error.message);
        }
    }

    showGradesLoading() {
        document.getElementById('gradesLoading').style.display = 'block';
        document.getElementById('gradesContent').style.display = 'none';
        document.getElementById('noGradesState').style.display = 'none';
        document.getElementById('gradesErrorState').style.display = 'none';
        document.getElementById('gradesSummary').style.display = 'none';
    }

    displayGrades(grades, gradesByExam, student) {
        if (!grades || grades.length === 0) {
            this.showNoGrades();
            return;
        }

        // Hide loading, show content
        document.getElementById('gradesLoading').style.display = 'none';
        document.getElementById('gradesContent').style.display = 'block';
        document.getElementById('noGradesState').style.display = 'none';
        document.getElementById('gradesErrorState').style.display = 'none';
        document.getElementById('gradesSummary').style.display = 'block';

        // Update summary
        this.updateGradesSummary(grades);

        // Generate grades content
        const gradesContent = document.getElementById('gradesContent');
        gradesContent.innerHTML = this.generateGradesHTML(gradesByExam);
    }

    updateGradesSummary(grades) {
        const totalGrades = grades.length;
        const passingGrades = grades.filter(grade => grade.letterGrade !== 'F').length;
        const excellentGrades = grades.filter(grade => grade.letterGrade === 'A').length;
        const averageGrade = grades.reduce((sum, grade) => sum + parseFloat(grade.gradePoint), 0) / totalGrades;

        document.getElementById('totalGrades').textContent = totalGrades;
        document.getElementById('passingGrades').textContent = passingGrades;
        document.getElementById('excellentGrades').textContent = excellentGrades;
        document.getElementById('averageGrade').textContent = averageGrade.toFixed(2);
    }

    generateGradesHTML(gradesByExam) {
        let html = '';
        
        for (const [examName, examGrades] of Object.entries(gradesByExam)) {
            html += `
                <div class="exam-section">
                    <div class="exam-header">
                        <h4>${examName}</h4>
                        <p>${(examGrades && examGrades.length && examGrades[0] && examGrades[0].examSchedule && examGrades[0].examSchedule.description) ? examGrades[0].examSchedule.description : 'No description available'}</p>
                    </div>
                    <table class="grades-table">
                        <thead>
                            <tr>
                                <th><i class="fas fa-book me-2"></i>Subject</th>
                                <th><i class="fas fa-calculator me-2"></i>Marks</th>
                                <th><i class="fas fa-percentage me-2"></i>Percentage</th>
                                <th><i class="fas fa-trophy me-2"></i>Grade</th>
                                <th><i class="fas fa-comment me-2"></i>Comments</th>
                                <th><i class="fas fa-star me-2"></i>Grade Point</th>
                            </tr>
                        </thead>
                        <tbody>
            `;
            
            examGrades.forEach(grade => {
                const subjectObj = grade.subject || {};
                const subjectName = subjectObj.subjectName || subjectObj.name || 'Subject';
                const subjectCode = subjectObj.subjectCode || subjectObj.code || '';
                html += `
                    <tr>
                        <td>
                            <strong>${subjectName}</strong>
                            <br>
                            <small class="text-muted">${subjectCode}</small>
                        </td>
                        <td class="marks-cell">${grade.marksObtained}/${grade.totalMarks}</td>
                        <td class="percentage-cell">${grade.percentage}%</td>
                        <td>
                            <span class="grade-badge grade-${grade.letterGrade.toLowerCase()}">${grade.letterGrade}</span>
                        </td>
                        <td class="comments-cell">${grade.comments || 'No comments'}</td>
                        <td><strong>${grade.gradePoint}</strong></td>
                    </tr>
                `;
            });
            
            html += `
                        </tbody>
                    </table>
                </div>
            `;
        }
        
        return html;
    }

    showNoGrades() {
        document.getElementById('gradesLoading').style.display = 'none';
        document.getElementById('gradesContent').style.display = 'none';
        document.getElementById('noGradesState').style.display = 'block';
        document.getElementById('gradesErrorState').style.display = 'none';
        document.getElementById('gradesSummary').style.display = 'none';
    }

    showGradesError(message) {
        document.getElementById('gradesLoading').style.display = 'none';
        document.getElementById('gradesContent').style.display = 'none';
        document.getElementById('noGradesState').style.display = 'none';
        document.getElementById('gradesErrorState').style.display = 'block';
        document.getElementById('gradesSummary').style.display = 'none';
        
        document.getElementById('gradesErrorMessage').textContent = message;
    }

    refreshGrades() {
        console.log('Refreshing grades...');
        this.loadGrades();
    }

    exportGrades() {
        console.log('Exporting grades...');
        // TODO: Implement grade export functionality
        this.showInfo('Grade export functionality will be available soon.');
    }
}

// Initialize when DOM is loaded
let childDetail;
document.addEventListener('DOMContentLoaded', function() {
    childDetail = new ChildDetail();
});