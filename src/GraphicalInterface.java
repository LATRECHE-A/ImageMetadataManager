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

/**
 * Classe principale qui implémente l'interface graphique du gestionnaire d'images.
 * Cette classe hérite de JFrame et fournit une interface utilisateur complète
 * pour la gestion et la manipulation d'images.
 * @author LATRECHE
 * @version 1.0
 */
public class GraphicalInterface extends JFrame {
    /**
      * Définition des couleurs pour le thème sombre de l'interface
      */
    /**
     * A collection of predefined color constants for a dark theme.
     */
    private final Color DARK_BACKGROUND = new Color(26, 26, 26);
    /** 
     * The background color used for dark-themed components.
     */
    private final Color DARK_COMPONENT = new Color(60, 63, 65);
    /**
     * The color used for hover effects in a dark theme.
     */
    private final Color DARK_HOVER = new Color(76, 80, 82);
    /**
     * The text color used for dark-themed interfaces.
     */
    private final Color DARK_TEXT = new Color(255, 255, 255);


    /** 
     * Déclaration des composants principaux de l'interface
     */
    /**
      * Champ pour afficher le chemin courant
      */
    private JTextField pathField;                    
    /**
      * Panneau pour les boutons d'action
      */
    private JPanel actionButtonsPanel;              
    /**
      * Grille pour afficher les images
      */
    private JPanel imageGrid;                       
    /**
      * Panneau défilant
      */
    private JScrollPane scrollPane;                 
    /**
      * Chemin actuel sélectionné
      */
    private Path currentPath;                       
    /**
      * Map des panneaux d'images
      */
    private final Map<Path, ImagePanel> imagePanels = new HashMap<>();  
    /**
      * Barre de statut
      */
    private JPanel statusBar;                       
    /**
      * Indicateur de chargement
      */
    private JProgressBar loadingIndicator;          
    /**
      * Étiquette de statut
      */
    private JLabel statusLabel;                     
    /**
      * Panneau principal
      */
    private JPanel mainPanel;                       
    /**
      * Bouton de suppression
      */
    private JButton deleteSelectedButton;           
    /**
      * Bouton de modification
      */
    private JButton modifySelectedButton;           
    /**
      * Bouton de rafraîchissement                                                   
      */
    private JButton refreshButton;                  

    /**
      * Liste des images sélectionnées
      */
    private final List<Path> selectedImages = new ArrayList<>();  

    /**
     * Constructeur de la classe GraphicalInterface.
     * Initialise la fenêtre principale et configure les paramètres de base.
     *
     * @see #setupUI() Configure l'interface utilisateur pour la fenêtre principale
     * @see javax.swing.JFrame Documentation officielle de JFrame
     */
    public GraphicalInterface() {
        setTitle("Gestionnaire d'images");          // Définit le titre de la fenêtre
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Définit l'action de fermeture
        setupUI();                                  // Configure l'interface utilisateur
        setSize(1200, 800);                        // Définit la taille de la fenêtre
        setLocationRelativeTo(null);               // Centre la fenêtre sur l'écran
    }

    /**
     * Crée un bouton stylisé avec le thème sombre de l'application.
     * Cette méthode applique une apparence cohérente à tous les boutons de l'interface.
     * 
     * @param text Le texte à afficher sur le bouton
     * @return JButton Le bouton stylisé
     *
     * @see #DARK_COMPONENT Couleur de base pour le thème sombre
     * @see #DARK_HOVER Couleur utilisée lors du survol des boutons
     * @see javax.swing.JButton Documentation officielle de JButton
     * @see javax.swing.BorderFactory Documentation officielle de BorderFactory
     * @see java.awt.event.MouseAdapter Documentation officielle de MouseAdapter
     */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);

        // Application du style de base
        button.setBackground(DARK_COMPONENT);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DARK_COMPONENT),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
                    ));
        button.setFocusPainted(false);  // Désactive l'effet de focus

        // Ajout des effets de survol
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(DARK_HOVER);  // Change la couleur au survol
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(DARK_COMPONENT);  // Restaure la couleur normale
            }
        });

        return button;
    }

    /**
     * Crée un champ de texte stylisé selon le thème sombre de l'application.
     * Cette méthode uniformise l'apparence de tous les champs de texte de l'interface.
     * 
     * @param placeholder Le texte indicatif à afficher dans le champ
     * @return JTextField Le champ de texte stylisé
     *
     * @see #createStyledButton(String) Méthode pour créer des boutons stylisés selon le thème sombre
     * @see #DARK_COMPONENT Couleur de fond utilisée pour le thème sombre
     * @see javax.swing.JTextField Documentation officielle de JTextField
     * @see javax.swing.BorderFactory Documentation officielle de BorderFactory
     */
    private JTextField createStyledTextField(String placeholder) {
        /**
          * Crée un champ de texte avec une largeur de 20 colonnes
          */
        JTextField field = new JTextField(20);  

        // Application du style selon le thème sombre
        field.setBackground(DARK_COMPONENT);    // Fond sombre
        field.setForeground(Color.white);       // Texte en blanc
        field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DARK_COMPONENT),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)  // Ajoute de la marge interne
                    ));
        field.setCaretColor(Color.WHITE);       // Curseur en blanc

        return field;
    }

    /**
     * Crée le panneau supérieur de l'interface contenant les contrôles principaux.
     * Ce panneau inclut les boutons de sélection de fichier/répertoire, le champ de chemin
     * et le bouton de rafraîchissement.
     * 
     * @return JPanel Le panneau supérieur configuré
     *
     * @see #createStyledButton(String) Méthode utilisée pour créer des boutons stylisés
     * @see #chooseDirectory() Méthode pour ouvrir le sélecteur de répertoire
     * @see #chooseFile() Méthode pour ouvrir le sélecteur de fichier
     * @see #updateImageGrid(List) Méthode pour rafraîchir la grille d'images
     * @see #DARK_BACKGROUND Couleur de fond du panneau
     * @see #DARK_COMPONENT Couleur de fond des champs et boutons
     * @see javax.swing.JPanel Documentation officielle de JPanel
     * @see javax.swing.JTextField Documentation officielle de JTextField
     * @see javax.swing.JButton Documentation officielle de JButton
     */
    private JPanel createTopPanel() {
        // Création du panneau avec un FlowLayout aligné à gauche
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(DARK_BACKGROUND);

        // Création des boutons de contrôle
        JButton dirButton = createStyledButton("Choisir un répertoire");
        JButton fileButton = createStyledButton("Choisir un fichier");
        refreshButton = createStyledButton("Actualiser");

        // Configuration du champ de chemin
        pathField = new JTextField(30);
        pathField.setEditable(false);           // Rend le champ non éditable
        pathField.setBackground(DARK_COMPONENT);
        pathField.setForeground(DARK_TEXT);

        // Configuration des actions des boutons
        dirButton.addActionListener(e -> chooseDirectory());      // Ouvre le sélecteur de répertoire
        fileButton.addActionListener(e -> chooseFile());          // Ouvre le sélecteur de fichier
        refreshButton.addActionListener(e -> updateImageGrid(null));  // Rafraîchit la grille d'images
        refreshButton.setEnabled(false);        // Désactive initialement le bouton de rafraîchissement

        // Ajout des composants au panneau
        panel.add(dirButton);
        panel.add(fileButton);
        panel.add(pathField);
        panel.add(refreshButton);

        return panel;
    }

    /**
     * Gère la sélection d'un répertoire via une boîte de dialogue.
     * Cette méthode ouvre un sélecteur de fichiers en mode répertoire,
     * met à jour le chemin courant et rafraîchit l'affichage des images.
     *
     * @see #currentPath Champ pour stocker le chemin du répertoire sélectionné
     * @see #pathField Champ de texte affichant le chemin courant
     * @see #refreshButton Bouton permettant de rafraîchir la grille d'images
     * @see #showDirectoryButtons() Méthode pour afficher les boutons spécifiques aux répertoires
     * @see #updateImageGrid(List) Méthode pour rafraîchir la grille d'images avec un chemin donné
     * @see javax.swing.JFileChooser Documentation officielle de JFileChooser
     */
    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);  // Restreint la sélection aux répertoires

        // Si l'utilisateur valide la sélection
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentPath = chooser.getSelectedFile().toPath();  // Stocke le chemin sélectionné
            pathField.setText(currentPath.toString());         // Affiche le chemin dans le champ
            pathField.revalidate();                           // Force la mise à jour du champ
            pathField.repaint();
            refreshButton.setEnabled(true);                   // Active le bouton de rafraîchissement
            showDirectoryButtons();                           // Affiche les boutons spécifiques aux répertoires
            updateImageGrid(null);                           // Met à jour la grille d'images
        }
    }

    /**
     * Gère la sélection d'un fichier image via une boîte de dialogue.
     * Cette méthode ouvre un sélecteur de fichiers filtré pour les images,
     * met à jour le chemin courant et affiche les métadonnées de l'image.
     *
     * @see #currentPath Champ pour stocker le chemin du fichier sélectionné
     * @see #pathField Champ de texte affichant le chemin courant
     * @see #showFileButtons() Méthode pour afficher les boutons spécifiques aux fichiers
     * @see #updateImageGrid(List) Méthode pour rafraîchir la grille d'images avec un chemin donné
     * @see #showImageMetadata(Path) Méthode pour afficher les métadonnées de l'image sélectionnée
     * @see javax.swing.JFileChooser Documentation officielle de JFileChooser
     * @see javax.swing.filechooser.FileNameExtensionFilter Documentation officielle de FileNameExtensionFilter
     */
    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();

        // Configure le filtre pour n'afficher que les fichiers images
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Image Files", "png", "jpg", "jpeg", "webp"
                    ));

        // Si l'utilisateur valide la sélection
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentPath = chooser.getSelectedFile().toPath();  // Stocke le chemin du fichier
            pathField.setText(currentPath.toString());         // Affiche le chemin dans le champ
            pathField.revalidate();                           // Force la mise à jour du champ
            pathField.repaint();
            showFileButtons();                                // Affiche les boutons spécifiques aux fichiers
            updateImageGrid(null);                           // Met à jour la grille d'images
            showImageMetadata(currentPath);                   // Affiche les métadonnées de l'image
        }
    }

    /**
     * Gère l'affichage des boutons spécifiques aux fichiers.
     * Cette méthode nettoie et réinitialise le panneau des boutons d'action
     * pour n'afficher que les options pertinentes pour un fichier unique.
     *
     * @see #actionButtonsPanel Panneau contenant les boutons d'action
     * @see javax.swing.JPanel Documentation officielle de JPanel
     */
    private void showFileButtons() {
        actionButtonsPanel.removeAll();             // Supprime tous les boutons existants
        actionButtonsPanel.setVisible(true);        // Rend le panneau visible
        actionButtonsPanel.revalidate();            // Force la mise à jour du panneau
        actionButtonsPanel.repaint();
    }

    /**
     * Configure l'interface utilisateur principale de l'application.
     * Cette méthode est responsable de créer et d'organiser tous les composants
     * de l'interface, y compris les panneaux, la grille d'images et la barre de statut.
     *
     * @see javax.swing.JPanel Documentation officielle de JPanel
     * @see javax.swing.JScrollPane Documentation officielle de JScrollPane
     * @see #DARK_BACKGROUND Couleur de fond du panneau
     * @see #createTopPanel() Méthode pour créer la partie supérieure de l'interface
     * @see #createActionButtonsPanel() Méthode pour créer le panneau des boutons d'action
     * @see #createImageGrid() Méthode pour créer la grille d'affichage des images
     * @see #createBottomActionPanel() Méthode pour créer le panneau des boutons d'action du bas
     * @see #createStatusBar() Méthode pour créer la barre de statut
     * @see #applyDarkTheme() Méthode pour appliquer le thème sombre
     */
    private void setupUI() {
        // Création et configuration du panneau principal
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));  // Layout avec espacement de 10 pixels
        mainPanel.setBackground(DARK_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));  // Marge externe

        // Configuration de la partie supérieure
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Configuration du panneau central
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setBackground(DARK_BACKGROUND);

        // Configuration du panneau des boutons d'action
        actionButtonsPanel = createActionButtonsPanel();
        actionButtonsPanel.setVisible(false);         // Caché initialement
        centerPanel.add(actionButtonsPanel, BorderLayout.NORTH);

        // Configuration de la grille d'images avec défilement
        scrollPane = new JScrollPane(imageGrid = createImageGrid());
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Ajout des boutons d'action en bas
        JPanel bottomActionPanel = createBottomActionPanel();
        centerPanel.add(bottomActionPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Configuration de la barre de statut
        statusBar = createStatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        // Finalisation de la configuration
        setContentPane(mainPanel);
        applyDarkTheme();        // Application du thème sombre
    }

    /**
     * Crée le panneau des boutons d'action.
     * Ce panneau est destiné à contenir les boutons d'action dynamiques qui changent
     * en fonction du contexte (fichier unique ou répertoire).
     * 
     * @return JPanel Le panneau des boutons d'action configuré
     *
     * @see javax.swing.JPanel Documentation officielle de JPanel
     * @see java.awt.FlowLayout Documentation officielle de FlowLayout
     * @see #DARK_BACKGROUND Couleur de fond du panneau
     */
    private JPanel createActionButtonsPanel() {
        // Création d'un panneau avec un FlowLayout aligné à gauche
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(DARK_BACKGROUND);
        panel.setVisible(false);    // Le panneau est invisible par défaut
        return panel;
    }

    /**
     * Crée la grille d'affichage des images.
     * Cette méthode configure un panneau avec une disposition en grille pour afficher
     * les miniatures des images avec défilement automatique.
     * 
     * @return JPanel La grille configurée pour l'affichage des images
     *
     * @see #DARK_BACKGROUND Couleur de fond du panneau
     * @see javax.swing.JPanel Documentation officielle de JPanel
     * @see javax.swing.JScrollPane Documentation officielle de JScrollPane
     * @see java.awt.GridLayout Documentation officielle de GridLayout
     * @see javax.swing.ScrollPaneConstants Documentation officielle de ScrollPaneConstants
     * @see javax.swing.BorderFactory Documentation officielle de BorderFactory
     */
    private JPanel createImageGrid() {
        // Création de la grille avec 4 colonnes et un nombre dynamique de lignes
        JPanel grid = new JPanel(new GridLayout(0, 4, 15, 15));  // Espacement de 15 pixels
        grid.setBackground(DARK_BACKGROUND);

        // Configuration du panneau de défilement
        JScrollPane scrollableGrid = new JScrollPane(grid);
        scrollableGrid.setBackground(DARK_BACKGROUND);
        scrollableGrid.getViewport().setBackground(DARK_BACKGROUND);

        // Personnalisation de l'apparence du défilement
        scrollableGrid.setBorder(BorderFactory.createEmptyBorder());

        // Configuration des barres de défilement
        scrollableGrid.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);  // Désactive le défilement horizontal
        scrollableGrid.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);  // Active le défilement vertical si nécessaire

        return grid;
    }

    /**
     * Crée le panneau des boutons d'action du bas de l'interface.
     * Ce panneau contient les boutons de suppression et de modification des images sélectionnées.
     * Les boutons sont initialement désactivés jusqu'à ce qu'une sélection soit faite.
     * 
     * @return JPanel Le panneau configuré avec les boutons d'action
     *
     * @see #DARK_BACKGROUND Couleur de fond du panneau
     * @see javax.swing.JButton Documentation officielle de JButton
     * @see javax.swing.JPanel Documentation officielle de JPanel
     * @see #createStyledButton(String) Méthode pour créer des boutons stylisés
     * @see #deleteSelectedImages() Méthode pour supprimer les images sélectionnées
     * @see #modifySelectedImage() Méthode pour modifier l'image sélectionnée
     * @see java.awt.FlowLayout Documentation officielle de FlowLayout
     */
    private JPanel createBottomActionPanel() {
        // Création du panneau avec un FlowLayout centré
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBackground(DARK_BACKGROUND);

        // Création des boutons d'action
        deleteSelectedButton = createStyledButton("Supprimer les images sélectionnées");
        modifySelectedButton = createStyledButton("Modifier l'image sélectionnée");

        // Désactivation initiale des boutons
        deleteSelectedButton.setEnabled(false);
        modifySelectedButton.setEnabled(false);

        // Configuration des actions des boutons
        deleteSelectedButton.addActionListener(e -> deleteSelectedImages());
        modifySelectedButton.addActionListener(e -> modifySelectedImage());

        // Ajout des boutons au panneau
        panel.add(deleteSelectedButton);
        panel.add(modifySelectedButton);

        return panel;
    }

    /**
     * Gère la suppression des images sélectionnées.
     * Cette méthode affiche une confirmation, supprime les fichiers sélectionnés
     * et met à jour l'interface en conséquence. Elle gère également les erreurs
     * potentielles lors de la suppression.
     *
     * @see java.nio.file.Files Documentation officielle de la classe Files pour la gestion des fichiers
     * @see java.nio.file.Path Documentation officielle de la classe Path pour représenter les chemins de fichiers
     * @see java.io.IOException Documentation officielle de IOException pour les erreurs d'E/S
     * @see javax.swing.JOptionPane Documentation officielle de JOptionPane pour les dialogues de confirmation et d'erreur
     * @see #selectedImages Liste des images actuellement sélectionnées
     * @see #updateImageGrid(List) Méthode pour rafraîchir la grille d'images après modification
     * @see #updateSelectionButtons() Méthode pour mettre à jour l'état des boutons en fonction de la sélection
     * @see #showInfo(String) Méthode pour afficher des informations à l'utilisateur
     * @see #showError(String) Méthode pour afficher un message d'erreur à l'utilisateur
     */
    private void deleteSelectedImages() {
        // Vérification de la sélection (redondant car le bouton devrait être désactivé)
        if (selectedImages.isEmpty()) {
            showError("Aucune image sélectionnée pour la suppression.");
            return;
        }

        // Demande de confirmation à l'utilisateur
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Êtes-vous sûr de vouloir supprimer " + selectedImages.size() + " fichier(s) sélectionné(s)?\n" +
                "Cette action ne peut pas être annulée !",
                "Confirmer la suppression",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
                );

        // Procéder à la suppression si confirmée
        if (confirm == JOptionPane.YES_OPTION) {
            int successCount = 0;    // Compteur de suppressions réussies
            int failureCount = 0;    // Compteur d'échecs
            StringBuilder errorMessages = new StringBuilder();  // Collection des messages d'erreur

            // Tentative de suppression de chaque image sélectionnée
            for (Path imagePath : selectedImages) {
                try {
                    Files.delete(imagePath);
                    successCount++;
                } catch (IOException e) {
                    failureCount++;
                    errorMessages.append("Échec de la suppression : ")
                        .append(imagePath.getFileName())
                        .append(" (")
                        .append(e.getMessage())
                        .append(")\n");
                }
            }

            // Nettoyage et mise à jour de l'interface
            selectedImages.clear();
            updateImageGrid(null);
            updateSelectionButtons();

            // Affichage des résultats
            if (failureCount == 0) {
                showInfo(successCount + " fichier(s) supprimé(s) avec succès.");
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        successCount + " fichier(s) supprimé(s) avec succès.\n" +
                        failureCount + " fichier(s) n'ont pas pu être supprimés.\n\n" +
                        "Détails de l'erreur :\n" + errorMessages,
                        "Résultats de la suppression",
                        JOptionPane.WARNING_MESSAGE
                        );
            }
        }
    }

    /**
     * Gère la modification du nom de l'image sélectionnée.
     * Cette méthode permet de renommer une image sélectionnée en affichant
     * une boîte de dialogue pour saisir le nouveau nom. Elle ne fonctionne
     * que lorsqu'une seule image est sélectionnée.
     *
     * @see java.nio.file.Files Documentation officielle de la classe Files pour la gestion des fichiers
     * @see java.nio.file.Path Documentation officielle de la classe Path pour représenter les chemins de fichiers
     * @see java.io.IOException Documentation officielle de IOException pour les erreurs d'E/S
     * @see javax.swing.JOptionPane Documentation officielle de JOptionPane pour les dialogues de saisie
     * @see #selectedImages Liste des images actuellement sélectionnées
     * @see #updateImageGrid(List) Méthode pour rafraîchir la grille d'images après modification
     * @see #showInfo(String) Méthode pour afficher des informations à l'utilisateur
     * @see #showError(String) Méthode pour afficher un message d'erreur à l'utilisateur
     */
    private void modifySelectedImage() {
        // Vérifie qu'une seule image est sélectionnée
        if (selectedImages.size() == 1) {
            Path selectedImage = selectedImages.get(0);
            String currentFileName = selectedImage.getFileName().toString();

            // Affiche une boîte de dialogue pour saisir le nouveau nom
            String newName = JOptionPane.showInputDialog(
                    this, 
                    "Entrez le nouveau nom de l'image :", 
                    currentFileName
                    );

            // Procède au renommage si un nouveau nom est fourni
            if (newName != null && !newName.trim().isEmpty()) {
                try {
                    // Crée le nouveau chemin et déplace le fichier
                    Path newPath = selectedImage.resolveSibling(newName);
                    Files.move(selectedImage, newPath);

                    // Met à jour l'interface
                    updateImageGrid(null);
                    showInfo("Image renommée avec succès !");
                } catch (IOException e) {
                    showError("Erreur lors du changement de nom de l'image : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Met à jour l'état des boutons de sélection en fonction des images sélectionnées.
     * Cette méthode active ou désactive les boutons de suppression et de modification
     * selon le nombre d'images sélectionnées.
     *
     * @see javax.swing.JButton Documentation officielle de JButton pour la gestion des boutons
     * @see #deleteSelectedButton Bouton pour supprimer les images sélectionnées
     * @see #modifySelectedButton Bouton pour modifier l'image sélectionnée
     * @see #selectedImages Liste des images actuellement sélectionnées
     */
    private void updateSelectionButtons() {
        // Active le bouton de suppression si au moins une image est sélectionnée
        deleteSelectedButton.setEnabled(!selectedImages.isEmpty());

        // Active le bouton de modification uniquement si une seule image est sélectionnée
        modifySelectedButton.setEnabled(selectedImages.size() == 1);
    }

    /**
     * Crée un bouton de fermeture pour une boîte de dialogue.
     * Cette méthode ajoute un bouton "Fermer" stylisé au bas d'une boîte de dialogue
     * et configure son action pour fermer la fenêtre.
     * 
     * @param dialog La boîte de dialogue à laquelle ajouter le bouton de fermeture
     *
     * @see javax.swing.JDialog Classe utilisée pour créer des boîtes de dialogue modales
     * @see javax.swing.JButton Documentation officielle de JButton pour créer des boutons
     * @see javax.swing.JPanel Documentation officielle de JPanel pour organiser les composants
     * @see java.awt.FlowLayout Layout utilisé pour aligner le bouton à droite
     * @see #createStyledButton(String) Méthode pour créer un bouton avec style personnalisé
     * @see #DARK_BACKGROUND Couleur de fond utilisée pour maintenir le thème sombre
     */
    private void createCloseButton(JDialog dialog) {
        // Création du bouton de fermeture
        JButton closeButton = createStyledButton("Fermer");
        closeButton.addActionListener(e -> dialog.dispose());

        // Création du panneau pour le bouton
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(DARK_BACKGROUND);
        buttonPanel.add(closeButton);

        // Ajout du panneau à la boîte de dialogue
        dialog.add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Gère l'état de chargement de l'interface.
     * Met à jour l'indicateur de chargement et le texte de statut
     * de manière thread-safe via SwingUtilities.
     * 
     * @param isLoading true si un chargement est en cours, false sinon
     *
     * @see javax.swing.SwingUtilities Classe utilitaire pour exécuter du code thread-safe avec l'EDT (Event Dispatch Thread)
     * @see javax.swing.JLabel Documentation officielle pour afficher et mettre à jour des textes dans l'interface
     * @see javax.swing.JComponent#setVisible(boolean) Méthode pour afficher ou masquer un composant
     * @see #loadingIndicator Indicateur de chargement affiché pendant les opérations longues
     * @see #statusLabel Label utilisé pour afficher les messages d'état de l'application
     */
    private void setLoadingState(boolean isLoading) {
        SwingUtilities.invokeLater(() -> {
            loadingIndicator.setVisible(isLoading);
            statusLabel.setText(isLoading ? "Chargement..." : "Prêt");
        });
    }

    /**
     * Configure et affiche les boutons spécifiques au mode répertoire.
     * Cette méthode initialise les boutons pour la recherche, les snapshots,
     * et les statistiques lorsqu'un répertoire est sélectionné.
     *
     * @see javax.swing.JButton Documentation officielle pour créer des boutons dans l'interface
     * @see javax.swing.JPanel Méthode pour ajouter ou enlever des composants dynamiquement
     * @see javax.swing.JComponent#revalidate() Utilisé pour recalculer la disposition après modification
     * @see javax.swing.JComponent#repaint() Redessine un composant pour refléter les changements
     * @see #actionButtonsPanel Panneau contenant les boutons spécifiques au mode répertoire
     * @see #currentPath Champ pour stocker le chemin du répertoire sélectionné
     * @see #createStyledButton(String) Méthode pour créer des boutons stylisés selon le thème sombre
     * @see #showSearchDialog() Affiche une boîte de dialogue pour rechercher des images
     * @see #updateImageGrid(List) Met à jour la grille d'images affichée
     * @see #showInfo(String) Affiche un message d'information à l'utilisateur
     * @see #executeCliCommand(String...) Exécute une commande CLI pour enregistrer un snapshot
     * @see #showCompareDialog() Affiche une boîte de dialogue pour comparer les snapshots
     * @see #showDirectoryStats() Affiche les statistiques du répertoire courant
     */
    private void showDirectoryButtons() {
        actionButtonsPanel.removeAll();  // Nettoie le panneau

        // Création des boutons
        JButton searchButton = createStyledButton("Rechercher des images");
        JButton clearSearchButton = createStyledButton("Effacer la recherche");
        JButton snapshotButton = createStyledButton("Prendre un Snapshot");
        JButton compareButton = createStyledButton("Comparer avec Snapshot");
        JButton statsButton = createStyledButton("Afficher les statistiques");

        // Configuration des actions des boutons
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

        // Ajout des boutons au panneau
        actionButtonsPanel.add(searchButton);
        actionButtonsPanel.add(clearSearchButton);
        actionButtonsPanel.add(snapshotButton);
        actionButtonsPanel.add(compareButton);
        actionButtonsPanel.add(statsButton);

        // Mise à jour de l'affichage
        actionButtonsPanel.setVisible(true);
        actionButtonsPanel.revalidate();
        actionButtonsPanel.repaint();
    }

    /**
     * Affiche une boîte de dialogue permettant de comparer les snapshots d'images.
     * Cette fonction crée une fenêtre modale qui exécute une commande CLI pour obtenir
     * et afficher les différences entre les snapshots.
     *
     * @see javax.swing.JDialog Documentation officielle pour la création de boîtes de dialogue modales
     * @see javax.swing.JTextArea Zone de texte utilisée pour afficher les résultats de la comparaison
     * @see javax.swing.JScrollPane Composant permettant de gérer le défilement dans la zone de texte
     * @see javax.swing.JButton Documentation officielle pour créer un bouton interactif
     * @see javax.swing.JPanel Panneau utilisé pour organiser les boutons dans la boîte de dialogue
     * #see javax.swing.BorderLayout Mise en page utilisée pour organiser les composants de la boîte de dialogue
     * @see java.awt.FlowLayout Mise en page utilisée pour aligner les boutons à droite
     * @see #executeCliCommand(String...) Exécute une commande CLI pour obtenir les différences entre snapshots
     * @see #currentPath Champ pour stocker le chemin du répertoire sélectionné
     * @see #DARK_COMPONENT Couleur de fond de la zone de texte pour le thème sombre
     * @see #DARK_BACKGROUND Couleur de fond pour le panneau des boutons
     * @see #DARK_TEXT Couleur utilisée pour le texte dans le thème sombre
     * @see #createStyledButton(String) Méthode pour créer un bouton stylisé
     */
    private void showCompareDialog() {
        // Création d'une boîte de dialogue modale avec un titre spécifique
        JDialog dialog = new JDialog(this, "Snapshot Comparison", true);
        dialog.setLayout(new BorderLayout(10, 10));  // Configuration de la mise en page avec des marges

        // Exécution de la commande CLI pour obtenir la comparaison des snapshots
        String output = executeCliCommand("-d", currentPath.toString(), "--compare-snapshot");

        // Création d'une zone de texte non éditable pour afficher les résultats
        JTextArea textArea = new JTextArea(output);
        textArea.setEditable(false);
        textArea.setBackground(DARK_COMPONENT);  // Application du thème sombre
        textArea.setForeground(DARK_TEXT);

        // Configuration de la scrollbar et ajout des composants
        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane);

        // Configuration du bouton de fermeture et son panneau
        JButton closeButton = createStyledButton("Fermer");
        closeButton.addActionListener(e -> dialog.dispose());  // Fermeture de la boîte de dialogue
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(DARK_BACKGROUND);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Configuration finale et affichage de la boîte de dialogue
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Affiche une boîte de dialogue contenant les statistiques du répertoire courant.
     * Cette fonction récupère et affiche les statistiques via une commande CLI.
     *
     * @see javax.swing.JDialog Documentation officielle pour créer une boîte de dialogue modale
     * @see javax.swing.JTextArea Zone de texte utilisée pour afficher les statistiques du répertoire
     * @see javax.swing.JScrollPane Composant permettant d'ajouter une barre de défilement à la zone de texte
     * @see javax.swing.JButton Documentation pour créer un bouton interactif
     * @see javax.swing.JPanel Panneau utilisé pour organiser les composants, en particulier les boutons
     * #see javax.swing.BorderLayout Mise en page utilisée pour organiser les composants dans la boîte de dialogue
     * @see java.awt.FlowLayout Mise en page utilisée pour aligner les boutons à droite
     * @see #currentPath Champ pour stocker le chemin du répertoire sélectionné
     * @see #executeCliCommand(String...) Méthode pour exécuter une commande CLI et récupérer les statistiques
     * @see #DARK_COMPONENT Couleur de fond de la zone de texte pour le thème sombre
     * @see #DARK_BACKGROUND Couleur de fond pour le panneau des boutons
     * @see #DARK_TEXT Couleur utilisée pour le texte dans le thème sombre
     * @see #createStyledButton(String) Méthode pour créer un bouton stylisé
     */
    private void showDirectoryStats() {
        // Création d'une boîte de dialogue modale pour les statistiques
        JDialog dialog = new JDialog(this, "Statistiques du répertoire", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // Exécution de la commande CLI pour obtenir les statistiques
        String stats = executeCliCommand("-d", currentPath.toString(), "--stat");

        // Configuration de la zone de texte pour afficher les statistiques
        JTextArea textArea = new JTextArea(stats);
        textArea.setEditable(false);
        textArea.setBackground(DARK_COMPONENT);
        textArea.setForeground(DARK_TEXT);

        // Ajout d'une scrollbar et configuration des composants
        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane);

        // Configuration du bouton de fermeture
        JButton closeButton = createStyledButton("Fermer");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(DARK_BACKGROUND);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Configuration finale et affichage
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Affiche les métadonnées d'une image spécifique dans une boîte de dialogue.
     * Cette fonction charge également une miniature de l'image et affiche les informations
     * détaillées obtenues via une commande CLI.
     * 
     * @param imagePath Le chemin vers l'image dont on veut afficher les métadonnées
     *
     * @see javax.swing.JDialog Documentation officielle pour créer une boîte de dialogue modale
     * @see javax.swing.JTextArea Zone de texte utilisée pour afficher les métadonnées de l'image
     * @see javax.swing.JScrollPane Composant permettant d'ajouter une barre de défilement à la zone de texte
     * @see javax.swing.JButton Documentation pour créer un bouton interactif
     * @see javax.swing.JPanel Panneau utilisé pour organiser les composants, notamment le bouton de fermeture
     * #see javax.swing.BorderLayout Mise en page utilisée pour organiser les composants dans la boîte de dialogue
     * @see java.awt.FlowLayout Mise en page utilisée pour aligner les boutons à droite
     * @see javax.swing.JLabel Utilisé pour afficher la miniature de l'image
     * @see javax.swing.ImageIcon Classe utilisée pour représenter l'image redimensionnée
     * @see java.awt.Image Documentation pour manipuler et redimensionner les images
     * @see javax.imageio.ImageIO Classe utilisée pour lire les images à partir du système de fichiers
     * @see #executeCliCommand(String...) Méthode pour exécuter une commande CLI et obtenir les métadonnées
     * @see #DARK_COMPONENT Couleur de fond de la zone de texte pour le thème sombre
     * @see #DARK_BACKGROUND Couleur de fond pour le panneau des boutons et la miniature
     * @see #DARK_TEXT Couleur utilisée pour le texte dans le thème sombre
     * @see #createStyledButton(String) Méthode pour créer un bouton stylisé
     * @see #showError(String) Méthode pour afficher les messages d'erreur dans une boîte de dialogue
     */
    private void showImageMetadata(Path imagePath) {
        // Création de la boîte de dialogue avec le nom du fichier
        JDialog dialog = new JDialog(this, "Image Metadata: " + imagePath.getFileName(), true);
        dialog.setLayout(new BorderLayout(10, 10));

        try {
            // Récupération des métadonnées via la commande CLI
            String metadata = executeCliCommand("-f", imagePath.toString(), "-i", "--stat");

            // Tentative de chargement et d'affichage de la miniature
            try {
                Image image = ImageIO.read(imagePath.toFile());
                if (image != null){
                    // Redimensionnement et affichage de l'image
                    ImageIcon icon = new ImageIcon(image.getScaledInstance(300, -1, Image.SCALE_SMOOTH));
                    JLabel imageLabel = new JLabel(icon);
                    imageLabel.setBackground(DARK_BACKGROUND);
                    dialog.add(imageLabel, BorderLayout.NORTH);
                }
            } catch (IOException thumbnailException) {
                // Gestion des erreurs de chargement de la miniature
                System.err.println("Impossible de charger la miniature de l'image: " + thumbnailException.getMessage());
            }

            // Configuration de l'affichage des métadonnées
            JTextArea textArea = new JTextArea(metadata);
            textArea.setEditable(false);
            textArea.setBackground(DARK_COMPONENT);
            textArea.setForeground(DARK_TEXT);

            // Configuration de la scrollbar et des composants
            JScrollPane scrollPane = new JScrollPane(textArea);
            dialog.add(scrollPane, BorderLayout.CENTER);

            // Configuration du bouton de fermeture
            JButton closeButton = createStyledButton("Fermer");
            closeButton.addActionListener(e -> dialog.dispose());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(DARK_BACKGROUND);
            buttonPanel.add(closeButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            // Configuration finale et affichage
            dialog.setSize(500, 600);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        } catch (Exception e){
            showError("Erreur lors de l'affichage des métadonnées de l'image: " + e.getMessage());
        }
    }

    /**
     * Affiche une boîte de dialogue permettant de rechercher des images selon différents critères.
     * Cette fonction permet la recherche par nom de fichier, année et dimensions, et utilise
     * un SwingWorker pour effectuer la recherche de manière asynchrone.
     *
     * @see javax.swing.JDialog pour créer et afficher une fenêtre de dialogue modale utilisée pour rechercher des images.
     * @see javax.swing.SwingWorker pour exécuter l'opération de recherche de manière asynchrone sans bloquer l'interface utilisateur.
     * @see java.nio.file.Files pour les opérations sur les fichiers, comme vérifier l'existence d'un fichier.
     * @see java.nio.file.Paths pour convertir des chemins sous forme de chaînes en objets Path.
     * @see java.nio.file.Path pour représenter les chemins des fichiers retournés par la recherche.
     * @see java.util.List pour gérer la collection des résultats de recherche (chemins) retournés par l'opération.
     * @see #createStyledButton(String) Méthode pour créer des boutons stylisés selon le thème sombre
     * @see #createStyledTextField(String) pour créer des champs de texte personnalisés utilisés pour saisir les critères de recherche.
     * @see #executeCliCommand(String...) pour exécuter la commande en ligne de commande en fonction des critères saisis par l'utilisateur.
     * @see #setLoadingState(boolean) pour gérer l'état de chargement de l'interface utilisateur pendant la recherche asynchrone.
     * @see #updateImageGrid(List) pour mettre à jour l'interface utilisateur avec les résultats de recherche.
     * @see #showError(String) pour afficher des messages d'erreur en cas de problème lors de la recherche.
     */
    private void showSearchDialog() {
        // Création de la boîte de dialogue de recherche
        JDialog dialog = new JDialog(this, "Rechercher des images", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // Configuration du panneau principal
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(DARK_BACKGROUND);

        // Création des champs de recherche stylisés
        JTextField nameField = createStyledTextField("Entrer le nom du fichier");
        JTextField yearField = createStyledTextField("Entrez l'année (AAAA)");
        JTextField dimensionsField = createStyledTextField("Entrez les dimensions (L x H)");

        // Configuration du bouton de recherche et de son comportement
        JButton searchButton = createStyledButton("Recherche");
        searchButton.addActionListener(e -> {
            // Collecte des critères de recherche non vides
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

            // Préparation des arguments pour la commande CLI
            String[] args = new String[criteria.size() + 2];
            args[0] = "--search";
            args[1] = currentPath.toString();
            System.arraycopy(criteria.toArray(), 0, args, 2, criteria.size());

            // Exécution asynchrone de la recherche
            setLoadingState(true);
            SwingWorker<List<Path>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<Path> doInBackground() {
                    // Exécution de la recherche et traitement des résultats
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
                    // Mise à jour de l'interface avec les résultats
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

        // Ajout des composants au panneau
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

        // Configuration finale et affichage
        createCloseButton(dialog);
        dialog.add(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Crée et configure un label stylisé avec le texte spécifié.
     * Cette fonction applique le style visuel cohérent avec le thème sombre de l'application.
     * 
     * @param text Le texte à afficher dans le label
     * @return JLabel Un label stylisé avec le texte fourni
     *
     * @see javax.swing.JLabel pour créer et configurer le composant d'étiquette dans Swing.
     * @see java.awt.Color pour spécifier la couleur du texte de l'étiquette.
     * @see #createStyledTextField(String) pour créer d'autres éléments d'interface utilisateur avec un style visuel cohérent.
     */
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);  // Configuration de la couleur du texte en blanc
        return label;
    }

    /**
     * Classe interne représentant un panneau d'image personnalisé.
     * Gère l'affichage des miniatures d'images, leur sélection, et les interactions utilisateur.
     * Implémente le chargement asynchrone des miniatures pour une meilleure performance.
     */
    private class ImagePanel extends JPanel {
        /**
         * Chemin vers l'image
         */
        private final Path imagePath;  
        /**
         * Miniature de l'image
         */
        private Image thumbnail;       
        /**
         * État de sélection
         */
        private boolean isSelected = false;  
        /**
          * Taille fixe des miniatures
          */
        private final int THUMBNAIL_SIZE = 200;  

        /**
         * Constructeur du panneau d'image
         * @param path Chemin vers l'image à afficher
         *
         * @see java.nio.file.Path pour représenter le chemin absolu de l'image à afficher.
         * @see javax.swing.JPanel#setPreferredSize(java.awt.Dimension) pour définir la taille fixe du panneau.
         * @see javax.swing.JPanel#setBackground(java.awt.Color) pour définir la couleur de fond du panneau.
         * @see javax.swing.BorderFactory#createLineBorder(java.awt.Color) pour appliquer une bordure personnalisée au panneau.
         * @see #DARK_COMPONENT Couleur de base pour le thème sombre
         * @see #DARK_HOVER Couleur utilisée lors du survol des boutons
         * @see #imagePath pour stocker le chemin de l'image associée à ce panneau.
         * @see #THUMBNAIL_SIZE pour définir la taille fixe des panneaux d'images.
         * @see #loadThumbnail() pour lancer le chargement asynchrone de la miniature.
         * @see #setupMouseListeners() pour configurer les interactions utilisateur liées à la souris.
         */
        public ImagePanel(Path path) {
            this.imagePath = path;
            setPreferredSize(new Dimension(THUMBNAIL_SIZE, THUMBNAIL_SIZE));  // Taille fixe du panneau
            setBackground(DARK_COMPONENT);  // Couleur de fond
            setBorder(BorderFactory.createLineBorder(DARK_HOVER));  // Bordure par défaut
            loadThumbnail();  // Chargement asynchrone de la miniature
            setupMouseListeners();  // Configuration des événements souris
        }

        /**
         * Configure les écouteurs d'événements souris pour la sélection
         * et l'affichage des métadonnées
         *
         * @see java.awt.event.MouseAdapter pour implémenter les événements liés à la souris.
         * @see java.awt.event.MouseEvent#getClickCount() pour détecter le nombre de clics sur le panneau.
         * @see #toggleSelection() pour basculer l'état de sélection lorsque l'utilisateur clique sur le panneau.
         * @see #showImageMetadata(java.nio.file.Path) pour afficher les métadonnées de l'image lors d'un double clic.
         * @see #imagePath pour identifier l'image associée lorsque l'utilisateur interagit.
         */
        private void setupMouseListeners() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        toggleSelection();  // Simple clic pour sélectionner
                    } else if (e.getClickCount() == 2) {
                        showImageMetadata(imagePath);  // Double clic pour les métadonnées
                    }
                }
            });
        }

        /**
         * Bascule l'état de sélection de l'image et met à jour l'interface
         *
         * @see java.util.List#add(Object) pour ajouter le chemin de l'image à la liste des images sélectionnées.
         * @see java.util.List#remove(Object) pour retirer le chemin de l'image de la liste des images sélectionnées.
         * @see javax.swing.BorderFactory#createLineBorder(java.awt.Color, int) pour changer la bordure en fonction de l'état de sélection.
         * @see #updateSelectionButtons() pour actualiser l'état des boutons en fonction de la sélection.
         * @see javax.swing.JComponent#repaint() pour rafraîchir l'affichage du panneau après une modification de l'état.
         * @see #isSelected pour stocker l'état de sélection de l'image.
         * @see #selectedImages pour ajouter ou supprimer le chemin de l'image dans la liste des images sélectionnées.
         * @see #imagePath pour obtenir le chemin actuel de l'image.
         * @see #DARK_HOVER pour rétablir la bordure par défaut si l'image est désélectionnée.
         * @see java.awt.Color#GREEN pour indiquer visuellement la sélection.
         */
        private void toggleSelection() {
            isSelected = !isSelected;
            if (isSelected) {
                selectedImages.add(imagePath);  // Ajout à la sélection
                setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));  // Bordure verte pour indiquer la sélection
            } else {
                selectedImages.remove(imagePath);  // Retrait de la sélection
                setBorder(BorderFactory.createLineBorder(DARK_HOVER));  // Retour à la bordure par défaut
            }
            updateSelectionButtons();  // Mise à jour des boutons de sélection
            repaint();  // Rafraîchissement de l'affichage
        }

        /**
         * Lance le chargement asynchrone de la miniature de l'image
         *
         * @see javax.swing.SwingWorker pour exécuter des opérations longues (comme le chargement d'image) de manière asynchrone.
         * @see javax.imageio.ImageIO#read(java.io.File) pour lire l'image à partir d'un fichier.
         * @see java.awt.Image pour représenter la miniature une fois redimensionnée.
         * @see #createThumbnail(java.awt.Image) pour convertir l'image originale en une miniature adaptée à l'affichage.
         * @see javax.swing.SwingWorker#get() pour récupérer la miniature chargée une fois l'exécution terminée.
         * @see javax.swing.JComponent#repaint() pour redessiner le panneau après le chargement de la miniature.
         * @see #showError(String) pour afficher un message d'erreur si le chargement échoue.
         * @see #imagePath pour lire l'image depuis son chemin.
         * @see #thumbnail pour stocker la miniature chargée.
         * @see #THUMBNAIL_SIZE pour redimensionner l'image originale.
         */
        private void loadThumbnail() {
            SwingWorker<Image, Void> worker = new SwingWorker<>() {
                @Override
                /**
                 * Cette méthode effectue une tâche en arrière-plan pour créer une miniature d'image.
                 * Elle lit l'image originale à partir du chemin spécifié et la traite.
                 * 
                 * @throws exception not thrown: java.lang.Exception
                 */
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
                        showError("Erreur lors du chargement de la miniature : " + e.getMessage());
                    }
                }
            };
            worker.execute();
        }

        /**
         * Crée une miniature de l'image originale en conservant ses proportions.
         * La taille finale est calculée pour s'adapter à THUMBNAIL_SIZE tout en
         * préservant le ratio d'aspect.
         * 
         * @param original L'image originale à redimensionner
         * @return Image La miniature créée
         *
         * @see java.awt.Image#getWidth(java.awt.image.ImageObserver) pour obtenir la largeur de l'image originale.
         * @see java.awt.Image#getHeight(java.awt.image.ImageObserver) pour obtenir la hauteur de l'image originale.
         * @see java.awt.Image#SCALE_SMOOTH pour assurer un redimensionnement fluide et de haute qualité.
         * @see java.awt.Image#getScaledInstance(int, int, int) pour redimensionner l'image originale en miniature.
         * @see java.lang.Math#min(double, double) pour calculer le facteur d'échelle permettant de maintenir les proportions.
         * @see #THUMBNAIL_SIZE pour limiter la taille de la miniature.
         */
        private Image createThumbnail(Image original) {
            if (original == null){
                return null;
            } 

            // Calcul des dimensions originales
            int originalWidth = original.getWidth(null);
            int originalHeight = original.getHeight(null);

            // Calcul du ratio de redimensionnement pour maintenir les proportions
            double scale = Math.min(
                    (double) THUMBNAIL_SIZE / originalWidth,
                    (double) THUMBNAIL_SIZE / originalHeight
                    );

            // Calcul des nouvelles dimensions
            int thumbnailWidth = (int) (originalWidth * scale);
            int thumbnailHeight = (int) (originalHeight * scale);

            // Création de la miniature redimensionnée
            return original.getScaledInstance(thumbnailWidth, thumbnailHeight, Image.SCALE_SMOOTH);
        }

        /**
         * Surcharge de la méthode de dessin du composant.
         * Gère l'affichage de la miniature et du nom du fichier.
         *
         * @see java.awt.Graphics#drawImage(java.awt.Image, int, int, java.awt.image.ImageObserver) pour dessiner la miniature centrée dans le panneau.
         * @see java.awt.Graphics#drawString(String, int, int) pour dessiner le nom du fichier sous l'image.
         * @see java.awt.Font pour définir la police utilisée pour afficher le nom de fichier.
         * @see java.awt.FontMetrics pour mesurer la largeur et la hauteur du texte, afin de le centrer correctement.
         * @see java.awt.Color pour définir la couleur du texte et créer un fond semi-transparent.
         * @see java.nio.file.Path#getFileName() pour extraire le nom du fichier à partir du chemin de l'image.
         * @see javax.swing.JComponent#repaint() pour redessiner la miniature et le texte lors de la mise à jour du panneau.
         * @see #thumbnail pour dessiner l'image miniature si elle est disponible.
         * @see #imagePath pour obtenir le nom du fichier affiché sous la miniature.
         * @see #DARK_TEXT pour configurer la couleur du texte.
         * @see #THUMBNAIL_SIZE pour ajuster le positionnement et l'échelle du contenu.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (thumbnail != null) {
                // Calcul de la position centrée pour la miniature
                int x = (getWidth() - thumbnail.getWidth(null)) / 2;
                int y = (getHeight() - thumbnail.getHeight(null)) / 2;
                g.drawImage(thumbnail, x, y, null);

                // Configuration pour l'affichage du nom de fichier
                g.setColor(DARK_TEXT);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                String fileName = imagePath.getFileName().toString();

                // Troncature du nom si trop long
                if (fileName.length() > 20) {
                    fileName = fileName.substring(0, 17) + "...";
                }

                // Calcul de la position du texte
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(fileName);
                int textX = (getWidth() - textWidth) / 2;
                int textY = getHeight() - 10;

                // Création d'un fond semi-transparent pour la lisibilité du texte
                g.setColor(new Color(0, 0, 0, 128));
                g.fillRect(0, getHeight() - 20, getWidth(), 20);

                // Affichage du nom de fichier
                g.setColor(DARK_TEXT);
                g.drawString(fileName, textX, textY);
            } else {
                // Affichage d'un message de chargement si la miniature n'est pas prête
                g.setColor(DARK_TEXT);
                g.drawString("Chargement...", 10, getHeight() / 2);
            }
        }
    }

    /**
     * Met à jour la grille d'images avec une nouvelle liste de chemins.
     * Gère le chargement asynchrone des images et la mise à jour de l'interface.
     * 
     * @param SearchResults Liste optionnelle de résultats de recherche
     * @see #imageGrid pour réinitialiser et ajouter les nouveaux panneaux d'images.
     * @see #imagePanels pour stocker les panneaux d'images associés à leurs chemins.
     * @see #selectedImages pour effacer et gérer les images sélectionnées après la mise à jour.
     * @see #currentPath pour obtenir le chemin actuel utilisé pour rechercher les fichiers.
     * @see #setLoadingState(boolean) pour indiquer l'état de chargement à l'utilisateur.
     * @see #executeCliCommand(String...) pour exécuter une commande CLI et récupérer la liste des fichiers.
     * @see ImagePanel pour créer des panneaux d'images à partir des chemins récupérés.
     * @see #updateSelectionButtons() pour mettre à jour les boutons de sélection après la réinitialisation.
     * @see #showError(String) pour afficher une erreur en cas de problème lors du chargement des images.
     */
    private void updateImageGrid(List<Path> SearchResults) {
        // Réinitialisation de la grille et des sélections
        imageGrid.removeAll();
        imagePanels.clear();
        selectedImages.clear();
        updateSelectionButtons();

        setLoadingState(true);
        SwingWorker<List<Path>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Path> doInBackground() {
                Path path = Paths.get(currentPath.toString());
                if (Files.isDirectory(path)) {
                    // Récupération de la liste des fichiers pour un répertoire
                    String output = executeCliCommand("-d", currentPath.toString(), "--list");
                    return output.lines()
                        .filter(line -> !line.startsWith("Liste des fichiers image :"))
                        .map(line -> Paths.get(line.trim()))
                        .filter(Files::exists)
                        .collect(Collectors.toList());
                } else {
                    // Cas d'une seule image
                    List<Path> output = new ArrayList<>();
                    output.add(path);
                    return output;
                }
            }

            @Override
            protected void done() {
                try {
                    // Utilisation des résultats de recherche s'ils existent
                    List<Path> paths = get();
                    if (SearchResults != null){
                        paths = SearchResults;
                    }
                    // Création des panneaux d'images
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
                    showError("Erreur lors du chargement des images : " + e.getMessage());
                    setLoadingState(false);
                }
            }
        };

        worker.execute();
    }

    /**
     * Crée et configure la barre de statut de l'application.
     * Contient un label de statut et un indicateur de chargement.
     * 
     * @return JPanel Le panneau de la barre de statut configuré
     *
     * @see #statusLabel pour afficher les messages de statut dans la barre.
     * @see #loadingIndicator pour indiquer visuellement l'état de chargement.
     * @see java.awt.BorderLayout pour organiser les composants dans la barre de statut.
     * @see javax.swing.JPanel pour créer un panneau contenant les éléments de la barre de statut.
     * @see javax.swing.JLabel pour configurer et afficher le label de statut.
     * @see javax.swing.JProgressBar pour configurer l'indicateur de progression.
     * @see javax.swing.BorderFactory#createEmptyBorder(int, int, int, int) pour ajouter des marges autour du panneau.
     * @see java.awt.Color pour configurer la couleur de fond et le texte.
     * @see #DARK_COMPONENT Couleur de base pour le thème sombre
     * @see #DARK_TEXT Couleur utilisée pour le texte dans le thème sombre
     */
    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DARK_COMPONENT);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Configuration du label de statut
        statusLabel = new JLabel("Prêt");
        statusLabel.setForeground(DARK_TEXT);
        panel.add(statusLabel, BorderLayout.WEST);

        // Configuration de l'indicateur de chargement
        loadingIndicator = new JProgressBar();
        loadingIndicator.setIndeterminate(true);
        loadingIndicator.setVisible(false);
        panel.add(loadingIndicator, BorderLayout.EAST);

        return panel;
    }

    /**
     * Applique le thème sombre à l'ensemble de l'interface utilisateur.
     * Configure les couleurs et styles par défaut pour tous les composants Swing.
     *
     * @see #DARK_TEXT pour définir la couleur du texte dans plusieurs composants.
     * @see #DARK_COMPONENT pour configurer le fond des champs et des boutons.
     * @see #DARK_BACKGROUND pour personnaliser le fond des panneaux défilants.
     * @see javax.swing.UIManager#put(Object, Object) pour appliquer des styles globaux aux composants Swing.
     * @see javax.swing.BorderFactory#createEmptyBorder() pour supprimer les bordures des composants.
     */
    private void applyDarkTheme() {
        // Configuration des couleurs et styles pour différents composants
        UIManager.put("TextField.foreground", DARK_TEXT);  // Couleur du texte des champs
        UIManager.put("TextArea.background", DARK_COMPONENT);  // Fond des zones de texte
        UIManager.put("TextArea.foreground", DARK_TEXT);  // Couleur du texte des zones de texte
        UIManager.put("ScrollPane.background", DARK_BACKGROUND);  // Fond des panneaux défilants
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());  // Suppression des bordures
        UIManager.put("OptionPane.buttonBackground", DARK_COMPONENT);  // Fond des boutons de dialogue
        UIManager.put("OptionPane.buttonForeground", DARK_TEXT);  // Couleur du texte des boutons de dialogue
    }

    /**
     * Affiche une boîte de dialogue d'information avec le message spécifié.
     * 
     * @param message Le message à afficher à l'utilisateur
     *
     * @see javax.swing.JOptionPane#showMessageDialog(Component, Object, String, int) pour afficher des dialogues d'information.
     * @see JOptionPane#INFORMATION_MESSAGE pour le type de message affiché.
     */
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Affiche une boîte de dialogue d'erreur avec le message spécifié.
     * 
     * @param message Le message d'erreur à afficher à l'utilisateur
     *
     * @see javax.swing.JOptionPane#showMessageDialog(Component, Object, String, int) pour afficher des dialogues d'erreur.
     * @see JOptionPane#ERROR_MESSAGE pour le type de message affiché.
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Exécute une commande CLI avec les arguments spécifiés.
     * Capture la sortie standard et la retourne sous forme de chaîne.
     * 
     * @param args Les arguments de la commande à exécuter
     * @return String La sortie de la commande
     *
     * @see java.io.ByteArrayOutputStream pour capturer la sortie standard en mémoire.
     * @see java.io.PrintStream pour rediriger et gérer la sortie standard.
     * @see java.lang.System#setOut(PrintStream) pour rediriger temporairement la sortie standard.
     * @see java.lang.System#setOut(PrintStream) pour restaurer la sortie standard d'origine.
     * @see ConsoleInterface#start(String[]) pour exécuter la commande CLI avec les arguments fournis.
     */
    private String executeCliCommand(String... args) {
        // Préparation de la capture de la sortie standard
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        PrintStream originalOut = System.out;
        System.setOut(printStream);

        try {
            // Exécution de la commande et récupération de la sortie
            ConsoleInterface.start(args);
            return outputStream.toString();
        } finally {
            // Restauration de la sortie standard
            System.setOut(originalOut);
        }
    }

    /**
     * Point d'entrée principal pour l'interface graphique.
     * Configure le Look et Feel du système et lance l'application de manière sécurisée
     * dans l'EDT (Event Dispatch Thread).
     *
     * @see javax.swing.SwingUtilities#invokeLater(Runnable) pour exécuter l'initialisation de l'interface graphique dans l'Event Dispatch Thread (EDT).
     * @see javax.swing.UIManager#setLookAndFeel(String) pour configurer le Look et Feel natif du système.
     * @see javax.swing.UIManager#getSystemLookAndFeelClassName() pour obtenir le nom de la classe du Look et Feel natif du système.
     * @see GraphicalInterface#GraphicalInterface() pour initialiser l'interface principale de l'application.
     * @see javax.swing.JFrame#setVisible(boolean) pour rendre la fenêtre principale visible.
     */
    public static void start() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Configuration du Look et Feel natif du système
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Création et affichage de l'interface principale
            new GraphicalInterface().setVisible(true);
        });
    }
}