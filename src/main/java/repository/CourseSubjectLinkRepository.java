package com.wsims.repository;

import com.parami.wsims.entity.CourseSubjectLink;
import com.parami.wsims.entity.Course;
import com.parami.wsims.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseSubjectLinkRepository extends JpaRepository<CourseSubjectLink, Long> {
    
    List<CourseSubjectLink> findByCourse(Course course);
    List<CourseSubjectLink> findBySubject(Subject subject);
    
    @Query("SELECT csl FROM CourseSubjectLink csl WHERE csl.course.id = :courseId")
    List<CourseSubjectLink> findByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT csl FROM CourseSubjectLink csl WHERE csl.subject.id = :subjectId")
    List<CourseSubjectLink> findBySubjectId(@Param("subjectId") Long subjectId);
    
    Optional<CourseSubjectLink> findByCourseAndSubject(Course course, Subject subject);
    
    @Query("SELECT csl FROM CourseSubjectLink csl WHERE csl.course.id = :courseId AND csl.subject.id = :subjectId")
    Optional<CourseSubjectLink> findByCourseIdAndSubjectId(@Param("courseId") Long courseId, @Param("subjectId") Long subjectId);
    
    List<CourseSubjectLink> findByIsMandatoryTrue();
    
    @Query("SELECT csl FROM CourseSubjectLink csl WHERE csl.course.id = :courseId AND csl.isMandatory = true")
    List<CourseSubjectLink> findMandatoryByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT csl FROM CourseSubjectLink csl WHERE csl.course.id = :courseId AND csl.isMandatory = false")
    List<CourseSubjectLink> findOptionalByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT s FROM Subject s JOIN CourseSubjectLink csl ON s.id = csl.subject.id WHERE csl.course.id = :courseId")
    List<Subject> findSubjectsByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT c FROM Course c JOIN CourseSubjectLink csl ON c.id = csl.course.id WHERE csl.subject.id = :subjectId")
    List<Course> findCoursesBySubjectId(@Param("subjectId") Long subjectId);
    
    boolean existsByCourseAndSubject(Course course, Subject subject);
    boolean existsByCourseIdAndSubjectId(Long courseId, Long subjectId);
}
