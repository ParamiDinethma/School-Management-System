package com.wsims.repository;

import com.parami.wsims.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    Optional<Subject> findBySubjectCode(String subjectCode);
    
    List<Subject> findByIsActiveTrue();
    
    Page<Subject> findByIsActiveTrue(Pageable pageable);
    
    @Query("SELECT s FROM Subject s WHERE " +
           "LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.subjectCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Subject> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT s FROM Subject s WHERE s.isActive = true AND " +
           "(LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.subjectCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Subject> findActiveBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    boolean existsBySubjectCode(String subjectCode);
    boolean existsBySubjectCodeAndIdNot(String subjectCode, Long id);
}
