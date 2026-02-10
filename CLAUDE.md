# CLAUDE.md — Istruzioni per Claude Code

## Panoramica Progetto

**BusPullman** è un'app Android privata (non distribuita su Play Store) che mostra in tempo reale quando passano i pullman alle fermate GTT di Torino. L'utente può chiedere a voce "quando passa il 15?" e l'app risponde vocalmente con i tempi di attesa.

## API Dati Bus

L'app usa l'API gratuita di GPA MadBob (no auth, no API key):

- **Fermata 578:** `https://gpa.madbob.org/query.php?stop=578` → Linee monitorate: **15, 68**
- **Fermata 1805:** `https://gpa.madbob.org/query.php?stop=1805` → Linea monitorata: **61**

### Formato risposta API
```json
[
  {"line": "15", "hour": "21:07:53", "realtime": true},
  {"line": "68", "hour": "21:08:20", "realtime": false}
]
```

- `line`: numero linea bus
- `hour`: orario passaggio (HH:mm:ss, timezone locale Italia)
- `realtime`: `true` = dato GPS reale, `false` = orario programmato

### Logica di calcolo
- Confrontare `hour` con l'ora attuale del dispositivo per calcolare i minuti mancanti
- Mostrare solo le linee monitorate (15, 68 dalla fermata 578; 61 dalla fermata 1805)
- Ignorare passaggi già passati (minuti negativi)
- Ordinare per minuti mancanti (prossimo in arrivo prima)

## Architettura App

### Stack
- **Linguaggio:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **HTTP:** Retrofit + Gson
- **Widget:** Glance (Jetpack)
- **Background:** WorkManager
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34

### Package structure
```
com.redergo.buspullman/
├── MainActivity.kt          # Entry point
├── data/
│   ├── BusModels.kt         # Data classes + BusConfig
│   └── BusApiService.kt     # Retrofit interface
├── ui/
│   ├── BusScreen.kt         # Main Compose screen (DA IMPLEMENTARE)
│   └── theme/Theme.kt       # Material 3 theme
├── widget/                   # Home screen widget (DA IMPLEMENTARE)
└── service/                  # TTS + Speech recognition service (DA IMPLEMENTARE)
```

## Funzionalità da Implementare

### 1. Schermata Principale (`ui/BusScreen.kt`)
- Mostrare le prossime corse per le linee 15, 68, 61
- Per ogni corsa mostrare: numero linea, minuti mancanti, indicatore realtime/programmato
- Auto-refresh ogni 30 secondi
- Bottone grande per attivare il microfono (speech-to-text)
- Pull-to-refresh manuale
- Design pulito, leggibile a colpo d'occhio (testo grande per i minuti)

### 2. Voice Input (Speech-to-Text)
- Usare `android.speech.SpeechRecognizer` nativo (GRATIS, no API esterne)
- L'utente dice cose come:
  - "quando passa il 15?" → mostra/legge solo la linea 15
  - "pullman" o "bus" → mostra/legge tutte le linee
  - "61" → mostra/legge solo la linea 61
- Parsing semplice: cercare i numeri "15", "68", "61" nel testo riconosciuto
- Se nessun numero trovato, mostrare tutte le linee

### 3. Voice Output (Text-to-Speech)
- Usare `android.speech.tts.TextToSpeech` nativo (GRATIS)
- Impostare lingua italiana: `Locale.ITALIAN`
- Formato risposta vocale:
  - "Il 15 passa tra 4 minuti" (se realtime)
  - "Il 68 passa tra 12 minuti, da orario" (se NON realtime)
  - "Il 61 è appena passato" (se 0 minuti)
  - "Nessun passaggio previsto per il 15" (se nessun dato)
- Leggere automaticamente dopo il riconoscimento vocale

### 4. Widget Home Screen (`widget/`)
- Widget compatto per la home screen e lock screen (Android 14+)
- Mostra le prossime 3 corse (tutte le linee)
- Tap sul widget → apre l'app
- Bottone "aggiorna" nel widget
- Refresh automatico ogni 15 minuti (limite minimo Android)
- Usare Glance (Jetpack) per il widget

### 5. Auto-Update da GitHub Releases
- All'avvio dell'app, chiamare `https://api.github.com/repos/OWNER/REPO/releases/latest`
- Confrontare `tag_name` con la versione corrente (`BuildConfig.VERSION_NAME`)
- Se c'è una versione più recente:
  - Mostrare un banner/dialog "Aggiornamento disponibile v1.2.0"
  - Scaricare l'APK dall'asset della release
  - Aprire l'installer Android per installare l'APK
- Il repo GitHub va configurato in `BusConfig` (aggiungere `GITHUB_REPO_OWNER` e `GITHUB_REPO_NAME`)

## Regole di Sviluppo

### IMPORTANTE
- **ZERO costi**: nessuna API a pagamento, nessun servizio esterno a pagamento
- **Tutto nativo Android**: SpeechRecognizer e TextToSpeech sono built-in, NON usare Google Cloud Speech, Amazon Polly, ecc.
- **Keep it simple**: un'Activity, un ViewModel, pochi file. Non overengineerare.
- **Lingua UI**: Italiano
- **Lingua TTS**: Italiano

### Convenzioni codice
- Kotlin idiomatico (data classes, coroutines, extension functions)
- Compose con state hoisting
- ViewModel con StateFlow per lo stato UI
- Retrofit con coroutines (suspend functions)
- Commenti in italiano dove utile

### Testing
- Per testare l'API: `curl https://gpa.madbob.org/query.php?stop=578`
- Per buildare: `./gradlew assembleDebug`
- APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Priorità Implementazione

1. **Prima**: Schermata principale con dati API funzionanti (senza voce)
2. **Poi**: TTS - risposta vocale automatica
3. **Poi**: STT - input vocale con microfono
4. **Poi**: Widget home screen
5. **Infine**: Auto-update da GitHub

## Note Aggiuntive

- L'API GPA MadBob a volte restituisce anche linee che non ci interessano (es. 3107, 77, 30) → filtrarle via
- Il campo `hour` è nell'orario locale italiano → usare `LocalTime.parse()` con il formato corretto
- Per il calcolo dei minuti: attenzione al cambio giorno (es. ora 23:50, bus alle 00:10 = 20 minuti)
- Il flag `realtime` è importante da comunicare all'utente (dato reale vs orario teorico)
