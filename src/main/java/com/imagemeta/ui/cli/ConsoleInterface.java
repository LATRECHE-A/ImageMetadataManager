package com.imagemeta.ui.cli;

import com.imagemeta.core.*;
import com.imagemeta.metadata.ImageMetadata;
import com.imagemeta.metadata.MetadataExtractor;
import com.imagemeta.util.logging.Logger;
import com.imagemeta.util.terminal.AnsiColors;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Command-line interface entry point for ImageMeta operations.
 * <p>
 * Supports directory-wide analysis (listing, statistics, snapshot comparison),
 * single-file inspection (stats stats and metadataamp; metadata), snapshot persistence, and simple
 * search by name/year/dimensions. Output is colorized when the terminal supports ANSI.
 * <p>
 * This class performs only coordination and presentation; all heavy-lifting is
 * delegated to {@link DirectoryAnalyzer}, {@link ImageFile}, {@link SnapshotManager},
 * and {@link MetadataExtractor}.
 *
 * @implNote The CLI may call {@link System#exit(int)} after printing help when no
 *           actionable option is provided. Error handling logs via {@code Logger}
 *           and prints a concise, colorized message to stderr.
 */
public final class ConsoleInterface {
    
    /**
     * Parses CLI arguments and dispatches to the appropriate handler.
     * <p>
     * Usage summary (see {@link #printHelp()} for details):
     * <ul>
     *   <li><code>-d &lt;dir&gt; --list|--stat|--compare-snapshot</code></li>
     *   <li><code>-f &lt;file&gt; --stat|--info [--stat]</code></li>
     *   <li><code>--snapshotsave &lt;dir&gt;</code></li>
     *   <li><code>--search &lt;dir&gt; name=&lt;txt&gt; date=&lt;yyyy&gt; dimensions=WxH</code></li>
     * </ul>
     *
     * @param args raw command-line arguments
     */
    public static void start(String[] args) {
        try {
            if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
                printHelp();
                System.exit(0);
            }

            String option = args[0];
            switch (option) {
                case "-d":
                case "--directory":
                    handleDirectoryMode(args);
                    break;
                case "-f":
                case "--file":
                    handleFileMode(args);
                    break;
                case "--snapshotsave":
                    handleSnapshotMode(args);
                    break;
                case "--search":
                    handleSearchMode(args);
                    break;
                default:
                    error("Unrecognized option: " + option);
                    printHelp();
            }
        } catch (Exception e) {
            Logger.error("CLI error", e);
            error("Error: " + e.getMessage());
        }
    }

    /**
     * Handles directory-scoped commands: list/stat/compare-snapshot.
     *
     * @param args CLI args; expects <code>-d &lt;dir&gt; &lt;subcommand&gt;</code>
     * @throws IOException on filesystem errors during analysis
     */
    private static void handleDirectoryMode(String[] args) throws IOException {
        if (args.length < 3) {
            error("Usage: -d <directory> <--list|--stat|--compare-snapshot>");
            return;
        }

        Path directory = Paths.get(args[1]);
        if (!Files.isDirectory(directory)) {
            error("Not a directory: " + directory);
            return;
        }

        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(directory);
        String subCommand = args[2];

        switch (subCommand) {
            case "--list":
                handleList(analyzer);
                break;
            case "--stat":
                handleStats(analyzer);
                break;
            case "--compare-snapshot":
                handleCompareSnapshot(analyzer, directory);
                break;
            default:
                error("Unknown directory option: " + subCommand);
        }
    }

    /**
     * Prints a simple list of discovered image files.
     *
     * @param analyzer directory analyzer bound to the root directory
     * @throws IOException on traversal error
     */
    private static void handleList(DirectoryAnalyzer analyzer) throws IOException {
        List<ImageFile> files = analyzer.listImageFiles();
        if (files.isEmpty()) {
            info("No image files found.");
        } else {
            success("Image files found: " + files.size());
            files.forEach(f -> System.out.println("  " + f.getPath()));
        }
    }

    /**
     * Prints a set of directory statistics (counts, sizes, common type, etc.).
     *
     * @param analyzer directory analyzer bound to the root directory
     * @throws IOException on traversal error
     */
    private static void handleStats(DirectoryAnalyzer analyzer) throws IOException {
        success("Directory Statistics:");
        System.out.println("─".repeat(50));
        
        stat("Total files", analyzer.getTotalFileCount());
        stat("Image files", analyzer.getImageFileCount());
        stat("Total size", formatSize(analyzer.getTotalFileSize()));
        stat("Average size", formatSize(analyzer.getAverageFileSize()));
        
        Path largest = analyzer.getLargestFile();
        if (largest != null) {
            stat("Largest file", largest.getFileName() + 
                " (" + formatSize(Files.size(largest)) + ")");
        }
        
        Path smallest = analyzer.getSmallestFile();
        if (smallest != null) {
            stat("Smallest file", smallest.getFileName() + 
                " (" + formatSize(Files.size(smallest)) + ")");
        }
        
        stat("Most common type", analyzer.getMostCommonFileType());
        stat("Subdirectories", analyzer.getSubdirectoryCount());
        stat("Empty files", analyzer.getEmptyFileCount());
        
        List<String> extensions = analyzer.getFileExtensions();
        stat("File types", String.join(", ", extensions));
    }

    /**
     * Handles file-scoped subcommands for a single image.
     * <p>
     * <ul>
     *   <li><code>--stat</code> – file statistics (size, MIME, dimensions, timestamps)</li>
     *   <li><code>--info</code> – metadata (EXIF/XMP); optionally followed by <code>--stat</code></li>
     * </ul>
     *
     * @param args CLI args; expects <code>-f &lt;file&gt; &lt;subcommand&gt;</code>
     * @throws IOException if the file cannot be read
     */
    private static void handleFileMode(String[] args) throws IOException {
        if (args.length < 3) {
            error("Usage: -f <file> <--stat|--info>");
            return;
        }

        Path filePath = Paths.get(args[1]);
        if (!Files.isRegularFile(filePath)) {
            error("Not a file: " + filePath);
            return;
        }

        ImageFile imageFile = new ImageFile(filePath);
        String subCommand = args[2];

        switch (subCommand) {
            case "--stat":
                printFileStats(imageFile);
                break;
            case "-i":
            case "--info":
                printFileInfo(imageFile);
                if (args.length == 4 && args[3].equals("--stat")) {
                    System.out.println();
                    printFileStats(imageFile);
                }
                break;
            default:
                error("Unknown file option: " + subCommand);
        }
    }

    /**
     * Pretty-prints statistics for a single image file.
     *
     * @param imageFile image abstraction with pre-extracted attributes
     */
    private static void printFileStats(ImageFile imageFile) {
        success("File Statistics:");
        System.out.println("─".repeat(50));
        stat("File name", imageFile.getPath().getFileName().toString());
        stat("Size", formatSize(imageFile.getSize()));
        stat("MIME type", imageFile.getMimeType());
        stat("Dimensions", imageFile.getDimensions());
        stat("Format", imageFile.getFileFormat());
        stat("Last modified", imageFile.getLastModified().toString());
        stat("Created", imageFile.getCreationDate().toString());
    }

    /**
     * Extracts and prints metadata (EXIF/XMP) for a single image file.
     *
     * @param imageFile target file
     * @throws IOException if low-level I/O fails (rare; extraction handles most errors)
     */
    private static void printFileInfo(ImageFile imageFile) throws IOException {
        ImageMetadata metadata = MetadataExtractor.extract(imageFile.getPath().toFile());
        
        if (metadata == null) {
            warn("No metadata available");
            return;
        }

        success("Image Metadata:");
        System.out.println("─".repeat(50));
        stat("Dimensions", metadata.getWidth() + "x" + metadata.getHeight());
        stat("DPI", metadata.getDpi());
        stat("Title", metadata.getTitle() != null ? metadata.getTitle() : "N/A");
        stat("Description", metadata.getDescription() != null ? metadata.getDescription() : "N/A");
        stat("GPS", metadata.getGpsCoordinates());
        stat("Thumbnail", metadata.hasThumbnail() ? "Yes" : "No");
    }

    /**
     * Saves a snapshot of the directory’s current image files to disk.
     *
     * @param args CLI args; expects <code>--snapshotsave &lt;dir&gt;</code>
     * @throws IOException on snapshot write error
     */
    private static void handleSnapshotMode(String[] args) throws IOException {
        if (args.length < 2) {
            error("Usage: --snapshotsave <directory>");
            return;
        }

        Path directory = Paths.get(args[1]);
        if (!Files.isDirectory(directory)) {
            error("Not a directory: " + directory);
            return;
        }

        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(directory);
        List<ImageFile> files = analyzer.listImageFiles();

        SnapshotManager manager = new SnapshotManager(directory);
        manager.saveSnapshot(files);
        
        success("Snapshot saved for: " + directory.getFileName());
        info("Files captured: " + files.size());
    }

    /**
     * Compares current directory contents with the latest saved snapshot and prints deltas.
     *
     * @param analyzer analyzer bound to the directory
     * @param directory directory path (used to locate snapshot files)
     * @throws IOException if snapshot load/verify fails
     */
    private static void handleCompareSnapshot(DirectoryAnalyzer analyzer, Path directory) 
            throws IOException {
        List<ImageFile> current = analyzer.listImageFiles();
        SnapshotManager manager = new SnapshotManager(directory);
        Map<String, List<Path>> comparison = manager.compareSnapshots(current);

        if (comparison.isEmpty()) {
            info("No snapshot found to compare.");
            return;
        }

        success("Snapshot Comparison:");
        System.out.println("─".repeat(50));

        List<Path> newFiles = comparison.get("Nouveau");
        List<Path> modified = comparison.get("Modifie");
        List<Path> deleted = comparison.get("Supprime");

        if (!newFiles.isEmpty()) {
            System.out.println(AnsiColors.colorize("\n● New files (" + newFiles.size() + "):", 
                AnsiColors.GREEN));
            newFiles.forEach(p -> System.out.println("  + " + p.getFileName()));
        }

        if (!modified.isEmpty()) {
            System.out.println(AnsiColors.colorize("\n● Modified files (" + modified.size() + "):", 
                AnsiColors.YELLOW));
            modified.forEach(p -> System.out.println("  ~ " + p.getFileName()));
        }

        if (!deleted.isEmpty()) {
            System.out.println(AnsiColors.colorize("\n● Deleted files (" + deleted.size() + "):", 
                AnsiColors.RED));
            deleted.forEach(p -> System.out.println("  - " + p.getFileName()));
        }

        if (newFiles.isEmpty() && modified.isEmpty() && deleted.isEmpty()) {
            info("No changes detected.");
        }
    }

    /**
     * Handles search requests with simple filters (name, date/year, dimensions).
     *
     * @param args CLI args; expects <code>--search &lt;dir&gt; &lt;criteria...&gt;</code>
     * @throws IOException if directory traversal fails
     */
    private static void handleSearchMode(String[] args) throws IOException {
        if (args.length < 3) {
            error("Usage: --search <directory> <criteria...>");
            System.out.println("\nCriteria:");
            System.out.println("  name=<filename>");
            System.out.println("  date=<year>");
            System.out.println("  dimensions=<width>x<height>");
            return;
        }

        Path directory = Paths.get(args[1]);
        if (!Files.isDirectory(directory)) {
            error("Not a directory: " + directory);
            return;
        }

        Map<String, String> criteria = parseCriteria(args);
        List<Path> results = performSearch(directory, criteria);

        if (results.isEmpty()) {
            info("No files match the criteria.");
        } else {
            success("Found " + results.size() + " matching files:");
            results.forEach(p -> System.out.println("  " + p));
        }
    }

    /**
     * Parses <code>key=value</code> pairs from the CLI into a criteria map.
     *
     * @param args full CLI arguments
     * @return map of criteria (keys like {@code name}, {@code date}, {@code dimensions})
     */
    private static Map<String, String> parseCriteria(String[] args) {
        Map<String, String> criteria = new HashMap<>();
        for (int i = 2; i < args.length; i++) {
            String[] parts = args[i].split("=", 2);
            if (parts.length == 2) {
                criteria.put(parts[0], parts[1]);
            }
        }
        return criteria;
    }

    /**
     * Executes a directory search using the provided criteria.
     *
     * @param directory root directory
     * @param criteria parsed criteria
     * @return matching file paths
     * @throws IOException if traversal fails
     */
    private static List<Path> performSearch(Path directory, Map<String, String> criteria) 
            throws IOException {
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(directory);
        List<ImageFile> allFiles = analyzer.listImageFiles();

        return allFiles.stream()
            .filter(f -> matchesCriteria(f, criteria))
            .map(ImageFile::getPath)
            .collect(Collectors.toList());
    }

    /**
     * Checks whether an {@link ImageFile} matches the provided search criteria.
     * <p>
     * Supported keys: {@code name} (contains, case-insensitive), {@code date} (year),
     * {@code dimensions} (<code>W</code>x<code>H</code> exact).
     *
     * @param file image file
     * @param criteria search criteria
     * @return {@code true} if the file matches all provided filters
     */
    private static boolean matchesCriteria(ImageFile file, Map<String, String> criteria) {
        String name = criteria.get("name");
        if (name != null && !file.getPath().getFileName().toString().toLowerCase()
                .contains(name.toLowerCase())) {
            return false;
        }

        String year = criteria.get("date");
        if (year != null) {
            String fileYear = String.valueOf(file.getLastModified().getYear());
            if (!fileYear.equals(year)) {
                return false;
            }
        }

        String dimensions = criteria.get("dimensions");
        if (dimensions != null) {
            String[] parts = dimensions.split("x");
            if (parts.length == 2) {
                try {
                    int targetWidth = Integer.parseInt(parts[0]);
                    int targetHeight = Integer.parseInt(parts[1]);
                    if (file.getWidth() != targetWidth || file.getHeight() != targetHeight) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid dimensions format: {}", dimensions);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Prints CLI usage/help with available commands.
     */
    private static void printHelp() {
        System.out.println(AnsiColors.BOLD + "ImageMetadataManager CLI" + AnsiColors.RESET);
        System.out.println("═".repeat(60));
        System.out.println("\nUSAGE:");
        System.out.println("  java -jar ImageMetadataManager.jar cli [OPTIONS]\n");
        System.out.println("DIRECTORY OPTIONS:");
        System.out.println("  -d <dir> --list              List image files");
        System.out.println("  -d <dir> --stat              Show directory statistics");
        System.out.println("  -d <dir> --compare-snapshot  Compare with snapshot\n");
        System.out.println("FILE OPTIONS:");
        System.out.println("  -f <file> --stat             Show file statistics");
        System.out.println("  -f <file> --info             Show metadata\n");
        System.out.println("SNAPSHOT:");
        System.out.println("  --snapshotsave <dir>         Save directory snapshot\n");
        System.out.println("SEARCH:");
        System.out.println("  --search <dir> name=<text>   Search by name");
        System.out.println("  --search <dir> date=<year>   Search by year");
        System.out.println("  --search <dir> dimensions=WxH");
    }

    // Formatting helpers

    /** Prints a success line with a leading check mark. */
    private static void success(String msg) {
        System.out.println(AnsiColors.colorize("✓ " + msg, AnsiColors.GREEN));
    }

    /** Prints an informational line. */
    private static void info(String msg) {
        System.out.println(AnsiColors.colorize("ℹ " + msg, AnsiColors.BLUE));
    }

    /** Prints a warning line. */
    private static void warn(String msg) {
        System.out.println(AnsiColors.colorize("⚠ " + msg, AnsiColors.YELLOW));
    }

    /** Prints an error line to stderr. */
    private static void error(String msg) {
        System.err.println(AnsiColors.colorize("✗ " + msg, AnsiColors.RED));
    }

    /**
     * Prints a labeled value with alignment suitable for stats sections.
     *
     * @param label field name
     * @param value field value
     */
    private static void stat(String label, Object value) {
        System.out.printf("  %-20s : %s%n", label, value);
    }

    /**
     * Formats raw byte counts into human-readable units (KiB, MiB, ...).
     *
     * @param bytes raw size
     * @return formatted string (e.g., {@code 13.4 MB})
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f %sB", 
            bytes / Math.pow(1024, exp), 
            "KMGTPE".charAt(exp - 1));
    }
}
