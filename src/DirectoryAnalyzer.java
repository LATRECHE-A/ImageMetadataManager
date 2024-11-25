package test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryAnalyzer {
    private final Path directory;

    public DirectoryAnalyzer(Path directory) {
        this.directory = directory;
    }

    public List<ImageFile> listImageFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .filter(this::isSupportedImageFormat)
                        .map(this::createImageFile)
                        .collect(Collectors.toList());
        }
    }

    public int getTotalFileCount() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return (int) paths.filter(Files::isRegularFile).count();
        }
    }

    public int getImageFileCount() throws IOException {
        return listImageFiles().size();
    }

    public long getTotalFileSize() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .mapToLong(this::getFileSize)
                        .sum();
        }
    }

    public Path getLargestFile() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .max(Comparator.comparingLong(this::getFileSize))
                        .orElse(null);
        }
    }

    public Path getSmallestFile() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .min(Comparator.comparingLong(this::getFileSize))
                        .orElse(null);
        }
    }

    public long getAverageFileSize() throws IOException {
        long totalSize = getTotalFileSize();
        int fileCount = getTotalFileCount();
        return fileCount == 0 ? 0 : totalSize / fileCount;
    }

    public String getMostCommonFileType() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .map(this::getFileExtension)
                        .filter(ext -> ext != null && !ext.isEmpty())
                        .collect(Collectors.groupingBy(ext -> ext, Collectors.counting()))
                        .entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");
        }
    }

    public int getSubdirectoryCount() throws IOException {
        try (Stream<Path> paths = Files.walk(directory, 1)) {
            return (int) paths.filter(Files::isDirectory)
                              .count() - 1; // Subtract 1 for the root directory itself
        }
    }

    public Path getLastModifiedFile() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .max(Comparator.comparingLong(this::getLastModifiedTime))
                        .orElse(null);
        }
    }

    public Path getOldestFile() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .min(Comparator.comparingLong(this::getLastModifiedTime))
                        .orElse(null);
        }
    }

    public List<String> getFileExtensions() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .map(this::getFileExtension)
                        .filter(ext -> ext != null && !ext.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
        }
    }

    public int getEmptyFileCount() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return (int) paths.filter(Files::isRegularFile)
                              .filter(path -> getFileSize(path) == 0)
                              .count();
        }
    }

    public long getTotalDirectorySize() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .mapToLong(this::getFileSize)
                        .sum();
        }
    }

    // Helper methods
    private boolean isSupportedImageFormat(Path path) {
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(path);
        } catch (IOException e) {
            // Handle if necessary
        }
        return mimeType != null && (mimeType.equals("image/jpeg") || mimeType.equals("image/png") || mimeType.equals("image/webp"));
    }

    private ImageFile createImageFile(Path path) {
        try {
            return new ImageFile(path);
        } catch (IOException e) {
            return null; // or handle as necessary
        }
    }

    private long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0; // Handle or log error
        }
    }

    private long getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0; // Handle or log error
        }
    }

    private String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
    }
}
