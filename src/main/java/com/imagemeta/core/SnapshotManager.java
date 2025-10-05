package com.imagemeta.core;

import com.imagemeta.util.logging.Logger;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates and manages on-disk directory snapshots with integrity verification.
 * <p>
 * Each snapshot is a simple {@code key=value} list of absolute file paths and sizes,
 * accompanied by a metadata file containing a content hash and basic attributes.
 * The most recent snapshot is used as the baseline for comparisons.
 *
 * @implNote Permissions are restricted on supported platforms. On POSIX filesystems,
 *           directory and file permissions are set to owner-only. On Windows,
 *           an ACL entry is applied for the owner when supported.
 */
public final class SnapshotManager {
    private final Path snapshotDirectory;
    private final Path metadataDirectory;
    private final boolean isPosix;
    private final String targetDirName;
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Constructs a manager tied to a specific target directory name.
     * <p>
     * Snapshot and metadata files are stored in sibling directories named
     * {@code snapshots} and {@code snapshot_metadata} relative to the process CWD.
     *
     * @param targetDirectory the directory whose name is encoded into snapshot filenames
     * @throws NullPointerException if {@code targetDirectory} is {@code null}
     */
    public SnapshotManager(Path targetDirectory) {
        Objects.requireNonNull(targetDirectory, "Target directory cannot be null");
        
        this.snapshotDirectory = Paths.get("snapshots");
        this.metadataDirectory = Paths.get("snapshot_metadata");
        this.targetDirName = targetDirectory.getFileName() != null 
            ? targetDirectory.getFileName().toString() 
            : "unknown";
        this.isPosix = FileSystems.getDefault()
            .supportedFileAttributeViews().contains("posix");
        
        initializeDirectories();
    }
    
    /**
     * Ensures snapshot storage directories exist and sets restrictive permissions where possible.
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(snapshotDirectory);
            Files.createDirectories(metadataDirectory);
            setRestrictivePermissions(snapshotDirectory);
            setRestrictivePermissions(metadataDirectory);
            Logger.debug("Initialized snapshot directories");
        } catch (IOException e) {
            Logger.error("Failed to create snapshot directories", e);
        }
    }
    
    /**
     * Persists a new snapshot for the provided file list and stores a matching metadata file.
     * <p>
     * Any older snapshots for the same target are removed before saving the new one.
     *
     * @param files the current image files; may be {@code null} (treated as empty)
     * @throws IOException if writing snapshot or metadata files fails
     */
    public void saveSnapshot(List<ImageFile> files) throws IOException {
        if (files == null) {
            files = Collections.emptyList();
            Logger.warn("Saving empty snapshot");
        }
        
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = targetDirName + "_snapshot_" + timestamp + ".txt";
        Path snapshotFile = snapshotDirectory.resolve(filename);
        Path metadataFile = metadataDirectory.resolve(filename + ".metadata");
        
        // Remove old snapshots
        deleteOldSnapshots(targetDirName);
        
        // Create snapshot data
        Map<String, Long> data = files.stream()
            .collect(Collectors.toMap(
                f -> f.getPath().toString(),
                ImageFile::getSize,
                (v1, v2) -> v1,
                LinkedHashMap::new
            ));
        
        // Write snapshot
        List<String> lines = data.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.toList());
        Files.write(snapshotFile, lines, StandardOpenOption.CREATE);
        
        // Generate and store hash
        String hash = generateHash(snapshotFile);
        List<String> metadata = Arrays.asList(
            "HASH:" + hash,
            "TIMESTAMP:" + timestamp,
            "FILE_SIZE:" + Files.size(snapshotFile),
            "FILE_COUNT:" + files.size()
        );
        Files.write(metadataFile, metadata, StandardOpenOption.CREATE);
        
        // Set permissions
        setRestrictiveFilePermissions(snapshotFile);
        setRestrictiveFilePermissions(metadataFile);
        
        Logger.info("Snapshot saved: {} ({} files)", filename, files.size());
    }
    
    /**
     * Compares the latest saved snapshot to the current files and categorizes differences.
     * <p>
     * The result map contains three keys: {@code "Nouveau"}, {@code "Modifie"}, and {@code "Supprime"},
     * each mapping to a list of affected {@link Path}s.
     *
     * @param currentFiles the currently observed files
     * @return a map of change categories to file paths; empty if no baseline snapshot exists
     * @throws IOException if loading the latest snapshot or verifying integrity fails
     */
    public Map<String, List<Path>> compareSnapshots(List<ImageFile> currentFiles) 
            throws IOException {
        Map<String, Long> previous = loadLatestSnapshot();
        
        if (previous == null) {
            Logger.warn("No previous snapshot found");
            return Collections.emptyMap();
        }
        
        Map<String, Long> current = currentFiles.stream()
            .collect(Collectors.toMap(
                f -> f.getPath().toAbsolutePath().toString(),
                ImageFile::getSize,
                (v1, v2) -> v1
            ));
        
        List<Path> newFiles = new ArrayList<>();
        List<Path> modified = new ArrayList<>();
        List<Path> deleted = new ArrayList<>();
        
        Set<String> allPaths = new HashSet<>();
        allPaths.addAll(previous.keySet());
        allPaths.addAll(current.keySet());
        
        Set<String> processed = new HashSet<>();
        
        for (String path : allPaths) {
            Long prevSize = previous.get(path);
            Long currSize = current.get(path);
            
            if (prevSize == null && currSize != null) {
                // New file
                if (!processed.contains(path)) {
                    newFiles.add(Paths.get(path));
                }
            } else if (prevSize != null && currSize == null) {
                // Possibly deleted or renamed
                boolean renamed = findRenamedFile(path, prevSize, current, modified, processed);
                if (!renamed) {
                    deleted.add(Paths.get(path));
                }
            } else if (prevSize != null && !prevSize.equals(currSize)) {
                // Modified
                modified.add(Paths.get(path));
                processed.add(path);
            }
        }
        
        Map<String, List<Path>> result = new HashMap<>();
        result.put("Nouveau", newFiles);
        result.put("Modifie", modified);
        result.put("Supprime", deleted);
        
        Logger.info("Snapshot comparison: {} new, {} modified, {} deleted",
            newFiles.size(), modified.size(), deleted.size());
        
        return result;
    }
    
    /**
     * Attempts to detect a rename event by matching a missing file's size to an
     * existing path with the same size in the current set. If found, the path is
     * treated as modified.
     *
     * @param oldPath   the path missing from current set
     * @param size      the previous file size
     * @param current   the current map of path-to-size
     * @param modified  output list to record modified paths
     * @param processed set of paths already processed
     * @return {@code true} if a likely rename was detected; {@code false} otherwise
     */
    private boolean findRenamedFile(String oldPath, Long size, 
            Map<String, Long> current, List<Path> modified, Set<String> processed) {
        for (Map.Entry<String, Long> entry : current.entrySet()) {
            if (entry.getValue().equals(size) && !entry.getKey().equals(oldPath)) {
                modified.add(Paths.get(entry.getKey()));
                processed.add(entry.getKey());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Loads the most recent snapshot for the target directory and validates its integrity.
     *
     * @return a map of absolute path strings to file sizes, or {@code null} if none exist
     * @throws IOException if integrity verification fails or the snapshot cannot be read
     */
    private Map<String, Long> loadLatestSnapshot() throws IOException {
        String pattern = targetDirName + "_snapshot_*.txt";
        Path latest = null;
        
        try (DirectoryStream<Path> stream = 
                Files.newDirectoryStream(snapshotDirectory, pattern)) {
            for (Path file : stream) {
                if (latest == null || 
                        Files.getLastModifiedTime(file)
                            .compareTo(Files.getLastModifiedTime(latest)) > 0) {
                    latest = file;
                }
            }
        }
        
        if (latest == null) {
            return null;
        }
        
        Path metaFile = metadataDirectory.resolve(latest.getFileName() + ".metadata");
        
        // Verify integrity
        if (!verifyIntegrity(latest, metaFile)) {
            throw new IOException("Snapshot integrity check failed!");
        }
        
        // Load snapshot
        Map<String, Long> data = new HashMap<>();
        List<String> lines = Files.readAllLines(latest);
        
        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                try {
                    String path = Paths.get(parts[0]).toAbsolutePath().toString();
                    Long size = Long.parseLong(parts[1].trim());
                    data.put(path, size);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid size in snapshot line: {}", line);
                }
            }
        }
        
        return data;
    }
    
    /**
     * Creates a SHA-256 hash for the snapshot content plus select file metadata.
     *
     * @param file the snapshot file
     * @return the lowercase hex-encoded hash
     * @throws IOException if the snapshot cannot be read or hashing fails
     */
    private String generateHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] content = Files.readAllBytes(file);
            digest.update(content);
            
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            String metadata = attr.creationTime() + "|" + attr.lastModifiedTime();
            digest.update(metadata.getBytes());
            
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }
    
    /**
     * Verifies the integrity of a snapshot against its metadata file by recomputing
     * and comparing the stored hash.
     *
     * @param snapshot the snapshot data file
     * @param metadata the associated metadata file
     * @return {@code true} if the hash matches; {@code false} otherwise
     * @throws IOException if the metadata cannot be read
     */
    private boolean verifyIntegrity(Path snapshot, Path metadata) throws IOException {
        if (!Files.exists(metadata)) {
            Logger.warn("Metadata file missing: {}", metadata);
            return false;
        }
        
        List<String> lines = Files.readAllLines(metadata);
        String storedHash = lines.stream()
            .filter(l -> l.startsWith("HASH:"))
            .findFirst()
            .map(l -> l.substring(5).trim())
            .orElse(null);
        
        if (storedHash == null) {
            Logger.warn("No hash found in metadata");
            return false;
        }
        
        String currentHash = generateHash(snapshot);
        return storedHash.equals(currentHash);
    }
    
    /**
     * Deletes any existing snapshots and metadata files for the given directory name.
     *
     * @param dirName the directory name prefix to match (target)
     */
    private void deleteOldSnapshots(String dirName) {
        try {
            String pattern = dirName + "_snapshot_*.txt";
            try (DirectoryStream<Path> stream = 
                    Files.newDirectoryStream(snapshotDirectory, pattern)) {
                for (Path file : stream) {
                    Files.deleteIfExists(file);
                }
            }
            
            try (DirectoryStream<Path> stream = 
                    Files.newDirectoryStream(metadataDirectory, pattern + ".metadata")) {
                for (Path file : stream) {
                    Files.deleteIfExists(file);
                }
            }
        } catch (IOException e) {
            Logger.error("Failed to delete old snapshots", e);
        }
    }
    
    /**
     * Applies owner-only permissions to a directory, when supported by the platform.
     *
     * @param dir the directory to secure
     * @throws IOException if applying POSIX permissions fails
     */
    private void setRestrictivePermissions(Path dir) throws IOException {
        if (isPosix) {
            Set<PosixFilePermission> perms = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            );
            Files.setPosixFilePermissions(dir, perms);
        } else {
            // Windows ACL handling
            try {
                AclFileAttributeView acl = Files.getFileAttributeView(
                    dir, AclFileAttributeView.class);
                if (acl != null) {
                    UserPrincipal owner = Files.getOwner(dir);
                    AclEntry entry = AclEntry.newBuilder()
                        .setType(AclEntryType.ALLOW)
                        .setPrincipal(owner)
                        .setPermissions(
                            AclEntryPermission.READ_DATA,
                            AclEntryPermission.WRITE_DATA,
                            AclEntryPermission.EXECUTE,
                            AclEntryPermission.READ_ATTRIBUTES,
                            AclEntryPermission.WRITE_ATTRIBUTES
                        )
                        .build();
                    acl.setAcl(Collections.singletonList(entry));
                }
            } catch (UnsupportedOperationException e) {
                Logger.debug("ACL not supported on this filesystem");
            }
        }
    }
    
    /**
     * Applies owner read/write permissions to a regular file on POSIX systems.
     * <p>
     * On non-POSIX systems, no action is taken.
     *
     * @param file the file to secure
     * @throws IOException if applying POSIX permissions fails
     */
    private void setRestrictiveFilePermissions(Path file) throws IOException {
        if (isPosix) {
            Set<PosixFilePermission> perms = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(file, perms);
        }
    }
}
