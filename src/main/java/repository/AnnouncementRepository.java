package com.wsims.repository;

import com.parami.wsims.entity.Announcement;
import com.parami.wsims.entity.Announcement.TargetAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    
    // Find announcements by target audience, ordered by creation date (newest first)
    List<Announcement> findByTargetAudienceOrderByCreatedAtDesc(TargetAudience targetAudience);
    
    // Find all announcements ordered by creation date (newest first)
    List<Announcement> findAllByOrderByCreatedAtDesc();
}


