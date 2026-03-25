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

    // TẢI ẢNH BẰNG TAY (UPLOAD LOCAL)
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
            String fileName = "local_" + safeTitle + "_" + System.currentTimeMillis() + getExtension(file);
            Path targetLocation = this.fileStorageLocation.resolve(folder);
            Files.createDirectories(targetLocation);
            Path finalPath = targetLocation.resolve(fileName);
            Files.copy(file.getInputStream(), finalPath, StandardCopyOption.REPLACE_EXISTING);
            return "/" + folder + "/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + ". Please try again!", ex);
        }
    }

    // TẢI ẢNH TỪ API (OMDb) - VƯỢT TƯỜNG LỬA BẰNG USER-AGENT
    public String storeFileFromUrl(String imageUrl, String folder, String title) {
        if (imageUrl == null || imageUrl.equals("N/A")) return null;
        try {
            String safeTitle = title.replaceAll("[^a-zA-Z0-9.-]", "_");

            // Dùng chung tên file không có UUID để tránh rác dung lượng
            String fileName = "omdb_" + safeTitle + ".jpg";

            Path targetLocation = this.fileStorageLocation.resolve(folder);
            Files.createDirectories(targetLocation);
            Path finalPath = targetLocation.resolve(fileName);

            // FIX LỖI BỊ AMAZON/OMDB CHẶN (403 FORBIDDEN)
            // Đóng giả làm trình duyệt Google Chrome để vượt qua bảo mật
            HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setConnectTimeout(5000); // Tối đa 5s để kết nối
            connection.setReadTimeout(5000);    // Tối đa 5s để tải

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, finalPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/" + folder + "/" + fileName;
        } catch (Exception ex) {
            System.err.println("Cannot fetch image from URL: " + ex.getMessage());
            return null;
        }
    }

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

    public boolean isFileExists(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) return false;
        try {
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = this.fileStorageLocation.resolve(relativePath).normalize();
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
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