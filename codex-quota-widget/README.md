# Codex Quota Widget

Widget Android natif 4×1 pour afficher le dernier quota Codex synchronisé.

## Fonctionnement

- L’app utilise le flux officiel de connexion ChatGPT/Codex par code appareil : le navigateur système s’ouvre sur `auth.openai.com`, puis l’app récupère la session OAuth.
- Les jetons OAuth sont chiffrés dans Android Keystore. Aucun mot de passe, clé API ou jeton n’est affiché ni sauvegardé en clair.
- Le code appareil est conservé chiffré pendant 15 minutes et le polling reprend après une coupure réseau ou la fermeture de l’écran.
- Après connexion, l’app lit les fenêtres Codex 5 h/semaine et convertit `used_percent` en pourcentage restant.
- La fréquence automatique se choisit dans l’app : 15 min, 30 min par défaut, 1 h, 2 h, 4 h, 8 h, 12 h, 24 h ou désactivée. Elle utilise uniquement Internet lorsque la batterie n’est pas faible et aucun service permanent ne tourne.
- Un appui sur le widget demande une synchronisation silencieuse immédiate. L’écran de connexion ne s’ouvre que si l’authentification manque.
- La saisie manuelle reste disponible si le service est momentanément indisponible.

La lecture du quota reprend les routes backend utilisées par Codex (`/backend-api/wham/usage`). Elles peuvent évoluer côté service ; l’app affiche alors le dernier résultat local au lieu de solliciter la batterie en continu.

## Construction

Ouvrir le dossier dans Android Studio avec Android Gradle Plugin 8.7.3 et compiler la variante `release`. Le projet n’utilise aucune dépendance Android externe.

## Installation sur le Samsung S24 Ultra

Installer l’APK, ouvrir `Codex Quota`, appuyer sur `Connecter ChatGPT / Codex`, suivre le code affiché dans le navigateur, puis ajouter le widget `Codex Quota` en taille 4×1 depuis l’écran d’accueil.
