# Datenbankschema

Room, Version 1, Datei `runeveil.db`. Schema-Exporte liegen unter `data/schemas/`
und werden für Migrationstests verwendet.

## Grundprinzip

Jede Tabelle ist nach `saveSlot` partitioniert. Ein Spielstand ist damit eine
Partition, keine eigene Datei. Das macht Autosave billig: Monster, Inventar und
Questfortschritt sind bereits geschrieben, sobald sie sich ändern — gespeichert
wird nur noch die Kopfzeile.

Slot 0 ist der Autosave, Slots 1–3 sind manuell.

```mermaid
erDiagram
    GAME_STATE ||--o{ MONSTERS : "saveSlot"
    GAME_STATE ||--o{ INVENTORY : "saveSlot"
    GAME_STATE ||--o{ QUEST_PROGRESS : "saveSlot"
    GAME_STATE ||--o{ BESTIARY : "saveSlot"
    GAME_STATE ||--o{ ACHIEVEMENTS : "saveSlot"
    GAME_STATE ||--o{ REGION_WEATHER : "saveSlot"

    GAME_STATE {
        int saveSlot PK
        int schemaVersion
        string playerName
        string gender
        string appearanceJson
        int playerLevel
        long playerExperience
        int gold
        string equipmentJson
        string activeTitleId
        string unlockedTitlesJson
        int moralScore
        long playtimeSeconds
        long stepsWalked
        string currentRegionId
        string currentLocationId
        string respawnLocationId
        string statisticsJson
        int chapter
        string storyFlagsJson
        string discoveredLocationsJson
        string fastTravelLocationsJson
        string defeatedTrainersJson
        string openedChestsJson
        string unlockedRecipesJson
        string factionReputationJson
        long inGameMinutes
        int endlessBestFloor
        string activeEndingId
        int newGamePlusCount
        long savedAtEpochMs
        bool isAutosave
    }

    MONSTERS {
        string uid PK
        int saveSlot
        string speciesId
        string nickname
        int level
        long experience
        int gene_hp "… sieben Werte"
        int train_hp "… sieben Werte"
        int talentbonus_hp "… sieben Werte"
        string temperament
        string gender
        bool isShiny
        string abilityId
        string movesJson
        int currentHp
        string status
        int statusTurns
        int friendship
        string unlockedTalents
        int spentTalentPoints
        string heldItemId
        string boundRunes
        string originLocationId
        string originalTrainerName
        long caughtAtEpochMs
        string caughtWithOrbId
        int eggHatchStepsRemaining
        bool isEgg
        bool inParty
        int partyOrder
        bool atRoost
    }

    INVENTORY {
        int saveSlot PK
        string itemId PK
        int quantity
    }

    QUEST_PROGRESS {
        int saveSlot PK
        string questId PK
        string state
        string objectiveCountsJson
        long startedAtEpochMs
        long completedAtEpochMs
        long lastResetEpochMs
    }

    BESTIARY {
        int saveSlot PK
        string speciesId PK
        bool seen
        bool caught
        bool shinyCaught
        int caughtCount
        string firstSeenLocationId
        long firstCaughtEpochMs
    }

    ACHIEVEMENTS {
        int saveSlot PK
        string achievementId PK
        int progress
        long unlockedAtEpochMs
    }

    REGION_WEATHER {
        int saveSlot PK
        string regionId PK
        string weather
        long startedAtEpochMs
        long durationMs
    }
```

## Entwurfsentscheidungen

**Werte eingebettet, nicht als JSON.** Gene, Trainingswerte und Talentboni liegen
als `@Embedded`-Blöcke mit Spaltenpräfix vor (`gene_hp`, `train_attack` …). Das
Gewölbe sortiert nach Werten — das geht nur in SQL, wenn die Werte Spalten sind.

**Mengen als JSON-Arrays.** Story-Flags, entdeckte Orte, geöffnete Truhen werden
immer vollständig gelesen und geschrieben, nie einzeln abgefragt. Eine
Verknüpfungstabelle brächte hier nur Kosten.

**Team und Gewölbe in einer Tabelle.** Die Zugehörigkeit steckt in `inParty` und
`partyOrder`. Ein Umzug ist ein Spalten-Update statt Löschen + Einfügen; die
Zeilenidentität (und damit Fangdatum und Originaltrainer) bleibt erhalten.

**Kein destruktiver Migrations-Fallback.** `fallbackToDestructiveMigration()`
wird bewusst nicht gesetzt. Eine fehlende Migration soll im Entwicklungsbau laut
scheitern, statt im Feld einen Spielstand zu löschen.

## Migrationen

Beim Erhöhen von `RuneveilDatabase.VERSION`:

1. `Migration(n, n+1)` schreiben und in `CoreDataModule.provideDatabase` eintragen.
2. Schema-JSON aus `data/schemas/` committen.
3. Test mit `MigrationTestHelper` gegen das exportierte Schema ergänzen.

`GameSave.CURRENT_SCHEMA_VERSION` beschreibt davon unabhängig das *Format* des
Spielstands innerhalb einer Zeile und wird erhöht, wenn sich die Bedeutung von
Feldern ändert.
