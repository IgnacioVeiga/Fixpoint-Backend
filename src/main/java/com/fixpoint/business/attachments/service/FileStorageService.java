package com.fixpoint.business.attachments.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final String FILE_NOT_FOUND = "File not found: ";
    private static final String THUMBNAIL_NOT_FOUND = "Thumbnail not found: ";
    private static final String STORAGE_ERROR = "Could not store file. Please try again!";
    private final Path fileStorageLocation;
    private final Path thumbnailStorageLocation;

    public FileStorageService(@Value("${app.file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();
        this.thumbnailStorageLocation = this.fileStorageLocation.resolve(".thumbnails");
        try {
            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.thumbnailStorageLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String originalFileName = StringUtils.cleanPath(
            Objects.requireNonNull(file.getOriginalFilename(), AttachmentServiceImpl.ORIGINAL_FILENAME_MUST_NOT_BE_NULL)
        );
        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Invalid file path sequence " + originalFileName);
        }

        int extensionIndex = originalFileName.lastIndexOf(".");
        String fileExtension = extensionIndex >= 0 ? originalFileName.substring(extensionIndex) : "";
        String fileName = UUID.randomUUID() + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new IllegalStateException(STORAGE_ERROR, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = resolveStoredFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new EntityNotFoundException(FILE_NOT_FOUND + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new EntityNotFoundException(FILE_NOT_FOUND + fileName);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = resolveStoredFilePath(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete file " + fileName, ex);
        }
    }

    public Path resolveStoredFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }

    public Path resolveThumbnailPath(String fileName) {
        String baseName = fileName;
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = fileName.substring(0, extensionIndex);
        }
        return this.thumbnailStorageLocation.resolve(baseName + ".png").normalize();
    }

    public Resource loadThumbnailAsResource(String fileName) {
        try {
            Path thumbnailPath = resolveThumbnailPath(fileName);
            Resource resource = new UrlResource(thumbnailPath.toUri());
            if (resource.exists()) {
                return resource;
            }
            throw new EntityNotFoundException(THUMBNAIL_NOT_FOUND + fileName);
        } catch (MalformedURLException ex) {
            throw new EntityNotFoundException(THUMBNAIL_NOT_FOUND + fileName);
        }
    }

    public void deleteThumbnail(String fileName) {
        try {
            Files.deleteIfExists(resolveThumbnailPath(fileName));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete thumbnail " + fileName, ex);
        }
    }

    public Optional<StorageCapacity> resolveStorageCapacity() {
        try {
            FileStore fileStore = Files.getFileStore(this.fileStorageLocation);
            return Optional.of(new StorageCapacity(fileStore.getTotalSpace(), fileStore.getUsableSpace()));
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            return Optional.empty();
        }
    }

    public record StorageCapacity(long totalBytes, long availableBytes) {
    }
}
