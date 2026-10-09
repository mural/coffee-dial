package com.coffeedial.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Bean
import com.coffeedial.domain.BeanDraft
import com.coffeedial.photos.BeanPhoto
import com.coffeedial.photos.PhotoPicker
import com.coffeedial.photos.decodePhoto
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BeansScreen(
    beans: List<Bean>,
    canAddPhoto: Boolean = false,
    photos: PhotoPicker = remember {
        PhotoPicker()
    },
    saving: Boolean,
    saveError: String?,
    onSaveBean: (String?, BeanDraft, () -> Unit) -> Unit,
    onDeleteBean: (String) -> Unit
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var roaster by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable(
        stateSaver = Saver<BeanPhoto?, String>(
            save = { it?.let { value -> Json.encodeToString(value) } ?: "" },
            restore = { value -> if (value.isEmpty()) null else Json.decodeFromString(value) }
        )
    ) {
        mutableStateOf<BeanPhoto?>(null)
    }
    var enlarged by remember {
        mutableStateOf<BeanPhoto?>(null)
    }
    DisposableEffect(photos) { onDispose { photos.cancel() } }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun closeEditor() {
        focusManager.clearFocus(force = true)
        photos.cancel()
        editorOpen = false
    }

    val draft = BeanDraft(name, roaster, photo)
    val errors = if (submitted) draft.errors() else emptyMap()

    fun open(bean: Bean?) {
        editingId = bean?.id
        name = bean?.name.orEmpty()
        roaster = bean?.roaster.orEmpty()
        photo = bean?.photo
        submitted = false
        editorOpen = true
    }

    if (!editorOpen) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Cafés guardados", style = MaterialTheme.typography.headlineSmall)
                Text("Eliminar un café no borra los shots registrados.")
                Button(onClick = { open(null) }, enabled = !saving) { Text("Agregar café") }
            }
            if (beans.isEmpty()) item { Text("Todavía no agregaste ningún café.") }
            items(beans, key = { it.id }) { bean ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BeanPhotoImage(bean.photo, 88) { enlarged = bean.photo }
                        Text(bean.name, style = MaterialTheme.typography.titleMedium)
                        if (bean.roaster.isNotBlank()) {
                            Text("Tostador: ${bean.roaster}")
                        }
                        FlowRow {
                            TextButton(onClick = {
                                open(bean)
                            }, enabled = !saving) { Text("Editar") }
                            TextButton(onClick = {
                                onDeleteBean(bean.id)
                            }, enabled = !saving) {
                                Text("Eliminar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            if (saveError != null && !editorOpen) {
                item {
                    Text(saveError, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (editorOpen) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (editingId == null) "Nuevo café" else "Editar café",
                style = MaterialTheme.typography.headlineSmall
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BeanPhotoImage(photo, 160) { enlarged = photo }
                FlowRow {
                    TextButton(enabled = canAddPhoto && !saving && !photos.busy, onClick = {
                        photos.pick {
                            photo =
                                it
                        }
                    }) {
                        Text(
                            if (photos.busy) {
                                "Preparando foto…"
                            } else if (photo ==
                                null
                            ) {
                                "Agregar foto"
                            } else {
                                "Cambiar foto"
                            }
                        )
                    }
                    if (photo !=
                        null
                    ) {
                        TextButton(enabled = !saving && !photos.busy, onClick = {
                            photo = null
                        }) { Text("Quitar foto") }
                    }
                }
                if (!canAddPhoto) Text("Iniciá sesión en Cuenta para agregar fotos.")
                photos.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del café") },
                    enabled = !saving,
                    singleLine = true,
                    isError = errors["name"] != null,
                    supportingText = errors["name"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = roaster,
                    onValueChange = { roaster = it },
                    label = { Text("Tostador (opcional)") },
                    enabled = !saving,
                    singleLine = true
                )
                if (editingId != null) {
                    Text(
                        "Los shots anteriores conservan la información que tenían al registrarlos."
                    )
                }
                if (submitted && saveError != null) {
                    Text(saveError, color = MaterialTheme.colorScheme.error)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !saving && !photos.busy, onClick = {
                    submitted = true
                    if (draft.errors().isEmpty()) onSaveBean(editingId, draft) { closeEditor() }
                }) { Text(if (saving) "Guardando…" else "Guardar") }
                TextButton(onClick = { closeEditor() }, enabled = !saving) { Text("Cancelar") }
            }
        }
    }
    enlarged?.let { image ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { enlarged = null }) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    BeanPhotoImage(image, 320) { enlarged = null }
                    TextButton(onClick = { enlarged = null }) { Text("Cerrar") }
                }
            }
        }
    }
}

@Composable
private fun BeanPhotoImage(photo: BeanPhoto?, side: Int, onClick: () -> Unit) {
    val bitmap = remember(photo) {
        runCatching {
            photo?.validate()
            photo?.jpeg?.let {
                decodePhoto(Base64.decode(it))
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap,
            "Foto del café",
            Modifier.fillMaxWidth().then(Modifier.height(side.dp)).clickable(onClick = onClick),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BeansPreview() {
    MaterialTheme {
        BeansScreen(
            beans = listOf(mockBean, Bean("2", "Colombia Excelso", "Puerto Blest")),
            saving = false,
            saveError = null,
            onSaveBean = { _, _, _ -> },
            onDeleteBean = {}
        )
    }
}
