package com.runeveil.saga.di

/**
 * The app module currently owns no bindings of its own.
 *
 * Everything data-related is provided by `com.runeveil.saga.data.di`, and
 * [com.runeveil.saga.audio.AudioEngine] is constructor-injected with
 * `@ApplicationContext` / `@ApplicationScope`, so adding a provider here would
 * create a duplicate binding. The file is kept as the documented home for
 * future UI-layer bindings.
 */
