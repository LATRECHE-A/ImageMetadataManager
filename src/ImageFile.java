package test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.imageio.ImageIO;

public class ImageFile {
    private final Path path;
    private final long size;
    private final String mimeType;
    private final LocalDateTime lastModified;
    private final LocalDateTime creationDate;
    private final String fileFormat;
    private final int width;
    private final int height;

    public ImageFile(Path path) throws IOException {
        this.path = path;
        this.size = Files.size(path);
        this.mimeType = Files.probeContentType(path);

        // File attributes
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        this.lastModified = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        this.creationDate = attr.creationTime() != null
                ? LocalDateTime.ofInstant(attr.creationTime().toInstant(), ZoneId.systemDefault())
                : null;

        // Image-specific attributes
        BufferedImage image = ImageIO.read(path.toFile());
        if (image != null) {
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.fileFormat = getFileExtension(path.toString());
        } else {
            this.width = 0;
            this.height = 0;
            this.fileFormat = "unknown";
        }
    }

    // Helper method to get the file extension
    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        return (lastIndex == -1) ? "unknown" : fileName.substring(lastIndex + 1).toLowerCase();
    }

    public Path getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getDimensions() {
        return width + "x" + height;
    }
}

/*
package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ImageFile {
    private final Path path;
    private final long size;
    private final String mimeType;
    private final LocalDateTime lastModified;

    public ImageFile(Path path) throws IOException {
        this.path = path;
        this.size = Files.size(path);
        this.mimeType = Files.probeContentType(path);
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        this.lastModified = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault());
    }

    public Path getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }
}
*/
