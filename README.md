# Fond d'écran - Diaporama & Musique

Application Android native complète (Kotlin) et application web interactive (React + TypeScript).

## Architecture Android (Kotlin)

1. **Architecture Multi-Écrans (`MainActivity.kt`)** :
   - **Page Accueil (`HOME`)** :
     - Liste de vos diaporamas et images uniques sauvegardés en persistance JSON locale.
     - Bouton « + Nouveau Diaporama / Fond » avec dialogue de choix du mode (Diaporama ou Image Unique).
     - Raccourcis directs vers les Sons Système, Paramètres, et Recherche Pinterest.
   - **Page Édition du Diaporama / Image Unique (`PROJECT_EDIT`)** :
     - Liste ordonnée des images avec miniatures.
     - **Durée par image (individuelle)** : sélecteur de temps par diapositive (5s, 10s, 15s, 30s, 1 min, 5 min, 15 min, 1 h).
     - **Son individuel** : association et découpe audio par image.
     - Boutons d'action pour modifier, déplacer, recadrer et supprimer.
     - Paramètres de cible : Écran d'accueil, Écran de verrouillage ou les deux.
     - Lancement du diaporama périodique (`WorkManager`) et application immédiate.
   - **Page Éditeur d'Image (`IMAGE_EDITOR`)** :
     - Ajustements en temps réel via `ColorMatrix` : Luminosité, Contraste, Saturation.
     - Filtres prédéfinis 1-clic : Normal, Vif, N&B, Rétro.
     - Possibilité d'appliquer l'image comme fond d'écran interne de l'application elle-même.
   - **Page Éditeur de Son (`AUDIO_EDITOR`)** :
     - Importation de fichiers audio (`audio/*`).
     - Découpage précis du point de début (`audioStartMs`) et réglage du volume sonore.
     - Pré-écoute instantanée en boucle avec `MediaPlayer`.
   - **Page Sons Système (`SYSTEM_SOUNDS`)** :
     - Écoute et prévisualisation des sonneries d'appel, notifications et alertes d'alarme système.
   - **Page Paramètres (`SETTINGS`)** :
     - Gestion du fond d'écran de l'application (activer/réinitialiser).
     - Protection sécurisée au démarrage (Code PIN / Empreinte digitale `KeyguardManager`).

2. **Fond d'Écran Animé Android (`LiveWallpaperService.kt`)** :
   - `SlideEngine` sur `SurfaceHolder` avec rendu canvas filtré.
   - Respect scrupuleux de la durée individuelle de chaque image (`slide.durationSec * 1000L`).
   - Lecture sonore synchronisée et découpée en boucle (`MediaPlayer.seekTo(slide.audioStartMs)`).
   - Gestion des pauses et reprises en arrière-plan (`onVisibilityChanged`).

3. **Tâches d'Arrière-Plan (`SlideWorker.kt`)** :
   - `PeriodicWorkRequest` géré par `WorkManager` pour alterner les fonds d'écran selon l'intervalle configuré.
