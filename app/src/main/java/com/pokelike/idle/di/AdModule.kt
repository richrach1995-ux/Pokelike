package com.pokelike.idle.di

import com.pokelike.idle.ads.FakeRewardedAdSource
import com.pokelike.idle.ads.RewardedAdSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindet die Quelle der Belohnungsvideos.
 *
 * Solange kein Werbe-SDK angebunden ist, gibt es genau eine Umsetzung, und die
 * Bindung ist entsprechend schlicht. Bewusst kein Schalter auf einen noch
 * nicht vorhandenen Anbieter: Ein `if`, dessen beide Zweige dasselbe liefern,
 * waere eine Entscheidung, die es nicht gibt.
 *
 * Beim Anbinden von AdMob wird aus dieser Bindung ein
 * [dagger.Provides]-Verfahren, das anhand eines `BuildConfig`-Schalters
 * zwischen Nachbildung und echtem SDK waehlt. Bis dahin steht hier der Weg,
 * nicht seine Ankuendigung.
 *
 * Der Rest der App kennt ausschliesslich [RewardedAdSource]. Ein Wechsel des
 * Werbevermittlers beschraenkt sich damit auf dieses Modul und eine neue
 * Umsetzung der Schnittstelle.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdModule {

    @Binds
    @Singleton
    abstract fun bindRewardedAdSource(
        implementation: FakeRewardedAdSource,
    ): RewardedAdSource
}
