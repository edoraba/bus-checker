#!/bin/bash
# Script per rilasciare una nuova versione di BusPullman
# Uso: ./release.sh 1.0.2

set -e

VERSION="$1"

if [ -z "$VERSION" ]; then
    echo "Uso: ./release.sh <versione>"
    echo "Esempio: ./release.sh 1.0.2"
    exit 1
fi

# Calcola versionCode incrementale dal versionName
MAJOR=$(echo "$VERSION" | cut -d. -f1)
MINOR=$(echo "$VERSION" | cut -d. -f2)
PATCH=$(echo "$VERSION" | cut -d. -f3)
VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))

echo "Rilascio BusPullman v${VERSION} (versionCode: ${VERSION_CODE})"

# Aggiorna versionName e versionCode in build.gradle.kts
sed -i "s/versionCode = [0-9]*/versionCode = ${VERSION_CODE}/" app/build.gradle.kts
sed -i "s/versionName = \"[^\"]*\"/versionName = \"${VERSION}\"/" app/build.gradle.kts

echo "build.gradle.kts aggiornato"

# Commit, tag, push
git add app/build.gradle.kts
git commit -m "release: v${VERSION}"
git tag "v${VERSION}"
git push origin main
git push origin "v${VERSION}"

echo ""
echo "Fatto! GitHub Actions buildera' l'APK e creera' la release."
echo "Controlla: https://github.com/edoraba/bus-checker/actions"
