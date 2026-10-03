package com.coffeedial

import androidx.compose.ui.window.ComposeUIViewController
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.coffeedial.data.ShotRepository
import com.coffeedial.database.CoffeeDatabase
import com.coffeedial.ui.App

private val repository by lazy {
    ShotRepository(NativeSqliteDriver(CoffeeDatabase.Schema, "coffee-dial.db"))
}

fun mainViewController() = ComposeUIViewController { App(repository) }
