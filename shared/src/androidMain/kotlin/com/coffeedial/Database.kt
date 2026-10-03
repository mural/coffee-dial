package com.coffeedial

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.coffeedial.data.ShotRepository
import com.coffeedial.database.CoffeeDatabase

fun createRepository(context: Context): ShotRepository = ShotRepository(
    AndroidSqliteDriver(CoffeeDatabase.Schema, context.applicationContext, "coffee-dial.db")
)
