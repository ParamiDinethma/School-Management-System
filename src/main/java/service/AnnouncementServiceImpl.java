package com.wsims.service;

import com.parami.wsims.entity.Announcement;
import com.parami.wsims.entity.Announcement.TargetAudience;
import com.parami.wsims.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {
    
    private final AnnouncementRepository announcementRepository;
    
    @Autowired
    public AnnouncementServiceImpl(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }
    
    @Override
    public Announcement createAnnouncement(Announcement announcement) {
        return announcementRepository.save(announcement);
    }
    
    @Override
    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new IllegalArgumentException("Announcement not found with id: " + id);
        }
        announcementRepository.deleteById(id);
    }
    
    @Override
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }
    
    @Override
    public List<Announcement> getAnnouncementsByTargetAudience(TargetAudience targetAudience) {
        return announcementRepository.findByTargetAudienceOrderByCreatedAtDesc(targetAudience);
    }
    
    @Override
    public Optional<Announcement> findById(Long id) {
        return announcementRepository.findById(id);
    }
    
    @Override
    public List<Announcement> getAnnouncementsForRole(String roleName) {
        List<Announcement> announcements = new ArrayList<>();
        
        // Always include ALL_USERS announcements
        announcements.addAll(announcementRepository.findByTargetAudienceOrderByCreatedAtDesc(TargetAudience.ALL_USERS));
        
        // Add role-specific announcements based on user role
        switch (roleName.toUpperCase()) {
            case "STUDENT":
                announcements.addAll(announcementRepository.findByTargetAudienceOrderByCreatedAtDesc(TargetAudience.ALL_STUDENTS));
                break;
            case "TEACHER":
                announcements.addAll(announcementRepository.findByTargetAudienceOrderByCreatedAtDesc(TargetAudience.ALL_TEACHERS));
                break;
            case "PARENT":
                announcements.addAll(announcementRepository.findByTargetAudienceOrderByCreatedAtDesc(TargetAudience.ALL_PARENTS));
                break;
            case "PRINCIPAL":
            case "IT_ADMIN":
            case "REGISTRAR":
                // Admin roles can see all announcements
                announcements.addAll(announcementRepository.findByTargetAudienceOrderByCreatedAtDesc(TargetAudience.ALL_STUDENTS));
                announcements.addAll(announcementRepository.findByTargetAudienceOrderByCreatedAtDesc(TargetAudience.ALL_TEACHERS));
                announcements.addAll(announcementRepository.findByTargetAudienceOrderByCreatedAtDesc(TargetAudience.ALL_PARENTS));
                break;
        }
        
        // Remove duplicates and sort by creation date (newest first)
        return announcements.stream()
                .distinct()
                .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                .limit(10) // Limit to 10 most recent announcements
                .collect(Collectors.toList());
    }
}

