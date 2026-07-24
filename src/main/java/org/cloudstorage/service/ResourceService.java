package org.cloudstorage.service;

import org.cloudstorage.dto.ResourceResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface ResourceService {
    ResourceResponse getResource(Long userId, String path);

    void deleteResource(Long userId, String path);

    void downloadFile(Long userId, String path, OutputStream outputStream);

    void downloadFolder(Long userId, String path,  OutputStream outputStream);

    ResourceResponse moveResource(Long userId, String srcPath, String destPath);

    List<ResourceResponse> searchResources(Long userId, String query);

    List<ResourceResponse> uploadResource(Long userId, String path, List<MultipartFile> files);

    List<ResourceResponse> getDirectory(Long userId, String path);

    ResourceResponse createDirectory(Long userId, String path);

    String extractName(String path);
}
