package com.wsims.service;

import com.parami.wsims.entity.*;
import com.parami.wsims.entity.StudentParentLink.StudentParentLinkId;
import com.parami.wsims.repository.StudentParentLinkRepository;
import com.parami.wsims.repository.StudentRepository;
import com.parami.wsims.repository.ParentRepository;
import com.parami.wsims.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ParentLinkServiceImpl implements ParentLinkService {

    private final StudentParentLinkRepository linkRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;

    @Autowired
    public ParentLinkServiceImpl(StudentParentLinkRepository linkRepository,
                                StudentRepository studentRepository,
                                ParentRepository parentRepository,
                                UserRepository userRepository) {
        this.linkRepository = linkRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public StudentParentLink createLink(Long studentUserId, Long parentUserId, Long createdBy) {
        // Check if link already exists
        if (linkExists(studentUserId, parentUserId)) {
            throw new IllegalArgumentException("Link between student and parent already exists");
        }

        // Get Student entity (this will verify it exists)
        Student student = studentRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with User ID: " + studentUserId));
        
        // Get Parent entity, create if doesn't exist
        Parent parent = parentRepository.findById(parentUserId)
                .orElse(null);
        
        if (parent == null) {
            // Check if user exists and has PARENT role, then create Parent entity
            User user = userRepository.findById(parentUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + parentUserId));
            
            if (!"PARENT".equals(user.getRole().getName())) {
                throw new IllegalArgumentException("User " + parentUserId + " is not a parent");
            }
            
            // Create Parent entity
            parent = new Parent();
            parent.setUserId(parentUserId);
            parent.setOccupation("Not specified");
            parent.setUser(user);
            parent = parentRepository.save(parent);
        }

        // Create the link
        StudentParentLink link = new StudentParentLink();
        StudentParentLinkId linkId = new StudentParentLinkId(studentUserId, parentUserId);
        link.setId(linkId);
        link.setStudent(student);
        link.setParent(parent);

        return linkRepository.save(link);
    }

    @Override
    public void deleteLink(Long studentUserId, Long parentUserId) {
        StudentParentLinkId id = new StudentParentLinkId(studentUserId, parentUserId);
        if (!linkRepository.existsById(id)) {
            throw new IllegalArgumentException("Parent-student link not found");
        }
        linkRepository.deleteById(id);
    }

    @Override
    public boolean linkExists(Long studentUserId, Long parentUserId) {
        return linkRepository.existsByStudentUserIdAndParentUserId(studentUserId, parentUserId);
    }

    @Override
    public boolean isParentLinkedToStudent(Long parentUserId, Long studentUserId) {
        return linkRepository.existsByStudentUserIdAndParentUserId(studentUserId, parentUserId);
    }

    @Override
    public Page<StudentParentLink> getAllLinks(String searchTerm, Pageable pageable) {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return linkRepository.findBySearchTerm(searchTerm, pageable);
        }
        // Use a custom implementation to handle JOIN FETCH with pagination
        List<StudentParentLink> allLinks = linkRepository.findAllWithUsers();
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allLinks.size());
        List<StudentParentLink> pageContent = allLinks.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allLinks.size());
    }

    @Override
    public Page<StudentParentLink> getAllLinks(Pageable pageable) {
        // Use the same custom implementation
        return getAllLinks(null, pageable);
    }

    @Override
    public Page<StudentParentLink> searchLinksByStudentName(String searchTerm, Pageable pageable) {
        return linkRepository.findBySearchTerm(searchTerm, pageable);
    }

    @Override
    public Page<StudentParentLink> searchLinksByParentName(String searchTerm, Pageable pageable) {
        return linkRepository.findBySearchTerm(searchTerm, pageable);
    }

    @Override
    public List<User> getLinkedStudentsForParent(Long parentUserId) {
        return linkRepository.findStudentsByParentUserId(parentUserId);
    }

    @Override
    public List<User> getLinkedParentsForStudent(Long studentUserId) {
        return linkRepository.findParentsByStudentUserId(studentUserId);
    }

    @Override
    public Map<String, Long> getLinkStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalLinks", linkRepository.count());
        stats.put("unlinkedStudents", linkRepository.countUnlinkedStudents());
        stats.put("unlinkedParents", linkRepository.countUnlinkedParents());
        return stats;
    }

    @Override
    public List<Student> getUnlinkedStudents() {
        return linkRepository.findUnlinkedStudents();
    }

    @Override
    public List<Parent> getUnlinkedParents() {
        return linkRepository.findUnlinkedParents();
    }
}