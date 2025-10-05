package com.imagemeta.ui.gui;

import com.imagemeta.core.*;
import com.imagemeta.metadata.*;
import com.imagemeta.ui.gui.dialogs.*;
import com.imagemeta.util.i18n.Localization;
import com.imagemeta.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Swing-based graphical interface with a modern, Material-inspired theme.
 * <p>
 * Provides directory browsing, thumbnail grid, metadata inspection, snapshot
 * creation/comparison, and filtered search. Long-running tasks (directory scans,
 * thumbnail loading) are executed via {@link SwingWorker} to keep the UI responsive,
 * and UI updates occur on the Event Dispatch Thread (EDT). 
 *
 * @implNote Swing components must be created/updated on the EDT. Background work
 *           is delegated to {@link SwingWorker}, whose lifecycle safely marshals
 *           results back to the EDT. :contentReference[oaicite:1]{index=1}
 */
public class GraphicalInterface extends JFrame {
    private static final long serialVersionUID = 1L;
    
    // Modern color palette
    private static final Color PRIMARY = new Color(33, 150, 243);
    private static final Color PRIMARY_DARK = new Color(25, 118, 210);
    private static final Color ACCENT = new Color(255, 64, 129);
    private static final Color BG_DARK = new Color(18, 18, 18);
    private static final Color BG_SURFACE = new Color(30, 30, 30);
    private static final Color BG_HOVER = new Color(45, 45, 45);
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(176, 190, 197);
    
    private JTextField pathField;
    private JPanel actionPanel;
    private JPanel imageGrid;
    private JScrollPane scrollPane;
    private transient Path currentPath;
    private transient Map<Path, ImagePanel> imagePanels = new HashMap<>();
    private transient List<Path> selectedImages = new ArrayList<>();
    private transient Map<String, String> currentSearchCriteria = new HashMap<>();
    private JProgressBar loadingBar;
    private JLabel statusLabel;
    private JLabel searchStatusLabel;
    private JButton deleteBtn;
    private JButton modifyBtn;
    private JButton refreshBtn;
    
    /**
     * Creates and initializes the main window.
     * <p>
     * If the application requires a language selection, the dialog is shown first and the
     * UI is created afterward so text resources reflect the choice.
     */
    public GraphicalInterface() {
        super(Localization.tr("app.title"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        // Check language selection BEFORE showing window
        if (Localization.needsSelection()) {
            // Don't create UI yet
            SwingUtilities.invokeLater(() -> {
                LanguageSelectionDialog dialog = new LanguageSelectionDialog(null);
                dialog.setVisible(true);
                // Now create and show the GUI with selected language
                initUI();
                setSize(1400, 900);
                setLocationRelativeTo(null);
                showWelcomePlaceholder();
                setVisible(true);
            });
        } else {
            initUI();
            setSize(1400, 900);
            setLocationRelativeTo(null);
            showWelcomePlaceholder();
        }
    }
    
    /**
     * Builds the main content layout and child components.
     * <p>
     * Top bar: directory/file selection and path display. Center: actions + image grid.
     * Bottom: status bar with a slim progress indicator.
     */
    private void initUI() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(BG_DARK);
        
        // Top bar with modern design
        main.add(createTopBar(), BorderLayout.NORTH);
        
        // Center content
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG_DARK);
        
        actionPanel = createActionPanel();
        actionPanel.setVisible(false);
        center.add(actionPanel, BorderLayout.NORTH);
        
        imageGrid = new JPanel(new GridLayout(0, 4, 20, 20));
        imageGrid.setBackground(BG_DARK);
        imageGrid.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        scrollPane = new JScrollPane(imageGrid);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        center.add(scrollPane, BorderLayout.CENTER);
        
        // Bottom action bar
        center.add(createBottomBar(), BorderLayout.SOUTH);
        
        main.add(center, BorderLayout.CENTER);
        main.add(createStatusBar(), BorderLayout.SOUTH);
        
        setContentPane(main);
        createMenuBar();
    }
    
    /**
     * Creates the top application bar with directory/file pickers and path field.
     *
     * @return configured top bar panel
     */
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 10));
        bar.setBackground(BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, PRIMARY),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(BG_SURFACE);
        
        JButton dirBtn = createModernButton(Localization.tr("button.choose_directory"), PRIMARY);
        JButton fileBtn = createModernButton(Localization.tr("button.choose_file"), PRIMARY);
        refreshBtn = createModernButton(Localization.tr("button.refresh"), PRIMARY_DARK);
        refreshBtn.setEnabled(false);
        
        dirBtn.addActionListener(e -> chooseDirectory());
        fileBtn.addActionListener(e -> chooseFile());
        refreshBtn.addActionListener(e -> refreshGrid());
        
        leftPanel.add(dirBtn);
        leftPanel.add(fileBtn);
        leftPanel.add(refreshBtn);
        
        pathField = new JTextField(40);
        pathField.setEditable(false);
        pathField.setBackground(BG_HOVER);
        pathField.setForeground(TEXT_PRIMARY);
        pathField.setCaretColor(TEXT_PRIMARY);
        pathField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_DARK, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        bar.add(leftPanel, BorderLayout.WEST);
        bar.add(pathField, BorderLayout.CENTER);
        
        return bar;
    }
    
    /**
     * Creates the action panel shown when a directory is selected (search, snapshot, stats).
     *
     * @return configured action panel
     */
    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        
        JButton searchBtn = createModernButton(Localization.tr("button.search"), ACCENT);
        JButton clearBtn = createModernButton(Localization.tr("button.clear_search"), PRIMARY_DARK);
        JButton snapshotBtn = createModernButton(Localization.tr("button.take_snapshot"), PRIMARY);
        JButton compareBtn = createModernButton(Localization.tr("button.compare_snapshot"), PRIMARY);
        JButton statsBtn = createModernButton(Localization.tr("button.show_stats"), PRIMARY);
        
        searchBtn.addActionListener(e -> showSearchDialog());
        clearBtn.addActionListener(e -> clearSearch());
        snapshotBtn.addActionListener(e -> saveSnapshot());
        compareBtn.addActionListener(e -> new CompareDialog(this, currentPath).setVisible(true));
        statsBtn.addActionListener(e -> new StatsDialog(this, currentPath).setVisible(true));
        
        panel.add(searchBtn);
        panel.add(clearBtn);
        panel.add(snapshotBtn);
        panel.add(compareBtn);
        panel.add(statsBtn);
        
        // Search status label
        searchStatusLabel = new JLabel();
        searchStatusLabel.setForeground(ACCENT);
        searchStatusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        panel.add(searchStatusLabel);
        
        return panel;
    }
    
    /**
     * Opens the search dialog and wires a callback to apply filters.
     */
    private void showSearchDialog() {
        SearchDialog dialog = new SearchDialog(this, currentPath, currentSearchCriteria);
        dialog.setSearchCallback(this::applySearch);
        dialog.setVisible(true);
    }
    
    /**
     * Applies search criteria and refreshes the grid.
     *
     * @param criteria map of active filters
     */
    private void applySearch(Map<String, String> criteria) {
        currentSearchCriteria.clear();
        currentSearchCriteria.putAll(criteria);
        updateSearchStatus();
        refreshGrid();
    }
    
    /**
     * Clears all active search filters and refreshes the grid.
     */
    private void clearSearch() {
        currentSearchCriteria.clear();
        updateSearchStatus();
        refreshGrid();
        showInfo(Localization.tr("msg.search_cleared"));
    }
    
    /**
     * Updates the status text that summarizes active filters (if any).
     */
    private void updateSearchStatus() {
        if (currentSearchCriteria.isEmpty()) {
            searchStatusLabel.setText("");
        } else {
            StringBuilder status = new StringBuilder("Active filters: ");
            currentSearchCriteria.forEach((key, value) -> 
                status.append(key).append("=").append(value).append(" "));
            searchStatusLabel.setText(status.toString());
        }
    }
    
    /**
     * Creates the bottom bar with destructive/modify actions for selected items.
     *
     * @return configured bottom bar panel
     */
    private JPanel createBottomBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        panel.setBackground(BG_DARK);
        
        deleteBtn = createModernButton(Localization.tr("button.delete_selected"), new Color(244, 67, 54));
        modifyBtn = createModernButton(Localization.tr("button.modify_selected"), ACCENT);
        
        deleteBtn.setEnabled(false);
        modifyBtn.setEnabled(false);
        
        deleteBtn.addActionListener(e -> deleteSelected());
        modifyBtn.addActionListener(e -> modifySelected());
        
        panel.add(deleteBtn);
        panel.add(modifyBtn);
        
        return panel;
    }
    
    /**
     * Creates the persistent status bar (left: status text, right: indeterminate progress).
     *
     * @return configured status bar
     */
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, PRIMARY),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        
        statusLabel = new JLabel(Localization.tr("app.ready"));
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setPreferredSize(new Dimension(200, 4));
        loadingBar.setBackground(BG_HOVER);
        loadingBar.setForeground(PRIMARY);
        loadingBar.setBorderPainted(false);
        
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(loadingBar, BorderLayout.EAST);
        
        return bar;
    }
    
    /**
     * Builds the main menu bar (File/View/Tools) and installs it on the frame.
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(BG_SURFACE);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, PRIMARY));
        
        JMenu fileMenu = createMenu("File");
        fileMenu.add(createMenuItem("Exit", e -> System.exit(0)));
        
        JMenu viewMenu = createMenu("View");
        viewMenu.add(createMenuItem("Refresh", e -> refreshGrid()));
        
        JMenu toolsMenu = createMenu("Tools");
        toolsMenu.add(createMenuItem("Settings", e -> showSettings()));
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        
        setJMenuBar(menuBar);
    }
    
    /**
     * Convenience to create a themed {@link JMenu}.
     *
     * @param text menu title
     * @return menu instance
     */
    private JMenu createMenu(String text) {
        JMenu menu = new JMenu(text);
        menu.setForeground(TEXT_PRIMARY);
        return menu;
    }
    
    /**
     * Convenience to create a {@link JMenuItem} with the given action listener.
     *
     * @param text item label
     * @param listener click handler
     * @return menu item
     */
    private JMenuItem createMenuItem(String text, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(BG_SURFACE);
        item.setForeground(TEXT_PRIMARY);
        item.addActionListener(listener);
        return item;
    }
    
    /**
     * Creates a styled button with hover feedback.
     *
     * @param text label
     * @param color base background color
     * @return configured button
     */
    private JButton createModernButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        
        return btn;
    }
    
    /**
     * Opens a directory chooser and refreshes the image grid upon selection.
     * <p>
     * Uses {@link JFileChooser} in {@code DIRECTORIES_ONLY} mode. :contentReference[oaicite:2]{index=2}
     */
    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentPath = chooser.getSelectedFile().toPath();
            pathField.setText(currentPath.toString());
            refreshBtn.setEnabled(true);
            actionPanel.setVisible(true);
            refreshGrid();
        }
    }
    
    /**
     * Opens a file chooser filtered to common image extensions and shows metadata.
     * <p>
     * Images are later read via {@link ImageIO} for thumbnailing. :contentReference[oaicite:3]{index=3}
     */
    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Images", "png", "jpg", "jpeg", "webp", "gif", "bmp"));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentPath = chooser.getSelectedFile().toPath();
            pathField.setText(currentPath.toString());
            actionPanel.setVisible(false);
            refreshGrid();
            new MetadataDialog(this, currentPath).setVisible(true);
        }
    }
    
    /**
     * Rebuilds the image grid. Directory scans and thumbnail reads are offloaded to a
     * background {@link SwingWorker}; UI updates occur on the EDT when done. :contentReference[oaicite:4]{index=4}
     */
    private void refreshGrid() {
        imageGrid.removeAll();
        imagePanels.clear();
        selectedImages.clear();
        updateButtons();
        
        // Show placeholder
        showPlaceholder("Loading images...");
        
        setLoading(true);
        
        SwingWorker<List<Path>, Void> worker = new SwingWorker<>() {
            protected List<Path> doInBackground() throws Exception {
                if (Files.isDirectory(currentPath)) {
                    DirectoryAnalyzer analyzer = new DirectoryAnalyzer(currentPath);
                    List<Path> paths = analyzer.listImageFiles().stream()
                        .map(ImageFile::getPath)
                        .toList();
                    
                    // Apply search filters if any
                    if (!currentSearchCriteria.isEmpty()) {
                        return paths.stream()
                            .filter(p -> matchesSearchCriteria(p))
                            .toList();
                    }
                    return paths;
                } else {
                    return List.of(currentPath);
                }
            }
            
            protected void done() {
                try {
                    List<Path> paths = get();
                    imageGrid.removeAll();
                    
                    if (paths.isEmpty()) {
                        showPlaceholder(currentSearchCriteria.isEmpty() ? 
                            "No images found in this directory" :
                            "No images match your search criteria");
                    } else {
                        // Reset to grid layout for images
                        imageGrid.setLayout(new GridLayout(0, 4, 20, 20));
                        
                        for (Path path : paths) {
                            ImagePanel panel = new ImagePanel(path);
                            imagePanels.put(path, panel);
                            imageGrid.add(panel);
                        }
                    }
                    
                    imageGrid.revalidate();
                    imageGrid.repaint();
                } catch (Exception e) {
                    Logger.error("Failed to load images", e);
                    showPlaceholder("Error loading images: " + e.getMessage());
                } finally {
                    setLoading(false);
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Checks if a path matches the currently active UI search filters.
     *
     * @param path candidate image path
     * @return {@code true} if the file satisfies all active criteria
     */
    private boolean matchesSearchCriteria(Path path) {
        try {
            ImageFile file = new ImageFile(path);
            
            String name = currentSearchCriteria.get("name");
            if (name != null && !path.getFileName().toString()
                    .toLowerCase().contains(name.toLowerCase())) {
                return false;
            }
            
            String year = currentSearchCriteria.get("year");
            if (year != null) {
                String fileYear = String.valueOf(file.getLastModified().getYear());
                if (!fileYear.equals(year)) return false;
            }
            
            String dims = currentSearchCriteria.get("dimensions");
            if (dims != null && dims.contains("x")) {
                String[] parts = dims.split("x");
                try {
                    int w = Integer.parseInt(parts[0].trim());
                    int h = Integer.parseInt(parts[1].trim());
                    if (file.getWidth() != w || file.getHeight() != h) return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Shows a centered placeholder message in the grid area.
     *
     * @param message text to display
     */
    private void showPlaceholder(String message) {
        imageGrid.removeAll();
        imageGrid.setLayout(new GridBagLayout()); // Use GridBagLayout for perfect centering
        
        JPanel placeholder = new JPanel();
        placeholder.setLayout(new BoxLayout(placeholder, BoxLayout.Y_AXIS));
        placeholder.setBackground(BG_DARK);
        placeholder.setOpaque(false);
        
        JLabel label = new JLabel(message);
        label.setForeground(TEXT_SECONDARY);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        placeholder.add(label);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        
        imageGrid.add(placeholder, gbc);
        imageGrid.revalidate();
        imageGrid.repaint();
    }
    
    /**
     * Displays a branded welcome view with app name and quick instructions.
     */
    private void showWelcomePlaceholder() {
        imageGrid.removeAll();
        imageGrid.setLayout(new GridBagLayout());
        
        JPanel welcome = new JPanel();
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        welcome.setBackground(BG_DARK);
        welcome.setOpaque(false);
        
        // App name in big fancy font
        JLabel appName = new JLabel("ImageMeta");
        appName.setForeground(PRIMARY);
        appName.setFont(new Font("Segoe UI Light", Font.BOLD, 72));
        appName.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Subtitle
        JLabel subtitle = new JLabel(Localization.tr("welcome.subtitle"));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Instructions
        JLabel instructions = new JLabel(Localization.tr("welcome.instructions"));
        instructions.setForeground(TEXT_SECONDARY);
        instructions.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        instructions.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        welcome.add(appName);
        welcome.add(Box.createVerticalStrut(20));
        welcome.add(subtitle);
        welcome.add(Box.createVerticalStrut(30));
        welcome.add(instructions);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        
        imageGrid.add(welcome, gbc);
        imageGrid.revalidate();
        imageGrid.repaint();
    }
    
    /**
     * Collects current directory images and persists a snapshot.
     * <p>
     * On success, a localized confirmation message is shown.
     */
    private void saveSnapshot() {
        try {
            DirectoryAnalyzer analyzer = new DirectoryAnalyzer(currentPath);
            List<ImageFile> files = analyzer.listImageFiles();
            SnapshotManager manager = new SnapshotManager(currentPath);
            manager.saveSnapshot(files);
            showInfo(Localization.tr("msg.snapshot_saved"));
        } catch (IOException e) {
            Logger.error("Snapshot failed", e);
            showError("Snapshot error: " + e.getMessage());
        }
    }
    
    /**
     * Deletes all currently selected images after user confirmation.
     * <p>
     * Filesystem errors are logged and the grid is refreshed afterward.
     */
    private void deleteSelected() {
        if (selectedImages.isEmpty()) return;
        
        int confirm = JOptionPane.showConfirmDialog(this,
            Localization.tr("msg.delete_confirm", selectedImages.size()),
            "Confirm",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            int success = 0;
            for (Path path : selectedImages) {
                try {
                    Files.delete(path);
                    success++;
                } catch (IOException e) {
                    Logger.error("Delete failed: {}", path, e);
                }
            }
            selectedImages.clear();
            refreshGrid();
            showInfo(Localization.tr("msg.delete_success", success));
        }
    }
    
    /**
     * Renames a single selected image (if exactly one is selected).
     * <p>
     * Prompts for a new filename and attempts an atomic {@link Files#move(Path, Path, java.nio.file.CopyOption...)}.
     */
    private void modifySelected() {
        if (selectedImages.size() != 1) return;
        
        Path path = selectedImages.get(0);
        String newName = JOptionPane.showInputDialog(this,
            "New name:", path.getFileName().toString());
        
        if (newName != null && !newName.trim().isEmpty()) {
            try {
                Files.move(path, path.resolveSibling(newName));
                refreshGrid();
                showInfo("File renamed");
            } catch (IOException e) {
                Logger.error("Rename failed", e);
                showError("Rename failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Shows the settings dialog and recreates the main window to apply changes.
     */
    private void showSettings() {
        new LanguageSelectionDialog(this).setVisible(true);
        // Refresh UI with new language
        dispose();
        new GraphicalInterface().setVisible(true);
    }
    
    /**
     * Enables/disables action buttons based on the current selection size.
     */
    private void updateButtons() {
        deleteBtn.setEnabled(!selectedImages.isEmpty());
        modifyBtn.setEnabled(selectedImages.size() == 1);
    }
    
    /**
     * Toggles the loading indicator and status text (thread-safe for EDT).
     *
     * @param loading whether a background operation is in progress
     */
    private void setLoading(boolean loading) {
        SwingUtilities.invokeLater(() -> {
            loadingBar.setVisible(loading);
            statusLabel.setText(loading ? Localization.tr("app.loading") : Localization.tr("app.ready"));
        });
    }
    
    /**
     * Shows an informational dialog.
     *
     * @param msg message text
     */
    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Shows an error dialog.
     *
     * @param msg message text
     */
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Single-tile image panel with thumbnail preview and selection behavior.
     * <p>
     * Thumbnails are loaded asynchronously via {@link SwingWorker} and scaled to fit
     * within a fixed square while preserving aspect ratio. Double-click opens the
     * metadata dialog for the underlying image.
     */
    // Inner class for image panels
    private class ImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private transient final Path path;
        private transient Image thumbnail;
        private boolean selected = false;
        private static final int SIZE = 220;
        
        /**
         * Creates a panel for the given image path and begins thumbnail loading.
         *
         * @param path image path
         */
        ImagePanel(Path path) {
            this.path = path;
            setPreferredSize(new Dimension(SIZE, SIZE));
            setBackground(BG_SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BG_HOVER, 2, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            loadThumbnail();
            
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        toggleSelection();
                    } else if (e.getClickCount() == 2) {
                        new MetadataDialog(GraphicalInterface.this, path).setVisible(true);
                    }
                }
            });
        }
        
        /**
         * Starts a background task to decode and scale the image for preview.
         * <p>
         * Uses {@link ImageIO#read(File)} to decode; completion updates the UI on the EDT. :contentReference[oaicite:5]{index=5}
         */
        private void loadThumbnail() {
            new SwingWorker<Image, Void>() {
                protected Image doInBackground() throws Exception {
                    Image img = ImageIO.read(path.toFile());
                    if (img == null) return null;
                    
                    int w = img.getWidth(null);
                    int h = img.getHeight(null);
                    double scale = Math.min((double) SIZE / w, (double) SIZE / h);
                    int newW = (int) (w * scale);
                    int newH = (int) (h * scale);
                    
                    return img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                }
                
                protected void done() {
                    try {
                        thumbnail = get();
                        repaint();
                    } catch (Exception e) {
                        Logger.warn("Thumbnail load failed: {}", path);
                    }
                }
            }.execute();
        }
        
        /**
         * Toggles the selection state and updates the panel border to reflect it.
         */
        private void toggleSelection() {
            selected = !selected;
            if (selected) {
                selectedImages.add(path);
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY, 3, true),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            } else {
                selectedImages.remove(path);
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BG_HOVER, 2, true),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            }
            updateButtons();
            repaint();
        }
        
        /**
         * Paints the thumbnail (when available) and overlays the filename.
         *
         * @param g graphics context
         */
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (thumbnail != null) {
                int x = (getWidth() - thumbnail.getWidth(null)) / 2;
                int y = (getHeight() - thumbnail.getHeight(null)) / 2 - 15;
                g2.drawImage(thumbnail, x, y, null);
                
                // Filename
                g2.setColor(TEXT_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String name = path.getFileName().toString();
                if (name.length() > 25) name = name.substring(0, 22) + "...";
                
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(name)) / 2;
                int textY = getHeight() - 10;
                
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRoundRect(5, textY - 15, getWidth() - 10, 20, 5, 5);
                
                g2.setColor(TEXT_PRIMARY);
                g2.drawString(name, textX, textY);
            } else {
                g2.setColor(TEXT_SECONDARY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                String msg = "Loading...";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            }
        }
    }
    
    /**
     * Launches the Swing UI on the Event Dispatch Thread (EDT).
     * <p>
     * The system Look &amp; Feel is applied when available; failures are logged but non-fatal. :contentReference[oaicite:6]{index=6}
     */
    public static void start() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                Logger.warn("Failed to set L&F", e);
            }
            new GraphicalInterface().setVisible(true);
        });
    }
}
