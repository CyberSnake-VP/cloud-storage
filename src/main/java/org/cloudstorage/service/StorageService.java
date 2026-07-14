package org.cloudstorage.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {
    void ensureBucketExists();
    void uploadFile(Long userId, String filePath, MultipartFile file);
    byte[] downloadFile(Long userId, String filePath);
    void deleteObject(Long userId, String filePath);
    void createFolder(Long userId, String folderName);
    List<String> listFolder(Long userId, String folderPath);
}
