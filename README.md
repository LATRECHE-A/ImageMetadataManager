# Gestionnaire de Métadonnées d'Images

## Aperçu du Projet

Gestionnaire de Métadonnées d'Images est une application Java conçue pour aider les utilisateurs à gérer et à analyser efficacement les fichiers image. Le projet propose une Interface en Ligne de Commande (CLI) et une Interface Graphique Utilisateur (GUI) prévue.

## Fonctionnalités

### Interface en Ligne de Commande (CLI)
- Lister les fichiers image dans un répertoire
- Générer des statistiques de répertoire
- Extraire des métadonnées détaillées d'images
- Créer et comparer des instantanés de répertoire
- Recherche avancée d'images avec plusieurs critères

### Opérations Supportées

#### Mode Répertoire
- Lister tous les fichiers image
- Générer des statistiques détaillées de répertoire
- Comparer l'état actuel du répertoire avec un instantané sauvegardé

#### Mode Fichier
- Afficher les statistiques du fichier
- Extraire les métadonnées détaillées de l'image
- Récupérer des informations techniques et descriptives

#### Mode Recherche
Rechercher des images avec des critères flexibles tels que :
- Correspondance partielle du nom
- Année de création
- Dimensions minimales de l'image

## Prérequis

- Java 17 ou supérieur
- Bibliothèques requises (incluses dans le dossier `lib/`) :
  - metadata-extractor-2.18.0.jar
  - commons-imaging-1.0-alpha3.jar
  - xmpcore-6.1.11.jar

## Cloner le Dépôt

### Utilisation de HTTPS
```bash
# Cloner le dépôt
git clone https://github.com/LATRECHE-A/CYU-E06

# Naviguer dans le répertoire du projet
cd CYU-E06 

## Structure du Projet

```
CYU-E06/
│
├── src/                    # Code source
│   └── test/
│       ├── Main.java
│       ├── ConsoleInterface.java
│       ├── DirectoryAnalyzer.java
│       ├── ImageFile.java
│       ├── ImageMetadata.java
│       ├── MetadataExtractor.java
│       └── SnapshotManager.java
│
├── lib/                    # Bibliothèques externes
│   ├── metadata-extractor-2.18.0.jar
│   ├── commons-imaging-1.0-alpha3.jar
│   └── xmpcore-6.1.11.jar
│
├── bin/                    # Classes compilées
└── manifest.txt            # Fichier manifeste JAR
```

## Instructions de Compilation

### Compiler le Projet

```bash
# Compiler les classes Java
javac --release 17 -cp .:lib/metadata-extractor-2.18.0.jar:lib/commons-imaging-1.0-alpha3.jar:lib/xmpcore-6.1.11.jar -d bin src/*.java

# Créer le JAR exécutable
jar --create --file cli.jar --manifest manifest.txt -C bin .
```

## Exemples d'Utilisation

### Listage de Répertoire
```bash
java -jar cli.jar -d /chemin/vers/repertoire/images --list
```

### Statistiques de Répertoire
```bash
java -jar cli.jar -d /chemin/vers/repertoire/images --stat
```

### Métadonnées de Fichier
```bash
java -jar cli.jar -f /chemin/vers/image.jpg --info
```

### Recherche d'Images
```bash
# Rechercher des images par nom
java -jar cli.jar --search /chemin/vers/repertoire name=coucher-soleil

# Rechercher des images par année
java -jar cli.jar --search /chemin/vers/repertoire date=2023

# Rechercher des images par dimensions minimales
java -jar cli.jar --search /chemin/vers/repertoire dimensions=1920x1080

# Rechercher des images par multiples critères
java -jar cli.jar --search /chemin/vers/repertoire dimensions=1920x1080 name=coucher-soleil date=2023
```

### Gestion d'Instantanés
```bash
# Sauvegarder un instantané de répertoire
java -jar cli.jar --snapshotsave /chemin/vers/repertoire/images

# Comparer avec l'instantané précédent
java -jar cli.jar -d /chemin/vers/repertoire/images --compare-snapshot
```

## ToDo 
- Développer une Interface Graphique Utilisateur (GUI)
- Améliorer les capacités d'extraction de métadonnées (car mtn j'affiche trop, il faut suivre le pdf du projet:)

## Licence
[Licence MIT]
