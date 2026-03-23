package com.G7.CTBS.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
    
    public FileStorageService() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
    
    // Lưu file upload từ máy tính
    public String storeFile(MultipartFile file, String folder, String title) {
        if (file == null || file.isEmpty()) return null;
        
        String contentType = file.getContentType();
        if (folder.equals("banners") && (contentType == null || !contentType.startsWith("image/"))) {
            throw new RuntimeException("Error: Only image files are allowed for banners!");
        }
        if (folder.equals("trailers") && (contentType == null || !contentType.equals("video/mp4"))) {
            throw new RuntimeException("Error: Only MP4 files are allowed for trailers!");
        }
        
        try {
            String safeTitle = title.replaceAll("[^a-zA-Z0-9.-]", "_");
            String fileName = UUID.randomUUID().toString() + "_" + safeTitle + "." + getExtension(file);
            
            Path targetLocation = this.fileStorageLocation.resolve(folder);
            Files.createDirectories(targetLocation);
            Path finalPath = targetLocation.resolve(fileName);
            
            Files.copy(file.getInputStream(), finalPath, StandardCopyOption.REPLACE_EXISTING);
            return "/" + folder + "/" + fileName;
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }
    
    public String storeFileFromUrl(String imageUrl, String folder, String title) {
        if (imageUrl == null || imageUrl.equals("N/A")) return null;
        try {
            String safeTitle = title.replaceAll("[^a-zA-Z0-9.-]", "_");
            String fileName = java.util.UUID.randomUUID().toString() + "_" + safeTitle + ".jpg";
            Path targetLocation = this.fileStorageLocation.resolve(folder);
            Files.createDirectories(targetLocation);
            Path finalPath = targetLocation.resolve(fileName);
            
            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, finalPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return "/" + folder + "/" + fileName;
        } catch (Exception ex) { return null; }
    }
    
    // Xóa file vật lý
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) return;
        try {
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = this.fileStorageLocation.resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println("Error: Cannot delete file " + fileUrl);
        }
    }
    
    private String getExtension(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        assert fileName != null;
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}