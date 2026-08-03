#!/usr/bin/env bash
# Installiert die Debug-APK auf einem bereits laufenden Emulator, startet sie
# und schreibt jede Absturzmeldung ins Job-Protokoll.
#
# Warum eine eigene Datei? `android-emulator-runner` führt jede Zeile seines
# `script:`-Blocks in einer *eigenen* Shell aus. Variablen überleben den
# Zeilenwechsel also nicht — ein `APK=$(find …)` in Zeile 1 ist in Zeile 2
# bereits wieder leer. Der gesamte Ablauf muss deshalb in einem Skript stehen,
# das als einzelne Zeile aufgerufen wird.
set -u

PKG="com.runeveil.saga.debug"
ACT="com.runeveil.saga.MainActivity"

APK=$(find app/build/outputs/apk/debug -name '*.apk' | head -n 1)
if [ -z "$APK" ]; then
    echo "::error::Keine APK unter app/build/outputs/apk/debug gefunden."
    exit 1
fi
echo "APK: $APK"

if ! adb install -r "$APK"; then
    echo "::error::Installation der APK fehlgeschlagen."
    exit 1
fi

adb logcat -c
adb shell am start -W -n "$PKG/$ACT"

# Genug Zeit für Application.onCreate, Hilt-Graph, Splash und das Laden der
# 2,3 MiB Inhalte. Ein Absturz passiert lange vorher.
sleep 25

echo "=================== PROZESS ==================="
PID=$(adb shell pidof "$PKG" | tr -d '\r\n')
echo "pid='$PID'"

echo "=================== CRASH-PUFFER ==================="
adb logcat -d -b crash || true

echo "=================== FEHLER IM HAUPTPUFFER ==================="
adb logcat -d '*:E' | tail -n 200 || true

echo "=================== APP-EIGENE ZEILEN ==================="
adb logcat -d | grep -iE 'runeveil|AndroidRuntime|FATAL|Hilt|Dagger|Room|DataStore' \
    | tail -n 150 || true

if [ -z "$PID" ]; then
    echo "::error::Die App läuft nach dem Start nicht mehr — Stacktrace siehe oben."
    exit 1
fi

echo "App läuft (pid $PID)."
