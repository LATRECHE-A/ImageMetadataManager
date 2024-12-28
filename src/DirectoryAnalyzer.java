package test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * La classe DirectoryAnalyzer permet d'analyser les fichiers et sous-répertoires d'un dossier donné,
 * en offrant des statistiques détaillées comme le nombre de fichiers, les tailles moyennes, et les formats les plus courants.
 *
 * Elle repose sur les API NIO de Java pour explorer les fichiers de manière efficace.
 * 
 * @author DIALLO
 * @version 1.0
 */
public class DirectoryAnalyzer {
    private final Path directory;

    /**
     * Constructeur de la classe DirectoryAnalyzer.
     * @param directory le chemin du répertoire
     */
    public DirectoryAnalyzer(Path directory) {
        this.directory = directory;
    }

    /**
     * Liste les fichiers image pris en charge dans le répertoire et ses sous-répertoires.
     *
     * @return une liste des fichiers image
     * @throws IOException si une erreur d'entrée/sortie se produit
     */
    public List<ImageFile> listImageFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .filter(this::isSupportedImageFormat)
                        .map(this::createImageFile)
                        .collect(Collectors.toList());
        }
    }
    
    /**
     * Calcule le nombre total de fichiers dans le répertoire.
     *
     * @return le nombre total de fichiers
     * @throws IOException si une erreur d'entrée/sortie se produit
     */
    public int getTotalFileCount() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return (int) paths.filter(Files::isRegularFile).count();
        }
    }

    /**
     * Retourne le nombre de fichiers image dans le répertoire.
     *
     * @return le nombre de fichiers image
     * @throws IOException si une erreur d'entrée/sortie se produit
     */
    public int getImageFileCount() throws IOException {
        return listImageFiles().size();
    }

    /**
     * Retourne la taille totale des fichiers dans le répertoire.
     *
     * @return la taille totale en octets
     * @throws IOException si une erreur d'entrée/sortie se produit
     */
    public long getTotalFileSize() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .mapToLong(this::getFileSize)
                        .sum();
        }
    }

    /**
     * Retourne le fichier le plus volumineux dans le répertoire.
     *
     * @return le chemin du fichier le plus volumineux ou {@code null} si aucun fichier n'est trouvé
     * @throws IOException si une erreur d'entrée/sortie se produit
     */
    public Path getLargestFile() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .max(Comparator.comparingLong(this::getFileSize))
                        .orElse(null);
        }
    }

    /**
     * Retourne le fichier le moins volumineux dans le répertoire.
     *
     * @return le chemin du fichier le moins volumineux ou {@code null} si aucun fichier n'est trouvé
     * @throws IOException si une erreur d'entrée/sortie se produit
     */
    public Path getSmallestFile() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .min(Comparator.comparingLong(this::getFileSize))
                        .orElse(null);
        }
    }

    /**
     * Calcule la taille moyenne des fichiers dans le répertoire.
     *
     * @return la taille moyenne des fichiers en octets, ou 0 si aucun fichier n'est trouvé.
     * @throws IOException si une erreur d'entrée/sortie se produit
     */
    public long getAverageFileSize() throws IOException {
        long totalSize = getTotalFileSize();
        int fileCount = getTotalFileCount();
        return fileCount == 0 ? 0 : totalSize / fileCount;
    }

    /**
     * Trouve le type de fichier (extension) le plus commun dans le répertoire.
     *
     * @return l'extension de fichier la plus commune ou "Unknown" si aucune n'est trouvée.
     * @throws IOException si une erreur survient lors de l'accès au répertoire.
     */
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

    /**
     * Compte le nombre de sous-répertoires dans le répertoire.
     *
     * @return le nombre de sous-répertoires.
     * @throws IOException si une erreur survient lors de l'accès au répertoire.
     */
    public int getSubdirectoryCount() throws IOException {
        try (Stream<Path> paths = Files.walk(directory, 1)) {
            return (int) paths.filter(Files::isDirectory)
                              .count() - 1; // Subtract 1 for the root directory itself
        }
    }

    /**
     * Trouve le fichier le plus récemment modifié dans le répertoire.
     *
     * @return le chemin du fichier le plus récemment modifié, ou {@code null} si aucun fichier n'existe.
     * @throws IOException si une erreur survient lors de l'accès au répertoire.
     */
    public Path getLastModifiedFile() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .max(Comparator.comparingLong(this::getLastModifiedTime))
                        .orElse(null);
        }
    }

    /**
     * Trouve le fichier le plus ancien dans le répertoire.
     *
     * @return le chemin du fichier le plus ancien, ou {@code null} si aucun fichier n'existe.
     * @throws IOException si une erreur survient lors de l'accès au répertoire.
     */
    public Path getOldestFile() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .min(Comparator.comparingLong(this::getLastModifiedTime))
                        .orElse(null);
        }
    }

    /**
     * Retourne les extensions de fichiers uniques dans le répertoire.
     *
     * @return une liste d'extensions uniques
     * @throws IOException si une erreur d'entrée/sortie se produit
     */
    public List<String> getFileExtensions() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .map(this::getFileExtension)
                        .filter(ext -> ext != null && !ext.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
        }
    }

    /**
     * Retourne le nombre de fichiers vides dans le répertoire.
     *
     * @return le nombre de fichiers de taille zéro
     * @throws IOException si une erreur d'entrée/sortie se produit
     */
    public int getEmptyFileCount() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return (int) paths.filter(Files::isRegularFile)
                              .filter(path -> getFileSize(path) == 0)
                              .count();
        }
    }

    /*public long getTotalDirectorySize() throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .mapToLong(this::getFileSize)
                        .sum();
        }
    }*/

    private boolean isSupportedImageFormat(Path path) {
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(path);
        } catch (IOException e) {
            e.printStackTrace();
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
