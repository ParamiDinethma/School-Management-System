/**
 * Grade Entry JavaScript
 */
const gradeEntry = {
    selectedCourseId: null,
    selectedExamId: null,
    students: [],
    subjects: [],
    existingGrades: [],
    
    init() {
        this.setupEventListeners();
    },
    
    setupEventListeners() {
        // Auto-calculate grades when marks change
        document.addEventListener('input', (event) => {
            if (event.target.classList.contains('marks-input')) {
                this.calculateGrade(event.target);
            }
        });
    },
    
    async loadStudents() {
        const courseSelect = document.getElementById('courseSelect');
        const courseId = courseSelect.value;
        
        if (!courseId) {
            this.hideGradesTable();
            return;
        }
        
        this.selectedCourseId = courseId;
        
        try {
            this.showAlert('Loading students...', 'info');
            
            // Load students for the selected course
            const studentsResponse = await fetch(`/teacher/grades/api/course/${courseId}/students`);
            const studentsData = await studentsResponse.json();
            
            if (studentsData.success) {
                this.students = studentsData.students;
                
                // Load subjects for the selected course
                const subjectsResponse = await fetch(`/teacher/grades/api/course/${courseId}/subjects`);
                const subjectsData = await subjectsResponse.json();
                
                if (subjectsData.success) {
                    this.subjects = subjectsData.subjects;
                    console.log('Subjects loaded:', this.subjects); // Debug log
                    this.renderGradesTable();
                } else {
                    this.showAlert('Error loading subjects: ' + subjectsData.message, 'danger');
                }
            } else {
                this.showAlert('Error loading students: ' + studentsData.message, 'danger');
            }
        } catch (error) {
            console.error('Error loading students:', error);
            this.showAlert('Error loading students. Please try again.', 'danger');
        }
    },
    
    async loadExistingGrades() {
        const examSelect = document.getElementById('examSelect');
        const examId = examSelect.value;
        
        if (!examId || !this.selectedCourseId) {
            return;
        }
        
        this.selectedExamId = examId;
        
        try {
            const response = await fetch(`/teacher/grades/api/course/${this.selectedCourseId}/exam/${examId}/grades`);
            const data = await response.json();
            
            if (data.success) {
                this.existingGrades = data.grades;
                this.populateExistingGrades();
            } else {
                console.error('Error loading existing grades:', data.message);
            }
        } catch (error) {
            console.error('Error loading existing grades:', error);
        }
    },
    
    renderGradesTable() {
        if (this.students.length === 0) {
            this.showEmptyState('No students enrolled in this course.');
            return;
        }
        
        const tableBody = document.getElementById('gradesTableBody');
        const subjectHeader = document.getElementById('subjectHeader');
        
        const numSubjects = this.subjects.length;

        // Update main headers colspan
        document.getElementById('marksMainHeader').setAttribute('colspan', numSubjects > 0 ? numSubjects : 1);
        document.getElementById('gradesMainHeader').setAttribute('colspan', numSubjects > 0 ? numSubjects : 1);
        document.getElementById('commentsMainHeader').setAttribute('colspan', numSubjects > 0 ? numSubjects : 1);

        // Generate sub-headers for subjects
        const subHeaderRow = document.getElementById('subjectSubHeaders');
        subHeaderRow.innerHTML = ''; // Clear previous sub-headers

        if (numSubjects > 0) {
            // Sub-headers for Marks
            this.subjects.forEach(subject => {
                subHeaderRow.insertAdjacentHTML('beforeend', `<th style="text-align: center; font-size: 12px; min-width: 80px;">${subject.subjectName || subject.name || 'Marks'}</th>`);
            });
            // Sub-headers for Grades
            this.subjects.forEach(subject => {
                subHeaderRow.insertAdjacentHTML('beforeend', `<th style="text-align: center; font-size: 12px; min-width: 80px;">${subject.subjectName || subject.name || 'Grade'}</th>`);
            });
            // Sub-headers for Comments
            this.subjects.forEach(subject => {
                subHeaderRow.insertAdjacentHTML('beforeend', `<th style="text-align: center; font-size: 12px; min-width: 150px;">${subject.subjectName || subject.name || 'Comments'}</th>`);
            });
        } else {
            // Fallback for no subjects
            subHeaderRow.insertAdjacentHTML('beforeend', `<th style="text-align: center; font-size: 12px;">Marks</th>`);
            subHeaderRow.insertAdjacentHTML('beforeend', `<th style="text-align: center; font-size: 12px;">Grades</th>`);
            subHeaderRow.insertAdjacentHTML('beforeend', `<th style="text-align: center; font-size: 12px;">Comments</th>`);
        }
        
        // Generate table rows
        tableBody.innerHTML = this.students.map(student => {
            const studentInitials = this.getStudentInitials(student);
            
            // Create cells for each subject
            const subjectCells = this.subjects.map(subject => `
                <td class="subject-cell">
                    <input type="number" 
                           class="marks-input" 
                           placeholder="Enter marks" 
                           min="0" 
                           max="100"
                           step="0.01"
                           data-student-id="${student.id}"
                           data-subject-id="${subject.id}"
                           data-type="marks"
                           title="Enter marks for ${subject.subjectName || subject.name || 'Subject'}">
                </td>
            `).join('');
            
            // Create grade display cells for each subject
            const gradeCells = this.subjects.map(subject => `
                <td class="subject-cell">
                    <div class="grade-display" 
                         data-student-id="${student.id}"
                         data-subject-id="${subject.id}"
                         style="background: #f8f9fa; color: #6c757d; text-align: center; padding: 8px; border-radius: 4px;">
                        -
                    </div>
                </td>
            `).join('');
            
            // Create comment cells for each subject
            const commentCells = this.subjects.map(subject => `
                <td class="subject-cell">
                    <textarea class="comments-input" 
                              placeholder="Enter comments..."
                              rows="2"
                              data-student-id="${student.id}"
                              data-subject-id="${subject.id}"
                              title="Add comments for ${subject.subjectName || subject.name || 'Subject'}"></textarea>
                </td>
            `).join('');
            
            return `
                <tr>
                    <td>
                        <div class="student-info">
                            <div class="student-avatar">${studentInitials}</div>
                            <div class="student-details">
                                <div class="student-name">${student.firstName} ${student.lastName}</div>
                                <div class="student-id">ID: ${student.id}</div>
                            </div>
                        </div>
                    </td>
                    ${subjectCells}
                    ${gradeCells}
                    ${commentCells}
                </tr>
            `;
        }).join('');
        
        // Show the table
        this.showGradesTable();

        // If existing grades already loaded (exam selected), populate now
        if (this.existingGrades && this.existingGrades.length > 0) {
            this.populateExistingGrades();
        }
    },
    
    populateExistingGrades(retryCount = 0) {
        // Populate existing grades into the form
        if (!this.existingGrades || this.existingGrades.length === 0) return;
        let populated = 0;
        this.existingGrades.forEach(grade => {
            const studentId = grade.studentId || (grade.student && grade.student.id);
            const subjectId = grade.subjectId || (grade.subject && grade.subject.id);
            const marksInput = document.querySelector(
                `input[data-student-id="${studentId}"][data-subject-id="${subjectId}"][data-type="marks"]`
            );
            
            if (marksInput) {
                marksInput.value = grade.marksObtained || '';
                this.calculateGrade(marksInput);
                populated++;
            }
            
            const commentInput = document.querySelector(
                `textarea[data-student-id="${studentId}"][data-subject-id="${subjectId}"][class*="comments-input"]`
            );
            
            if (commentInput && grade.comments) {
                commentInput.value = grade.comments;
            }

            // Attach grade id to inputs for potential delete/update operations
            if (marksInput) {
                marksInput.dataset.gradeId = grade.id;
            }
            if (commentInput) {
                commentInput.dataset.gradeId = grade.id;
            }

            // Add a delete button in the same subject cell to clear this grade
            if (marksInput && grade.id) {
                const td = marksInput.closest('td');
                if (td && !td.querySelector('.grade-delete-btn')) {
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'btn btn-sm btn-outline-danger grade-delete-btn';
                    btn.style.marginTop = '6px';
                    btn.innerHTML = '<i class="fas fa-trash"></i>';
                    btn.title = 'Delete this grade';
                    btn.addEventListener('click', () => this.deleteGrade(grade.id, marksInput, commentInput));
                    td.appendChild(btn);
                }
            }
        });

        // If nothing populated yet (table may not be ready), retry a few times
        if (populated === 0 && retryCount < 5) {
            setTimeout(() => this.populateExistingGrades(retryCount + 1), 200);
        }
    },

    async deleteGrade(gradeId, marksInput, commentInput) {
        if (!confirm('Are you sure you want to delete this grade?')) return;
        try {
            const resp = await fetch(`/teacher/grades/api/${gradeId}`, { method: 'DELETE' });
            if (resp.ok) {
                // Clear UI
                if (marksInput) {
                    marksInput.value = '';
                    delete marksInput.dataset.gradeId;
                    this.calculateGrade(marksInput);
                }
                if (commentInput) {
                    commentInput.value = '';
                    delete commentInput.dataset.gradeId;
                }
                const btn = marksInput?.closest('td')?.querySelector('.grade-delete-btn');
                if (btn) btn.remove();
                this.showAlert('Grade deleted successfully.', 'success');
            } else {
                const t = await resp.text();
                this.showAlert('Failed to delete grade: ' + t, 'danger');
            }
        } catch (e) {
            this.showAlert('Error deleting grade: ' + e.message, 'danger');
        }
    },
    
    calculateGrade(input) {
        const marks = parseFloat(input.value) || 0;
        const totalMarks = 100; // Default total marks
        
        const studentId = input.dataset.studentId;
        const subjectId = input.dataset.subjectId;
        
        const gradeDisplay = document.querySelector(
            `.grade-display[data-student-id="${studentId}"][data-subject-id="${subjectId}"]`
        );
        
        if (gradeDisplay) {
            if (marks === 0) {
                gradeDisplay.textContent = '-';
                gradeDisplay.className = 'grade-display';
                gradeDisplay.style.background = '#f8f9fa';
                gradeDisplay.style.color = '#6c757d';
            } else {
                const percentage = (marks / totalMarks) * 100;
                const letterGrade = this.getLetterGrade(percentage);
                
                gradeDisplay.textContent = letterGrade.displayName;
                gradeDisplay.className = `grade-display grade-${letterGrade.category}`;
            }
        }
        
        // Update total obtained marks for the student
        this.updateStudentTotal(studentId);
    },
    
    updateStudentTotal(studentId) {
        const studentRow = document.querySelector(`tr:has(input[data-student-id="${studentId}"])`);
        if (!studentRow) return;
        
        let totalObtained = 0;
        let hasAnyMarks = false;
        
        this.subjects.forEach(subject => {
            const marksInput = document.querySelector(
                `input[data-student-id="${studentId}"][data-subject-id="${subject.id}"][data-type="marks"]`
            );
            
            if (marksInput && marksInput.value) {
                totalObtained += parseFloat(marksInput.value) || 0;
                hasAnyMarks = true;
            }
        });
        
        const totalDisplay = studentRow.querySelector('td:nth-child(4) div');
        if (totalDisplay) {
            if (hasAnyMarks) {
                totalDisplay.textContent = totalObtained;
                totalDisplay.style.color = '#2c3e50';
            } else {
                totalDisplay.textContent = '-';
                totalDisplay.style.color = '#6c757d';
            }
        }
    },
    
    getLetterGrade(percentage) {
        if (percentage >= 90) {
            return { displayName: 'A+', category: 'excellent' };
        } else if (percentage >= 80) {
            return { displayName: 'A', category: 'excellent' };
        } else if (percentage >= 75) {
            return { displayName: 'A-', category: 'good' };
        } else if (percentage >= 70) {
            return { displayName: 'B+', category: 'good' };
        } else if (percentage >= 65) {
            return { displayName: 'B', category: 'good' };
        } else if (percentage >= 60) {
            return { displayName: 'B-', category: 'average' };
        } else if (percentage >= 55) {
            return { displayName: 'C+', category: 'average' };
        } else if (percentage >= 50) {
            return { displayName: 'C', category: 'average' };
        } else if (percentage >= 45) {
            return { displayName: 'C-', category: 'below-average' };
        } else if (percentage >= 40) {
            return { displayName: 'D', category: 'below-average' };
        } else {
            return { displayName: 'F', category: 'failing' };
        }
    },
    
    async saveGrades() {
        if (!this.selectedCourseId || !this.selectedExamId) {
            this.showAlert('Please select both course and exam schedule.', 'danger');
            return;
        }
        
        const gradeEntries = [];
        
        // Collect all grade data
        this.students.forEach(student => {
            this.subjects.forEach(subject => {
                const marksInput = document.querySelector(
                    `input[data-student-id="${student.id}"][data-subject-id="${subject.id}"][data-type="marks"]`
                );
                
                const commentInput = document.querySelector(
                    `textarea[data-student-id="${student.id}"][data-subject-id="${subject.id}"][class*="comments-input"]`
                );
                
                if (marksInput && marksInput.value) {
                    gradeEntries.push({
                        studentId: student.id,
                        subjectId: subject.id,
                        marksObtained: parseFloat(marksInput.value),
                        totalMarks: 100,
                        comments: commentInput ? commentInput.value : ''
                    });
                }
            });
        });
        
        if (gradeEntries.length === 0) {
            this.showAlert('Please enter at least one grade before saving.', 'danger');
            return;
        }
        
        // Debug: Log the data being sent
        console.log('Sending grade entries:', gradeEntries);
        console.log('Course ID:', this.selectedCourseId);
        console.log('Exam Schedule ID:', this.selectedExamId);
        
        try {
            this.showAlert('Saving grades...', 'info');
            
            const response = await fetch('/teacher/grades/api/save', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    courseId: this.selectedCourseId,
                    examScheduleId: this.selectedExamId,
                    gradeEntries: gradeEntries
                })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.showAlert(`Successfully saved ${data.savedCount} grades!`, 'success');
                // Reload existing grades to update the form
                await this.loadExistingGrades();
            } else {
                console.error('Server error response:', data);
                this.showAlert('Error saving grades: ' + data.message, 'danger');
            }
        } catch (error) {
            console.error('Error saving grades:', error);
            this.showAlert('Error saving grades. Please try again.', 'danger');
        }
    },
    
    resetForm() {
        if (confirm('Are you sure you want to reset all entered grades? This action cannot be undone.')) {
            // Clear all input fields
            document.querySelectorAll('.marks-input').forEach(input => {
                input.value = '';
            });
            
            document.querySelectorAll('.comments-input').forEach(input => {
                input.value = '';
            });
            
            // Reset grade displays
            document.querySelectorAll('.grade-display').forEach(display => {
                display.textContent = '-';
                display.className = 'grade-display';
                display.style.background = '#f8f9fa';
                display.style.color = '#6c757d';
            });
            
            // Reset total displays
            document.querySelectorAll('td:nth-child(4) div').forEach(display => {
                display.textContent = '-';
                display.style.color = '#6c757d';
            });
            
            this.showAlert('Form has been reset.', 'info');
        }
    },
    
    showGradesTable() {
        document.getElementById('tableLoading').style.display = 'none';
        document.getElementById('tableContent').style.display = 'block';
        document.getElementById('gradesTableContainer').style.display = 'block';
        document.getElementById('actionsContainer').style.display = 'block';
    },
    
    hideGradesTable() {
        document.getElementById('gradesTableContainer').style.display = 'none';
        document.getElementById('actionsContainer').style.display = 'none';
    },
    
    showEmptyState(message) {
        document.getElementById('tableLoading').style.display = 'none';
        document.getElementById('tableContent').style.display = 'none';
        
        const container = document.getElementById('gradesTableContainer');
        container.style.display = 'block';
        
        const tableContent = document.getElementById('tableContent');
        tableContent.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-users"></i>
                <h3>No Students Found</h3>
                <p>${message}</p>
            </div>
        `;
        tableContent.style.display = 'block';
        
        document.getElementById('actionsContainer').style.display = 'none';
    },
    
    getStudentInitials(student) {
        const firstInitial = student.firstName ? student.firstName.charAt(0).toUpperCase() : 'S';
        const lastInitial = student.lastName ? student.lastName.charAt(0).toUpperCase() : 'T';
        return firstInitial + lastInitial;
    },
    
    showAlert(message, type) {
        const alertContainer = document.getElementById('alertContainer');
        if (!alertContainer) return;
        
        const alertId = 'alert-' + Date.now();
        const alertHtml = `
            <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" onclick="document.getElementById('${alertId}').remove()"></button>
            </div>
        `;
        
        alertContainer.innerHTML = alertHtml;
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            const alertElement = document.getElementById(alertId);
            if (alertElement) {
                alertElement.remove();
            }
        }, 5000);
    }
};

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    gradeEntry.init();
});
