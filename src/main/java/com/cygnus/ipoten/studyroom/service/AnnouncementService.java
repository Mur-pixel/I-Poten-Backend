package com.cygnus.ipoten.studyroom.service;

import com.cygnus.ipoten.studyroom.service.request.CreateAnnouncementRequest;
import com.cygnus.ipoten.studyroom.service.request.UpdateAnnouncementRequest;
import com.cygnus.ipoten.studyroom.service.response.CreateAnnouncementResponse;
import com.cygnus.ipoten.studyroom.service.response.ListAnnouncementResponse;
import com.cygnus.ipoten.studyroom.service.response.ReadAnnouncementResponse;

import java.util.List;

public interface AnnouncementService {
    CreateAnnouncementResponse createAnnouncement(CreateAnnouncementRequest request);

    List<ListAnnouncementResponse> findAllAnnouncements(Long studyRoomId);

    void toggleAnnouncementPin(Long studyRoomId, Long announcementId);

    ReadAnnouncementResponse findAnnouncementById(Long announcementId);

    ReadAnnouncementResponse updateAnnouncement(Long announcementId, UpdateAnnouncementRequest request);

    void deleteAnnouncement(Long announcementId);
}