package org.cloudstorage.service.minio;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface StorageService {
    void ensureBucketExists();
    void uploadFile(Long userId, String filePath, MultipartFile file);
    InputStream downloadResource(Long userId, String filePath);
    void deleteObject(Long userId, String filePath);
    void createFolder(Long userId, String folderName);
    List<String> listFolder(Long userId, String folderPath);
    Long getFileSize(Long userId, String filePath);
    boolean exists(Long userId, String filePath);
    List<String> listFolderRecursive(Long userId, String path);
    void uploadFile(Long userId, String filePath, byte[] data);
    List<String> listAllFiles(Long userId);
}
