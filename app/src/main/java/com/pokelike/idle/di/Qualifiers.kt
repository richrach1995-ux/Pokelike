package com.pokelike.idle.di

import javax.inject.Qualifier

/**
 * Kennzeichnet den prozessweiten [kotlinx.coroutines.CoroutineScope].
 *
 * Ein eigener Qualifier ist noetig, weil sonst nicht unterscheidbar waere,
 * welcher der injizierten Scopes gemeint ist, sobald weitere hinzukommen.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
