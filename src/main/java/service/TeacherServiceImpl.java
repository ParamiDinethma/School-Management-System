package com.wsims.service;

import com.parami.wsims.entity.Teacher;
import com.parami.wsims.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;

    @Autowired
    public TeacherServiceImpl(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Override
    public Optional<Teacher> findById(Long id) {
        return teacherRepository.findById(id);
    }

    @Override
    public List<Teacher> findAllTeachers() {
        return teacherRepository.findAll();
    }

    @Override
    public Teacher saveTeacher(Teacher teacher) {
        return teacherRepository.save(teacher);
    }

    @Override
    public void deleteTeacher(Long id) {
        teacherRepository.deleteById(id);
    }

    @Override
    public Page<Teacher> findTeachersWithPagination(Pageable pageable) {
        return teacherRepository.findAll(pageable);
    }

    @Override
    public Page<Teacher> searchTeachers(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return teacherRepository.findAll(pageable);
        }
        return teacherRepository.findBySearchTerm(searchTerm.trim(), pageable);
    }

    @Override
    public Page<Teacher> findTeachersByDepartment(String department, Pageable pageable) {
        if (department == null || department.trim().isEmpty() || "ALL".equals(department)) {
            return teacherRepository.findAll(pageable);
        }
        return teacherRepository.findByDepartment(department, pageable);
    }

    @Override
    public Page<Teacher> searchTeachersByDepartment(String department, String searchTerm, Pageable pageable) {
        if ((department == null || department.trim().isEmpty() || "ALL".equals(department)) &&
            (searchTerm == null || searchTerm.trim().isEmpty())) {
            return teacherRepository.findAll(pageable);
        }

        if (department == null || department.trim().isEmpty() || "ALL".equals(department)) {
            return searchTeachers(searchTerm, pageable);
        }

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findTeachersByDepartment(department, pageable);
        }

        return teacherRepository.findByDepartmentAndSearchTerm(department, searchTerm.trim(), pageable);
    }

    @Override
    public long getTotalTeacherCount() {
        return teacherRepository.countTotalTeachers();
    }

    @Override
    public long getActiveTeacherCount(LocalDate cutoffDate) {
        return teacherRepository.countActiveTeachers(cutoffDate);
    }

    @Override
    public long getInactiveTeacherCount(LocalDate cutoffDate) {
        return teacherRepository.countInactiveTeachers(cutoffDate);
    }

    @Override
    public List<String> getDistinctDepartments() {
        return teacherRepository.findDistinctDepartments();
    }

    @Override
    public Optional<Teacher> findByUserId(Long userId) {
        return teacherRepository.findByUserId(userId);
    }
}

