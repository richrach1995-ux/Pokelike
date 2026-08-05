package com.pokelike.idle.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.pokelike.idle.data.database.entity.AchievementEntity
import com.pokelike.idle.data.database.entity.ActiveBoosterEntity
import com.pokelike.idle.data.database.entity.AdCooldownEntity
import com.pokelike.idle.data.database.entity.BuildingEntity
import com.pokelike.idle.data.database.entity.DailyLoginEntity
import com.pokelike.idle.data.database.entity.GameStateEntity
import com.pokelike.idle.data.database.entity.QuestBaselineEntity
import com.pokelike.idle.data.database.entity.QuestBaselineValueEntity
import com.pokelike.idle.data.database.entity.QuestClaimEntity
import com.pokelike.idle.data.database.entity.ResourceEntity
import com.pokelike.idle.data.database.entity.UpgradeEntity

/**
 * Datenbankzugriff auf den Spielstand.
 *
 * Alle Methoden sind `suspend`: Room verweigert dadurch bereits beim
 * Uebersetzen den Aufruf auf dem UI-Thread. Ein Datenbankzugriff waehrend eines
 * Frames ist die haeufigste Ursache fuer Ruckler in Compose-Anwendungen.
 *
 * Als abstrakte Klasse statt als Interface: Die mit [Transaction] versehenen
 * Methoden bringen eine eigene Umsetzung mit, und fuer diese Kombination ist
 * die abstrakte Klasse der von Room ausdruecklich vorgesehene Weg.
 */
@Dao
abstract class GameStateDao {

    /**
     * Es gibt nur eine Zeile, deshalb ohne Parameter.
     *
     * Room unterstuetzt zwar Standardwerte fuer DAO-Parameter, aber eine
     * Abfrage ohne Parameter kann gar nicht erst mit dem falschen Schluessel
     * aufgerufen werden.
     */
    @Query("SELECT * FROM game_state LIMIT 1")
    abstract suspend fun findState(): GameStateEntity?

    @Query("SELECT * FROM resources")
    abstract suspend fun findResources(): List<ResourceEntity>

    @Query("SELECT * FROM buildings")
    abstract suspend fun findBuildings(): List<BuildingEntity>

    @Query("SELECT * FROM upgrades")
    abstract suspend fun findUpgrades(): List<UpgradeEntity>

    @Query("SELECT * FROM achievements")
    abstract suspend fun findAchievements(): List<AchievementEntity>

    @Query("SELECT * FROM quest_baselines")
    abstract suspend fun findQuestBaselines(): List<QuestBaselineEntity>

    @Query("SELECT * FROM quest_baseline_values")
    abstract suspend fun findQuestBaselineValues(): List<QuestBaselineValueEntity>

    @Query("SELECT * FROM quest_claims")
    abstract suspend fun findQuestClaims(): List<QuestClaimEntity>

    @Query("SELECT * FROM daily_login LIMIT 1")
    abstract suspend fun findDailyLogin(): DailyLoginEntity?

    @Query("SELECT * FROM active_boosters")
    abstract suspend fun findActiveBoosters(): List<ActiveBoosterEntity>

    @Query("SELECT * FROM ad_cooldowns")
    abstract suspend fun findAdCooldowns(): List<AdCooldownEntity>

    /**
     * Schreibt den vollstaendigen Spielstand in einem Zug.
     *
     * Die Transaktion ist hier nicht optional. Der Spielstand besteht aus zwei
     * Tabellen, und die Pruefsumme deckt beide ab. Wuerde der Vorgang zwischen
     * den Schritten abbrechen - etwa weil das System den Prozess beendet -,
     * blieben skalare Felder und Ressourcen aus verschiedenen Staenden
     * uebrig. Die Pruefung schluege beim naechsten Start fehl, und der Spieler
     * verloere seinen gesamten Fortschritt.
     *
     * Die Ressourcen werden geloescht und neu geschrieben, statt sie
     * abzugleichen. Nur so verschwinden Zeilen, die es im neuen Stand nicht
     * mehr gibt - etwa nach einem Prestige-Reset.
     */
    @Transaction
    open suspend fun saveState(
        state: GameStateEntity,
        resources: List<ResourceEntity>,
        buildings: List<BuildingEntity>,
        upgrades: List<UpgradeEntity>,
        achievements: List<AchievementEntity>,
        questBaselines: List<QuestBaselineEntity>,
        questBaselineValues: List<QuestBaselineValueEntity>,
        questClaims: List<QuestClaimEntity>,
        dailyLogin: DailyLoginEntity?,
        boosters: List<ActiveBoosterEntity>,
        adCooldowns: List<AdCooldownEntity>,
    ) {
        upsertState(state)
        deleteAllResources()
        insertResources(resources)
        deleteAllBuildings()
        insertBuildings(buildings)
        deleteAllUpgrades()
        insertUpgrades(upgrades)
        deleteAllAchievements()
        insertAchievements(achievements)
        deleteAllQuestBaselines()
        insertQuestBaselines(questBaselines)
        deleteAllQuestBaselineValues()
        insertQuestBaselineValues(questBaselineValues)
        deleteAllQuestClaims()
        insertQuestClaims(questClaims)
        // Loeschen und nur bei Bedarf neu schreiben: Ein Spielstand ohne
        // Anmeldeserie muss eine leere Tabelle hinterlassen, sonst bliebe eine
        // Zeile mit Standardwerten stehen und veraenderte die Pruefsumme.
        deleteDailyLogin()
        if (dailyLogin != null) insertDailyLogin(dailyLogin)
        deleteAllBoosters()
        insertBoosters(boosters)
        deleteAllAdCooldowns()
        insertAdCooldowns(adCooldowns)
    }

    /**
     * Loescht den gesamten Spielstand.
     *
     * Ebenfalls als Transaktion, damit kein Zustand entsteht, in dem die
     * skalaren Felder geloescht sind, die Ressourcen aber noch stehen.
     */
    @Transaction
    open suspend fun clearAll() {
        deleteState()
        deleteAllResources()
        deleteAllBuildings()
        deleteAllUpgrades()
        deleteAllAchievements()
        deleteAllQuestBaselines()
        deleteAllQuestBaselineValues()
        deleteAllQuestClaims()
        deleteDailyLogin()
        deleteAllBoosters()
        deleteAllAdCooldowns()
    }

    @Upsert
    abstract suspend fun upsertState(state: GameStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertResources(resources: List<ResourceEntity>)

    @Query("DELETE FROM resources")
    abstract suspend fun deleteAllResources()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertBuildings(buildings: List<BuildingEntity>)

    @Query("DELETE FROM buildings")
    abstract suspend fun deleteAllBuildings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUpgrades(upgrades: List<UpgradeEntity>)

    @Query("DELETE FROM upgrades")
    abstract suspend fun deleteAllUpgrades()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Query("DELETE FROM achievements")
    abstract suspend fun deleteAllAchievements()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertQuestBaselines(baselines: List<QuestBaselineEntity>)

    @Query("DELETE FROM quest_baselines")
    abstract suspend fun deleteAllQuestBaselines()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertQuestBaselineValues(values: List<QuestBaselineValueEntity>)

    @Query("DELETE FROM quest_baseline_values")
    abstract suspend fun deleteAllQuestBaselineValues()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertQuestClaims(claims: List<QuestClaimEntity>)

    @Query("DELETE FROM quest_claims")
    abstract suspend fun deleteAllQuestClaims()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDailyLogin(dailyLogin: DailyLoginEntity)

    @Query("DELETE FROM daily_login")
    abstract suspend fun deleteDailyLogin()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertBoosters(boosters: List<ActiveBoosterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAdCooldowns(cooldowns: List<AdCooldownEntity>)

    @Query("DELETE FROM ad_cooldowns")
    abstract suspend fun deleteAllAdCooldowns()

    @Query("DELETE FROM active_boosters")
    abstract suspend fun deleteAllBoosters()

    @Query("DELETE FROM game_state")
    abstract suspend fun deleteState()
}
