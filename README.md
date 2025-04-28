# Music Player Android App

Music Player est une application Android moderne permettant aux utilisateurs de profiter pleinement de leur musique avec une interface fluide et intuitive.
Elle propose trois activités principales :

	-Home : Liste de tous les titres musicaux disponibles.

	-Library : Regroupe toutes les playlists créées par l'utilisateur.

	-Discover : Permet de découvrir l'historique d'écoute, les morceaux recommandés, les titres les plus tendances, 	 ainsi que de se connecter et de s'authentifier à un compte.

## 📱 Fonctionnalités principales

### 🏠 Accueil (MainActivity)
- Liste complète des morceaux locaux
- Barre de recherche intelligente avec suggestions
- Accès rapide aux playlists
- Mini-player interactif
- Transitions fluides entre les écrans

### 📚 Bibliothèque (LibraryActivity)
- Gestion complète des playlists (création/suppression)
- Système de recherche avancé :
  - Suggestions en temps réel
  - Filtrage par titres et playlists
- Affichage des playlists personnalisées
- Intégration avec les favoris (❤️)

### 🔍 Découverte (DecouvrirActivity)
- Historique d'écoute détaillé
- Section "Tendances du moment"
- Recommandations personnalisées
- [En développement] Intégration de compte utilisateur :
  - Authentification
  - Synchronisation cloud
  - Partage social


## 🛠 Technologies utilisées
- Language : Java
- Architecture : Singleton (MusicPlayer), Adapters RecyclerView
- Multimédia : MediaPlayer, MediaMetadataRetriever
- Bibliothèques :
  - Glide (chargement d'images)
  - Room (gestion de base de données locale)
  - Gestion avancée des permissions
- Services :
  - Foreground Service pour la lecture continue
  - Intent Services pour les partages
- UI/UX :
  - Animations matérielles
  - Transitions partagées
  - Design responsive
