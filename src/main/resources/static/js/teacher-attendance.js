class TeacherAttendance {
    constructor() {
        this.subjects = [];
        this.students = [];
        this.selectedSubjectId = null;
        this.selectedDate = null;
        this.teacherId = null;
        this.attendanceData = new Map(); // studentId -> {status, remarks, attendanceId?}
        
        this.init();
    }

    init() {
        // Set today's date as default
        const today = new Date().toISOString().split('T')[0];
        document.getElementById('dateSelect').value = today;
        this.selectedDate = today;
        
        // Load subjects
        this.loadSubjects();
        
        // Set up event listeners
        document.getElementById('subjectSelect').addEventListener('change', (e) => {
            this.selectedSubjectId = e.target.value;
            this.updateLoadButtonState();
        });
        
        document.getElementById('dateSelect').addEventListener('change', (e) => {
            this.selectedDate = e.target.value;
            this.updateLoadButtonState();
        });
    }

    async loadSubjects() {
        try {
            const response = await fetch('/teacher/attendance/api/subjects');
            if (response.ok) {
                this.subjects = await response.json();
                this.populateSubjectsDropdown();
            } else {
                this.showError('Failed to load subjects');
            }
        } catch (error) {
            console.error('Error loading subjects:', error);
            this.showError('Error loading subjects');
        }
    }

    populateSubjectsDropdown() {
        const select = document.getElementById('subjectSelect');
        select.innerHTML = '<option value="">Choose Subject...</option>';
        
        this.subjects.forEach(subject => {
            const option = document.createElement('option');
            option.value = subject.id;
            option.textContent = `${subject.subjectCode} - ${subject.subjectName}`;
            select.appendChild(option);
        });
    }

    updateLoadButtonState() {
        const loadButton = document.querySelector('.btn-primary');
        loadButton.disabled = !this.selectedSubjectId || !this.selectedDate;
    }

    async loadStudents() {
        if (!this.selectedSubjectId || !this.selectedDate) {
            this.showError('Please select a subject and date');
            return;
        }

        try {
            console.log('Loading students for subject:', this.selectedSubjectId);
            // Show loading state
            this.showLoading();
            
            // Load students for the selected subject
            const response = await fetch(`/teacher/attendance/api/subjects/${this.selectedSubjectId}/students`);
            console.log('Response status:', response.status);
            
            if (response.ok) {
                this.students = await response.json();
                console.log('Loaded students:', this.students);
                
                // Load existing attendance if any
                await this.loadExistingAttendance();
                
                // Render students table
                this.renderStudentsTable();
                
                // Show students container
                document.getElementById('studentsContainer').style.display = 'block';
                
            } else {
                const errorText = await response.text();
                console.error('Failed to load students:', response.status, errorText);
                this.showError('Failed to load students: ' + response.status);
            }
        } catch (error) {
            console.error('Error loading students:', error);
            this.showError('Error loading students: ' + error.message);
        }
    }

    async loadExistingAttendance() {
        try {
            const response = await fetch(`/teacher/attendance/api/subjects/${this.selectedSubjectId}/date/${this.selectedDate}`);
            if (response.ok) {
                const existingAttendance = await response.json();
                
                // Populate attendance data map
                existingAttendance.forEach(record => {
                    this.attendanceData.set(record.studentId, {
                        attendanceId: record.id,
                        status: record.status,
                        remarks: record.remarks || ''
                    });
                });
            }
        } catch (error) {
            console.error('Error loading existing attendance:', error);
        }
    }

    renderStudentsTable() {
        const tbody = document.getElementById('studentsTableBody');
        tbody.innerHTML = '';
        
        if (this.students.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="3" class="no-students">
                        <i class="fas fa-users-slash"></i>
                        <div>No students enrolled in this subject</div>
                    </td>
                </tr>
            `;
            return;
        }

        this.students.forEach(student => {
            const existingData = this.attendanceData.get(student.id);
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>
                    <div class="student-info">
                        <div class="student-avatar">
                            ${student.firstName.charAt(0)}${student.lastName.charAt(0)}
                        </div>
                        <div class="student-details">
                            <h6>${student.firstName} ${student.lastName}</h6>
                            <small>${student.username}</small>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="attendance-options">
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="status_${student.id}" 
                                   value="PRESENT" id="present_${student.id}" 
                                   ${existingData?.status === 'PRESENT' ? 'checked' : ''}>
                            <label class="form-check-label status-present" for="present_${student.id}">
                                Present
                            </label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="status_${student.id}" 
                                   value="ABSENT" id="absent_${student.id}"
                                   ${existingData?.status === 'ABSENT' ? 'checked' : ''}>
                            <label class="form-check-label status-absent" for="absent_${student.id}">
                                Absent
                            </label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="status_${student.id}" 
                                   value="LATE" id="late_${student.id}"
                                   ${existingData?.status === 'LATE' ? 'checked' : ''}>
                            <label class="form-check-label status-late" for="late_${student.id}">
                                Late
                            </label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="status_${student.id}" 
                                   value="EXCUSED" id="excused_${student.id}"
                                   ${existingData?.status === 'EXCUSED' ? 'checked' : ''}>
                            <label class="form-check-label status-excused" for="excused_${student.id}">
                                Excused
                            </label>
                        </div>
                    </div>
                </td>
                <td>
                    <input type="text" class="form-control remarks-input" 
                           placeholder="Optional remarks" 
                           value="${existingData?.remarks || ''}"
                           onchange="teacherAttendance.updateRemarks(${student.id}, this.value)">
                    ${existingData?.attendanceId ? `
                    <button class="btn btn-sm btn-outline-danger mt-2" onclick="teacherAttendance.clearAttendance(${student.id})">
                        <i class="fas fa-trash"></i> Clear
                    </button>` : ''}
                </td>
            `;
            tbody.appendChild(row);
        });

        // Update attendance count
        document.getElementById('attendanceCount').textContent = this.students.length;
        
        // Enable save button
        document.querySelector('.btn-save').disabled = false;
        
        // Add event listeners for radio buttons
        this.addAttendanceEventListeners();
    }

    addAttendanceEventListeners() {
        this.students.forEach(student => {
            const radioButtons = document.querySelectorAll(`input[name="status_${student.id}"]`);
            radioButtons.forEach(radio => {
                radio.addEventListener('change', (e) => {
                    this.updateAttendanceStatus(student.id, e.target.value);
                });
            });
        });
    }

    updateAttendanceStatus(studentId, status) {
        if (!this.attendanceData.has(studentId)) {
            this.attendanceData.set(studentId, { status: status, remarks: '' });
        } else {
            this.attendanceData.get(studentId).status = status;
        }
    }

    updateRemarks(studentId, remarks) {
        if (!this.attendanceData.has(studentId)) {
            this.attendanceData.set(studentId, { status: '', remarks: remarks });
        } else {
            this.attendanceData.get(studentId).remarks = remarks;
        }
    }

    async clearAttendance(studentId) {
        const existing = this.attendanceData.get(studentId);
        if (!existing || !existing.attendanceId) {
            // Nothing to clear
            return;
        }
        if (!confirm('Delete existing attendance for this student?')) return;
        try {
            const resp = await fetch(`/teacher/attendance/api/${existing.attendanceId}`, { method: 'DELETE' });
            if (resp.ok) {
                // Remove local state and uncheck radios
                this.attendanceData.delete(studentId);
                const radios = document.querySelectorAll(`input[name="status_${studentId}"]`);
                radios.forEach(r => r.checked = false);
                const input = document.querySelector(`.remarks-input[onchange="teacherAttendance.updateRemarks(${studentId}, this.value)"]`);
                if (input) input.value = '';
                this.showSuccess('Attendance cleared');
            } else {
                const t = await resp.text();
                this.showError('Failed to clear: ' + t);
            }
        } catch (e) {
            this.showError('Error clearing attendance: ' + e.message);
        }
    }

    markAllPresent() {
        this.students.forEach(student => {
            const radioButton = document.getElementById(`present_${student.id}`);
            if (radioButton) {
                radioButton.checked = true;
                this.updateAttendanceStatus(student.id, 'PRESENT');
            }
        });
    }

    markAllAbsent() {
        this.students.forEach(student => {
            const radioButton = document.getElementById(`absent_${student.id}`);
            if (radioButton) {
                radioButton.checked = true;
                this.updateAttendanceStatus(student.id, 'ABSENT');
            }
        });
    }

    markAllLate() {
        this.students.forEach(student => {
            const radioButton = document.getElementById(`late_${student.id}`);
            if (radioButton) {
                radioButton.checked = true;
                this.updateAttendanceStatus(student.id, 'LATE');
            }
        });
    }

    clearAll() {
        this.students.forEach(student => {
            const radioButtons = document.querySelectorAll(`input[name="status_${student.id}"]`);
            radioButtons.forEach(radio => {
                radio.checked = false;
            });
            this.attendanceData.delete(student.id);
        });
    }

    async saveAttendance() {
        if (this.attendanceData.size === 0) {
            this.showError('Please mark attendance for at least one student');
            return;
        }

        try {
            // Split into creations and updates
            const createArray = [];
            const updateArray = [];
            this.attendanceData.forEach((data, studentId) => {
                if (!data.status) return;
                const payload = {
                    studentId: studentId,
                    status: data.status,
                    remarks: data.remarks || ''
                };
                if (data.attendanceId) {
                    updateArray.push({ attendanceId: data.attendanceId, status: data.status, remarks: data.remarks || '' });
                } else {
                    createArray.push(payload);
                }
            });

            // Perform updates first
            if (updateArray.length > 0) {
                await Promise.all(updateArray.map(u => fetch(`/teacher/attendance/api/${u.attendanceId}` , {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ status: u.status, remarks: u.remarks })
                })));
            }

            // Then create new ones
            if (createArray.length > 0) {
                const requestData = {
                    subjectId: this.selectedSubjectId,
                    date: this.selectedDate,
                    teacherId: this.getTeacherId(),
                    attendance: createArray
                };
                const response = await fetch('/teacher/attendance/api/mark', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(requestData)
                });
                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error('Create failed: ' + errorText);
                }
            }

            this.showSuccess('Attendance saved successfully!');
            // Reload for fresh state
            await this.loadExistingAttendance();
            this.renderStudentsTable();

        } catch (error) {
            console.error('Error saving attendance:', error);
            this.showError('Error saving attendance: ' + error.message);
        }
    }

    getTeacherId() {
        // You might need to get this from the server or pass it from the template
        // For now, we'll assume it's available in a global variable or we'll need to modify the template
        return window.teacherId || null;
    }

    showLoading() {
        const tbody = document.getElementById('studentsTableBody');
        tbody.innerHTML = `
            <tr>
                <td colspan="3" class="loading">
                    <i class="fas fa-spinner fa-spin"></i>
                    <div>Loading students...</div>
                </td>
            </tr>
        `;
    }

    showError(message) {
        // You can implement a toast notification or alert here
        alert('Error: ' + message);
    }

    showSuccess(message) {
        // You can implement a toast notification or alert here
        alert('Success: ' + message);
    }
}

// Initialize the attendance system when the page loads
let teacherAttendance;
document.addEventListener('DOMContentLoaded', function() {
    teacherAttendance = new TeacherAttendance();
});
