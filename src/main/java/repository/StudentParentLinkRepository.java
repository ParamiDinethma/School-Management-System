package com.wsims.repository;

import com.parami.wsims.entity.StudentParentLink;
import com.parami.wsims.entity.StudentParentLink.StudentParentLinkId;
import com.parami.wsims.entity.Student;
import com.parami.wsims.entity.Parent;
import com.parami.wsims.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentParentLinkRepository extends JpaRepository<StudentParentLink, StudentParentLinkId> {

    // Find all students linked to a specific parent
    @Query("SELECT spl.student.user FROM StudentParentLink spl WHERE spl.parent.userId = :parentUserId")
    List<User> findStudentsByParentUserId(@Param("parentUserId") Long parentUserId);

    // Find all parents linked to a specific student
    @Query("SELECT spl.parent.user FROM StudentParentLink spl WHERE spl.student.userId = :studentUserId")
    List<User> findParentsByStudentUserId(@Param("studentUserId") Long studentUserId);

    // Check if a link already exists
    boolean existsByIdStudentUserIdAndIdParentUserId(Long studentUserId, Long parentUserId);

    // Alternative method name for checking if link exists
    @Query("SELECT COUNT(spl) > 0 FROM StudentParentLink spl WHERE spl.student.userId = :studentUserId AND spl.parent.userId = :parentUserId")
    boolean existsByStudentUserIdAndParentUserId(@Param("studentUserId") Long studentUserId, @Param("parentUserId") Long parentUserId);

    // Find links with pagination and search
    @Query("SELECT spl FROM StudentParentLink spl " +
           "JOIN FETCH spl.student s " +
           "JOIN FETCH s.user " +
           "JOIN FETCH spl.parent p " +
           "JOIN FETCH p.user " +
           "WHERE (:searchTerm IS NULL OR " +
           "LOWER(s.user.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.user.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.user.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.user.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<StudentParentLink> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Count unlinked students
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = 'STUDENT' AND u.id NOT IN (SELECT spl.student.userId FROM StudentParentLink spl)")
    long countUnlinkedStudents();

    // Count unlinked parents
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = 'PARENT' AND u.id NOT IN (SELECT spl.parent.userId FROM StudentParentLink spl)")
    long countUnlinkedParents();

    // Find unlinked students (simplified - returns Student entities)
    @Query("SELECT s FROM Student s WHERE s.userId NOT IN (SELECT spl.student.userId FROM StudentParentLink spl)")
    List<Student> findUnlinkedStudents();

    // Find unlinked parents (simplified - returns Parent entities)
    @Query("SELECT p FROM Parent p WHERE p.userId NOT IN (SELECT spl.parent.userId FROM StudentParentLink spl)")
    List<Parent> findUnlinkedParents();

    // Find all links with proper JOIN FETCH
    @Query("SELECT spl FROM StudentParentLink spl " +
           "JOIN FETCH spl.student s " +
           "JOIN FETCH s.user " +
           "JOIN FETCH spl.parent p " +
           "JOIN FETCH p.user")
    List<StudentParentLink> findAllWithUsers();
}