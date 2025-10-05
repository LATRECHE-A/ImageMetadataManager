package com.imagemeta.core;

import com.imagemeta.util.logging.Logger;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analyzes a root {@link Path} and provides comprehensive directory statistics,
 * including image discovery, size metrics, and file-type breakdowns.
 * <p>
 * This implementation is thread-safe and memoizes results using a per-method cache
 * to avoid repeated filesystem traversal.
 *
 * @implNote Results are cached in a {@link ConcurrentHashMap}. If the directory
 *           contents may have changed, call {@link #clearCache()} to invalidate
 *           cached values.
 */
public final class DirectoryAnalyzer {
    private final Path directory;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private static final Set<String> SUPPORTED_EXTENSIONS = 
        Set.of("jpg", "jpeg", "png", "webp", "gif", "bmp");
    
    /**
     * Creates a new analyzer for the given directory.
     *
     * @param directory the directory to analyze; must exist and be a directory
     * @throws IllegalArgumentException if the path does not exist or is not a directory
     * @throws NullPointerException if {@code directory} is {@code null}
     */
    public DirectoryAnalyzer(Path directory) {
        Objects.requireNonNull(directory, "Directory cannot be null");
        if (!Files.exists(directory)) {
            throw new IllegalArgumentException("Directory does not exist: " + directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }
        this.directory = directory;
        Logger.debug("Created DirectoryAnalyzer for: {}", directory);
    }
    
    /**
     * Recursively lists all supported image files in the directory tree.
     * <p>
     * A file is considered an image if its extension is in {@link #SUPPORTED_EXTENSIONS}
     * and the probed MIME type (when available) starts with {@code image/}.
     *
     * @return a list of {@link ImageFile} instances for all discovered images; never {@code null}
     * @throws IOException if directory traversal fails
     */
    public List<ImageFile> listImageFiles() throws IOException {
        return getCached("imageFiles", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedImageFormat)
                    .map(this::createImageFile)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }
        });
    }
    
    /**
     * Counts all regular files in the directory tree.
     *
     * @return the total number of regular files
     * @throws IOException if directory traversal fails
     */
    public int getTotalFileCount() throws IOException {
        return getCached("totalFileCount", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return (int) paths.filter(Files::isRegularFile).count();
            }
        });
    }
    
    /**
     * Counts all supported image files in the directory tree.
     *
     * @return the number of image files
     * @throws IOException if directory traversal fails
     */
    public int getImageFileCount() throws IOException {
        return listImageFiles().size();
    }
    
    /**
     * Computes the total size (in bytes) of all regular files in the directory tree.
     *
     * @return the sum of file sizes in bytes
     * @throws IOException if directory traversal fails
     */
    public long getTotalFileSize() throws IOException {
        return getCached("totalFileSize", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return paths
                    .filter(Files::isRegularFile)
                    .mapToLong(this::getFileSize)
                    .sum();
            }
        });
    }
    
    /**
     * Finds the largest file by size within the directory tree.
     *
     * @return the {@link Path} to the largest file, or {@code null} if no files exist
     * @throws IOException if directory traversal fails
     */
    public Path getLargestFile() throws IOException {
        return getCached("largestFile", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return paths
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparingLong(this::getFileSize))
                    .orElse(null);
            }
        });
    }
    
    /**
     * Finds the smallest non-empty file by size within the directory tree.
     *
     * @return the {@link Path} to the smallest non-empty file, or {@code null} if none exist
     * @throws IOException if directory traversal fails
     */
    public Path getSmallestFile() throws IOException {
        return getCached("smallestFile", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> getFileSize(p) > 0) // Exclude empty files
                    .min(Comparator.comparingLong(this::getFileSize))
                    .orElse(null);
            }
        });
    }
    
    /**
     * Computes the average size (in bytes) across all regular files.
     *
     * @return the average file size in bytes, or {@code 0} when no files are present
     * @throws IOException if directory traversal fails
     */
    public long getAverageFileSize() throws IOException {
        long total = getTotalFileSize();
        int count = getTotalFileCount();
        return count == 0 ? 0 : total / count;
    }
    
    /**
     * Determines the most frequently occurring file extension (case-insensitive).
     *
     * @return the most common file extension, or {@code "unknown"} if none are found
     * @throws IOException if directory traversal fails
     */
    public String getMostCommonFileType() throws IOException {
        return getCached("mostCommonFileType", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return paths
                    .filter(Files::isRegularFile)
                    .map(this::getFileExtension)
                    .filter(ext -> ext != null && !ext.isEmpty())
                    .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("unknown");
            }
        });
    }
    
    /**
     * Counts the number of direct subdirectories (depth = 1).
     *
     * @return the number of subdirectories directly under the root directory
     * @throws IOException if directory traversal fails
     */
    public int getSubdirectoryCount() throws IOException {
        return getCached("subdirectoryCount", () -> {
            try (Stream<Path> paths = Files.walk(directory, 1)) {
                return (int) paths.filter(Files::isDirectory).count() - 1;
            }
        });
    }
    
    /**
     * Finds the most recently modified file in the directory tree.
     *
     * @return the {@link Path} to the last-modified file, or {@code null} if none exist
     * @throws IOException if directory traversal fails
     */
    public Path getLastModifiedFile() throws IOException {
        return getCached("lastModifiedFile", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return paths
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparingLong(this::getLastModifiedTime))
                    .orElse(null);
            }
        });
    }
    
    /**
     * Finds the oldest (least recently modified) file in the directory tree.
     *
     * @return the {@link Path} to the oldest file, or {@code null} if none exist
     * @throws IOException if directory traversal fails
     */
    public Path getOldestFile() throws IOException {
        return getCached("oldestFile", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return paths
                    .filter(Files::isRegularFile)
                    .min(Comparator.comparingLong(this::getLastModifiedTime))
                    .orElse(null);
            }
        });
    }
    
    /**
     * Lists distinct file extensions (lowercase) observed in the directory tree.
     *
     * @return a sorted, de-duplicated list of extensions without the dot; never {@code null}
     * @throws IOException if directory traversal fails
     */
    public List<String> getFileExtensions() throws IOException {
        return getCached("fileExtensions", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return paths
                    .filter(Files::isRegularFile)
                    .map(this::getFileExtension)
                    .filter(ext -> ext != null && !ext.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            }
        });
    }
    
    /**
     * Counts empty files (size = 0 bytes) in the directory tree.
     *
     * @return the number of empty files
     * @throws IOException if directory traversal fails
     */
    public int getEmptyFileCount() throws IOException {
        return getCached("emptyFileCount", () -> {
            try (Stream<Path> paths = Files.walk(directory)) {
                return (int) paths
                    .filter(Files::isRegularFile)
                    .filter(p -> getFileSize(p) == 0)
                    .count();
            }
        });
    }
    
    /**
     * Clears all cached results.
     * <p>
     * Call this when the underlying directory contents may have changed.
     */
    public void clearCache() {
        cache.clear();
        Logger.debug("Cache cleared for DirectoryAnalyzer");
    }
    
    // Helper methods
    
    /**
     * Determines if a file appears to be a supported image format.
     * <p>
     * First checks the extension against {@link #SUPPORTED_EXTENSIONS}; if matched,
     * attempts to verify the MIME type using {@link Files#probeContentType(Path)}.
     *
     * @param path the candidate file
     * @return {@code true} if the file looks like a supported image; {@code false} otherwise
     */
    private boolean isSupportedImageFormat(Path path) {
        String ext = getFileExtension(path);
        if (ext == null || ext.isEmpty()) return false;
        
        boolean supported = SUPPORTED_EXTENSIONS.contains(ext.toLowerCase());
        
        // Additional MIME type verification for better accuracy
        if (supported) {
            try {
                String mimeType = Files.probeContentType(path);
                return mimeType != null && mimeType.startsWith("image/");
            } catch (IOException e) {
                Logger.debug("Could not probe content type for {}", path);
                return true; // Trust extension if MIME check fails
            }
        }
        return false;
    }
    
    /**
     * Safely builds an {@link ImageFile} from a path.
     *
     * @param path the image file path
     * @return a new {@link ImageFile}, or {@code null} if creation failed
     */
    private ImageFile createImageFile(Path path) {
        try {
            return new ImageFile(path);
        } catch (IOException e) {
            Logger.warn("Failed to create ImageFile for {}: {}", path, e.getMessage());
            return null;
        }
    }
    
    /**
     * Returns the file size in bytes, or {@code 0} when it cannot be read.
     *
     * @param path the file path
     * @return the size in bytes, or {@code 0} if an error occurs
     */
    private long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            Logger.debug("Could not read size of {}", path);
            return 0;
        }
    }
    
    /**
     * Returns the last modification time in epoch milliseconds, or {@code 0} on error.
     *
     * @param path the file path
     * @return last-modified time in millis since the epoch, or {@code 0} if unavailable
     */
    private long getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            Logger.debug("Could not read modification time of {}", path);
            return 0;
        }
    }
    
    /**
     * Extracts the lowercase file extension (without dot) from the given path.
     *
     * @param path the file path
     * @return the extension, an empty string if none, or {@code null} if {@code path} is invalid
     */
    private String getFileExtension(Path path) {
        String filename = path.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex == -1 ? "" : filename.substring(dotIndex + 1).toLowerCase();
    }
    
    /**
     * Fetches a memoized value from the internal cache or computes it on demand.
     *
     * @param <T>      the value type
     * @param key      cache key
     * @param supplier computation that may perform I/O
     * @return the cached or freshly-computed value
     * @throws IOException if the supplier throws during computation
     */
    @SuppressWarnings("unchecked")
    private <T> T getCached(String key, IOSupplier<T> supplier) throws IOException {
        Object cached = cache.get(key);
        if (cached != null) {
            return (T) cached;
        }
        
        T value = supplier.get();
        cache.put(key, value);
        return value;
    }
    
    /**
     * Functional interface like {@link java.util.function.Supplier} but allowing {@link IOException}.
     *
     * @param <T> the supplied value type
     */
    @FunctionalInterface
    private interface IOSupplier<T> {
        /**
         * Gets a value, potentially performing I/O.
         *
         * @return the value
         * @throws IOException if an I/O error occurs
         */
        T get() throws IOException;
    }
}
