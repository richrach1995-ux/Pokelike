package com.pokelike.idle.data.database.mapper

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.data.database.entity.ResourceBucket
import com.pokelike.idle.data.database.entity.ResourceEntity
import com.pokelike.idle.data.security.SaveSignature
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.GameStatistics
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
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
