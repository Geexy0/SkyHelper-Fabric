#!/usr/bin/env bash
# Replace FarmHelper Fabric JAR in Vivien's OneClient 26.3 mods folder.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODS_DIR="/home/geexy/.local/share/Polyfrost/OneClient/.minecraft/mods/26.3 Fabric"
LIBS_DIR="${ROOT}/build/libs"

if [[ ! -d "${MODS_DIR}" ]]; then
  echo "error: mods dir missing: ${MODS_DIR}" >&2
  exit 1
fi

JAR="${1:-}"
if [[ -z "${JAR}" ]]; then
  # Newest non-sources farmhelper jar from build/libs
  JAR="$(ls -1t "${LIBS_DIR}"/farmhelper-fabric-*.jar 2>/dev/null | grep -v -- '-sources\.jar$' | grep -v -- '-dev\.jar$' | head -n1 || true)"
fi

if [[ -z "${JAR}" || ! -f "${JAR}" ]]; then
  echo "error: no farmhelper-fabric JAR found (pass a path or run ./gradlew build first)" >&2
  exit 1
fi

echo "Installing: ${JAR}"
echo "Into:       ${MODS_DIR}"

# Remove prior FarmHelper jars only (files or symlinks).
shopt -s nullglob
for old in \
  "${MODS_DIR}"/farmhelper-fabric-*.jar \
  "${MODS_DIR}"/FarmHelper*.jar \
  "${MODS_DIR}"/farmhelper*.jar
do
  base="$(basename "${old}")"
  # Keep unrelated jars that merely contain the substring elsewhere — we already glob farmhelper*
  echo "Removing old: ${base}"
  rm -f "${old}"
done
shopt -u nullglob

dest="${MODS_DIR}/$(basename "${JAR}")"
cp -f "${JAR}" "${dest}"
echo "Installed:  ${dest}"
ls -la "${dest}"
