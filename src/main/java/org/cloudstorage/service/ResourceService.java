package org.cloudstorage.service;

import org.cloudstorage.dto.ResourceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceService {
    ResourceResponse getResource(Long userId, String path);

    void deleteResource(Long userId, String path);

    byte[] downloadResource(Long userId, String path);

    ResourceResponse moveResource(Long userId, String srcPath, String destPath);

    List<ResourceResponse> searchResources(Long userId, String query);

    List<ResourceResponse> uploadResource(Long userId, String path, MultipartFile file);

    List<ResourceResponse> getDirectory(Long userId, String path);

    ResourceResponse createDirectory(Long userId, String path);

    String extractName(String path);
}
