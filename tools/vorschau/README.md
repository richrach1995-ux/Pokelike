# Vorschau: das Spiel ohne Android-Gerät starten

Dieser eigenständige Build startet den **echten Spielcode** aus `app/src/main/java`
auf dem PC: `GameView` wird erzeugt, bekommt Tastendrücke und zeichnet jedes Bild
über die normale `renderFrame`-Schleife. So lassen sich Bildschirmfotos machen und
Anzeigefehler finden, ohne ein Handy oder einen Emulator zu brauchen.

Möglich wird das durch schlanke Nachbauten der benutzten Android-Klassen
(`Canvas`, `Paint`, `Bitmap`, `Path`, `SurfaceView`, …) auf Basis von Java AWT,
die unter `shim/java` liegen. Der Spielcode selbst wird **nicht** verändert.

## Aufruf

```bash
cd tools/vorschau
gradle test --rerun-tasks
```

Die Bilder landen unter `build/bilder/`.

## Was geprüft wird

* `Spielen.kt` – Titelbild, Dorf, Menü, Typen-Info, ein kompletter Kampf,
  Team, Steckbrief und Beutel; bricht ab, wenn ein Bild leer bleibt oder ein
  Kampf nicht endet
* `Lebensbalken.kt` – misst die **Pixelbreite der KP-Balken** vor und nach einem
  Treffer und vergleicht sie mit den echten KP. Genau dieser Test hätte den
  Fehler gefunden, bei dem der KP-Balken nie gesunken ist.
* `Attacken.kt` – Kampfmenü, Attackenliste und Beutel im Kampf

Wichtig: Gezeichnet wird über AWT statt über Skia. Formen, Farben und Anordnung
stimmen mit dem Gerät überein, Textbreiten können minimal abweichen.
