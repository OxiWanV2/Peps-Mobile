# Pep's Mobile

Application Android pour consulter son compte PepSchool (sjbsm.pepschool.be). Écrite en Kotlin avec Jetpack Compose et Material 3.

## Ce que fait l'app

- Connexion avec l'email et le mot de passe du compte école. Ils restent sur le téléphone, chiffrés, et servent à se reconnecter tout seul quand la session expire.
- Accueil avec des widgets que l'on active, désactive et réordonne : aujourd'hui, évaluations de la semaine, évaluations à venir, devoirs, résumé des résultats, actualités, raccourcis.
- Journal de classe par semaine.
- Mes cours, avec les évaluations et les statistiques de chaque cours.
- Résultats avec les notes colorées : rouge sous 50 %, orange jusqu'à 64 %, vert au-dessus.
- Notifications de l'école (lire, supprimer).
- Un widget pour l'écran d'accueil du téléphone qui affiche les cours restants du jour et la prochaine évaluation.
- Réglages avec nom, prénom et photo du compte.

## Compiler

Il faut un JDK 17 et le SDK Android (plateforme 36). `local.properties` doit contenir le chemin du SDK.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Les APK sortent dans `app/build/outputs/apk/`. Pour juger la fluidité, installez la version release : la debug est nettement plus lente avec Compose.

Sans fichier `keystore.properties` à la racine, la release est signée avec la clé de debug, ce qui ne convient que pour des tests. Pour une vraie signature :

```properties
storeFile=chemin/vers/cle.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## Sécurité

- Identifiants et cookies chiffrés en AES-256-GCM avec une clé de l'Android Keystore.
- Pas de sauvegarde cloud ni de transfert vers un autre appareil.
- Uniquement du HTTPS vers sjbsm.pepschool.be, certificats système seulement.
- Verrouillage par biométrie ou code de l'appareil, et blocage des captures d'écran, tous deux désactivables dans les réglages.
- Le navigateur intégré ne peut ouvrir que le site de l'école et efface ses cookies en se fermant.

## Organisation du code

Le code est dans `app/src/main/java/be/lorenzogallo/pepsmobile` :

- `data` : appels réseau (`network`), lecture des pages (`parser`), chiffrement (`security`) et accès unique aux données (`repository`)
- `ui` : un dossier par écran
- `widget` : le widget d'écran d'accueil
- `di` : création des objets partagés

Les pages de résultats, de cours et de statistiques passent toutes par le même parseur, `PanelParser`.

L'adresse du site est écrite en dur dans `PepsApi.HOST`.

---


Développé par Lorenzo Gallo, © PepSchool - Tous droits réservés, projet non officiel, open-source.