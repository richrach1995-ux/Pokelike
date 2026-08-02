package com.runeveil.saga.data.repository

import com.runeveil.saga.data.di.IoDispatcher
import com.runeveil.saga.data.database.MonsterDao
import com.runeveil.saga.data.database.toDomain
import com.runeveil.saga.data.database.toEntity
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.player.PlayerProfile
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.MonsterRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed storage of every owned monster.
 *
 * Party membership is expressed by `inParty` + `partyOrder` on the same table
 * as the vault, so moving a monster between the two is a single column update
 * instead of a delete/insert pair (which would lose the row's identity).
 */
@Singleton
class MonsterRepositoryImpl @Inject constructor(
    private val dao: MonsterDao,
    private val content: ContentRepository,
    private val activeSlot: ActiveSlot,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MonsterRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeParty(): Flow<List<MonsterInstance>> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observeParty(slot).map { entities -> entities.mapNotNull { it.toDomainSafe() } }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeVault(): Flow<List<MonsterInstance>> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observeVault(slot).map { entities -> entities.mapNotNull { it.toDomainSafe() } }
        }

    override suspend fun party(): List<MonsterInstance> = withContext(ioDispatcher) {
        dao.party(activeSlot.current).mapNotNull { it.toDomainSafe() }
    }

    override suspend fun vault(): List<MonsterInstance> = withContext(ioDispatcher) {
        dao.vault(activeSlot.current).mapNotNull { it.toDomainSafe() }
    }

    override suspend fun byUid(uid: String): MonsterInstance? = withContext(ioDispatcher) {
        dao.byUid(uid)?.toDomainSafe()
    }

    /**
     * Inserts a newly caught or hatched monster.
     *
     * @param toParty when true the monster joins the party if there is room;
     *   otherwise (or when the party is full) it goes to the vault.
     * @return true when it ended up in the party.
     */
    override suspend fun insert(monster: MonsterInstance, toParty: Boolean): Boolean =
        withContext(ioDispatcher) {
            val slot = activeSlot.current
            val partyHasRoom = dao.partySize(slot) < PlayerProfile.MAX_PARTY_SIZE
            val joinsParty = toParty && partyHasRoom
            val order = if (joinsParty) dao.maxPartyOrder(slot) + 1 else 0
            dao.upsert(monster.toEntity(json, slot, joinsParty, order))
            joinsParty
        }

    override suspend fun update(monster: MonsterInstance) = withContext(ioDispatcher) {
        val existing = dao.byUid(monster.uid)
        dao.upsert(
            monster.toEntity(
                json = json,
                saveSlot = existing?.saveSlot ?: activeSlot.current,
                inParty = existing?.inParty ?: false,
                partyOrder = existing?.partyOrder ?: 0,
                atRoost = existing?.atRoost ?: false,
            ),
        )
    }

    override suspend fun updateAll(monsters: List<MonsterInstance>) = withContext(ioDispatcher) {
        if (monsters.isEmpty()) return@withContext
        val existing = monsters.mapNotNull { dao.byUid(it.uid) }.associateBy { it.uid }
        dao.upsertAll(
            monsters.map { monster ->
                val row = existing[monster.uid]
                monster.toEntity(
                    json = json,
                    saveSlot = row?.saveSlot ?: activeSlot.current,
                    inParty = row?.inParty ?: false,
                    partyOrder = row?.partyOrder ?: 0,
                    atRoost = row?.atRoost ?: false,
                )
            },
        )
    }

    override suspend fun release(uid: String) = withContext(ioDispatcher) {
        dao.delete(uid)
    }

    override suspend fun moveToParty(uid: String): Boolean = withContext(ioDispatcher) {
        val slot = activeSlot.current
        if (dao.partySize(slot) >= PlayerProfile.MAX_PARTY_SIZE) return@withContext false
        dao.setPartyState(uid, inParty = true, order = dao.maxPartyOrder(slot) + 1)
        true
    }

    /** The last healthy party member can never be stored away. */
    override suspend fun moveToVault(uid: String): Boolean = withContext(ioDispatcher) {
        val slot = activeSlot.current
        val party = dao.party(slot)
        val healthy = party.count { !it.isEgg && it.currentHp > 0 }
        val target = party.firstOrNull { it.uid == uid } ?: return@withContext false
        if (healthy <= 1 && target.currentHp > 0 && !target.isEgg) return@withContext false
        dao.setPartyState(uid, inParty = false, order = 0)
        true
    }

    override suspend fun reorderParty(uids: List<String>) = withContext(ioDispatcher) {
        dao.reorderParty(activeSlot.current, uids)
    }

    override suspend fun nextUid(): String = UUID.randomUUID().toString()

    /**
     * Restores every party member to full HP, clears status conditions and
     * refills PP — the inn / healer service.
     */
    override suspend fun healParty() = withContext(ioDispatcher) {
        val slot = activeSlot.current
        dao.clearStatusForParty(slot)
        val healed = dao.party(slot)
            .mapNotNull { it.toDomainSafe() }
            .map { it.fullyRestored() }
        dao.upsertAll(
            healed.mapIndexed { index, monster ->
                monster.toEntity(json, slot, inParty = true, partyOrder = index)
            },
        )
    }

    /** Monsters currently left at the breeding roost. */
    suspend fun atRoost(): List<MonsterInstance> = withContext(ioDispatcher) {
        dao.atRoost(activeSlot.current).mapNotNull { it.toDomainSafe() }
    }

    suspend fun setRoostState(uid: String, atRoost: Boolean) = withContext(ioDispatcher) {
        dao.setRoostState(uid, atRoost)
    }

    private fun com.runeveil.saga.data.database.MonsterEntity.toDomainSafe(): MonsterInstance? =
        toDomain(json) { content.species(it) }
}
