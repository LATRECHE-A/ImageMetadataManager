package test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;


public class SnapshotManager {
    private final Path snapshotDirectory;
    private final Path snapshotMetadataDirectory;
    private final boolean isPosix;

    public SnapshotManager(Path targetDirectory) {
        this.snapshotDirectory = Paths.get("snapshots");
        this.snapshotMetadataDirectory = Paths.get("snapshot_metadata");

        // check if the current file system supports POSIX permissions
        this.isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

        try {
            // create directories
            Files.createDirectories(snapshotDirectory);
            Files.createDirectories(snapshotMetadataDirectory);

            // set restrictive permissions for snapshot directories
            setRestrictivePermissions(snapshotDirectory);
            setRestrictivePermissions(snapshotMetadataDirectory);
        } catch (IOException e) {
            System.err.println("Erreur lors de la création des répertoires snapshot: " + e.getMessage());
        }
    }

    private void setRestrictivePermissions(Path directory) throws IOException {
        if (isPosix) {
            // POSIX-compliant systems (Linux, macOS)
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);

            Files.setPosixFilePermissions(directory, permissions);
        } else {
            // Windows-specific permission handling
            try {
                // Attempt to use Windows-specific file attribute view
                AclFileAttributeView aclView = Files.getFileAttributeView(directory, AclFileAttributeView.class);
                if (aclView != null) {
                    // Get the current owner
                    UserPrincipal owner = Files.getOwner(directory);

                    // Create an ACL that only allows the owner full control
                    List<AclEntry> acl = new ArrayList<>();
                    AclEntry ownerEntry = AclEntry.newBuilder()
                        .setType(AclEntryType.ALLOW)
                        .setPrincipal(owner)
                        .setPermissions(
                                AclEntryPermission.READ_DATA, 
                                AclEntryPermission.WRITE_DATA, 
                                AclEntryPermission.DELETE, 
                                AclEntryPermission.READ_ATTRIBUTES,
                                AclEntryPermission.WRITE_ATTRIBUTES,
                                AclEntryPermission.READ_NAMED_ATTRS,
                                AclEntryPermission.EXECUTE
                                )
                        .build();

                    acl.add(ownerEntry);

                    // Set the new ACL
                    aclView.setAcl(acl);
                }
            } catch (UnsupportedOperationException | SecurityException e) {
                System.err.println("Impossible de définir des autorisations spécifiques à Windows : " + e.getMessage());
            }
        }
    }

    private void setFileRestrictivePermissions(Path file) throws IOException {
        if (isPosix) {
            // POSIX-compliant systems
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);

            Files.setPosixFilePermissions(file, permissions);
        } else {
            // Windows-specific file permission handling
            try {
                AclFileAttributeView aclView = Files.getFileAttributeView(file, AclFileAttributeView.class);
                if (aclView != null) {
                    UserPrincipal owner = Files.getOwner(file);

                    List<AclEntry> acl = new ArrayList<>();
                    AclEntry ownerEntry = AclEntry.newBuilder()
                        .setType(AclEntryType.ALLOW)
                        .setPrincipal(owner)
                        .setPermissions(
                                AclEntryPermission.READ_DATA, 
                                AclEntryPermission.WRITE_DATA,
                                AclEntryPermission.READ_ATTRIBUTES,
                                AclEntryPermission.WRITE_ATTRIBUTES
                                )
                        .build();

                    acl.add(ownerEntry);

                    // Deny all other users
                    AclEntry denyOthers = AclEntry.newBuilder()
                        .setType(AclEntryType.DENY)
                        .setPrincipal(owner)
                        .setPermissions(
                                AclEntryPermission.READ_DATA, 
                                AclEntryPermission.WRITE_DATA
                                )
                        .build();

                    acl.add(denyOthers);

                    aclView.setAcl(acl);
                }
            } catch (UnsupportedOperationException | SecurityException e) {
                System.err.println("Impossible de définir les autorisations de fichiers spécifiques à Windows : " + e.getMessage());
            }
        }
    }

    public void saveSnapshot(List<ImageFile> files) throws IOException {
        // Format the current date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);

        // Create snapshot file
        String snapshotFileName = files.get(0).getPath().getParent().getFileName().toString() + 
            "_snapshot_" + timestamp + ".txt";
        Path snapshotFile = snapshotDirectory.resolve(snapshotFileName);
        Path metadataFile = snapshotMetadataDirectory.resolve(snapshotFileName + ".metadata");

        // Delete any existing snapshots for this directory
        deleteOldSnapshot(snapshotFile.getFileName().toString().split("_snapshot_")[0]);
        // Delete any existing snapshots for this directory
        deleteOldSnapshotMetadataFile(metadataFile.getFileName().toString().split("_snapshot_")[0]);

        // Prepare snapshot data
        Map<String, Long> snapshotData = files.stream()
            .collect(Collectors.toMap(
                        file -> file.getPath().toString(), 
                        ImageFile::getSize
                        ));

        // Write snapshot file
        Files.write(snapshotFile, snapshotData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.toList()));

        // Generate and store file hash
        String fileHash = generateFileHash(snapshotFile);

        // Write metadata including hash and additional verification information
        List<String> metadataContent = Arrays.asList(
                "HASH:" + fileHash,
                "TIMESTAMP:" + timestamp,
                "FILE_SIZE:" + Files.size(snapshotFile)
                );
        Files.write(metadataFile, metadataContent);

        // Set restrictive permissions on both files
        setFileRestrictivePermissions(snapshotFile);
        setFileRestrictivePermissions(metadataFile);
    }


    public Map<String, List<Path>> compareSnapshots(List<ImageFile> currentFiles) throws IOException {
        Map<String, Long> previousSnapshot = loadMostRecentSnapshot();

        if (previousSnapshot == null) {
            System.out.println("Aucun snapshot trouvé. Veuillez créer un snapshot avant de procéder à une comparaison.");
            return Collections.emptyMap();
        }

        Map<String, Long> currentSnapshot = currentFiles.stream()
            .collect(Collectors.toMap(
                        file -> file.getPath().toAbsolutePath().toString(),
                        ImageFile::getSize
                        ));

        // Categorize files into new, modified, and deleted
        List<Path> newFiles = new ArrayList<>();
        List<Path> modifiedFiles = new ArrayList<>();
        List<Path> deletedFiles = new ArrayList<>();

        Set<String> allFilePaths = new HashSet<>(previousSnapshot.keySet());
        allFilePaths.addAll(currentSnapshot.keySet());

        for (String filePath : allFilePaths) {
            Long previousSize = previousSnapshot.get(filePath);
            Long currentSize = currentSnapshot.get(filePath);

            // File is new (exists in current snapshot, not in previous)
            if (previousSize == null && currentSize != null) {
                newFiles.add(Paths.get(filePath));
            }
            // File is deleted (exists in previous snapshot, not in current)
            else if (previousSize != null && currentSize == null) {
                deletedFiles.add(Paths.get(filePath));
            }
            // File is modified (size has changed)
            else if (!previousSize.equals(currentSize)) {
                modifiedFiles.add(Paths.get(filePath));
            }
        }

        // Return the categorized lists
        Map<String, List<Path>> categorizedFiles = new HashMap<>();
        categorizedFiles.put("Nouveau", newFiles);
        categorizedFiles.put("Modifié", modifiedFiles);
        categorizedFiles.put("Supprimé", deletedFiles);

        return categorizedFiles;
    }

    private String generateFileHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Read file content
            byte[] hashBytes = Files.readAllBytes(file);

            // Add file metadata
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            String metadata = attributes.creationTime().toString() + attributes.lastModifiedTime().toString();

            // Combine content and metadata
            digest.update(hashBytes);
            digest.update(metadata.getBytes(StandardCharsets.UTF_8));

            // Generate final hash
            byte[] hashResult = digest.digest();

            // Convert to hexadecimal representation
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashResult) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Algorithme de hachage non disponible", e);
        }
    }


    private boolean verifySnapshotIntegrity(Path snapshotFile, Path metadataFile) throws IOException {
        // Read metadata
        List<String> metadataLines = Files.readAllLines(metadataFile);

        // Extract stored hash
        String storedHash = metadataLines.stream()
            .filter(line -> line.startsWith("HASH:"))
            .findFirst()
            .map(line -> line.substring(5).trim())
            .orElseThrow(() -> new IOException("Aucun hachage trouvé dans les métadonnées"));

        // Generate current hash with the updated logic
        String currentHash = generateFileHash(snapshotFile);

        // Compare hashes
        return storedHash.equals(currentHash);
    }

    private Map<String, Long> loadMostRecentSnapshot() throws IOException {
        // Create a glob pattern that matches snapshot files
        // Example pattern: "*_snapshot_????????_??????.txt"
        String globPattern = "*_snapshot_[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]_[0-9][0-9][0-9][0-9][0-9][0-9].txt";
        Path latestSnapshot = null;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapshotDirectory, globPattern)) {
            // Find the most recent snapshot file (based on file modification time)
            for (Path file : stream) {
                if (latestSnapshot == null || 
                        Files.getLastModifiedTime(file).compareTo(Files.getLastModifiedTime(latestSnapshot)) > 0) {
                    latestSnapshot = file;
                        }
            }

            if (latestSnapshot == null) {
                System.out.println("Aucun fichier instantané trouvé correspondant au modèle : " + globPattern);
                return null; // No snapshot found, return empty map
            }

            System.out.println("Chargement de snapshot depuis : " + latestSnapshot);
        } catch (IOException e) {
            System.out.println("Erreur lors du chargement de snapshot : " + e.getMessage());
            throw e; // Re-throw the exception to handle it at a higher level
        }

        // Construct corresponding metadata file path
        Path metadataFile = snapshotMetadataDirectory.resolve(
                latestSnapshot.getFileName().toString() + ".metadata"
                );

        // Verify file integrity before loading
        if (!verifySnapshotIntegrity(latestSnapshot, metadataFile)) {
            throw new IOException("Le fichier instantané a été falsifié !");
        }

        // If integrity check passes, proceed with loading snapshot
        Map<String, Long> snapshotData = new HashMap<>();
        List<String> lines = Files.readAllLines(latestSnapshot);
        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                try {
                    String filePath = Paths.get(parts[0]).toAbsolutePath().toString();
                    Long fileSize = Long.parseLong(parts[1].trim());
                    snapshotData.put(filePath, fileSize);
                } catch (NumberFormatException e) {
                    System.out.println("Format de taille de fichier non valide dans la ligne : " + line);
                }
            }
        }

        return snapshotData;
    }

    // Helper method to delete old snapshot files for the target directory
    private void deleteOldSnapshot(String directoryName) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapshotDirectory,
                    directoryName + "_snapshot_*.txt")) {
            for (Path file : stream) {
                Files.deleteIfExists(file);
            }
        } catch (NoSuchFileException e) {
            // No snapshot files exist for this directory yet; continue without any action
        } catch (IOException e) {
            System.out.println("Erreur lors de la suppression des anciens snapshots : " + e.getMessage());
        }
    }

    // Helper method to delete old snapshot metadata file for the target directory
    private void deleteOldSnapshotMetadataFile(String directoryName) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapshotMetadataDirectory,
                    directoryName + "_snapshot_*.txt.metadata")) {
            for (Path file : stream) {
                Files.deleteIfExists(file);
            }
        } catch (NoSuchFileException e) {
            // No snapshot files exist for this directory yet; continue without any action
        } catch (IOException e) {
            System.out.println("Erreur lors de la suppression des anciens snapshots : " + e.getMessage());
        }
    }

    // Additional method to provide cross-platform logging and error handling
    private void logPermissionError(String operation, Exception e) {
        System.err.println("Autorisation " + operation + " échoué: " + e.getMessage());
        System.err.println("Système d'exploitation actuel : " + System.getProperty("os.name"));
        System.err.println("Le système de fichiers prend en charge POSIX : " + isPosix);
    }
}
