package com.coffeedial

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.coffeedial.ui.App

class CoffeeDialApplication : Application() {
    val repository by lazy { createRepository(this) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App((application as CoffeeDialApplication).repository) }
    }
}
