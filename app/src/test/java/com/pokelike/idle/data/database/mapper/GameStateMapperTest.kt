package com.pokelike.idle.data.database.mapper

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.data.database.entity.AchievementEntity
import com.pokelike.idle.data.database.entity.ActiveBoosterEntity
import com.pokelike.idle.data.database.entity.AdCooldownEntity
import com.pokelike.idle.data.database.entity.BuildingEntity
import com.pokelike.idle.data.database.entity.ResourceBucket
import com.pokelike.idle.data.database.entity.ResourceEntity
import com.pokelike.idle.data.database.entity.UpgradeEntity
import com.pokelike.idle.data.security.SaveSignature
import com.pokelike.idle.domain.model.AchievementInventory
import com.pokelike.idle.domain.model.AchievementType
import com.pokelike.idle.domain.model.AdState
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BoosterState
import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.GameStatistics
import com.pokelike.idle.domain.model.LoginState
import com.pokelike.idle.domain.model.QuestBaseline
import com.pokelike.idle.domain.model.QuestMetric
import com.pokelike.idle.domain.model.QuestPeriod
import com.pokelike.idle.domain.model.QuestState
import com.pokelike.idle.domain.model.QuestType
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.RewardedAdPlacement
import com.pokelike.idle.domain.model.UpgradeInventory
import com.pokelike.idle.domain.model.UpgradeType
import org.junit.Test

class GameStateMapperTest {

    private val mapper = GameStateMapper(SaveSignature())

    private val sampleState = GameState(
        resources = ResourcePool.of(
            ResourceType.COINS to BigNumber.of(1.234, 42),
            ResourceType.DIAMONDS to BigNumber.of(500),
        ),
        statistics = GameStatistics(
            totalClicks = 12_345L,
            totalCriticalClicks = 678L,
            lifetimeEarned = ResourceBundle.single(ResourceType.COINS, BigNumber.of(9.9, 50)),
            lifetimeSpent = ResourceBundle.single(ResourceType.COINS, BigNumber.of(1.1, 40)),
            totalPlayTimeMillis = 987_654L,
            sessionCount = 42,
            prestigeCount = 7,
        ),
        createdAtMillis = 1_700_000_000_000L,
        lastSeenAtMillis = 1_700_000_999_000L,
    )

    /** Fester Startzeitpunkt fuer Booster in den Testfaellen. */
    private val boosterNow = 1_700_000_000_000L

    @Test
    fun `stellt einen Spielstand unveraendert wieder her`() {
        val persisted = mapper.toPersisted(sampleState)

        val restored = mapper.toDomain(persisted.state, persisted.resources)

        assertThat(restored).isEqualTo(sampleState)
    }

    @Test
    fun `haelt sehr grosse Betraege exakt`() {
        // Der eigentliche Grund fuer die getrennte Speicherung von Mantisse und
        // Exponent: Ein Umweg ueber Double waere hier bereits Infinity.
        val state = sampleState.copy(
            resources = ResourcePool.of(ResourceType.COINS to BigNumber.of(7.77, 400)),
        )

        val persisted = mapper.toPersisted(state)
        val restored = mapper.toDomain(persisted.state, persisted.resources)

        assertThat(restored?.get(ResourceType.COINS)).isEqualTo(BigNumber.of(7.77, 400))
    }

    @Test
    fun `trennt Kontostand und Lebenszeitsummen`() {
        val persisted = mapper.toPersisted(sampleState)

        val buckets = persisted.resources.map { it.bucket }.toSet()

        assertThat(buckets).containsExactly(
            ResourceBucket.CURRENT.id,
            ResourceBucket.LIFETIME_EARNED.id,
            ResourceBucket.LIFETIME_SPENT.id,
        )
    }

    // --- Gebaeude ---------------------------------------------------------

    @Test
    fun `stellt den Gebaeudebestand wieder her`() {
        val state = sampleState.copy(
            buildings = BuildingInventory.of(
                BuildingType.FINGER to 42,
                BuildingType.TIME_MACHINE to 3,
            ),
        )

        val persisted = mapper.toPersisted(state)
        val restored = mapper.toDomain(persisted.state, persisted.resources, persisted.buildings)

        assertThat(restored).isEqualTo(state)
    }

    @Test
    fun `verwirft einen Spielstand mit veraenderter Gebaeudeanzahl`() {
        // Die Pruefsumme muss auch die Gebaeudetabelle abdecken, sonst liesse
        // sich der Bestand direkt in der Datenbank hochsetzen.
        val state = sampleState.copy(
            buildings = BuildingInventory.of(BuildingType.FINGER to 5),
        )
        val persisted = mapper.toPersisted(state)

        val tampered = persisted.buildings.map { row -> row.copy(owned = 9_999) }

        assertThat(mapper.toDomain(persisted.state, persisted.resources, tampered)).isNull()
    }

    @Test
    fun `liest einen Spielstand ohne Gebaeude unveraendert`() {
        // Der entscheidende Punkt fuer die Vertraeglichkeit mit aelteren
        // Staenden: Ein Spielstand ohne Gebaeude muss dieselbe Pruefsumme
        // ergeben wie vor Einfuehrung des Gebaeudeabschnitts. Die Signatur
        // wird hier bewusst ohne Gebaeudeliste gebildet und mit einer
        // leeren Liste geprueft.
        val persisted = mapper.toPersisted(sampleState)

        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            buildings = emptyList(),
        )

        assertThat(restored).isEqualTo(sampleState)
        assertThat(restored?.buildings?.isEmpty).isTrue()
    }

    @Test
    fun `uebergeht Zeilen mit unbekanntem Gebaeude`() {
        val state = sampleState.copy(
            buildings = BuildingInventory.of(BuildingType.FINGER to 5),
        )
        val persisted = mapper.toPersisted(state)
        val withUnknown = persisted.buildings + BuildingEntity(
            buildingId = "gebaeude_aus_der_zukunft",
            owned = 7,
        )

        val unsigned = persisted.state.copy(signature = "")
        val resigned = unsigned.copy(
            signature = SaveSignature().sign(
                mapper.canonicalPayload(unsigned, persisted.resources, withUnknown),
            ),
        )

        val restored = mapper.toDomain(resigned, persisted.resources, withUnknown)

        assertThat(restored).isEqualTo(state)
    }

    @Test
    fun `ist unabhaengig von der Reihenfolge der Gebaeudezeilen`() {
        val state = sampleState.copy(
            buildings = BuildingInventory.of(
                BuildingType.FINGER to 5,
                BuildingType.MINE to 2,
                BuildingType.BANK to 1,
            ),
        )
        val persisted = mapper.toPersisted(state)

        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            buildings = persisted.buildings.reversed(),
        )

        assertThat(restored).isEqualTo(state)
    }

    // --- Upgrades ---------------------------------------------------------

    @Test
    fun `stellt die gekauften Upgrades wieder her`() {
        val state = sampleState.copy(
            upgrades = UpgradeInventory.of(
                UpgradeType.STRONGER_FINGERS,
                UpgradeType.NIGHT_SHIFT,
            ),
        )

        val persisted = mapper.toPersisted(state)
        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            buildings = persisted.buildings,
            upgrades = persisted.upgrades,
        )

        assertThat(restored).isEqualTo(state)
    }

    @Test
    fun `verwirft einen Spielstand mit nachtraeglich eingefuegtem Upgrade`() {
        // Ohne Abdeckung durch die Pruefsumme liesse sich jedes Upgrade
        // kostenlos in die Datenbank eintragen.
        val persisted = mapper.toPersisted(sampleState)

        val tampered = listOf(UpgradeEntity(upgradeId = UpgradeType.GOLDEN_TOUCH.id))

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                buildings = persisted.buildings,
                upgrades = tampered,
            ),
        ).isNull()
    }

    @Test
    fun `liest einen Spielstand ohne Upgrades unveraendert`() {
        // Dieselbe Vertraeglichkeitsregel wie bei den Gebaeuden: Der
        // Upgrade-Abschnitt wird angehaengt, nie eingefuegt. Ein Stand ohne
        // Upgrades ergibt deshalb dieselbe Pruefsumme wie zuvor.
        val persisted = mapper.toPersisted(sampleState)

        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            buildings = persisted.buildings,
            upgrades = emptyList(),
        )

        assertThat(restored).isEqualTo(sampleState)
    }

    @Test
    fun `uebergeht Zeilen mit unbekanntem Upgrade`() {
        val persisted = mapper.toPersisted(sampleState)
        val withUnknown = listOf(UpgradeEntity(upgradeId = "upgrade_aus_der_zukunft"))

        val unsigned = persisted.state.copy(signature = "")
        val resigned = unsigned.copy(
            signature = SaveSignature().sign(
                mapper.canonicalPayload(
                    unsigned,
                    persisted.resources,
                    persisted.buildings,
                    withUnknown,
                ),
            ),
        )

        val restored = mapper.toDomain(
            entity = resigned,
            resources = persisted.resources,
            buildings = persisted.buildings,
            upgrades = withUnknown,
        )

        assertThat(restored).isEqualTo(sampleState)
    }

    @Test
    fun `ist unabhaengig von der Reihenfolge der Upgrade-Zeilen`() {
        val state = sampleState.copy(
            upgrades = UpgradeInventory.of(
                UpgradeType.STRONGER_FINGERS,
                UpgradeType.LUCKY_CHARM,
                UpgradeType.WHOLESALE,
            ),
        )
        val persisted = mapper.toPersisted(state)

        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            buildings = persisted.buildings,
            upgrades = persisted.upgrades.reversed(),
        )

        assertThat(restored).isEqualTo(state)
    }

    // --- Ziele ------------------------------------------------------------

    @Test
    fun `stellt Achievements und Quests wieder her`() {
        val state = sampleState.copy(
            achievements = AchievementInventory.of(
                AchievementType.FIRST_CLICK,
                AchievementType.MILLION_COINS,
            ),
            quests = QuestState(
                baselines = mapOf(
                    QuestPeriod.DAILY to QuestBaseline(
                        startedAtMillis = 1_700_000_000_000L,
                        values = mapOf(
                            QuestMetric.CLICKS to BigNumber.of(12_000),
                            QuestMetric.COINS_EARNED to BigNumber.of(3.5, 20),
                        ),
                    ),
                    QuestPeriod.WEEKLY to QuestBaseline(
                        startedAtMillis = 1_699_000_000_000L,
                        values = mapOf(QuestMetric.CLICKS to BigNumber.of(9_000)),
                    ),
                ),
                claimed = setOf(QuestType.DAILY_HUNDRED_CLICKS),
            ),
        )

        val persisted = mapper.toPersisted(state)
        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            buildings = persisted.buildings,
            upgrades = persisted.upgrades,
            achievements = persisted.achievements,
            questBaselines = persisted.questBaselines,
            questBaselineValues = persisted.questBaselineValues,
            questClaims = persisted.questClaims,
        )

        assertThat(restored).isEqualTo(state)
    }

    @Test
    fun `verwirft einen Spielstand mit nachtraeglich eingetragenem Achievement`() {
        // Achievements zahlen Diamanten aus. Ohne Abdeckung durch die
        // Pruefsumme liesse sich die Belohnung beliebig oft eintragen.
        val persisted = mapper.toPersisted(sampleState)

        val tampered = listOf(AchievementEntity(achievementId = AchievementType.FIRST_CLICK.id))

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                achievements = tampered,
            ),
        ).isNull()
    }

    @Test
    fun `verwirft einen Spielstand mit zurueckgesetztem Ausgangswert`() {
        // Ein kleinerer Ausgangswert bedeutet mehr Fortschritt: Genau so
        // liessen sich Tagesquests beliebig oft abschliessen.
        val state = sampleState.copy(
            quests = QuestState(
                baselines = mapOf(
                    QuestPeriod.DAILY to QuestBaseline(
                        startedAtMillis = 1_700_000_000_000L,
                        values = mapOf(QuestMetric.CLICKS to BigNumber.of(12_000)),
                    ),
                ),
            ),
        )
        val persisted = mapper.toPersisted(state)

        val tampered = persisted.questBaselineValues.map { row -> row.copy(mantissa = 1.0) }

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                questBaselines = persisted.questBaselines,
                questBaselineValues = tampered,
            ),
        ).isNull()
    }

    @Test
    fun `verwirft einen Spielstand mit entferntem Abholvermerk`() {
        // Ohne den Vermerk liesse sich dieselbe Quest im selben Zeitraum
        // beliebig oft abholen.
        val state = sampleState.copy(
            quests = QuestState(claimed = setOf(QuestType.DAILY_HUNDRED_CLICKS)),
        )
        val persisted = mapper.toPersisted(state)

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                questClaims = emptyList(),
            ),
        ).isNull()
    }

    @Test
    fun `liest einen Spielstand ohne Ziele unveraendert`() {
        // Dieselbe Vertraeglichkeitsregel wie bei Gebaeuden und Upgrades: Die
        // vier Zielabschnitte werden angehaengt, nie eingefuegt. Ein Stand aus
        // Version 3 ergibt deshalb weiterhin dieselbe Pruefsumme.
        val persisted = mapper.toPersisted(sampleState)

        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            buildings = persisted.buildings,
            upgrades = persisted.upgrades,
            achievements = emptyList(),
            questBaselines = emptyList(),
            questBaselineValues = emptyList(),
            questClaims = emptyList(),
        )

        assertThat(restored).isEqualTo(sampleState)
    }

    @Test
    fun `ist unabhaengig von der Reihenfolge der Zielzeilen`() {
        val state = sampleState.copy(
            achievements = AchievementInventory.of(
                AchievementType.FIRST_CLICK,
                AchievementType.FIRST_BUILDING,
                AchievementType.FIRST_UPGRADE,
            ),
            quests = QuestState(
                baselines = mapOf(
                    QuestPeriod.DAILY to QuestBaseline(
                        startedAtMillis = 1_700_000_000_000L,
                        values = mapOf(
                            QuestMetric.CLICKS to BigNumber.of(12_000),
                            QuestMetric.CRITICAL_CLICKS to BigNumber.of(600),
                            QuestMetric.UPGRADES_OWNED to BigNumber.of(4),
                        ),
                    ),
                ),
                claimed = setOf(
                    QuestType.DAILY_HUNDRED_CLICKS,
                    QuestType.WEEKLY_MILLION_COINS,
                ),
            ),
        )
        val persisted = mapper.toPersisted(state)

        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            buildings = persisted.buildings,
            upgrades = persisted.upgrades,
            achievements = persisted.achievements.reversed(),
            questBaselines = persisted.questBaselines.reversed(),
            questBaselineValues = persisted.questBaselineValues.reversed(),
            questClaims = persisted.questClaims.reversed(),
        )

        assertThat(restored).isEqualTo(state)
    }

    @Test
    fun `uebergeht Zeilen mit unbekanntem Ziel`() {
        val persisted = mapper.toPersisted(sampleState)
        val withUnknown = listOf(AchievementEntity(achievementId = "ziel_aus_der_zukunft"))

        val unsigned = persisted.state.copy(signature = "")
        val resigned = unsigned.copy(
            signature = SaveSignature().sign(
                mapper.canonicalPayload(
                    entity = unsigned,
                    resources = persisted.resources,
                    buildings = persisted.buildings,
                    upgrades = persisted.upgrades,
                    achievements = withUnknown,
                ),
            ),
        )

        val restored = mapper.toDomain(
            entity = resigned,
            resources = persisted.resources,
            buildings = persisted.buildings,
            upgrades = persisted.upgrades,
            achievements = withUnknown,
        )

        assertThat(restored).isEqualTo(sampleState)
    }

    // --- Taeglicher Bonus -------------------------------------------------

    @Test
    fun `stellt die Anmeldeserie wieder her`() {
        val state = sampleState.copy(
            login = LoginState(
                streak = 5,
                longestStreak = 12,
                lastClaimedAtMillis = 1_700_000_000_000L,
                protectionCharges = 2,
            ),
        )

        val persisted = mapper.toPersisted(state)
        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            dailyLogin = persisted.dailyLogin,
        )

        assertThat(restored).isEqualTo(state)
    }

    @Test
    fun `schreibt ohne Anmeldeserie keine Zeile`() {
        // Grundlage der Vertraeglichkeit mit Version 4: Eine Zeile mit lauter
        // Nullwerten wuerde die Pruefsumme veraendern, und jeder aeltere
        // Spielstand wuerde beim naechsten Start als manipuliert gelten.
        val persisted = mapper.toPersisted(sampleState)

        assertThat(persisted.dailyLogin).isNull()
    }

    @Test
    fun `liest einen Spielstand ohne Anmeldeserie unveraendert`() {
        val persisted = mapper.toPersisted(sampleState)

        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            buildings = persisted.buildings,
            upgrades = persisted.upgrades,
            dailyLogin = null,
        )

        assertThat(restored).isEqualTo(sampleState)
    }

    @Test
    fun `verwirft einen Spielstand mit hochgesetzter Serie`() {
        // Die Serie entscheidet ueber den Tag des Zyklus und damit ueber
        // Diamanten. Ohne Abdeckung durch die Pruefsumme liesse sich der
        // grosse Abschlusstag beliebig oft einstellen.
        val state = sampleState.copy(
            login = LoginState(streak = 1, longestStreak = 1, lastClaimedAtMillis = 1L),
        )
        val persisted = mapper.toPersisted(state)

        val tampered = persisted.dailyLogin?.copy(streak = 7)

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                dailyLogin = tampered,
            ),
        ).isNull()
    }

    @Test
    fun `verwirft einen Spielstand mit erfundenem Serienschutz`() {
        val state = sampleState.copy(
            login = LoginState(streak = 1, longestStreak = 1, lastClaimedAtMillis = 1L),
        )
        val persisted = mapper.toPersisted(state)

        val tampered = persisted.dailyLogin?.copy(protectionCharges = 99)

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                dailyLogin = tampered,
            ),
        ).isNull()
    }

    @Test
    fun `unterscheidet nie abgeholt von der Zeit null`() {
        // Beide Faelle duerfen nicht dieselbe Pruefsumme ergeben, sonst liesse
        // sich der eine gegen den anderen tauschen.
        val never = sampleState.copy(login = LoginState(protectionCharges = 1))
        val atZero = sampleState.copy(
            login = LoginState(streak = 1, longestStreak = 1, lastClaimedAtMillis = 0L),
        )

        val neverPersisted = mapper.toPersisted(never)
        val zeroPersisted = mapper.toPersisted(atZero)

        assertThat(neverPersisted.state.signature)
            .isNotEqualTo(zeroPersisted.state.signature)
    }

    // --- Booster ----------------------------------------------------------

    @Test
    fun `stellt laufende Booster wieder her`() {
        val state = sampleState.copy(
            boosters = BoosterState.EMPTY
                .withStarted(BoosterType.DOUBLE_CLICKS, boosterNow)
                .withStarted(BoosterType.LUCKY_HOUR, boosterNow),
        )

        val persisted = mapper.toPersisted(state)
        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            boosters = persisted.boosters,
        )

        assertThat(restored).isEqualTo(state)
    }

    @Test
    fun `liest einen Spielstand ohne Booster unveraendert`() {
        // Vertraeglichkeit mit Version 5: Ohne laufenden Booster ist die
        // Tabelle leer und traegt zur Pruefsumme nichts bei.
        val persisted = mapper.toPersisted(sampleState)

        assertThat(persisted.boosters).isEmpty()
        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                boosters = emptyList(),
            ),
        ).isEqualTo(sampleState)
    }

    @Test
    fun `verwirft einen Spielstand mit verlaengertem Booster`() {
        // Ein von Hand nach hinten gesetzter Endzeitpunkt waere ein
        // dauerhafter Multiplikator zum Nulltarif.
        val state = sampleState.copy(
            boosters = BoosterState.EMPTY.withStarted(BoosterType.DOUBLE_INCOME, boosterNow),
        )
        val persisted = mapper.toPersisted(state)

        val tampered = persisted.boosters.map { row ->
            row.copy(endsAtMillis = row.endsAtMillis + 365L * 24L * 60L * 60L * 1_000L)
        }

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                boosters = tampered,
            ),
        ).isNull()
    }

    @Test
    fun `verwirft einen Spielstand mit erfundenem Booster`() {
        val persisted = mapper.toPersisted(sampleState)

        val tampered = listOf(
            ActiveBoosterEntity(
                boosterId = BoosterType.GOLD_RUSH.id,
                startedAtMillis = boosterNow,
                endsAtMillis = boosterNow + 1_000_000L,
            ),
        )

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                boosters = tampered,
            ),
        ).isNull()
    }

    @Test
    fun `uebergeht Zeilen mit unbekanntem Booster`() {
        val persisted = mapper.toPersisted(sampleState)
        val withUnknown = listOf(
            ActiveBoosterEntity(
                boosterId = "booster_aus_der_zukunft",
                startedAtMillis = boosterNow,
                endsAtMillis = boosterNow + 1_000L,
            ),
        )

        val unsigned = persisted.state.copy(signature = "")
        val resigned = unsigned.copy(
            signature = SaveSignature().sign(
                mapper.canonicalPayload(
                    entity = unsigned,
                    resources = persisted.resources,
                    boosters = withUnknown,
                ),
            ),
        )

        val restored = mapper.toDomain(
            entity = resigned,
            resources = persisted.resources,
            boosters = withUnknown,
        )

        assertThat(restored).isEqualTo(sampleState)
    }

    @Test
    fun `uebergeht Zeilen mit verdrehten Zeitpunkten`() {
        // ActiveBooster wuerde sie zurueckweisen, und eine Ausnahme mitten im
        // Ladevorgang verhinderte den Start der App.
        val persisted = mapper.toPersisted(sampleState)
        val broken = listOf(
            ActiveBoosterEntity(
                boosterId = BoosterType.GOLD_RUSH.id,
                startedAtMillis = boosterNow,
                endsAtMillis = boosterNow - 1L,
            ),
        )

        val unsigned = persisted.state.copy(signature = "")
        val resigned = unsigned.copy(
            signature = SaveSignature().sign(
                mapper.canonicalPayload(
                    entity = unsigned,
                    resources = persisted.resources,
                    boosters = broken,
                ),
            ),
        )

        assertThat(
            mapper.toDomain(
                entity = resigned,
                resources = persisted.resources,
                boosters = broken,
            ),
        ).isEqualTo(sampleState)
    }

    @Test
    fun `ist unabhaengig von der Reihenfolge der Booster-Zeilen`() {
        val state = sampleState.copy(
            boosters = BoosterState.EMPTY
                .withStarted(BoosterType.DOUBLE_CLICKS, boosterNow)
                .withStarted(BoosterType.DOUBLE_INCOME, boosterNow)
                .withStarted(BoosterType.GOLD_RUSH, boosterNow),
        )
        val persisted = mapper.toPersisted(state)

        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            boosters = persisted.boosters.reversed(),
        )

        assertThat(restored).isEqualTo(state)
    }

    // --- Werbe-Wartezeiten -------------------------------------------------

    @Test
    fun `stellt die Wartezeit eines Videos wieder her`() {
        val state = sampleState.copy(
            ads = AdState.EMPTY.withCooldownStarted(
                RewardedAdPlacement.BOOSTER_REWARD,
                boosterNow,
            ),
        )

        val persisted = mapper.toPersisted(state)
        val restored = mapper.toDomain(
            entity = persisted.state,
            resources = persisted.resources,
            adCooldowns = persisted.adCooldowns,
        )

        assertThat(restored).isEqualTo(state)
    }

    @Test
    fun `liest einen Spielstand ohne Wartezeit unveraendert`() {
        // Vertraeglichkeit mit Version 6: Ohne laufende Wartezeit ist die
        // Tabelle leer und traegt zur Pruefsumme nichts bei.
        val persisted = mapper.toPersisted(sampleState)

        assertThat(persisted.adCooldowns).isEmpty()
        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                adCooldowns = emptyList(),
            ),
        ).isEqualTo(sampleState)
    }

    @Test
    fun `verwirft einen Spielstand mit entfernter Wartezeit`() {
        // Die Wartezeit ist das einzige, was die Wirtschaft des Spiels vor
        // unbegrenzter Werbebelohnung schuetzt. Eine geloeschte Zeile waere ein
        // Booster im Minutentakt.
        val state = sampleState.copy(
            ads = AdState.EMPTY.withCooldownStarted(
                RewardedAdPlacement.BOOSTER_REWARD,
                boosterNow,
            ),
        )
        val persisted = mapper.toPersisted(state)

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                adCooldowns = emptyList(),
            ),
        ).isNull()
    }

    @Test
    fun `verwirft einen Spielstand mit vorgezogener Wartezeit`() {
        val state = sampleState.copy(
            ads = AdState.EMPTY.withCooldownStarted(
                RewardedAdPlacement.BOOSTER_REWARD,
                boosterNow,
            ),
        )
        val persisted = mapper.toPersisted(state)

        val tampered = persisted.adCooldowns.map { row -> row.copy(availableAtMillis = 0L) }

        assertThat(
            mapper.toDomain(
                entity = persisted.state,
                resources = persisted.resources,
                adCooldowns = tampered,
            ),
        ).isNull()
    }

    @Test
    fun `uebergeht Zeilen mit unbekannter Werbestelle`() {
        val persisted = mapper.toPersisted(sampleState)
        val withUnknown = listOf(
            AdCooldownEntity(
                placementId = "stelle_aus_der_zukunft",
                availableAtMillis = boosterNow,
            ),
        )

        val unsigned = persisted.state.copy(signature = "")
        val resigned = unsigned.copy(
            signature = SaveSignature().sign(
                mapper.canonicalPayload(
                    entity = unsigned,
                    resources = persisted.resources,
                    adCooldowns = withUnknown,
                ),
            ),
        )

        val restored = mapper.toDomain(
            entity = resigned,
            resources = persisted.resources,
            adCooldowns = withUnknown,
        )

        assertThat(restored).isEqualTo(sampleState)
    }

    @Test
    fun `verwirft einen Spielstand mit veraenderten skalaren Feldern`() {
        val persisted = mapper.toPersisted(sampleState)

        val tampered = persisted.state.copy(prestigeCount = 999)

        assertThat(mapper.toDomain(tampered, persisted.resources)).isNull()
    }

    @Test
    fun `verwirft einen Spielstand mit veraendertem Kontostand`() {
        // Der wichtigste Fall: Die Pruefsumme muss auch die Ressourcentabelle
        // abdecken, sonst laesst sich der Muenzstand direkt in der Datenbank
        // hochsetzen, ohne sie zu beruehren.
        val persisted = mapper.toPersisted(sampleState)

        val tampered = persisted.resources.map { row ->
            if (row.bucket == ResourceBucket.CURRENT.id &&
                row.resourceId == ResourceType.COINS.id
            ) {
                row.copy(exponent = 300)
            } else {
                row
            }
        }

        assertThat(mapper.toDomain(persisted.state, tampered)).isNull()
    }

    @Test
    fun `verwirft einen Spielstand mit zusaetzlich eingefuegter Zeile`() {
        val persisted = mapper.toPersisted(sampleState)

        val tampered = persisted.resources + ResourceEntity(
            bucket = ResourceBucket.CURRENT.id,
            resourceId = ResourceType.PRESTIGE_POINTS.id,
            mantissa = 9.0,
            exponent = 9,
        )

        assertThat(mapper.toDomain(persisted.state, tampered)).isNull()
    }

    @Test
    fun `verwirft einen Spielstand mit entfernter Zeile`() {
        val persisted = mapper.toPersisted(sampleState)

        val tampered = persisted.resources.filterNot { row ->
            row.bucket == ResourceBucket.LIFETIME_SPENT.id
        }

        assertThat(mapper.toDomain(persisted.state, tampered)).isNull()
    }

    @Test
    fun `ist unabhaengig von der Reihenfolge der Zeilen`() {
        // Die Datenbank sichert keine Reihenfolge zu. Waere die Pruefsumme
        // davon abhaengig, schluege sie zufaellig fehl.
        val persisted = mapper.toPersisted(sampleState)

        val shuffled = persisted.resources.reversed()

        assertThat(mapper.toDomain(persisted.state, shuffled)).isEqualTo(sampleState)
    }

    @Test
    fun `uebergeht Zeilen mit unbekannter Ressource`() {
        // Tritt auf, wenn der Spieler nach einem Update auf eine aeltere
        // App-Version zurueckwechselt. Der Spielstand deswegen zu verwerfen
        // waere unverhaeltnismaessig - die uebrigen Werte sind gueltig.
        val persisted = mapper.toPersisted(sampleState)
        val withUnknown = persisted.resources + ResourceEntity(
            bucket = ResourceBucket.CURRENT.id,
            resourceId = "waehrung_aus_der_zukunft",
            mantissa = 5.0,
            exponent = 5,
        )

        // Die Signatur wird ueber den erweiterten Bestand neu gebildet - mit
        // der echten Regel des Mappers, nicht mit einer Kopie davon. Dadurch
        // prueft der Test ausschliesslich das Leseverhalten.
        val unsigned = persisted.state.copy(signature = "")
        val resigned = unsigned.copy(
            signature = SaveSignature().sign(mapper.canonicalPayload(unsigned, withUnknown)),
        )

        val restored = mapper.toDomain(resigned, withUnknown)

        assertThat(restored).isEqualTo(sampleState)
    }
}
