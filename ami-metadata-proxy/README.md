# AMI Metadata Proxy v2

Android app voor oudere Audi AMI/Bluetooth setups die liever `titel + artiest` tonen dan `titel + album`.

## Wat is nieuw in v2?

- **Spotify-only modus** om andere mediasessies te negeren.
- **Debuglog in de app** zodat je kunt zien welke metadata de proxy publiceert.
- **Agressievere metadata-invulling**:
  - `ALBUM = ARTIST`
  - `ALBUM_ARTIST = ARTIST`
  - `DISPLAY_SUBTITLE = ARTIST`
  - `DISPLAY_DESCRIPTION = ARTIST`
- **Laatste track/sessie zichtbaar** op het hoofdscherm.
- Handige snelknoppen naar **Notification access**, **Bluetooth settings** en **Developer options**.

## Verwachtingsmanagement

Dit is een workaround. Het hangt van jouw telefoon, Android-versie, AVRCP-gedrag en Audi AMI/Bluetooth module af of de auto de proxy-metadata kiest in plaats van de originele Spotify-metadata.

## Aanrader voor testen

1. Laat alleen Spotify actief zijn.
2. Zet in de app **Alleen Spotify als bron gebruiken** aan.
3. Test in Android **Developer options** met AVRCP 1.4, 1.5 en 1.6.
4. Kijk in de debuglog of de app de juiste track/artiest ziet.

## Cloud build via GitHub Actions

Na upload naar GitHub kun je in **Actions** de workflow **Build APK** draaien. Daarna staat de debug APK onder **Artifacts**.
