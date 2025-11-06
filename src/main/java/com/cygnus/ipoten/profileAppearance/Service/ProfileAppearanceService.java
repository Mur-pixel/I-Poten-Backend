package com.cygnus.ipoten.profileAppearance.Service;

import com.cygnus.ipoten.profileAppearance.Controller.response.AppearanceResponse;
import com.cygnus.ipoten.profileAppearance.Entity.ProfileAppearance;

import java.util.Optional;

public interface ProfileAppearanceService {
    Optional<ProfileAppearance> create(Long accountId);
    void delete(Long accountId);
    AppearanceResponse getMyAppearance(Long accountId);
    AppearanceResponse.PhotoResponse updatePhoto(Long accountId, String photoUrl);
    String generateUploadUrl(Long accountId, String filename, String contentType);
    String generateDownloadUrl(Long accountId);
    String getPhotoKey(Long accountId);
}
