package com.imagemeta.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;

class DirectoryAnalyzerTest {
    
    @TempDir
    Path tempDir;
    
    private Path createImage(String name, int size) throws IOException {
        Path path = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "jpg", path.toFile());
        return path;
    }
    
    private Path createTextFile(String name, String content) throws IOException {
        Path path = tempDir.resolve(name);
        Files.writeString(path, content);
        return path;
    }
    
    @Test
    void testConstructorValidDirectory() {
        assertDoesNotThrow(() -> new DirectoryAnalyzer(tempDir));
    }
    
    @Test
    void testConstructorNullDirectory() {
        assertThrows(NullPointerException.class, () -> new DirectoryAnalyzer(null));
    }
    
    @Test
    void testConstructorNonExistentDirectory() {
        Path nonExistent = tempDir.resolve("nonexistent");
        assertThrows(IllegalArgumentException.class, () -> new DirectoryAnalyzer(nonExistent));
    }
    
    @Test
    void testConstructorFile() throws IOException {
        Path file = createImage("test.jpg", 1000);
        assertThrows(IllegalArgumentException.class, () -> new DirectoryAnalyzer(file));
    }
    
    @Test
    void testListImageFilesEmpty() throws IOException {
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        List<ImageFile> images = analyzer.listImageFiles();
        
        assertTrue(images.isEmpty());
    }
    
    @Test
    void testListImageFilesSingle() throws IOException {
        createImage("test.jpg", 1000);
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        
        List<ImageFile> images = analyzer.listImageFiles();
        assertEquals(1, images.size());
    }
    
    @Test
    void testListImageFilesMultiple() throws IOException {
        createImage("img1.jpg", 1000);
        createImage("img2.png", 2000);
        createImage("img3.webp", 3000);
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        List<ImageFile> images = analyzer.listImageFiles();
        
        assertEquals(3, images.size());
    }
    
    @Test
    void testListImageFilesIgnoresNonImages() throws IOException {
        createImage("image.jpg", 1000);
        createTextFile("readme.txt", "test");
        createTextFile("data.csv", "a,b,c");
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        List<ImageFile> images = analyzer.listImageFiles();
        
        assertEquals(1, images.size());
    }
    
    @Test
    void testGetTotalFileCount() throws IOException {
        createImage("img1.jpg", 1000);
        createTextFile("file1.txt", "test");
        createTextFile("file2.txt", "test");
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        assertEquals(3, analyzer.getTotalFileCount());
    }
    
    @Test
    void testGetImageFileCount() throws IOException {
        createImage("img1.jpg", 1000);
        createImage("img2.png", 2000);
        createTextFile("file.txt", "test");
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        assertEquals(2, analyzer.getImageFileCount());
    }
    
    @Test
    void testGetTotalFileSize() throws IOException {
        createImage("img1.jpg", 1000);
        createImage("img2.jpg", 1000);
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        assertTrue(analyzer.getTotalFileSize() > 0);
    }
    
    @Test
    void testGetLargestFile() throws IOException {
        Path small = createImage("small.jpg", 100);
        Path large = createImage("large.jpg", 1000);
        
        // Make large file actually larger
        Files.write(large, new byte[5000]);
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        Path largest = analyzer.getLargestFile();
        
        assertNotNull(largest);
        assertTrue(largest.getFileName().toString().contains("large"));
    }
    
    @Test
    void testGetSmallestFile() throws IOException {
        Path small = createImage("small.jpg", 100);
        Path large = createImage("large.jpg", 1000);
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        Path smallest = analyzer.getSmallestFile();
        
        assertNotNull(smallest);
    }
    
    @Test
    void testGetAverageFileSize() throws IOException {
        createImage("img1.jpg", 1000);
        createImage("img2.jpg", 1000);
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        long average = analyzer.getAverageFileSize();
        
        assertTrue(average > 0);
    }
    
    @Test
    void testGetMostCommonFileType() throws IOException {
        createImage("img1.jpg", 1000);
        createImage("img2.jpg", 1000);
        createImage("img3.png", 1000);
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        String mostCommon = analyzer.getMostCommonFileType();
        
        assertEquals("jpg", mostCommon);
    }
    
    @Test
    void testGetSubdirectoryCount() throws IOException {
        Files.createDirectory(tempDir.resolve("subdir1"));
        Files.createDirectory(tempDir.resolve("subdir2"));
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        assertEquals(2, analyzer.getSubdirectoryCount());
    }
    
    @Test
    void testGetFileExtensions() throws IOException {
        createImage("img1.jpg", 1000);
        createImage("img2.png", 1000);
        createImage("img3.webp", 1000);
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        List<String> extensions = analyzer.getFileExtensions();
        
        assertTrue(extensions.contains("jpg"));
        assertTrue(extensions.contains("png"));
        assertTrue(extensions.contains("webp"));
    }
    
    @Test
    void testGetEmptyFileCount() throws IOException {
        createImage("img1.jpg", 1000);
        Files.createFile(tempDir.resolve("empty.txt"));
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        assertEquals(1, analyzer.getEmptyFileCount());
    }
    
    @Test
    void testClearCache() throws IOException {
        createImage("img1.jpg", 1000);
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        
        // First call
        List<ImageFile> images1 = analyzer.listImageFiles();
        
        // Add new image
        createImage("img2.jpg", 1000);
        
        // Clear cache
        analyzer.clearCache();
        
        // Second call should see new file
        List<ImageFile> images2 = analyzer.listImageFiles();
        
        assertEquals(1, images1.size());
        assertEquals(2, images2.size());
    }
    
    @Test
    void testNestedDirectories() throws IOException {
        Path subdir = Files.createDirectory(tempDir.resolve("subdir"));
        createImage("img1.jpg", 1000);
        
        // Create image in subdirectory
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "jpg", subdir.resolve("img2.jpg").toFile());
        
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(tempDir);
        List<ImageFile> images = analyzer.listImageFiles();
        
        assertEquals(2, images.size());
    }
}
