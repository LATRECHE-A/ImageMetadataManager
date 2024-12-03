package test;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class GraphicalInterface extends JFrame {
    private final Color DARK_BACKGROUND = new Color(26, 26, 26);
    private final Color DARK_COMPONENT = new Color(60, 63, 65);
    private final Color DARK_HOVER = new Color(76, 80, 82);
    private final Color DARK_TEXT = new Color(255, 255, 255);
    
    private JTextField pathField;
    private JPanel actionButtonsPanel;
    private JPanel imageGrid;
    private JScrollPane scrollPane;
    private Path currentPath;
    private final Map<Path, ImagePanel> imagePanels = new HashMap<>();
    private JPanel statusBar;
    private JProgressBar loadingIndicator;
    private JLabel statusLabel;
    private JPanel mainPanel;
    private JButton deleteSelectedButton;
    private JButton modifySelectedButton;
    private JButton refreshButton;
    
    private final List<Path> selectedImages = new ArrayList<>();

    public GraphicalInterface() {
        setTitle("Gestionnaire d'images");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupUI();
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(DARK_COMPONENT);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DARK_COMPONENT),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
                    ));
        button.setFocusPainted(false);

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(DARK_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(DARK_COMPONENT);
            }
        });

        return button;
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField(20);
        field.setBackground(DARK_COMPONENT);
        field.setForeground(Color.white);
        field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DARK_COMPONENT),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    ));
        field.setCaretColor(Color.WHITE);
        return field;
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(DARK_BACKGROUND);

        JButton dirButton = createStyledButton("Choisir un répertoire");
        JButton fileButton = createStyledButton("Choisir un fichier");
        refreshButton = createStyledButton("Actualiser");
        pathField = new JTextField(30);
        pathField.setEditable(false);
        pathField.setBackground(DARK_COMPONENT);
        pathField.setForeground(DARK_TEXT);

        dirButton.addActionListener(e -> chooseDirectory());
        fileButton.addActionListener(e -> chooseFile());
        refreshButton.addActionListener(e -> updateImageGrid(null));
        refreshButton.setEnabled(false);

        panel.add(dirButton);
        panel.add(fileButton);
        panel.add(pathField);
        panel.add(refreshButton);


        return panel;
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentPath = chooser.getSelectedFile().toPath();
            pathField.setText(currentPath.toString());
            pathField.revalidate(); // Ensure immediate update
            pathField.repaint();
            refreshButton.setEnabled(true);
            showDirectoryButtons();
            updateImageGrid(null);
        }
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Image Files", "png", "jpg", "jpeg", "webp"
                    ));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentPath = chooser.getSelectedFile().toPath();
            pathField.setText(currentPath.toString());
            pathField.revalidate(); // ensure immediate update
            pathField.repaint();
            showFileButtons();
            updateImageGrid(null);
            showImageMetadata(currentPath);
        }
    }

    private void showFileButtons() {
        actionButtonsPanel.removeAll();
        actionButtonsPanel.setVisible(true);
        actionButtonsPanel.revalidate();
        actionButtonsPanel.repaint();
    }

    private void setupUI() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBackground(DARK_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // top Panel
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // center Panel to hold the action buttons, image grid, and bottom buttons
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setBackground(DARK_BACKGROUND);

        // action Buttons Panel (Initially hidden)
        actionButtonsPanel = createActionButtonsPanel();
        actionButtonsPanel.setVisible(false); // Initially hidden
        centerPanel.add(actionButtonsPanel, BorderLayout.NORTH);

        // scroll Pane with the Image Grid
        scrollPane = new JScrollPane(imageGrid = createImageGrid());
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // bottom Buttons Panel (Delete and Modify)
        JPanel bottomActionPanel = createBottomActionPanel();
        centerPanel.add(bottomActionPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // status Bar with loading indicator
        statusBar = createStatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        applyDarkTheme();
    }

    private JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(DARK_BACKGROUND);
        panel.setVisible(false);
        return panel;
    }

    private JPanel createImageGrid() {
        // create a JPanel with a GridLayout for arranging image thumbnails
        JPanel grid = new JPanel(new GridLayout(0, 4, 15, 15)); // 4 columns, dynamic rows
        grid.setBackground(DARK_BACKGROUND); // set the background color to match the theme

        // Wrap the grid in a JScrollPane for scrollable content
        JScrollPane scrollableGrid = new JScrollPane(grid);
        scrollableGrid.setBackground(DARK_BACKGROUND);
        scrollableGrid.getViewport().setBackground(DARK_BACKGROUND);

        // Adjust scroll pane borders and settings for a seamless dark theme appearance
        scrollableGrid.setBorder(BorderFactory.createEmptyBorder());
        scrollableGrid.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollableGrid.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        return grid;
    }

    private JPanel createBottomActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); // Center-aligned
        panel.setBackground(DARK_BACKGROUND);

        deleteSelectedButton = createStyledButton("Supprimer les images sélectionnées");
        modifySelectedButton = createStyledButton("Modifier l'image sélectionnée");

        deleteSelectedButton.setEnabled(false);
        modifySelectedButton.setEnabled(false);

        deleteSelectedButton.addActionListener(e -> deleteSelectedImages());
        modifySelectedButton.addActionListener(e -> modifySelectedImage());

        panel.add(deleteSelectedButton);
        panel.add(modifySelectedButton);

        return panel;
    }

    private void deleteSelectedImages() {
        // check if any images are selected (this is usless cuz the button should be disabled)
        if (selectedImages.isEmpty()) {
            showError("Aucune image sélectionnée pour la suppression.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Êtes-vous sûr de vouloir supprimer " + selectedImages.size() + " fichier(s) sélectionné(s)?\n" +
                "Cette action ne peut pas être annulée !",
                "Confirmer la suppression",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
                );

        if (confirm == JOptionPane.YES_OPTION) {
            int successCount = 0;
            int failureCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            for (Path imagePath : selectedImages) {
                try {
                    Files.delete(imagePath);
                    successCount++;
                } catch (IOException e) {
                    failureCount++;
                    errorMessages.append("Échec de la suppression : ")
                        .append(imagePath.getFileName())
                        .append(" (")
                        .append(e.getMessage())
                        .append(")\n");
                }
            }

            selectedImages.clear();
            updateImageGrid(null);
            updateSelectionButtons();

            // show results
            if (failureCount == 0) {
                showInfo(successCount + " fichier(s) supprimé(s) avec succès.");
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        successCount + " fichier(s) supprimé(s) avec succès.\n" +
                        failureCount + " fichier(s) n'ont pas pu être supprimés.\n\n" +
                        "Détails de l'erreur :\n" + errorMessages,
                        "Résultats de la suppression",
                        JOptionPane.WARNING_MESSAGE
                        );
            }
        }
    }

    private void modifySelectedImage() {
        if (selectedImages.size() == 1) {
            Path selectedImage = selectedImages.get(0);
            String currentFileName = selectedImage.getFileName().toString();
            String newName = JOptionPane.showInputDialog(this, "Entrez le nouveau nom de l'image :", currentFileName);

            if (newName != null && !newName.trim().isEmpty()) {
                try {
                    Path newPath = selectedImage.resolveSibling(newName);
                    Files.move(selectedImage, newPath);
                    updateImageGrid(null);
                    showInfo("Image renommée avec succès !");
                } catch (IOException e) {
                    showError("Erreur lors du changement de nom de l'image : " + e.getMessage());
                }
            }
        }
    }

    private void updateSelectionButtons() {
        deleteSelectedButton.setEnabled(!selectedImages.isEmpty());
        modifySelectedButton.setEnabled(selectedImages.size() == 1);
    }

    private void createCloseButton(JDialog dialog) {
        JButton closeButton = createStyledButton("Fermer");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(DARK_BACKGROUND);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setLoadingState(boolean isLoading) {
        SwingUtilities.invokeLater(() -> {
            loadingIndicator.setVisible(isLoading);
            statusLabel.setText(isLoading ? "Chargement..." : "Prêt");
        });
    }

    private void showDirectoryButtons() {
        actionButtonsPanel.removeAll();
        
        JButton searchButton = createStyledButton("Rechercher des images");
        JButton clearSearchButton = createStyledButton("Effacer la recherche");
        JButton snapshotButton = createStyledButton("Prendre un Snapshot");
        JButton compareButton = createStyledButton("Comparer avec Snapshot");
        JButton statsButton = createStyledButton("Afficher les statistiques");
        
        searchButton.addActionListener(e -> showSearchDialog());
        clearSearchButton.addActionListener(e -> {
            updateImageGrid(null);
            showInfo("Recherche effacée. Affichage de toutes les images.");
        });
        snapshotButton.addActionListener(e -> {
            String output = executeCliCommand("--snapshotsave", currentPath.toString());
            showInfo(output);
        });
        compareButton.addActionListener(e -> showCompareDialog());
        statsButton.addActionListener(e -> showDirectoryStats());
        
        actionButtonsPanel.add(searchButton);
        actionButtonsPanel.add(clearSearchButton);
        actionButtonsPanel.add(snapshotButton);
        actionButtonsPanel.add(compareButton);
        actionButtonsPanel.add(statsButton);
        
        actionButtonsPanel.setVisible(true);
        actionButtonsPanel.revalidate();
        actionButtonsPanel.repaint();
    }

    private void showCompareDialog() {
        JDialog dialog = new JDialog(this, "Snapshot Comparison", true);
        dialog.setLayout(new BorderLayout(10, 10));

        String output = executeCliCommand("-d", currentPath.toString(), "--compare-snapshot");

        JTextArea textArea = new JTextArea(output);
        textArea.setEditable(false);
        textArea.setBackground(DARK_COMPONENT);
        textArea.setForeground(DARK_TEXT);

        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane);

        // Close Button
        JButton closeButton = createStyledButton("Fermer");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(DARK_BACKGROUND);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showDirectoryStats() {
        JDialog dialog = new JDialog(this, "Statistiques du répertoire", true);
        dialog.setLayout(new BorderLayout(10, 10));

        String stats = executeCliCommand("-d", currentPath.toString(), "--stat");

        JTextArea textArea = new JTextArea(stats);
        textArea.setEditable(false);
        textArea.setBackground(DARK_COMPONENT);
        textArea.setForeground(DARK_TEXT);

        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane);

        // Close Button
        JButton closeButton = createStyledButton("Fermer");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(DARK_BACKGROUND);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showImageMetadata(Path imagePath) {
        JDialog dialog = new JDialog(this, "Image Metadata: " + imagePath.getFileName(), true);
        dialog.setLayout(new BorderLayout(10, 10));

        try {
            String metadata = executeCliCommand("-f", imagePath.toString(), "-i", "--stat");

            try {
                Image image = ImageIO.read(imagePath.toFile());
                if (image != null){
                    ImageIcon icon = new ImageIcon(image.getScaledInstance(300, -1, Image.SCALE_SMOOTH));
                    JLabel imageLabel = new JLabel(icon);
                    imageLabel.setBackground(DARK_BACKGROUND);
                    dialog.add(imageLabel, BorderLayout.NORTH);
                }
            } catch (IOException thumbnailException) {
                //thumbnailException.printStackTrace();
                System.err.println("Impossible de charger la miniature de l'image: " + thumbnailException.getMessage());
            }

            JTextArea textArea = new JTextArea(metadata);
            textArea.setEditable(false);
            textArea.setBackground(DARK_COMPONENT);
            textArea.setForeground(DARK_TEXT);

            JScrollPane scrollPane = new JScrollPane(textArea);
            dialog.add(scrollPane, BorderLayout.CENTER);

            // Close Button
            JButton closeButton = createStyledButton("Fermer");
            closeButton.addActionListener(e -> dialog.dispose());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(DARK_BACKGROUND);
            buttonPanel.add(closeButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setSize(500, 600);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }catch (Exception e){
            showError("Erreur lors de l'affichage des métadonnées de l'image: " + e.getMessage());
        }
    }

    private void showSearchDialog() {
        JDialog dialog = new JDialog(this, "Rechercher des images", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(DARK_BACKGROUND);

        // Create styled fields
        JTextField nameField = createStyledTextField("Entrer le nom du fichier");
        JTextField yearField = createStyledTextField("Entrez l'année (AAAA)");
        JTextField dimensionsField = createStyledTextField("Entrez les dimensions (L x H)");

        // Create styled button
        JButton searchButton = createStyledButton("Recherche");
        searchButton.addActionListener(e -> {
            List<String> criteria = new ArrayList<>();

            if (!nameField.getText().isEmpty()) {
                criteria.add("name=" + nameField.getText());
            }
            if (!yearField.getText().isEmpty()) {
                criteria.add("date=" + yearField.getText());
            }
            if (!dimensionsField.getText().isEmpty()) {
                criteria.add("dimensions=" + dimensionsField.getText());
            }

            String[] args = new String[criteria.size() + 2];
            args[0] = "--search";
            args[1] = currentPath.toString();
            System.arraycopy(criteria.toArray(), 0, args, 2, criteria.size());

            setLoadingState(true);
            SwingWorker<List<Path>, Void> worker = new SwingWorker<>() {
                @Override
                protected  List<Path> doInBackground() {
                    String output = executeCliCommand(args);
                    List<Path> paths = output.lines()
                        .map(line -> Paths.get(line.trim()))
                        .filter(Files::exists)
                        .collect(Collectors.toList());
                    if (!paths.isEmpty() && paths.get(0).equals(Paths.get("Aucun fichier correspondant aux critères spécifiés."))) {
                        return Collections.emptyList();
                    }
                    return paths;
                }

                @Override
                protected void done() {
                    try {
                        List<Path> paths = get();
                        setLoadingState(false);
                        updateImageGrid(paths);
                        dialog.dispose();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("erreur lors de la recherche des images : " + e.getMessage());
                        setLoadingState(false);
                    }
                }
            };
            worker.execute();
        });

        // Add styled labels and fields
        contentPanel.add(createStyledLabel("Critères de recherche (sensibles à la casse)"));
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(createStyledLabel("Nom de fichier:"));
        contentPanel.add(nameField);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(createStyledLabel("Année:"));
        contentPanel.add(yearField);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(createStyledLabel("Dimensions:"));
        contentPanel.add(dimensionsField);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(searchButton);

        createCloseButton(dialog);
        dialog.add(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE); // Set text color to white
        return label;
    }

    private class ImagePanel extends JPanel {
        private final Path imagePath;
        private Image thumbnail;
        private boolean isSelected = false;
        private final int THUMBNAIL_SIZE = 200;

        public ImagePanel(Path path) {
            this.imagePath = path;
            setPreferredSize(new Dimension(THUMBNAIL_SIZE, THUMBNAIL_SIZE));
            setBackground(DARK_COMPONENT);
            setBorder(BorderFactory.createLineBorder(DARK_HOVER));
            loadThumbnail();
            setupMouseListeners();
        }

        private void setupMouseListeners() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        toggleSelection();
                    } else if (e.getClickCount() == 2) {
                        showImageMetadata(imagePath);
                    }
                }
            });
        }

        private void toggleSelection() {
            isSelected = !isSelected;
            if (isSelected) {
                selectedImages.add(imagePath);
                setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
            } else {
                selectedImages.remove(imagePath);
                setBorder(BorderFactory.createLineBorder(DARK_HOVER));
            }
            updateSelectionButtons();
            repaint();
        }
        private void loadThumbnail() {
            SwingWorker<Image, Void> worker = new SwingWorker<>() {
                @Override
                protected Image doInBackground() throws Exception {
                    Image originalImage = ImageIO.read(imagePath.toFile());
                    if (originalImage == null){
                        return null;
                    }
                    return createThumbnail(originalImage);
                }

                @Override
                protected void done() {
                    try {
                        thumbnail = get();
                        repaint();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Erreur lors du chargement de la miniature : " + e.getMessage());
                    }
                }
            };
            worker.execute();
        }

        private Image createThumbnail(Image original) {
            if (original == null){
                return null;
            } 
            
            int originalWidth = original.getWidth(null);
            int originalHeight = original.getHeight(null);
            
            double scale = Math.min(
                (double) THUMBNAIL_SIZE / originalWidth,
                (double) THUMBNAIL_SIZE / originalHeight
            );
            
            int thumbnailWidth = (int) (originalWidth * scale);
            int thumbnailHeight = (int) (originalHeight * scale);
            
            return original.getScaledInstance(thumbnailWidth, thumbnailHeight, Image.SCALE_SMOOTH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw thumbnail
            if (thumbnail != null) {
                int x = (getWidth() - thumbnail.getWidth(null)) / 2;
                int y = (getHeight() - thumbnail.getHeight(null)) / 2;
                g.drawImage(thumbnail, x, y, null);

                // Draw filename at the bottom of the thumbnail
                g.setColor(DARK_TEXT);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                String fileName = imagePath.getFileName().toString();

                // Truncate filename if too long
                if (fileName.length() > 20) {
                    fileName = fileName.substring(0, 17) + "...";
                }

                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(fileName);
                int textX = (getWidth() - textWidth) / 2;
                int textY = getHeight() - 10; // 10 pixels from bottom

                // Draw semi-transparent background for better readability
                g.setColor(new Color(0, 0, 0, 128)); // Semi-transparent black
                g.fillRect(0, getHeight() - 20, getWidth(), 20);

                // Draw filename
                g.setColor(DARK_TEXT);
                g.drawString(fileName, textX, textY);
            } else {
                g.setColor(DARK_TEXT);
                g.drawString("Chargement...", 10, getHeight() / 2);
            }
        }
    }

    private void updateImageGrid(List<Path> SearchResults) {
        imageGrid.removeAll();
        imagePanels.clear();
        selectedImages.clear();
        updateSelectionButtons();

        setLoadingState(true);
        SwingWorker<List<Path>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Path> doInBackground() {
                // here we should make sure if it's a file or a dir
                Path path = Paths.get(currentPath.toString());
                if (Files.isDirectory(path)) {
                    // dir
                    String output = executeCliCommand("-d", currentPath.toString(), "--list");
                    return output.lines()
                        .filter(line -> !line.startsWith("Liste des fichiers image :"))
                        .map(line -> Paths.get(line.trim()))
                        .filter(Files::exists)
                        .collect(Collectors.toList());
                }else {
                    // image
                    List<Path> output = new ArrayList<>();
                    output.add(path);
                    return output;
                }
            }

            @Override
            protected void done() {
                try {
                    List<Path> paths = get();
                    if (SearchResults != null){
                        paths = SearchResults;
                    }
                    for (Path path : paths) {
                        ImagePanel panel = new ImagePanel(path);
                        imagePanels.put(path, panel);
                        imageGrid.add(panel);
                    }
                    imageGrid.revalidate();
                    imageGrid.repaint();
                    setLoadingState(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Erreur lors du chargement des images : " + e.getMessage());
                    setLoadingState(false);
                }
            }
        };

        worker.execute();
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DARK_COMPONENT);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        statusLabel = new JLabel("Prêt");
        statusLabel.setForeground(DARK_TEXT);
        panel.add(statusLabel, BorderLayout.WEST);

        loadingIndicator = new JProgressBar();
        loadingIndicator.setIndeterminate(true);
        loadingIndicator.setVisible(false);
        panel.add(loadingIndicator, BorderLayout.EAST);

        return panel;
    }

    private void applyDarkTheme() {
        UIManager.put("TextField.foreground", DARK_TEXT);
        UIManager.put("TextArea.background", DARK_COMPONENT);
        UIManager.put("TextArea.foreground", DARK_TEXT);
        UIManager.put("ScrollPane.background", DARK_BACKGROUND);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("OptionPane.buttonBackground", DARK_COMPONENT);
        UIManager.put("OptionPane.buttonForeground", DARK_TEXT);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private String executeCliCommand(String... args) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        PrintStream originalOut = System.out;
        System.setOut(printStream);

        try {
            ConsoleInterface.start(args);
            return outputStream.toString();
        } finally {
            System.setOut(originalOut);
        }
    }

    public static void start() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new GraphicalInterface().setVisible(true);
        });
    }
}
