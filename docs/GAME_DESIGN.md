# Spieldesign

## Welt und Geschichte

Yggdrasil stirbt. Nicht plötzlich, sondern wie ein alter Baum: an den Rändern
zuerst. Wo seine Äste brechen, reißen die Grenzen zwischen den Neun Welten auf,
und was hindurchkommt, gehört nirgendwohin.

Der Chaoskult unter **Grimnir Bruchstimme** will das Ende nicht aufhalten,
sondern beschleunigen. Sein Argument ist unbequem: die Welten sind ohnehin
verloren, ein sauberer Abschluss sei gnädiger als ein langes Verrecken. Die
zweite Stimme, **Saga**, war einmal Runenhüterin — sie erklärt der Spielerin
gerne und ausführlich, warum sie es nicht mehr ist.

### Kapitel

| # | Titel | Region | Kern |
|---|---|---|---|
| 0 | Der brennende Ast | Herzland von Midgard | Prolog: ein Ast fällt brennend vom Himmel |
| 1 | Das Siegel der Runenwacht | Herzland von Midgard | Hrafn übergibt ein Amt, das er loswerden will |
| 2 | Risse im Licht | Prismenwälder | In Alfheim fällt das Licht falsch |
| 3 | Was unter den Feldern wächst | Blühende Marken | Zu üppige Ernte, bezahlt mit dem Jahr danach |
| 4 | Der Grat der Riesen | Frostkämme | Riesen fliehen aus den eigenen Bergen |
| 5 | Die Alte Esse | Tiefenstollen | Zwerge schmieden Ketten, die niemand bestellt hat |
| 6 | Der Ewige Brand | Glutfelder | Ein Feuer älter als die Zeit breitet sich aus |
| 7 | Elf Quellen, ein Brunnen | Nebelöden | Begegnung mit Saga im Nebel |
| 8 | Die Halle der Ungezählten | Totenmarken | Helheim ist voll, und das Tor steht offen |
| 9 | Thing der Neun | Goldene Höhen | Die Götter fragen, ob die Welt es wert ist |

### Enden

Vier Enden, gewählt nach Story-Flags, Moralwert und gebundenen Runen:

- **Der neue Ast** — Yggdrasil stirbt, aber nicht ohne Nachkommen. Die Welten
  werden kleiner, ärmer und sehr viel jünger. *(Moralwert ≥ 20, alle neun Runen)*
- **Die Neunte Fessel** — Der Bruch wird gebunden. Alles bleibt, wie es war,
  einschließlich allem, was daran falsch war.
- **Die Stimme des Bruchs** — Grimnir hat recht bekommen. *(Moralwert ≤ −30)*
- **Der letzte Runenwächter** — Keine Lösung, ein Versprechen. Vierzig Jahre lang
  gehalten.

## Elemente

14 Elemente. Die Matrix ist gegen Invarianten getestet: jedes Element ist gegen
2–4 andere stark und wird von 2–4 anderen hart getroffen; die Streuung der
Offensivwerte bleibt unter 5.

Vier Immunitäten, jede mit Begründung:

| Angriff | Ziel | Grund |
|---|---|---|
| Donner | Erde | Blitz wird von Stein und Boden geerdet |
| Schatten | Göttlich | Der Glanz der Asen lässt sich nicht verdunkeln |
| Chaos | Runen | Gebundene Zeichen lösen Formloses auf, bevor es trifft |
| Metall | Geist | Geschmiedetes Eisen geht durch Körperloses hindurch |

Nicht offensichtlich, aber gewollt: **Runen schlagen Göttlich**. Runen binden
Götter — das ist das älteste Gesetz der Neun und Kern der Endgame-Story.

## Kampf

Rundenbasiert, bis zu drei Wesen pro Seite. Bosskämpfe der Kapitel 6 und 9
nutzen zwei bzw. drei Felder.

**Schadensformel**

```
core   = ((2 × Stufe / 5 + 2) × Stärke × ANG / VER) / 50 + 2
Schaden = core × Affinität × Effektivität × Wetter × Tageszeit
                × Kritisch × Kombination × Zufall(0.85 … 1.0)
```

**Affinität** beträgt 1,5× bei passendem Element, 2,0× wenn zusätzlich eine
passende Rune gebunden ist — Runen sind damit eine echte Entscheidung, kein
reiner Zahlenzuwachs.

**Tageszeit** verschiebt Licht und Göttlich um ±10 % zugunsten des Tages,
Schatten und Geist zugunsten der Nacht. Genug, um Teamzusammenstellung zu
beeinflussen, zu wenig, um sie zu diktieren.

**Statusveränderungen** — Verbrennung, Vergiftung, Schlaf, Einfrieren,
Verwirrung, Blutung, Fluch, Lähmung. Jede hat einen mechanischen Haken, jede hat
immune Elemente, und jede erhöht die Fangchance (Schlaf und Einfrieren am
stärksten, mit 2,5×).

**KI-Profile**

| Profil | Zufall | Vorausschau | Wechsel | Gegenstände |
|---|---|---|---|---|
| Wild | 75 % | – | – | – |
| Instinktiv | 35 % | – | – | – |
| Taktisch | 15 % | – | ✓ | ✓ |
| Meisterhaft | 5 % | ✓ | ✓ | ✓ |
| Mythisch | 0 % | ✓ | – | – |

Die KI bewertet Attacken nach erwartetem Schadensanteil, sicherem K.-o.,
Statusnutzen, Wertveränderungen und — ab „meisterhaft" — dem Risiko der
gegnerischen Antwort. Sie liest keine verborgenen Informationen.

## Fangen

Acht Runenkugel-Grade mit situativen Boni. Die Fangchance ergibt sich aus
Restleben, Statusleiden, Stufe, Seltenheit, Kugel, Wetter und Ausrüstung; die
Auflösung erfolgt in vier Erschütterungen, jede mit `p^(1/4)`. Ein Fehlschlag
nach drei Erschütterungen war deshalb wirklich knapp — die Anzahl ist echte
Information, keine Inszenierung.

## Fortschritt

Stufen bis 100. Sieben Werte, dazu Gene (0–31 je Wert, beim Fang gewürfelt) und
Trainingswerte (max. 255 je Wert, 510 gesamt, erkämpft). Vierzehn Temperamente
verschieben einen Wert um +10 % und einen um −10 %.

Talentpunkte fallen alle zwei Stufen an; die neun Bäume folgen der Kampfrolle
der Art. Ein Seidr-Webstuhl erstattet sie vollständig — Experimentieren soll
nichts kosten.

## Zucht

Kompatibel sind Paare mit gemeinsamer Eigruppe und verschiedenem Geschlecht;
ein Seidr-Gefäß passt zu allem. Legendäre Wesen züchten nicht.

Vererbt werden drei von sieben Genen (fünf mit dem Sigill der Ahnen), das
Temperament (garantiert mit dem Wesenszauber), Ei-Attacken, die die Eltern
kennen, und bis zu zwei gemeinsame Talente. Mutationen setzen ein Gen auf 31
(6 %), Hybride übernehmen das Element des Vaters (4,5 %).

Schimmer-Chance: 1 zu 2048, oder **1 zu 512**, wenn die Eltern aus verschiedenen
Welten stammen. Das ist der beste Grund, quer durch die Neun zu reisen.

## Wirtschaft

Verkaufen bringt die Hälfte, Handwerk ist der eigentliche Weg zu gutem Gerät.
Legendäre Rezepte können fehlschlagen (60–88 %) und verbrauchen dabei die Hälfte
der Zutaten — das ist die Spannung der Svartalfheim-Endgame-Schleife.

## Endgame

Legendäre Wächter in jedem Heiligtum, geheime Bosse hinter dem Nornenkompass,
das Endlose Verlies, das Große Thing als Turnier, 100 % Bestiarium und New Game
Plus mit erhöhten Gegnerstufen bei erhaltenem Bestiarium und Titeln.

## Balance-Leitlinien

1. Kein Wesen ist in jeder Lage die beste Wahl — der Wertehaushalt hängt an Rolle
   und Entwicklungsstufe, nicht am Zufall.
2. Jede Art bleibt spielbar: Endformen liegen bei ~530 Punkten plus
   Seltenheitsbonus; kein Ausreißer nach oben.
3. Seltenheit erschwert den Fang und erhöht die Erfahrung, verschiebt aber die
   Kampfkraft nur moderat (max. +145 Punkte).
4. Statusveränderungen sind stark, aber jedes hat immune Elemente.
5. Wetter verschiebt Schaden um ±35 % — spürbar, aber kein Selbstläufer.
