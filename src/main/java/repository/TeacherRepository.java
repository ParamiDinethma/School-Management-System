package com.wsims.repository;

import com.parami.wsims.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Page<Teacher> findAll(Pageable pageable);

    Page<Teacher> findByDepartment(String department, Pageable pageable);

    Page<Teacher> findBySpecialization(String specialization, Pageable pageable);

    @Query("SELECT t FROM Teacher t JOIN t.user u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.specialization) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.department) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Teacher> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT t FROM Teacher t JOIN t.user u WHERE t.department = :department AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.specialization) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Teacher> findByDepartmentAndSearchTerm(@Param("department") String department,
                                               @Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Teacher t")
    long countTotalTeachers();

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.hireDate >= :cutoffDate")
    long countActiveTeachers(@Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.hireDate < :cutoffDate OR t.hireDate IS NULL")
    long countInactiveTeachers(@Param("cutoffDate") LocalDate cutoffDate);

    Optional<Teacher> findByUserId(Long userId);

    List<Teacher> findByDepartmentIn(List<String> departments);

    @Query("SELECT DISTINCT t.department FROM Teacher t WHERE t.department IS NOT NULL")
    List<String> findDistinctDepartments();
}
