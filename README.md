# 🚌 BusPullman

App Android privata per sapere in tempo reale quando passano i pullman GTT a Torino.

## Funzionalità

- **Orari in tempo reale** per le linee 15, 68 e 61
- **Chiedi a voce** "quando passa il 15?" e l'app ti risponde parlando
- **Widget** sulla home screen per vedere i prossimi bus con un colpo d'occhio
- **Aggiornamento automatico** tramite GitHub Releases

## Quick Start (da zero)

### 1. Apri il progetto in Android Studio
- Estrai lo zip
- Android Studio → File → Open → seleziona la cartella `BusPullman`
- Aspetta che Gradle finisca di sincronizzare (la prima volta ci mette un po')
- Se ti chiede di installare SDK o componenti mancanti, accetta tutto

### 2. Verifica che compili
- Premi ▶️ per runnare sull'emulatore o su un device fisico
- Dovresti vedere una schermata bianca con "BusPullman - Da implementare" — è normale, il progetto è uno scheletro

### 3. Crea il repo GitHub
```bash
cd /path/to/BusPullman
git init
git add .
git commit -m "Initial project structure"
git remote add origin git@github.com:TUO_USER/BusPullman.git
git push -u origin main
```

### 4. Apri Claude Code e sviluppa
```bash
cd /path/to/BusPullman
claude
```
Claude Code legge automaticamente `CLAUDE.md` con tutte le specifiche.
Per partire digli:
> "Implementa la schermata principale seguendo CLAUDE.md, parti dal punto 1"

Poi prosegui con:
> "Ora implementa il Text-to-Speech (punto 2)"
> "Ora aggiungi lo Speech-to-Text (punto 3)"
> "Implementa il widget (punto 4)"
> "Aggiungi l'auto-update da GitHub (punto 5)"

### 5. Testa e rilascia
- Dopo ogni step, premi ▶️ su Android Studio per testare
- Quando sei soddisfatto, builda la release e crea una GitHub Release:
```bash
./gradlew assembleRelease
```
- Vai su GitHub → Releases → Create new release → allega l'APK

---

## Setup dettagliato

### Prerequisiti
- Android Studio Hedgehog (2023.1.1) o successivo
- Android SDK 34
- Un dispositivo Android o emulatore (min SDK 26 / Android 8.0)

### Primi passi

1. **Apri il progetto** in Android Studio:
   - File → Open → seleziona la cartella `BusPullman`
   - Aspetta che Gradle sincronizzi (può volerci qualche minuto la prima volta)

2. **Crea un dispositivo virtuale** (se non ne hai uno):
   - Tools → Device Manager → Create Virtual Device
   - Scegli un Pixel 6 o simile con API 34

3. **Run** l'app:
   - Premi il bottone ▶️ verde
   - L'app si installa e apre sull'emulatore/device

### Sviluppo con Claude Code

```bash
cd /path/to/BusPullman
claude
```

Claude Code leggerà automaticamente `CLAUDE.md` per capire il progetto.
Tutte le istruzioni per lo sviluppo sono in quel file.

### Build manuale

```bash
# Debug APK
./gradlew assembleDebug

# L'APK si trova in:
# app/build/outputs/apk/debug/app-debug.apk
```

## API

L'app usa l'API gratuita [GPA MadBob](https://gpa.madbob.org/) per i dati GTT:

| Fermata | URL | Linee monitorate |
|---------|-----|-----------------|
| 578 | `https://gpa.madbob.org/query.php?stop=578` | 15, 68 |
| 1805 | `https://gpa.madbob.org/query.php?stop=1805` | 61 |

## Auto-Update

L'app controlla all'avvio se esiste una nuova release su GitHub.
Per rilasciare un aggiornamento:

1. Aggiorna `versionCode` e `versionName` in `app/build.gradle.kts`
2. Build: `./gradlew assembleRelease`
3. Crea una nuova Release su GitHub con tag (es. `v1.1.0`)
4. Allega l'APK alla release
5. L'app lo troverà automaticamente al prossimo avvio

## Struttura Progetto

```
BusPullman/
├── CLAUDE.md                    # Istruzioni per Claude Code
├── README.md                    # Questo file
├── app/
│   ├── build.gradle.kts         # Dipendenze e config
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/redergo/buspullman/
│           ├── MainActivity.kt
│           ├── data/            # Modelli e API
│           ├── ui/              # Schermate Compose
│           ├── widget/          # Widget home screen
│           └── service/         # TTS e STT
├── build.gradle.kts             # Config root
└── settings.gradle.kts
```

## Licenza

Uso privato — non distribuire.
