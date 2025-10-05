package com.imagemeta.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class ImageFileTest {
    
    @TempDir
    Path tempDir;
    
    private Path createTestImage(String name, int width, int height) throws IOException {
        Path imagePath = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "jpg", imagePath.toFile());
        return imagePath;
    }
    
    @Test
    void testConstructorValidImage() throws IOException {
        Path image = createTestImage("test.jpg", 100, 100);
        ImageFile imgFile = new ImageFile(image);
        
        assertNotNull(imgFile);
        assertEquals(image, imgFile.getPath());
        assertTrue(imgFile.getSize() > 0);
        assertEquals(100, imgFile.getWidth());
        assertEquals(100, imgFile.getHeight());
        assertTrue(imgFile.isValid());
    }
    
    @Test
    void testConstructorNullPath() {
        assertThrows(NullPointerException.class, () -> new ImageFile(null));
    }
    
    @Test
    void testConstructorNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.jpg");
        assertThrows(IOException.class, () -> new ImageFile(nonExistent));
    }
    
    @Test
    void testConstructorDirectory() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        assertThrows(IOException.class, () -> new ImageFile(dir));
    }
    
    @Test
    void testGetDimensions() throws IOException {
        Path image = createTestImage("test.jpg", 1920, 1080);
        ImageFile imgFile = new ImageFile(image);
        
        assertEquals("1920x1080", imgFile.getDimensions());
    }
    
    @Test
    void testGetFileFormat() throws IOException {
        Path image = createTestImage("test.jpg", 50, 50);
        ImageFile imgFile = new ImageFile(image);
        
        assertEquals("jpg", imgFile.getFileFormat());
    }
    
    @Test
    void testMimeTypeDetection() throws IOException {
        Path image = createTestImage("test.jpg", 50, 50);
        ImageFile imgFile = new ImageFile(image);
        
        assertTrue(imgFile.getMimeType().startsWith("image/"));
    }
    
    @Test
    void testTimestamps() throws IOException {
        Path image = createTestImage("test.jpg", 50, 50);
        ImageFile imgFile = new ImageFile(image);
        
        assertNotNull(imgFile.getLastModified());
        assertNotNull(imgFile.getCreationDate());
    }
    
    @Test
    void testEqualsAndHashCode() throws IOException {
        Path image1 = createTestImage("test1.jpg", 50, 50);
        Path image2 = createTestImage("test2.jpg", 50, 50);
        
        ImageFile imgFile1a = new ImageFile(image1);
        ImageFile imgFile1b = new ImageFile(image1);
        ImageFile imgFile2 = new ImageFile(image2);
        
        assertEquals(imgFile1a, imgFile1b);
        assertNotEquals(imgFile1a, imgFile2);
        assertEquals(imgFile1a.hashCode(), imgFile1b.hashCode());
    }
    
    @Test
    void testToString() throws IOException {
        Path image = createTestImage("test.jpg", 100, 200);
        ImageFile imgFile = new ImageFile(image);
        
        String str = imgFile.toString();
        assertTrue(str.contains("test.jpg"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("200"));
    }
}
