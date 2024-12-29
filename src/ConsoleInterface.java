package test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

//import test.ImageMetadata;

/**
 * Cette classe fournit une interface en ligne de commande pour interagir avec des fichiers image.
 * Elle permet de réaliser des opérations telles que la recherche de fichiers, l'affichage d'informations,
 * la gestion de répertoires et de snapshots, ainsi que la récupération des métadonnées des fichiers image.
 * 
 * * Les options disponibles sont les suivantes :
 * - Liste les fichiers d'un répertoire.
 * - Affiche des statistiques sur un répertoire ou un fichier.
 * - Sauvegarde l'état d'un répertoire (snapshot).
 * - Compare l'état actuel d'un répertoire avec un snapshot sauvegardé.
 * - Recherche des fichiers image dans un répertoire selon des critères (nom, date, dimensions).
 * 
 * @author DIALLO
 * @version 1.0
 */
public class ConsoleInterface {

	/**
     * Point d'entrée principal de l'application en ligne de commande.
     * Selon les arguments fournis, cette méthode sélectionne l'option à exécuter.
     * 
     * @param args Les arguments passés en ligne de commande.
     */
    public static void start(String[] args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            printHelp();
            return;
        }

        try {
            String option = args[0];
            switch (option) {
                case "-d":
                case "--directory":
                    handleDirectoryMode(args);
                    break;
                case "-f":
                case "--filename":
                    handleFileMode(args);
                    break;
                case "--snapshotsave":
                    handleSnapshotMode(args);
                    break;
                case "--search":
                    handleSearchMode(args);
                    break;
                default:
                    System.out.println("Option non reconnue. Utilisez -h ou --help pour obtenir de l'aide.");
                    printHelp();
                    break;
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de l'exécution : " + e.getMessage());
        }
    }

    /**
     * Gère le mode de recherche dans un répertoire selon des critères spécifiques (nom, date, dimensions).
     * 
     * @param args Les arguments en ligne de commande, dont le répertoire et les critères de recherche.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de la recherche.
     */
    private static void handleSearchMode(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Utilisation : --search <directory> <criteria>");
            System.out.println("Exemple de critères (séparés par des espaces) :");
            System.out.println("  name=<part_of_name>");
            System.out.println("  date=<year>");
            System.out.println("  dimensions=<width>x<height>");
            return;
        }

        Path directory = Paths.get(args[1]);
        if (!Files.isDirectory(directory)) {
            System.out.println("Le chemin spécifié n'est pas un répertoire.");
            return;
        }

        String[] criteria = java.util.Arrays.copyOfRange(args, 2, args.length);
        StringBuilder commandBuilder = new StringBuilder();
        boolean searchByDimensions = false;
        int targetWidth = 0, targetHeight = 0;
        
        // Detecting the OS
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            commandBuilder.append("dir /s /b ").append(directory.toString()).append("\\*");
        } else {
            commandBuilder.append("find ").append(directory.toString());
            commandBuilder.append(" \\( -iname '*.png' -o -iname '*.jpeg' -o -iname '*.jpg' -o -iname '*.webp' \\)");
        }

        // Process criteria
        for (String criterion : criteria) {
            if (criterion.startsWith("name=")) {
                String namePart = criterion.substring(5).toLowerCase();
                commandBuilder.append(" -iname '*").append(namePart).append("*'");
            } else if (criterion.startsWith("date=")) {
                String yearString = criterion.substring(5);
                try {
                    int year = Integer.parseInt(yearString);
                    if (os.contains("win")) {
                        commandBuilder.append(" | findstr /i \"").append(year).append("\"");
                    } else {
                        commandBuilder.append(" -newermt '").append(year).append("-01-01' ! -newermt '")
                            .append(year + 1).append("-01-01'");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Année invalide : " + yearString);
                    return;
                }
            } else if (criterion.startsWith("dimensions=")) {
            	
            	searchByDimensions = true;
                String dimensions = criterion.substring(11);
                String[] parts = dimensions.split("x");
                if (parts.length != 2) {
                    System.out.println("Format des dimensions invalide. Utilisez <largeur>x<hauteur>.");
                    return;
                }
                try {
                    targetWidth = Integer.parseInt(parts[0]);
                    targetHeight = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Dimensions invalides : " + dimensions);
                    return;
                }
            } else {
                System.out.println("Critère non reconnu : " + criterion);
                return;
            }
        }
        
     // Parcourir les fichiers dans le répertoire
        File dir = directory.toFile();
        File[] files = dir.listFiles((d, name) ->
            name.toLowerCase().endsWith(".png") ||
            name.toLowerCase().endsWith(".jpg") ||
            name.toLowerCase().endsWith(".jpeg") ||
            name.toLowerCase().endsWith(".webp")
        );

        if (files == null || files.length == 0) {
            System.out.println("Aucun fichier image trouvé dans le répertoire.");
            return;
        }

        // Vérification des dimensions
        if (searchByDimensions) {
            System.out.println(">>> Début de la vérification des dimensions <<<");
            List<String> matchingFiles = new ArrayList<>();
            for (File file : files) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    if (image != null && image.getWidth() == targetWidth && image.getHeight() == targetHeight) {
                        matchingFiles.add(file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    System.out.println("Erreur lors de la lecture de l'image : " + file.getName());
                }
            }
            if (!matchingFiles.isEmpty()) {
                System.out.println("Fichiers correspondant aux dimensions spécifiées :");
                matchingFiles.forEach(System.out::println);
            } else {
                System.out.println("Aucun fichier ne correspond aux dimensions spécifiées.");
            }
            System.out.println(">>> Fin des fichiers correspondant <<<");
            return;
        }

        // Execute the command
        String command = commandBuilder.toString();
        //System.out.println("Exécution de la commande : " + command);

        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        if (os.contains("win")) {
            processBuilder = new ProcessBuilder("cmd", "/c", command);
        }

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean hasResults = false;
            while ((line = reader.readLine()) != null) {
                hasResults = true;
                System.out.println(line);
            }
            if (!hasResults) {
                System.out.println("Aucun fichier correspondant aux critères spécifiés.");
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de l'exécution de la commande : " + e.getMessage());
        }
        
    }

    /**
     * Gère le mode répertoire, qui permet de lister les fichiers, afficher des statistiques et comparer avec un snapshot.
     * 
     * @param args Les arguments en ligne de commande, dont le répertoire et l'option de mode répertoire.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de l'exécution.
     */
    private static void handleDirectoryMode(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Spécifiez le dossier et l'option souhaitée (-list, --stat, --compare-snapshot).");
            return;
        }else if (args.length > 3){
            printHelp();
            return;
        }
        
        Path directory = Paths.get(args[1]);
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(directory);

        switch (args[2]) {
            case "--list":
                List<ImageFile> imageFiles = analyzer.listImageFiles();
                if (imageFiles.isEmpty()) {
                    System.out.println("Aucun fichier image trouvé dans le répertoire spécifié.");
                } else {
                    System.out.println("Liste des fichiers image :");
                    imageFiles.forEach(file -> System.out.println(file.getPath()));
                }
                break;

            case "--stat":
                System.out.println("Nombre total de fichiers : " + analyzer.getTotalFileCount());
                System.out.println("Nombre total de fichiers image : " + analyzer.getImageFileCount());
                System.out.println("Taille totale des fichiers : " + analyzer.getTotalFileSize() + " octets");

                Path largestFile = analyzer.getLargestFile();
                if (largestFile != null) {
                    System.out.println("Fichier le plus grand : " + largestFile.getFileName() +
                            " (" + Files.size(largestFile) + " octets)");
                } else {
                    System.out.println("Aucun fichier trouvé pour le plus grand fichier.");
                }

                Path smallestFile = analyzer.getSmallestFile();
                if (smallestFile != null) {
                    System.out.println("Fichier le plus petit : " + smallestFile.getFileName() +
                            " (" + Files.size(smallestFile) + " octets)");
                } else {
                    System.out.println("Aucun fichier trouvé pour le plus petit fichier.");
                }

                System.out.println("Taille moyenne des fichiers : " + analyzer.getAverageFileSize() + " octets");
                System.out.println("Type de fichier le plus commun : " + analyzer.getMostCommonFileType());
                System.out.println("Nombre de sous-dossiers : " + analyzer.getSubdirectoryCount());

                Path lastModifiedFile = analyzer.getLastModifiedFile();
                if (lastModifiedFile != null) {
                    System.out.println("Dernier fichier modifié : " + lastModifiedFile.getFileName() +
                            " (modifié le " + Files.getLastModifiedTime(lastModifiedFile) + ")");
                } else {
                    System.out.println("Aucun fichier trouvé pour le dernier fichier modifié.");
                }

                Path oldestFile = analyzer.getOldestFile();
                if (oldestFile != null) {
                    System.out.println("Fichier le plus ancien : " + oldestFile.getFileName() +
                            " (créé le " + Files.getAttribute(oldestFile, "creationTime") + ")");
                } else {
                    System.out.println("Aucun fichier trouvé pour le fichier le plus ancien.");
                }

                System.out.println("Extensions de fichiers trouvées : " + String.join(", ", analyzer.getFileExtensions()));
                System.out.println("Nombre de fichiers vides : " + analyzer.getEmptyFileCount());
                System.out.println("Taille totale (y compris sous-dossiers) : " + analyzer.getTotalFileSize() + " octets");
                break;
            case "--compare-snapshot":
                handleCompareSnapshotMode(args, directory);
                break;
            default:
                System.out.println("Option non reconnue pour le mode dossier.");
                break;
        }
    }

    /**
    * Gère le mode fichier, qui permet d'afficher des statistiques ou des métadonnées d'un fichier image spécifique.
    * 
    * @param args Les arguments en ligne de commande, dont le chemin du fichier et l'option du mode fichier.
    * @throws IOException Si une erreur d'entrée/sortie se produit lors de l'exécution.
    */
    private static void handleFileMode(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Spécifiez le fichier et l'option souhaitée (--stat ,-i ,--info).");
            return;
        }else if (args.length > 4){
            printHelp();
            return;
        }

        Path filePath = Paths.get(args[1]);
        ImageFile imageFile = new ImageFile(filePath);

        switch (args[2]) {
            case "--stat":
                System.out.println("Taille du fichier : " + imageFile.getSize());
                System.out.println("Type MIME : " + imageFile.getMimeType());
                System.out.println("Dernière modification : " + imageFile.getLastModified());
                System.out.println("Dimensions : " + imageFile.getWidth() + "x" + imageFile.getHeight());
                System.out.println("Format du fichier : " + imageFile.getFileFormat());
                System.out.println("Date de création : " + imageFile.getCreationDate());
                break;
            case "--info", "-i":
                if (args.length == 4 && !args[3].equals("--stat")){
                    System.out.println("L'option \"" + args[3] +"\" est non reconnue pour le mode fichier.");
                    printHelp();
                    break;
                }

                ImageMetadata metadata = MetadataExtractor.extract(filePath.toFile());
                System.out.println("Dimensions: " + metadata.getWidth() + "x" + metadata.getHeight());
                System.out.println("Resolution (DPI): " + metadata.getDpi());
                System.out.println("Title: " + metadata.getTitle());
                System.out.println("Description: " + metadata.getDescription());
                System.out.println("GPS Coordinates: " + metadata.getGpsCoordinates());
                if (metadata.hasThumbnail()) {
                    System.out.println("Thumbnail: Exists");
                    // Display the thumbnail in GUI
                } else {
                    System.out.println("Thumbnail: Not available");
                }

                if (args.length == 4){
                    // pour les stat
                    System.out.println("Taille du fichier : " + imageFile.getSize());
                    System.out.println("Type MIME : " + imageFile.getMimeType());
                    System.out.println("Dernière modification : " + imageFile.getLastModified());
                    System.out.println("Dimensions : " + imageFile.getWidth() + "x" + imageFile.getHeight());
                    System.out.println("Format du fichier : " + imageFile.getFileFormat());
                    System.out.println("Date de création : " + imageFile.getCreationDate());
                }
                break;
            default:
                System.out.println("Option non reconnue pour le mode fichier.");
                break;
        }
    }

    /**
     * Capture l'état actuel d'un répertoire, en sauvegardant un snapshot des fichiers présents.
     * 
     * @param args Les arguments en ligne de commande, spécifiant le répertoire à capturer.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de la sauvegarde du snapshot.
     */
    private static void handleSnapshotMode(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Spécifiez le dossier à capturer.");
            return;
        }else if (args.length > 2){
            printHelp();
            return;
        }

        Path directory = Paths.get(args[1]);
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(directory);
        List<ImageFile> imageFiles = analyzer.listImageFiles();

        // Initialize SnapshotManager with the target directory
        SnapshotManager snapshotManager = new SnapshotManager(directory);
        snapshotManager.saveSnapshot(imageFiles);

        System.out.println("Snapshot enregistré pour le dossier : " + directory.getFileName());
    }

    /**
     * Compare l'état actuel d'un répertoire avec un snapshot sauvegardé, en affichant les fichiers ajoutés, modifiés ou supprimés.
     * 
     * @param args Les arguments en ligne de commande, dont le répertoire à comparer avec un snapshot.
     * @param directory Le répertoire à analyser.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de la comparaison des snapshots.
     */
    private static void handleCompareSnapshotMode(String[] args, Path directory) throws IOException {
        DirectoryAnalyzer analyzer = new DirectoryAnalyzer(directory);
        List<ImageFile> currentImageFiles = analyzer.listImageFiles();

        SnapshotManager snapshotManager = new SnapshotManager(directory);
        Map<String, List<Path>> categorizedFiles = snapshotManager.compareSnapshots(currentImageFiles);

        if (categorizedFiles.isEmpty()) {
            System.out.println("Aucun fichier modifié ou supprimé par rapport au snapshot.");
            return;
        }

        // Print deleted files
        List<Path> deletedFiles = categorizedFiles.get("Supprime");
        if (!deletedFiles.isEmpty()) {
            System.out.println("Photos supprimées :");
            deletedFiles.forEach(file -> System.out.println(file));
        } else {
            System.out.println("Aucune photo supprimée.");
        }

        // Print modified files
        List<Path> modifiedFiles = categorizedFiles.get("Modifie");
        if (!modifiedFiles.isEmpty()) {
            System.out.println("Photos modifiées :");
            modifiedFiles.forEach(file -> System.out.println(file));
        } else {
            System.out.println("Aucune photo modifiée.");
        }

        // Print new files
        List<Path> newFiles = categorizedFiles.get("Nouveau");
        if (!newFiles.isEmpty()) {
            System.out.println("Photos nouvelles :");
            newFiles.forEach(file -> System.out.println(file));
        } else {
            System.out.println("Aucune photo nouvelle.");
        }
    }

    /**
     * Affiche l'aide de l'application en ligne de commande.
     * Cette méthode présente les options disponibles et leur utilisation.
     */
    private static void printHelp() {
        System.out.println("Usage : java -jar ImageMetadataManager.jar cli <options>");
        System.out.println("-d(, --directory) <directory> --list : Liste les fichiers images du dossier");
        System.out.println("-d(, --directory) <directory> --stat : Affiche les statistiques du dossier");
        System.out.println("-d(, --directory) <directory> --compare-snapshot : Compare l'état actuel avec le snapshot");
        System.out.println("-f(, --filename) <file> --stat : Affiche les statistiques du fichier");
        System.out.println("-f(, --filename) <file> -i(, --info) : Affiche les métadonnées du fichier");
        System.out.println("--snapshotsave <directory> : Capture l'état d'un dossier pour comparaison ultérieure");
        System.out.println("--search <directory> <criteria> : Recherche des images selon des critères");
        System.out.println("  Critères disponibles :");
        System.out.println("    name=<part_of_name> : Recherche par nom ou partie du nom");
        System.out.println("    date=<year> : Recherche par année de création");
        System.out.println("    dimensions=<width>x<height> : Recherche par dimensions minimales");
    }
}
