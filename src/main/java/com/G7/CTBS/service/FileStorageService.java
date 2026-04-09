package com.G7.CTBS.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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
    
    public String storeFile(MultipartFile file, String folder, String title) {
        if (file == null || file.isEmpty()) return null;
        
        String contentType = file.getContentType();
        
        // Đã bổ sung kiểm tra cho cả thư mục "posters"
        if ((folder.equals("banners") || folder.equals("posters") || folder.equals("combos")) && (contentType == null || !contentType.startsWith("image/"))) {
            throw new RuntimeException("Error: Only image files are allowed for " + folder + "!");
        }
        if (folder.equals("trailers") && (contentType == null || !contentType.equals("video/mp4"))) {
            throw new RuntimeException("Error: Only MP4 files are allowed for trailers!");
        }
        
        try {
            String safeTitle = title.replaceAll("[^a-zA-Z0-9.-]", "_");
            String fileName = safeTitle + "_" + System.currentTimeMillis() + getExtension(file);
            Path targetLocation = this.fileStorageLocation.resolve(folder);
            Files.createDirectories(targetLocation);
            Path finalPath = targetLocation.resolve(fileName);
            Files.copy(file.getInputStream(), finalPath, StandardCopyOption.REPLACE_EXISTING);
            return "/" + folder + "/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + ". Please try again!", ex);
        }
    }
    
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) return;
        
        // FIX: Nếu là link API ngoài (TMDB, Unsplash...) thì không xóa trên ổ cứng
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) return;
        
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
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return ".jpg";
    }
}
