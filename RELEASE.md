# Come rilasciare una nuova versione di BusPullman

## Prerequisiti

- Git configurato con accesso push a `github.com/edoraba/bus-checker`
- Git Bash installato (incluso con Git for Windows)

## Rilascio rapido (script automatico)

Da Git Bash nella cartella del progetto:

```bash
bash release.sh 1.0.2
```

Lo script fa tutto in automatico:
1. Aggiorna `versionName` e `versionCode` in `app/build.gradle.kts`
2. Crea un commit `release: v1.0.2`
3. Crea il tag `v1.0.2`
4. Pusha commit e tag su GitHub
5. GitHub Actions builda l'APK e crea la Release

## Rilascio manuale (passo per passo)

### 1. Committa le modifiche

```bash
git add .
git commit -m "descrizione delle modifiche"
git push origin main
```

### 2. Aggiorna la versione in `app/build.gradle.kts`

```kotlin
defaultConfig {
    versionCode = 10002       // MAJOR*10000 + MINOR*100 + PATCH
    versionName = "1.0.2"
}
```

Regola per il `versionCode`:
- `1.0.0` → `10000`
- `1.0.1` → `10001`
- `1.2.0` → `10200`
- `2.0.0` → `20000`

### 3. Committa il bump di versione

```bash
git add app/build.gradle.kts
git commit -m "release: v1.0.2"
git push origin main
```

### 4. Crea e pusha il tag

```bash
git tag v1.0.2
git push origin v1.0.2
```

### 5. Attendi GitHub Actions

- Il workflow si attiva automaticamente quando viene pushato un tag `v*`
- Builda l'APK release e crea una GitHub Release con l'APK allegato
- Durata: ~5-8 minuti (prima volta ~10 min)
- Controlla lo stato: https://github.com/edoraba/bus-checker/actions

### 6. Verifica

- Vai su https://github.com/edoraba/bus-checker/releases
- Dovresti vedere la release `BusPullman v1.0.2` con l'APK scaricabile

## Come funziona l'auto-update nell'app

1. All'avvio, l'app chiama `api.github.com/repos/edoraba/bus-checker/releases/latest`
2. Confronta il `tag_name` della release con `BuildConfig.VERSION_NAME`
3. Se la versione su GitHub e' piu' recente, mostra un banner "Aggiornamento disponibile"
4. L'utente tocca "Aggiorna" → scarica l'APK e apre l'installer Android

## Ritriggare una release fallita

Se il workflow fallisce e vuoi ritriggare lo stesso tag:

```bash
git tag -d v1.0.2
git push origin :refs/tags/v1.0.2
git tag v1.0.2
git push origin v1.0.2
```

## Troubleshooting

| Problema | Soluzione |
|----------|-----------|
| `Permission denied` su gradlew | Il workflow ha gia' `chmod +x` |
| `Resource not accessible` (403) | Il workflow ha `permissions: contents: write` |
| L'APK non appare nella release | Controlla che il path sia `app/build/outputs/apk/release/app-release-unsigned.apk` |
| L'app non rileva l'aggiornamento | Verifica che `GITHUB_REPO_OWNER` e `GITHUB_REPO_NAME` in `BusModels.kt` siano corretti |
