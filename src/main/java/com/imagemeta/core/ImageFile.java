package com.imagemeta.core;

import com.imagemeta.util.logging.Logger;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import javax.imageio.ImageIO;

/**
 * Immutable representation of an image file and its extracted metadata,
 * including size, MIME type, timestamps, format, and pixel dimensions.
 * <p>
 * Construction validates the underlying file and performs best-effort metadata
 * extraction. Failures are logged and surfaced via {@link IOException} where appropriate.
 */
public final class ImageFile {
    private final Path path;
    private final long size;
    private final String mimeType;
    private final LocalDateTime lastModified;
    private final LocalDateTime creationDate;
    private final String fileFormat;
    private final int width;
    private final int height;
    
    /**
     * Creates an {@code ImageFile} for the given path and extracts metadata.
     *
     * @param path the image file path; must be a regular, existing file
     * @throws IOException if the file does not exist, is not a regular file, or cannot be read
     * @throws NullPointerException if {@code path} is {@code null}
     */
    public ImageFile(Path path) throws IOException {
        this.path = Objects.requireNonNull(path, "Path cannot be null");
        
        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }
        
        if (!Files.isRegularFile(path)) {
            throw new IOException("Not a regular file: " + path);
        }
        
        try {
            this.size = Files.size(path);
            this.mimeType = detectMimeType(path);
            
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            this.lastModified = LocalDateTime.ofInstant(
                attr.lastModifiedTime().toInstant(), 
                ZoneId.systemDefault()
            );
            this.creationDate = attr.creationTime() != null
                ? LocalDateTime.ofInstant(attr.creationTime().toInstant(), ZoneId.systemDefault())
                : this.lastModified;
            
            // Extract image dimensions
            ImageDimensions dimensions = extractDimensions(path);
            this.width = dimensions.width;
            this.height = dimensions.height;
            this.fileFormat = extractFileFormat(path);
            
            Logger.debug("Created ImageFile: {} ({}x{}, {})", 
                path.getFileName(), width, height, formatSize(size));
                
        } catch (IOException e) {
            Logger.error("Failed to read image file: {}", path, e);
            throw e;
        }
    }
    
    /**
     * Detects the MIME type for the given file path, preferring a probed value and
     * falling back to an extension-based mapping.
     *
     * @param path the file path
     * @return a MIME type, e.g., {@code image/png}, or {@code application/octet-stream} when unknown
     */
    private String detectMimeType(Path path) {
        try {
            String detected = Files.probeContentType(path);
            if (detected != null && detected.startsWith("image/")) {
                return detected;
            }
            // Fallback to extension-based detection
            return getMimeTypeFromExtension(path);
        } catch (IOException e) {
            Logger.warn("Failed to probe content type for {}", path);
            return getMimeTypeFromExtension(path);
        }
    }
    
    /**
     * Resolves a MIME type using the file extension as a heuristic.
     *
     * @param path the file path
     * @return the inferred MIME type
     */
    private String getMimeTypeFromExtension(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".webp")) return "image/webp";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".bmp")) return "image/bmp";
        return "application/octet-stream";
    }
    
    /**
     * Simple width/height holder for image dimensions.
     */
    private static class ImageDimensions {
        final int width;
        final int height;
        
        ImageDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Attempts to read the image header to extract pixel dimensions.
     *
     * @param path the file path
     * @return a non-null {@link ImageDimensions}; defaults to {@code 0x0} if reading fails
     */
    private ImageDimensions extractDimensions(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image != null) {
                return new ImageDimensions(image.getWidth(), image.getHeight());
            }
        } catch (IOException e) {
            Logger.warn("Failed to read image dimensions for {}", path);
        }
        return new ImageDimensions(0, 0);
    }
    
    /**
     * Extracts the lowercase file extension without the dot.
     *
     * @param path the file path
     * @return a format string (e.g., {@code "png"}), or {@code "unknown"} if none exists
     */
    private String extractFileFormat(Path path) {
        String filename = path.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex == -1 ? "unknown" : filename.substring(dotIndex + 1).toLowerCase();
    }
    
    /**
     * Produces a human-readable byte size (e.g., {@code 13.7 MB}).
     *
     * @param bytes raw byte count
     * @return a formatted size string
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    // Getters
    
    /**
     * @return the underlying file {@link Path}
     */
    public Path getPath() { return path; }
    /**
     * @return file size in bytes
     */
    public long getSize() { return size; }
    /**
     * @return the detected or inferred MIME type
     */
    public String getMimeType() { return mimeType; }
    /**
     * @return the last modification timestamp in system default time zone
     */
    public LocalDateTime getLastModified() { return lastModified; }
    /**
     * @return the file creation timestamp (or last-modified when unavailable)
     */
    public LocalDateTime getCreationDate() { return creationDate; }
    /**
     * @return the lowercase file format (extension) or {@code "unknown"}
     */
    public String getFileFormat() { return fileFormat; }
    /**
     * @return image width in pixels (0 if unknown)
     */
    public int getWidth() { return width; }
    /**
     * @return image height in pixels (0 if unknown)
     */
    public int getHeight() { return height; }
    /**
     * @return a {@code WxH} string for pixel dimensions
     */
    public String getDimensions() { return width + "x" + height; }
    
    /**
     * Indicates whether the image has valid (non-zero) dimensions.
     *
     * @return {@code true} if both width and height are positive; {@code false} otherwise
     */
    public boolean isValid() {
        return width > 0 && height > 0;
    }
    
    /**
     * Two {@code ImageFile} instances are equal when they reference the same {@link Path}.
     *
     * @param o the object to compare
     * @return {@code true} if equal; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageFile that = (ImageFile) o;
        return path.equals(that.path);
    }
    
    /**
     * Computes a hash based on the underlying {@link Path}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
    
    /**
     * Returns a concise, human-readable description useful for logs.
     *
     * @return string representation including filename, formatted size, and dimensions
     */
    @Override
    public String toString() {
        return String.format("ImageFile[path=%s, size=%s, dimensions=%dx%d]",
            path.getFileName(), formatSize(size), width, height);
    }
}
