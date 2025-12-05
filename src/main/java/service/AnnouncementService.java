package com.wsims.service;

import com.parami.wsims.entity.Announcement;
import com.parami.wsims.entity.Announcement.TargetAudience;

import java.util.List;
import java.util.Optional;

public interface AnnouncementService {
    
    // Create a new announcement
    Announcement createAnnouncement(Announcement announcement);
    
    // Delete an announcement by ID
    void deleteAnnouncement(Long id);
    
    // Get all announcements ordered by creation date (newest first)
    List<Announcement> getAllAnnouncements();
    
    // Get announcements by target audience
    List<Announcement> getAnnouncementsByTargetAudience(TargetAudience targetAudience);
    
    // Find announcement by ID
    Optional<Announcement> findById(Long id);
    
    // Get announcements for a specific role (role-specific + ALL_USERS)
    List<Announcement> getAnnouncementsForRole(String roleName);
}

