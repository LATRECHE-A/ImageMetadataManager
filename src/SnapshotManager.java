package test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SnapshotManager {
    private final Path snapshotDirectory;
    private final Path snapshotFile;

    public SnapshotManager(Path targetDirectory) {
        this.snapshotDirectory = Paths.get("snapshots");

        // Ensure the snapshots folder exists
        try {
            Files.createDirectories(snapshotDirectory);
        } catch (IOException e) {
            System.out.println("Erreur lors de la création du dossier de snapshots : " + e.getMessage());
        }

        // Format the current date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);

        // Create a unique snapshot file name based on directory name and timestamp
        String snapshotFileName = targetDirectory.getFileName().toString() + "_snapshot_" + timestamp + ".txt";
        this.snapshotFile = snapshotDirectory.resolve(snapshotFileName);
    }

    public void saveSnapshot(List<ImageFile> files) throws IOException {
        // Delete any existing snapshots for this directory before saving a new one
        deleteOldSnapshots(snapshotFile.getFileName().toString().split("_snapshot_")[0]);

        Map<String, Long> snapshotData = new HashMap<>();
        for (ImageFile file : files) {
            snapshotData.put(file.getPath().toString(), file.getSize());
        }
        Files.write(snapshotFile, snapshotData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.toList()));
    }

    /*
    public List<Path> compareSnapshots(List<ImageFile> currentFiles) throws IOException {
        // Load the previous snapshot file based on directory name
        Map<String, Long> previousSnapshot = loadMostRecentSnapshot();

        // If no previous snapshot exists, notify the user and exit
        if (previousSnapshot == null) {
            System.out.println("Aucun snapshot trouvé. Veuillez créer un snapshot avant de procéder à une comparaison.");
            return Collections.emptyList();
        }

        // Convert currentFiles to a map of absolute paths to file sizes
        Map<String, Long> currentSnapshot = currentFiles.stream()
            .collect(Collectors.toMap(
                    file -> file.getPath().toAbsolutePath().toString(),
                    ImageFile::getSize
            ));

        List<Path> changedFiles = new ArrayList<>();

        // Create a combined set of all file paths from both snapshots
        Set<String> allFilePaths = new HashSet<>(previousSnapshot.keySet());
        allFilePaths.addAll(currentSnapshot.keySet());

        // Single loop over combined file paths to check for changes
        for (String filePath : allFilePaths) {
            Long previousSize = previousSnapshot.get(filePath);
            Long currentSize = currentSnapshot.get(filePath);

            // File was added or modified
            if (previousSize == null || !previousSize.equals(currentSize)) {
                changedFiles.add(Paths.get(filePath));
            }
            // File was removed
            else if (currentSize == null) {
                changedFiles.add(Paths.get(filePath));
            }
        }

        return changedFiles;
    }
    */
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
        categorizedFiles.put("New", newFiles);
        categorizedFiles.put("Modified", modifiedFiles);
        categorizedFiles.put("Deleted", deletedFiles);

        return categorizedFiles;
    }


    // Helper method to delete old snapshot files for the target directory
    private void deleteOldSnapshots(String directoryName) {
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


    private Map<String, Long> loadMostRecentSnapshot() throws IOException {
        Map<String, Long> snapshotData = new HashMap<>();

        // Create a glob pattern that matches snapshot files
        // Example pattern: "*_snapshot_????????_??????.txt"
        String globPattern = "*_snapshot_[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]_[0-9][0-9][0-9][0-9][0-9][0-9].txt";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapshotDirectory, globPattern)) {
            // Find the most recent snapshot file (based on file modification time)
            Path latestSnapshot = null;
            for (Path file : stream) {
                if (latestSnapshot == null || 
                        Files.getLastModifiedTime(file).compareTo(Files.getLastModifiedTime(latestSnapshot)) > 0) {
                    latestSnapshot = file;
                        }
            }

            if (latestSnapshot == null) {
                System.out.println("No snapshot files found matching pattern: " + globPattern);
                return null; // No snapshot found, return empty map
            }

            System.out.println("Loading snapshot from: " + latestSnapshot);

            // Read and parse the snapshot file
            List<String> lines = Files.readAllLines(latestSnapshot);
            for (String line : lines) {
                String[] parts = line.split("=", 2); // Limit split to 2 parts to handle = in file paths
                if (parts.length == 2) {
                    try {
                        String filePath = Paths.get(parts[0]).toAbsolutePath().toString();
                        Long fileSize = Long.parseLong(parts[1].trim());
                        snapshotData.put(filePath, fileSize);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid file size format in line: " + line);
                    }
                } else {
                    System.out.println("Invalid line format: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading snapshot: " + e.getMessage());
            throw e; // Re-throw the exception to handle it at a higher level
        }

        return snapshotData;
    }
}
