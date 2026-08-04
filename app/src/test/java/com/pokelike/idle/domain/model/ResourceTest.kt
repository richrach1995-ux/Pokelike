package com.pokelike.idle.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

class ResourceTypeTest {

    @Test
    fun `haelt die Schluessel eindeutig`() {
        // Ein doppelter Schluessel wuerde beim Laden eines Spielstands die
        // falsche Ressource treffen.
        val ids = ResourceType.entries.map { it.id }

        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `findet Ressourcen ueber ihren Schluessel`() {
        assertThat(ResourceType.fromId("coins")).isEqualTo(ResourceType.COINS)
        assertThat(ResourceType.fromId("diamonds")).isEqualTo(ResourceType.DIAMONDS)
    }

    @Test
    fun `liefert null fuer unbekannte Schluessel`() {
        // Kann auftreten, wenn ein Spielstand aus einer neueren Version stammt.
        // Ein Absturz waere hier die schlechteste aller Reaktionen.
        assertThat(ResourceType.fromId("gibt_es_nicht")).isNull()
    }

    @Test
    fun `setzt nur die weiche Waehrung beim Prestige zurueck`() {
        assertThat(ResourceType.resetOnPrestige).containsExactly(ResourceType.COINS)
    }

    @Test
    fun `behaelt Premiumwaehrung ueber den Prestige-Reset hinweg`() {
        // Gekaufte Diamanten zurueckzusetzen waere ein Totalschaden fuer den
        // Spieler und ein Erstattungsfall im Store.
        assertThat(ResourceType.DIAMONDS.resetOnPrestige).isFalse()
        assertThat(ResourceType.PRESTIGE_POINTS.resetOnPrestige).isFalse()
    }
}

class ResourceBundleTest {

    @Test
    fun `entfernt Nulleintraege`() {
        val bundle = ResourceBundle.of(
            ResourceType.COINS to BigNumber.of(100),
            ResourceType.DIAMONDS to BigNumber.ZERO,
        )

        assertThat(bundle.amounts.keys).containsExactly(ResourceType.COINS)
    }

    @Test
    fun `weist negative Betraege ab`() {
        // Ein Preis mit negativem Betrag wuerde beim Bezahlen Guthaben
        // gutschreiben statt abziehen.
        assertFailsWith<IllegalArgumentException> {
            ResourceBundle.single(ResourceType.COINS, BigNumber.of(-1))
        }
    }

    @Test
    fun `fuehrt zwei Buendel zusammen`() {
        val first = ResourceBundle.of(
            ResourceType.COINS to BigNumber.of(100),
            ResourceType.DIAMONDS to BigNumber.of(5),
        )
        val second = ResourceBundle.single(ResourceType.COINS, BigNumber.of(50))

        val merged = first + second

        assertThat(merged[ResourceType.COINS]).isEqualTo(BigNumber.of(150))
        assertThat(merged[ResourceType.DIAMONDS]).isEqualTo(BigNumber.of(5))
    }

    @Test
    fun `skaliert alle Betraege`() {
        val bundle = ResourceBundle.of(
            ResourceType.COINS to BigNumber.of(100),
            ResourceType.DIAMONDS to BigNumber.of(10),
        )

        val doubled = bundle.scaledBy(2.0)

        assertThat(doubled[ResourceType.COINS]).isEqualTo(BigNumber.of(200))
        assertThat(doubled[ResourceType.DIAMONDS]).isEqualTo(BigNumber.of(20))
    }

    @Test
    fun `ergibt beim Skalieren mit null ein leeres Buendel`() {
        val bundle = ResourceBundle.single(ResourceType.COINS, BigNumber.of(100))

        assertThat(bundle.scaledBy(0.0)).isEqualTo(ResourceBundle.EMPTY)
    }

    @Test
    fun `liefert null fuer nicht enthaltene Ressourcen`() {
        val bundle = ResourceBundle.single(ResourceType.COINS, BigNumber.of(100))

        assertThat(bundle[ResourceType.TICKETS]).isEqualTo(BigNumber.ZERO)
        assertThat(ResourceType.TICKETS in bundle).isFalse()
    }
}

class ResourcePoolTest {

    @Test
    fun `steht fuer ungefuehrte Ressourcen auf null`() {
        assertThat(ResourcePool.EMPTY[ResourceType.COINS]).isEqualTo(BigNumber.ZERO)
        assertThat(ResourcePool.EMPTY.has(ResourceType.COINS)).isFalse()
    }

    @Test
    fun `bucht Gutschriften auf`() {
        val pool = ResourcePool.EMPTY
            .grant(ResourceType.COINS, BigNumber.of(100))
            .grant(ResourceType.COINS, BigNumber.of(50))

        assertThat(pool[ResourceType.COINS]).isEqualTo(BigNumber.of(150))
    }

    @Test
    fun `weist negative Gutschriften ab`() {
        // Sonst liesse sich die Deckungspruefung in spend umgehen.
        assertFailsWith<IllegalArgumentException> {
            ResourcePool.EMPTY.grant(ResourceType.COINS, BigNumber.of(-100))
        }
    }

    @Test
    fun `erkennt ausreichende und unzureichende Deckung`() {
        val pool = ResourcePool.of(ResourceType.COINS to BigNumber.of(100))

        assertThat(pool.canAfford(ResourceBundle.single(ResourceType.COINS, BigNumber.of(100))))
            .isTrue()
        assertThat(pool.canAfford(ResourceBundle.single(ResourceType.COINS, BigNumber.of(101))))
            .isFalse()
    }

    @Test
    fun `bucht einen Preis ab`() {
        val pool = ResourcePool.of(ResourceType.COINS to BigNumber.of(100))

        val remaining = pool.spend(ResourceBundle.single(ResourceType.COINS, BigNumber.of(30)))

        assertThat(remaining).isNotNull()
        assertThat(remaining!![ResourceType.COINS]).isEqualTo(BigNumber.of(70))
    }

    @Test
    fun `erlaubt den Kauf bei exakt passendem Kontostand`() {
        // Die praktische Auswirkung der Rundungskorrektur in BigNumber: Ohne
        // sie bliebe der Kauf-Button genau dann grau, wenn der Spieler gerade
        // genug gespart hat - der aergerlichste denkbare Fehler.
        val pool = ResourcePool.EMPTY
            .grant(ResourceType.COINS, BigNumber.of(100))
            .spend(ResourceBundle.single(ResourceType.COINS, BigNumber.of(30)))!!

        val price = ResourceBundle.single(ResourceType.COINS, BigNumber.of(70))

        assertThat(pool.canAfford(price)).isTrue()
        assertThat(pool.spend(price)).isEqualTo(ResourcePool.EMPTY)
    }

    @Test
    fun `laesst den Pool bei fehlender Deckung unveraendert`() {
        val pool = ResourcePool.of(ResourceType.COINS to BigNumber.of(100))

        val result = pool.spend(ResourceBundle.single(ResourceType.COINS, BigNumber.of(500)))

        assertThat(result).isNull()
        assertThat(pool[ResourceType.COINS]).isEqualTo(BigNumber.of(100))
    }

    @Test
    fun `prueft alle Bestandteile eines mehrteiligen Preises`() {
        val pool = ResourcePool.of(
            ResourceType.COINS to BigNumber.of(100),
            ResourceType.DIAMONDS to BigNumber.of(1),
        )
        val cost = ResourceBundle.of(
            ResourceType.COINS to BigNumber.of(50),
            ResourceType.DIAMONDS to BigNumber.of(5),
        )

        // Muenzen reichen, Diamanten nicht - der ganze Kauf muss scheitern.
        assertThat(pool.spend(cost)).isNull()
    }

    @Test
    fun `ergibt beim vollstaendigen Leerkaufen einen leeren Pool`() {
        val pool = ResourcePool.of(ResourceType.COINS to BigNumber.of(100))

        val remaining = pool.spend(ResourceBundle.single(ResourceType.COINS, BigNumber.of(100)))

        // Ein leergekauftes Konto muss demselben Zustand entsprechen wie ein
        // Konto, das die Ressource nie gefuehrt hat.
        assertThat(remaining).isEqualTo(ResourcePool.EMPTY)
    }

    @Test
    fun `setzt beim Prestige nur die vergaenglichen Ressourcen zurueck`() {
        val pool = ResourcePool.of(
            ResourceType.COINS to BigNumber.of(1.0, 30),
            ResourceType.DIAMONDS to BigNumber.of(500),
            ResourceType.TICKETS to BigNumber.of(3),
        )

        val afterReset = pool.resetForPrestige()

        assertThat(afterReset[ResourceType.COINS]).isEqualTo(BigNumber.ZERO)
        assertThat(afterReset[ResourceType.DIAMONDS]).isEqualTo(BigNumber.of(500))
        assertThat(afterReset[ResourceType.TICKETS]).isEqualTo(BigNumber.of(3))
    }

    @Test
    fun `weist negative Ausgangsbestaende ab`() {
        assertFailsWith<IllegalArgumentException> {
            ResourcePool.of(ResourceType.COINS to BigNumber.of(-1))
        }
    }
}
