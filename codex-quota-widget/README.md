# Codex Quota Widget

Widget Android natif 4×1 pour afficher le dernier quota Codex synchronisé.

## Fonctionnement

- L’application ouvre une session ChatGPT/Codex locale dans une WebView sécurisée.
- La connexion ChatGPT est effectuée par l’utilisateur dans cette WebView, sans clé API.
- L’application lit uniquement le texte visible du tableau d’usage et conserve les valeurs localement.
- Le widget affiche deux cartes 5 h/semaine et une jauge lorsqu’un pourcentage est disponible.
- Le widget ne lance aucune requête en arrière-plan : il affiche le dernier résultat et ouvre l’écran de synchronisation au toucher.
- Si l’interface du tableau change, la saisie manuelle reste disponible.

Le protocole Codex app-server documente bien une lecture des rate limits avec l’auth ChatGPT, mais son intégration native Android nécessite d’embarquer ou d’atteindre un app-server Codex. La version légère actuelle garde donc la session locale WebView comme repli compatible, sans dépendre d’un endpoint privé non documenté.

## Construction

Ouvrir le dossier dans Android Studio avec Android Gradle Plugin 8.7.3 et compiler la variante `release`. Le projet n’utilise aucune dépendance Android externe.

## Installation sur le Samsung S24 Ultra

Installer l’APK, ouvrir `Codex Quota`, appuyer sur `Synchroniser depuis Codex`, se connecter si nécessaire, puis ajouter le widget `Codex Quota` en taille 4×1 depuis l’écran d’accueil.
