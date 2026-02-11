# BusPullman

App Android privata per sapere in tempo reale quando passano i pullman GTT a Torino.

## Funzionalita

- **Orari in tempo reale** per le linee 15, 68 e 61 dalle fermate GTT
- **Input vocale** — chiedi "quando passa il 15?" e l'app risponde parlando
- **Text-to-Speech** — legge gli orari in italiano
- **Widget** compatto (2x1) per la home screen con refresh manuale
- **Aggiornamento automatico** — rileva nuove versioni da GitHub Releases
- **Dark mode** — supporto completo tema chiaro/scuro

## Screenshot

L'app mostra le prossime corse con:
- Cerchio colorato per ogni linea (15 blu, 68 rosso, 61 verde)
- Minuti mancanti in grande
- Badge GPS (dato reale) o Orario (programmato)
- Bottone microfono per input vocale

## Requisiti

- Android 8.0+ (API 26)
- Connessione internet per i dati in tempo reale

## Installazione

Scarica l'APK dall'ultima [release](https://github.com/edoraba/bus-checker/releases/latest) e installalo sul tuo dispositivo Android.

## API

L'app usa l'API gratuita [GPA MadBob](https://gpa.madbob.org/) per i dati GTT:

| Fermata | URL | Linee |
|---------|-----|-------|
| 578 | `https://gpa.madbob.org/query.php?stop=578` | 15, 68 |
| 1805 | `https://gpa.madbob.org/query.php?stop=1805` | 61 |

## Rilascio nuova versione

Vedi [RELEASE.md](RELEASE.md) per la guida completa.

Rilascio rapido da Git Bash:
```bash
bash release.sh 1.0.3
```

## Build da sorgente

```bash
# Debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (minificato)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

## Stack tecnico

- Kotlin + Jetpack Compose + Material 3
- Retrofit + Gson per le API
- Glance per il widget
- WorkManager per il refresh periodico
- SpeechRecognizer + TextToSpeech nativi Android

## Licenza

Uso privato.
