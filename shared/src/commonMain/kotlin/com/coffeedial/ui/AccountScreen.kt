package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.auth.AuthProvider
import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.auth.InMemoryAuthRepository
import com.coffeedial.auth.User
import com.coffeedial.sync.SyncEngine
import com.coffeedial.sync.SyncState
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AccountScreen(authRepository: AuthRepository, syncEngine: SyncEngine? = null) {
    val state by authRepository.state.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Mi Cuenta", style = MaterialTheme.typography.headlineSmall)
        }

        when (val current = state) {
            is AuthState.LoggedOut, is AuthState.Error -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Acceso opcional", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tus cafés siguen guardados en este dispositivo. Iniciar sesión " +
                                    "te permite sincronizar en la nube.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (current is AuthState.Error) {
                                Text(current.message, color = MaterialTheme.colorScheme.error)
                            }

                            if (authRepository.isWebPlatform) {
                                Button(
                                    onClick = {
                                        scope.launch { authRepository.signInWithGoogle() }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Continuar con Google")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch { authRepository.signInWithGoogle() }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Continuar con Google")
                                }

                                if (authRepository.supportsBrowserSignIn) {
                                    OutlinedButton(onClick = {
                                        scope.launch { authRepository.signInWithBrowser() }
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Continuar con Google en el navegador")
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch { authRepository.signInWithApple() }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Continuar con Apple")
                                }
                            }
                        }
                    }
                }
            }

            is AuthState.Authenticating -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Iniciando sesión…", style = MaterialTheme.typography.titleMedium)
                            if (authRepository.supportsBrowserSignIn) {
                                OutlinedButton(onClick = {
                                    scope.launch { authRepository.signOut() }
                                }) {
                                    Text("Cancelar")
                                }
                            }
                        }
                    }
                }
            }

            is AuthState.LoggedIn -> {
                val user = current.user
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Sesión Iniciada", style = MaterialTheme.typography.titleMedium)
                            if (!user.displayName.isNullOrBlank()) {
                                Text(
                                    "Nombre: ${user.displayName}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            if (!user.email.isNullOrBlank()) {
                                Text(
                                    "Email: ${user.email}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                "Proveedor: ${if (user.provider == AuthProvider.GOOGLE) "Google" else "Apple"}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            OutlinedButton(
                                onClick = {
                                    scope.launch { authRepository.signOut() }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text("Cerrar sesión")
                            }
                        }
                    }
                }

                item {
                    val syncState by (
                        syncEngine?.state?.collectAsState()
                            ?: remember { mutableStateOf(SyncState.Idle) }
                        )
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Sincronización en la Nube",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Sincronizá con Google sin reemplazar cambios pendientes. " +
                                    "Recuperar combina los datos; si hay un conflicto, ambas " +
                                    "versiones se conservan.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            when (val sync = syncState) {
                                is SyncState.Idle -> {
                                    Text(
                                        "Estado: Listo para sincronizar",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                is SyncState.Syncing -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator()
                                        Text(
                                            "Sincronizando con la nube…",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                is SyncState.Success -> {
                                    Text(
                                        "Última sincronización exitosa (${sync.shotsCount} " +
                                            "shots sincronizados)",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                is SyncState.Error -> {
                                    Text(
                                        "Error al sincronizar: ${sync.message}",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch { syncEngine?.performSync() }
                                    },
                                    enabled = syncEngine != null && syncState !is SyncState.Syncing,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (syncState is SyncState.Syncing) {
                                            "Sincronizando…"
                                        } else {
                                            "Sincronizar"
                                        }
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch { syncEngine?.restoreFromCloud() }
                                    },
                                    enabled = syncEngine != null && syncState !is SyncState.Syncing,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Recuperar y combinar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Cuenta - Sin Sesión", showBackground = true)
@Composable
private fun AccountScreenLoggedOutPreview() {
    MaterialTheme {
        AccountScreen(authRepository = InMemoryAuthRepository(AuthState.LoggedOut))
    }
}

@Preview(name = "Cuenta - Con Sesión", showBackground = true)
@Composable
private fun AccountScreenLoggedInPreview() {
    MaterialTheme {
        AccountScreen(
            authRepository = InMemoryAuthRepository(
                AuthState.LoggedIn(
                    User(
                        id = "1",
                        email = "coffee@example.com",
                        displayName = "Coffee Lover",
                        provider = AuthProvider.GOOGLE
                    )
                )
            )
        )
    }
}
