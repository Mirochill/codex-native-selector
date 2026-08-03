# Codex Quota Widget

Widget Android natif 4×1 pour afficher le dernier quota Codex synchronisé.

## Fonctionnement

- L’application ouvre `https://chatgpt.com/codex/settings/usage` dans une WebView locale.
- La connexion ChatGPT est effectuée par l’utilisateur dans cette WebView, sans clé API.
- L’application lit uniquement le texte visible du tableau d’usage et conserve les valeurs localement.
- Le widget ne lance aucune requête en arrière-plan : il affiche le dernier résultat et ouvre l’écran de synchronisation au toucher.
- Si l’interface du tableau change, la saisie manuelle reste disponible.

Le tableau d’usage étant une page privée et aucune API publique de quota personnel n’étant documentée, l’extraction est volontairement limitée au texte visible et peut nécessiter une adaptation future.

## Construction

Ouvrir le dossier dans Android Studio avec Android Gradle Plugin 8.7.3 et compiler la variante `release`. Le projet n’utilise aucune dépendance Android externe.

## Installation sur le Samsung S24 Ultra

Installer l’APK, ouvrir `Codex Quota`, appuyer sur `Synchroniser depuis Codex`, se connecter si nécessaire, puis ajouter le widget `Codex Quota` en taille 4×1 depuis l’écran d’accueil.
