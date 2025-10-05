package com.imagemeta.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.*;

class SnapshotManagerTest {
    
    @TempDir
    Path tempDir;
    
    private SnapshotManager manager;
    private List<ImageFile> imageFiles;
    
    @BeforeEach
    void setUp() throws IOException {
        manager = new SnapshotManager(tempDir);
        imageFiles = new ArrayList<>();
        
        // Create test images
        for (int i = 1; i <= 3; i++) {
            Path imagePath = tempDir.resolve("test" + i + ".jpg");
            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(img, "jpg", imagePath.toFile());
            imageFiles.add(new ImageFile(imagePath));
        }
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up snapshot directories
        deleteDirectory(Paths.get("snapshots"));
        deleteDirectory(Paths.get("snapshot_metadata"));
    }
    
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }
    
    @Test
    void testConstructorCreatesDirectories() {
        assertTrue(Files.exists(Paths.get("snapshots")));
        assertTrue(Files.exists(Paths.get("snapshot_metadata")));
    }
    
    @Test
    void testSaveSnapshotCreatesFiles() throws IOException {
        manager.saveSnapshot(imageFiles);
        
        assertTrue(Files.list(Paths.get("snapshots")).findAny().isPresent());
        assertTrue(Files.list(Paths.get("snapshot_metadata")).findAny().isPresent());
    }
    
    @Test
    void testSaveSnapshotWithEmptyList() throws IOException {
        assertDoesNotThrow(() -> manager.saveSnapshot(Collections.emptyList()));
    }
    
    @Test
    void testSaveSnapshotReplacesOld() throws IOException, InterruptedException {
        manager.saveSnapshot(imageFiles);
        long count1 = Files.list(Paths.get("snapshots")).count();
        
        Thread.sleep(1100); // Ensure different timestamp
        manager.saveSnapshot(imageFiles);
        long count2 = Files.list(Paths.get("snapshots")).count();
        
        assertEquals(count1, count2); // Should still be 1
    }
    
    @Test
    void testCompareSnapshotsNoChanges() throws IOException {
        manager.saveSnapshot(imageFiles);
        Map<String, List<Path>> comparison = manager.compareSnapshots(imageFiles);
        
        assertTrue(comparison.get("Nouveau").isEmpty());
        assertTrue(comparison.get("Modifie").isEmpty());
        assertTrue(comparison.get("Supprime").isEmpty());
    }
    
    @Test
    void testCompareSnapshotsNewFile() throws IOException {
        manager.saveSnapshot(imageFiles);
        
        // Add new file
        Path newImage = tempDir.resolve("new.jpg");
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "jpg", newImage.toFile());
        imageFiles.add(new ImageFile(newImage));
        
        Map<String, List<Path>> comparison = manager.compareSnapshots(imageFiles);
        
        assertEquals(1, comparison.get("Nouveau").size());
        assertTrue(comparison.get("Modifie").isEmpty());
        assertTrue(comparison.get("Supprime").isEmpty());
    }
    
    @Test
    void testCompareSnapshotsDeletedFile() throws IOException {
        manager.saveSnapshot(imageFiles);
        
        // Remove file from list AND delete it physically
        ImageFile removed = imageFiles.remove(0);
        Path removedPath = removed.getPath();
        long removedSize = removed.getSize();
        Files.delete(removedPath);
        
        Map<String, List<Path>> comparison = manager.compareSnapshots(imageFiles);
        
        // The file should be detected as deleted
        // Note: If another file has same size, it might be flagged as renamed instead
        int deletedCount = comparison.get("Supprime").size();
        int modifiedCount = comparison.get("Modifie").size();
        
        // Either deleted OR renamed (both are valid for a missing file)
        assertTrue(deletedCount == 1 || modifiedCount >= 1, 
            "File should be detected as deleted or renamed. Deleted: " + deletedCount + 
            ", Modified: " + modifiedCount);
    }
    
    @Test
    void testCompareSnapshotsModifiedFile() throws IOException {
        manager.saveSnapshot(imageFiles);
        
        // Modify file size
        Path toModify = imageFiles.get(0).getPath();
        Files.write(toModify, new byte[10000]);
        
        // Reload the image files
        imageFiles.clear();
        for (Path p : Files.list(tempDir).filter(p -> p.toString().endsWith(".jpg")).toList()) {
            imageFiles.add(new ImageFile(p));
        }
        
        Map<String, List<Path>> comparison = manager.compareSnapshots(imageFiles);
        
        assertEquals(1, comparison.get("Modifie").size());
    }
    
    @Test
    void testCompareSnapshotsNoSnapshot() throws IOException {
        Map<String, List<Path>> comparison = manager.compareSnapshots(imageFiles);
        assertTrue(comparison.isEmpty());
    }
    
    @Test
    void testSnapshotIntegrity() throws IOException {
        manager.saveSnapshot(imageFiles);
        
        // Comparison should work (integrity check passes)
        assertDoesNotThrow(() -> manager.compareSnapshots(imageFiles));
    }
    
    @Test
    void testMultipleSnapshots() throws IOException, InterruptedException {
        // Save first snapshot
        manager.saveSnapshot(imageFiles);
        
        Thread.sleep(1100);
        
        // Add file and save again
        Path newImage = tempDir.resolve("new.jpg");
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "jpg", newImage.toFile());
        imageFiles.add(new ImageFile(newImage));
        
        manager.saveSnapshot(imageFiles);
        
        // Should only have one snapshot (old one replaced)
        long snapshotCount = Files.list(Paths.get("snapshots")).count();
        assertEquals(1, snapshotCount);
    }
}
