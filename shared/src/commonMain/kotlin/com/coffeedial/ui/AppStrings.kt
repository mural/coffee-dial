package com.coffeedial.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeedial.domain.AppLanguage
import com.coffeedial.domain.MilkFoam
import com.coffeedial.domain.RoastLevel

val LocalStrings = staticCompositionLocalOf<AppStrings> { SpanishStrings }

interface AppStrings : RemainingCopy {
    val languageName: String
    val languageSystem: String
    val languageSpanish: String
    val languageEnglish: String

    // Navigation & Tabs
    val navBrew: String
    val navShots: String
    val navEquipment: String
    val navAccount: String
    val navRecipes: String
    val navAnalysis: String
    val navDialIn: String

    // Actions
    val actionSave: String
    val actionCancel: String
    val actionDelete: String
    val actionEdit: String
    val actionBack: String
    val actionRepeat: String
    val actionShare: String
    val actionConfirm: String
    val actionRetry: String
    val couldNotReadHistory: String
    val styleLabel: String
    val totalLabel: String
    val favoriteLabel: String
    fun preinfusionText(preinfusion: String, extraction: String): String
    fun foamLabelText(foamLabel: String): String
    fun cupLabelText(cupName: String): String

    // Theme
    val themeLight: String
    val themeDark: String
    val themeSystem: String

    // Home
    val homeGreeting: String
    fun greeting(userName: String): String
    fun historyHeader(count: Int): String
    fun shotsSavedOnDevice(count: Int): String
    val homeSyncing: String
    val homeBrewCoffee: String
    val homeViewRecipes: String
    val homeRecentShots: String
    val homeViewAllShots: String
    val homeNoShotsYet: String
    val homeLogFirstShot: String
    val logAShot: String
    val nextExtraction: String
    val logShotAndFlavor: String
    val yourWay: String
    val repeatOrNew: String
    val myRecipesRepeat: String
    val startShotTimer: String
    val ratiosAndStyles: String
    fun viewHistoryCount(count: Int): String
    val filterAll: String
    val filterNoStyle: String
    fun shotsFilterCount(filtered: Int, total: Int): String
    val noShotsWithStyle: String

    // Shot Fields & Jargon
    val dose: String
    val yield: String
    val ratio: String
    val grind: String
    val grindFine: String
    val grindMedium: String
    val grindCoarse: String
    val time: String
    val firstDrop: String
    val addedWater: String
    val milk: String
    val milkFoam: String
    val milkFoamOptional: String
    val actionRemove: String
    val notRecorded: String
    val milkFoamNote: String
    val rating: String
    val notes: String
    val tastingNotes: String

    // Equipment
    val beans: String
    val roaster: String
    val roastDate: String
    val origin: String
    val process: String
    val bagWeight: String
    val machine: String
    val grinder: String
    val cup: String
    val capacity: String
    val emptyWeight: String
    val defaultLabel: String
    val setDefault: String
    val repeatWithTimer: String
    val viewOrAdjust: String
    val deleteRecipeQuestion: String
    val shotsPreservedNote: String
    val saveRecipe: String

    val rowDose: String
    val rowYield: String
    val rowRatio: String
    val rowTime: String
    val rowFirstDrop: String
    val rowTemperature: String
    val rowAddedWater: String
    val rowMilk: String
    val rowMilkFoam: String

    val coffeeInNumbers: String
    val averageRating: String
    val differentCoffees: String
    val totalCoffeeUsed: String
    val averageRecipe: String
    val averageRatio: String
    val shotBreakdownTitle: String
    val topCoffees: String

    val trackPillarDesc: String
    val improvePillarDesc: String
    val compareAttempts: String
    val yourRecords: String
    val exploreResults: String

    val myBeans: String
    val addBean: String
    val editBean: String
    val myMachines: String
    val addMachine: String
    val editMachine: String
    val myGrinders: String
    val addGrinder: String
    val editGrinder: String
    val myCups: String
    val addCup: String
    val editCup: String
    val nameField: String

    val newShotTitle: String
    val editShotTitle: String
    val saveShotLabel: String
    val manageBeans: String
    val manageMachines: String
    val manageGrinders: String
    val manageCups: String

    val savedBeans: String
    val deleteBeanNote: String
    val noBeansYet: String
    fun roasterLabelText(roaster: String): String
    fun bagSizeLabelText(grams: String): String
    val useByDefault: String
    val nameCoffee: String
    val roasterOptional: String
    val originOptional: String
    val processOptional: String
    val bagWeightOptional: String
    val addPhoto: String
    val changePhoto: String
    val removePhoto: String
    val preparingPhoto: String
    val signInForPhotos: String
    val savedMachines: String
    val noMachinesYet: String
    val savedGrinders: String
    val noGrindersYet: String
    val savedCups: String
    val noCupsYet: String

    // Milk Foam levels
    val foamNone: String
    val foamLow: String
    val foamMedium: String
    val foamHigh: String

    // Cup Profiles
    val cupSmall: String
    val cupMedium: String
    val cupLarge: String

    // Roast Levels
    val roastLight: String
    val roastMediumLight: String
    val roastMedium: String
    val roastMediumDark: String
    val roastDark: String
    val roastUnknown: String

    val methodFilter: String
    val methodMoka: String
    val methodOther: String
    val goalStronger: String
    val goalBalanced: String
    val goalMilder: String
    val basisEspresso: String
    val basisFinishedDrink: String

    // Dial-in Assistant
    val dialInTitle: String
    val roastLevel: String
    val brewMethod: String
    val strengthGoal: String
    val diagnosis: String
    val suggestions: String
    val grindFiner: String
    val grindCoarser: String

    // Sensory / Taste Profile
    val flavorProfile: String
    val body: String
    val acidity: String
    val bitterness: String
    val sweetness: String
    val impressionTooSour: String
    val impressionTooBitter: String
    val impressionWeak: String
    val impressionBalanced: String
    val impressionTooStrong: String
    val tasteOptionalNote: String
    val removeTasteRating: String
    val tasteScaleNote: String
    val bodyLight: String
    val bodyFull: String
    val levelLow: String
    val levelHigh: String
    val levelLowMasculine: String
    val levelHighMasculine: String
    val actionRegister: String

    // Account & Settings
    val myAccount: String
    val appearance: String
    val language: String
    val savedOnDeviceOnly: String
    val cloudSync: String
    val syncNow: String
    val restoreAndMerge: String
    val conflicts: String
    val backupExportImport: String
    val signInGoogle: String
    val signInApple: String
    val signOut: String
    val backupExportImportText: String
    val optionalAccessTitle: String
    val optionalAccessDesc: String
    val continueWithGoogleBrowser: String
    val signingInTitle: String
    val adminPanelButton: String
    val sessionStartedTitle: String
    fun userNameText(name: String): String
    fun userEmailText(email: String): String
    fun userProviderText(provider: String): String
    val cloudSyncTitle: String
    val cloudSyncDesc: String
    val syncReadyState: String
    val syncingState: String
    fun syncSuccessState(count: Int): String
    fun syncErrorState(message: String): String
    fun syncConflictState(count: Int): String
    val reviewConflictsButton: String
    val importAndOverwriteCloudButton: String
    val overwriteCloudButton: String

    // Drink Cup Chart
    val inTheCup: String
    val coffeeWaterMilkRatio: String
    val totalRatioPrefix: String
    val coffeeFromDose: String
    val legendCoffee: String
    val legendWater: String
    val legendMilk: String
    val legendFoam: String
    fun foamIllustrativeText(foamLabel: String, interactive: Boolean): String
    fun foamMilkRatioText(ratio: String): String
    fun foamBreakdownText(foamMl: String, liquidMl: String): String
    fun coffeeFromDoseText(doseGrams: String): String
    fun exceedsCapacityText(capacityCm3: String): String
    fun cupCapacityText(capacityCm3: String): String

    // PrepareCoffee
    val prepareTitle: String
    fun maxSizeNotice(maxSize: String): String
    val logWithMeasurements: String
    fun sizeLabel(size: String): String
    fun foamHowToText(drinkName: String, foam: MilkFoam): String

    companion object {
        fun forLanguage(language: AppLanguage): AppStrings = when (language) {
            AppLanguage.ENGLISH -> EnglishStrings
            else -> SpanishStrings
        }
    }
}

object SpanishStrings : AppStrings {
    override val languageName = "Español"
    override val languageSystem = "Sistema"
    override val languageSpanish = "Español"
    override val languageEnglish = "English"

    override val navBrew = "Preparar"
    override val navShots = "Diario"
    override val navEquipment = "Equipamiento"
    override val navAccount = "Cuenta"
    override val navRecipes = "Recetas"
    override val navAnalysis = "Análisis"
    override val navDialIn = "Dial-in"

    override val actionSave = "Guardar"
    override val actionCancel = "Cancelar"
    override val actionDelete = "Eliminar"
    override val actionEdit = "Editar"
    override val actionBack = "Volver"
    override val actionRepeat = "Repetir shot"
    override val actionShare = "Compartir shot"
    override val actionConfirm = "Confirmar"
    override val actionRetry = "Reintentar"
    override val couldNotReadHistory = "No pudimos leer tu historial."
    override val styleLabel = "Estilo"
    override val totalLabel = "Total"
    override val favoriteLabel = "Favorito"
    override fun preinfusionText(preinfusion: String, extraction: String) =
        "Pre-infusión: $preinfusion s · Extracción: $extraction s"
    override fun foamLabelText(foamLabel: String) = "Espuma: $foamLabel"
    override fun cupLabelText(cupName: String) = "Taza: $cupName"

    override val themeLight = "Claro"
    override val themeDark = "Oscuro"
    override val themeSystem = "Sistema"

    override val homeGreeting = "¡Hola!"
    override fun greeting(userName: String) = "¡Hola, $userName!"
    override fun historyHeader(count: Int) = "Historial ($count)"
    override fun shotsSavedOnDevice(count: Int) = "$count shots · guardados en este dispositivo"
    override val homeSyncing = "Sincronizando con la nube…"
    override val homeBrewCoffee = "Preparar café"
    override val homeViewRecipes = "Ver todas las recetas"
    override val homeRecentShots = "Últimos shots"
    override val homeViewAllShots = "Ver todos los shots"
    override val homeNoShotsYet = "No tenés shots registrados todavía."
    override val homeLogFirstShot = "Registrar primer shot"
    override val logAShot = "Registrar un shot"
    override val nextExtraction = "Tu próxima extracción"
    override val logShotAndFlavor = "Registrá las medidas y el sabor de tu café."
    override val yourWay = "A tu manera"
    override val repeatOrNew = "Repetí una receta o medí una nueva extracción."
    override val myRecipesRepeat = "Mis recetas · repetir café"
    override val startShotTimer = "Iniciar Shot Timer"
    override val ratiosAndStyles = "Ratios y estilos de café"
    override fun viewHistoryCount(count: Int) = "Ver historial ($count)"
    override val filterAll = "Todos"
    override val filterNoStyle = "Sin estilo"
    override fun shotsFilterCount(filtered: Int, total: Int) = "$filtered de $total shots"
    override val noShotsWithStyle = "No hay shots con este estilo."

    override val dose = "Dosis"
    override val yield = "Extracción"
    override val ratio = "Ratio"
    override val grind = "Molienda"
    override val grindFine = "Fino"
    override val grindMedium = "Medio"
    override val grindCoarse = "Grueso"
    override val time = "Tiempo total"
    override val firstDrop = "Primera gota"
    override val addedWater = "Agua extra"
    override val milk = "Leche"
    override val milkFoam = "Espuma de leche"
    override val milkFoamOptional = "Espuma de leche (opcional)"
    override val actionRemove = "Quitar"
    override val notRecorded = "Sin registrar"
    override val milkFoamNote = "Describe la textura; no suma volumen al total."
    override val rating = "Calificación"
    override val notes = "Notas"
    override val tastingNotes = "Notas de cata"

    override val beans = "Granos"
    override val roaster = "Tostador"
    override val roastDate = "Fecha de tueste"
    override val origin = "Origen"
    override val process = "Proceso"
    override val bagWeight = "Peso de bolsa"
    override val machine = "Máquina"
    override val grinder = "Molinillo"
    override val cup = "Taza"
    override val capacity = "Capacidad"
    override val emptyWeight = "Peso vacío"
    override val defaultLabel = "Predeterminado"
    override val setDefault = "Establecer como predeterminado"
    override val repeatWithTimer = "Repetir con timer"
    override val viewOrAdjust = "Ver / ajustar"
    override val deleteRecipeQuestion = "¿Eliminar receta?"
    override val shotsPreservedNote = "Los shots registrados se conservan."
    override val saveRecipe = "Guardar receta"

    override val rowDose = "Dosis · g"
    override val rowYield = "Output · g"
    override val rowRatio = "Ratio extracción"
    override val rowTime = "Tiempo · s"
    override val rowFirstDrop = "Primera gota · s"
    override val rowTemperature = "Temperatura · °C"
    override val rowAddedWater = "Agua extra · ml"
    override val rowMilk = "Leche · ml"
    override val rowMilkFoam = "Espuma"

    override val coffeeInNumbers = "Tu café en números"
    override val averageRating = "Puntuación media"
    override val differentCoffees = "Cafés diferentes"
    override val totalCoffeeUsed = "Café utilizado"
    override val averageRecipe = "Receta promedio"
    override val averageRatio = "Ratio medio por shot"
    override val shotBreakdownTitle = "Cómo salieron tus shots"
    override val topCoffees = "Cafés más usados"
    override val trackPillarDesc = "Tu café, tu equipo y cada extracción."
    override val improvePillarDesc = "Compará resultados y encontrá tu próxima receta."
    override val compareAttempts = "Comparar intentos y favoritos"
    override val yourRecords = "Tus registros"
    override val exploreResults = "Explorá tus resultados"

    override val myBeans = "Mis Granos"
    override val addBean = "Agregar café"
    override val editBean = "Editar café"
    override val myMachines = "Mis Máquinas"
    override val addMachine = "Agregar máquina"
    override val editMachine = "Editar máquina"
    override val myGrinders = "Mis Molinillos"
    override val addGrinder = "Agregar molinillo"
    override val editGrinder = "Editar molinillo"
    override val myCups = "Mis Tazas"
    override val addCup = "Agregar taza"
    override val editCup = "Editar taza"
    override val nameField = "Nombre"

    override val newShotTitle = "Nuevo shot"
    override val editShotTitle = "Editar shot"
    override val saveShotLabel = "Guardar shot"
    override val manageBeans = "Gestionar cafés"
    override val manageMachines = "Gestionar máquinas"
    override val manageGrinders = "Gestionar molinillos"
    override val manageCups = "Gestionar tazas"

    override val savedBeans = "Cafés guardados"
    override val deleteBeanNote = "Eliminar un café no borra los shots registrados."
    override val noBeansYet = "Todavía no agregaste ningún café."
    override fun roasterLabelText(roaster: String) = "Tostador: $roaster"
    override fun bagSizeLabelText(grams: String) = "Bolsa: $grams g"
    override val useByDefault = "Usar por defecto"
    override val nameCoffee = "Nombre del café"
    override val roasterOptional = "Tostador (opcional)"
    override val originOptional = "Origen (opcional)"
    override val processOptional = "Proceso (opcional)"
    override val bagWeightOptional = "Peso de bolsa en g (opcional)"
    override val addPhoto = "Agregar foto"
    override val changePhoto = "Cambiar foto"
    override val removePhoto = "Quitar foto"
    override val preparingPhoto = "Preparando foto…"
    override val signInForPhotos = "Iniciá sesión en Cuenta para agregar fotos."
    override val savedMachines = "Máquinas guardadas"
    override val noMachinesYet = "Todavía no agregaste ninguna máquina."
    override val savedGrinders = "Molinillos guardados"
    override val noGrindersYet = "Todavía no agregaste ningún molinillo."
    override val savedCups = "Tazas guardadas"
    override val noCupsYet = "Todavía no agregaste ninguna taza."

    override val foamNone = "Sin espuma"
    override val foamLow = "Poca"
    override val foamMedium = "Media"
    override val foamHigh = "Abundante"

    override val cupSmall = "Chica"
    override val cupMedium = "Media"
    override val cupLarge = "Grande"

    override val roastLight = "Claro"
    override val roastMediumLight = "Medio-claro"
    override val roastMedium = "Medio"
    override val roastMediumDark = "Medio-oscuro"
    override val roastDark = "Oscuro"
    override val roastUnknown = "Desconocido"

    override val methodFilter = "Filtro"
    override val methodMoka = "Moka"
    override val methodOther = "Otro"
    override val goalStronger = "Más intenso"
    override val goalBalanced = "Mantener equilibrio"
    override val goalMilder = "Más suave"
    override val basisEspresso = "Espresso solo"
    override val basisFinishedDrink = "Bebida con agua/leche"

    override val dialInTitle = "Asistente de Dial-in"
    override val roastLevel = "Nivel de tueste"
    override val brewMethod = "Método"
    override val strengthGoal = "Objetivo de extracción"
    override val diagnosis = "Diagnóstico"
    override val suggestions = "Sugerencias"
    override val grindFiner = "Molienda más fina"
    override val grindCoarser = "Molienda más gruesa"

    override val flavorProfile = "Perfil de sabor"
    override val body = "Cuerpo"
    override val acidity = "Acidez"
    override val bitterness = "Amargor"
    override val sweetness = "Dulzor"
    override val impressionTooSour = "Demasiado ácido"
    override val impressionTooBitter = "Demasiado amargo"
    override val impressionWeak = "Débil"
    override val impressionBalanced = "Equilibrado"
    override val impressionTooStrong = "Demasiado intenso"
    override val tasteOptionalNote = "Opcional · ¿Cómo lo sentiste?"
    override val removeTasteRating = "Quitar valoración de sabor"
    override val tasteScaleNote = "1 a 5 · Mové solo los controles que quieras registrar."
    override val bodyLight = "Ligero"
    override val bodyFull = "Con mucho cuerpo"
    override val levelLow = "Baja"
    override val levelHigh = "Alta"
    override val levelLowMasculine = "Bajo"
    override val levelHighMasculine = "Alto"
    override val actionRegister = "Registrar"

    override val myAccount = "Mi Cuenta"
    override val appearance = "Apariencia"
    override val language = "Idioma"
    override val savedOnDeviceOnly = "Se guarda solo en este dispositivo."
    override val cloudSync = "Sincronización en la Nube"
    override val syncNow = "Sincronizar"
    override val restoreAndMerge = "Recuperar y combinar"
    override val conflicts = "Conflictos"
    override val backupExportImport = "Backup: exportar / importar"
    override val signInGoogle = "Continuar con Google"
    override val signInApple = "Continuar con Apple"
    override val signOut = "Cerrar sesión"
    override val backupExportImportText = "Backup: exportar / importar"
    override val optionalAccessTitle = "Acceso opcional"
    override val optionalAccessDesc =
        "Tus cafés siguen guardados en este dispositivo. Iniciar sesión te permite sincronizar en la nube."
    override val continueWithGoogleBrowser = "Continuar con Google en el navegador"
    override val signingInTitle = "Iniciando sesión…"
    override val adminPanelButton = "Panel Admin"
    override val sessionStartedTitle = "Sesión Iniciada"
    override fun userNameText(name: String) = "Nombre: $name"
    override fun userEmailText(email: String) = "Email: $email"
    override fun userProviderText(provider: String) = "Proveedor: $provider"
    override val cloudSyncTitle = "Sincronización en la Nube"
    override val cloudSyncDesc =
        "Sincronizá con Google sin reemplazar cambios pendientes. Recuperar combina los datos; si hay un conflicto, ambas versiones se conservan."
    override val syncReadyState = "Estado: Listo para sincronizar"
    override val syncingState = "Sincronizando con la nube…"
    override fun syncSuccessState(count: Int) =
        "Última sincronización exitosa ($count shots sincronizados)"
    override fun syncErrorState(message: String) =
        "Error al sincronizar: ${userMessage(message)}"
    override fun syncConflictState(count: Int) =
        "Hay $count registros con cambios distintos. Elegí qué copia conservar. No se reemplazó ninguna."
    override val reviewConflictsButton = "Revisar conflictos"
    override val importAndOverwriteCloudButton = "Importar y sobreescribir desde la nube"
    override val overwriteCloudButton = "Sobreescribir datos en la nube"

    override val inTheCup = "En la taza"
    override val coffeeWaterMilkRatio = "Café : agua : leche"
    override val totalRatioPrefix = "Ratio total ≈ 1:"
    override val coffeeFromDose = "El café de la taza sale de"
    override val legendCoffee = "Café"
    override val legendWater = "Agua"
    override val legendMilk = "Leche"
    override val legendFoam = "Espuma"

    override fun foamIllustrativeText(foamLabel: String, interactive: Boolean) =
        if (interactive) "Espuma: $foamLabel" else "Espuma: $foamLabel · textura ilustrativa"

    override fun foamMilkRatioText(ratio: String) = "Espuma : leche ≈ $ratio"

    override fun foamBreakdownText(foamMl: String, liquidMl: String) =
        "De la leche, unos $foamMl ml son espuma y $liquidMl ml líquidos."

    override fun coffeeFromDoseText(doseGrams: String) =
        "El café de la taza sale de $doseGrams g de dosis."

    override fun exceedsCapacityText(capacityCm3: String) =
        "Supera los $capacityCm3 cm³ de la taza."

    override fun cupCapacityText(capacityCm3: String) =
        "Taza de $capacityCm3 cm³."

    override val prepareTitle = "Preparar un café"

    override fun maxSizeNotice(maxSize: String) =
        "El tamaño máximo para este café es $maxSize×."

    override val logWithMeasurements = "Registrar con estas medidas"

    override fun sizeLabel(size: String) = "Tamaño $size×"

    override fun foamHowToText(drinkName: String, foam: MilkFoam): String = when {
        drinkName == "Macchiato" && foam == MilkFoam.HIGH ->
            "Una cucharada de espuma firme sobre el espresso."

        foam == MilkFoam.HIGH ->
            "Estirá la leche hasta una capa gruesa, de 1 a 2 cm."

        foam == MilkFoam.MEDIUM ->
            "Estirá hasta una capa fina, de 0,5 a 1 cm, e integrala al verter."

        foam == MilkFoam.LOW ->
            "Vapor breve: leche sedosa, casi sin capa arriba."

        else -> "Calentá la leche sin estirar el vapor."
    }

    override val tipsTitle = "Tips y estilos"
    override val tabStyles = "Estilos"
    override val tabTimes = "Tiempos"
    override val tabGrains = "Granos"
    override val tipsSplitTitle = "La extracción y la bebida son dos estilos distintos."
    override val tipsSplitBody =
        "El ratio de extracción compara la dosis con el café de la taza. El " +
        "agua y la leche se agregan después y no lo cambian."
    override val extractionHeading = "Extracción"
    override val drinksHeading = "Bebidas"
    override val drinksIntro =
        "Las mismas de Preparar un café, en tamaño 1×. El 1 de café : agua : " +
        "leche es el café de la taza, no la dosis."
    override val otherMixesHeading = "Otras mezclas"
    override val otherMixesBody =
        "Sirven para reconocer un shot. En la app el agua y la leche se " +
        "anotan en ml; acá 1 ml equivale a 1 g sobre 36 g de café."
    override val macchiatoFootnote =
        "Macchiato también puede ir de 0,25 a 0,5 partes de leche. Latte " +
        "macchiato es otra bebida. Agregar agua después de extraer no " +
        "convierte un espresso en lungo."
    override val timeHeading = "Tiempo de extracción"
    override val timeIntro =
        "Se mide desde que entra el agua hasta cortar el flujo. Es una pista, " +
        "no una regla: el sabor manda."
    override val grainsHeading = "Granos"
    override val grainsIntro =
        "Variedad y origen cambian el aroma, la acidez y el cuerpo. Son " +
        "puntos de partida, no una ficha del café que compraste."
    override val shotTimerTitle = "Shot Timer"
    override val shotTimerHint =
        "Iniciá al activar la extracción y marcá cuándo aparece la primera " +
        "gota."
    override val extractionRunning = "Extracción en curso"
    override val markFirstDrop = "Marcar primera gota"
    override val removeMark = "Quitar marca"
    override val timerMaxReached = "Se alcanzó el máximo de 10 minutos."
    override val logTheExtraction = "Registrar la extracción"
    override val useTheseTimes = "Usar estos tiempos"
    override val logShotWithTimes = "Registrar shot con estos tiempos"
    override val timerIncludesFirstDrop =
        "El total se mide desde el inicio e incluye el tiempo hasta la " +
        "primera gota."
    override val timerPause = "Pausar"
    override val timerStart = "Iniciar"
    override val timerResume = "Continuar"
    override val timerReset = "Reiniciar"
    override val additionsHeading = "Agregados"
    override val backupTitle = "Backup"
    override val backupIntro =
        "Guardá una copia de tus cafés y shots, o recuperalos desde un " +
        "archivo."
    override val backupPrivacy =
        "El archivo incluye tus notas y no está cifrado. Elegí dónde " +
        "guardarlo."
    override val exportBackup = "Exportar backup"
    override val importBackup = "Importar backup"
    override val reviewBeforeImport = "Revisá antes de importar"
    override val mergeBackup = "Combinar datos del backup"
    override val replaceAllBackup = "Reemplazar todo con este backup"
    override val backupFailed = "No pudimos completar la operación. Tus datos locales se conservan."
    override val conflictsTitle = "Conflictos"
    override val conflictsBody =
        "Dos dispositivos cambiaron el mismo registro. Las dos copias siguen " +
        "guardadas hasta que elijas una. La otra no se borra en silencio."
    override val keepThisCopy = "Quedarme con esta copia"
    override val keepOtherCopy = "Quedarme con la otra"
    override val beanNameField = "Nombre del café"
    override val beanSection = "Café"
    override val addSavedBean = "+ Agregar café guardado"
    override val roasterOptionalField = "Tostador (opcional)"
    override val extractionSection = "Extracción"
    override val extractionHint = "Dosis, salida y tiempo desde el inicio de la extracción."
    override val measureWithTimer = "Medir con Shot Timer"
    override val firstDropOptional = "Primera gota · s (opcional)"
    override val temperatureOptional = "Temperatura · °C (opcional)"
    override val yourDrink = "Tu bebida"
    override val drinkHint = "Estilo, agua y leche agregados después de extraer."
    override val styleOptional = "Estilo (opcional)"
    override val waterAfterExtraction =
        "Agua agregada después de la extracción; no modifica el ratio."
    override val equipmentAndGrind = "Equipo y molienda"
    override val orTypeMachine = "O escribir máquina manualmente"
    override val machineOptional = "Máquina (opcional)"
    override val addSavedMachine = "+ Agregar máquina guardada"
    override val orTypeGrinder = "O escribir molinillo manualmente"
    override val addSavedGrinder = "+ Agregar molinillo guardado"
    override val orTypeCup = "O escribir taza manualmente"
    override val cupOptional = "Taza (opcional)"
    override val addSavedCup = "+ Agregar taza guardada"
    override val howItTasted = "Cómo salió"
    override val notesOptional = "Notas (opcional)"
    override val ratingQuestion = "¿Qué tal salió? · 1 a 5"
    override val nextAttempt = "Próximo intento"
    override val checkMarkedFields = "Revisá los campos marcados."
    override val doseIn = "Dosis de entrada"
    override val outputField = "Output"
    override val yearField = "Año"
    override val grinderTypeElectric = "Eléctrico"
    override val grinderTypeManual = "Manual"
    override val grinderOptionalField = "Molinillo (opcional)"
    override val logShot = "Registrar shot"
    override val actionClose = "Cerrar"
    override val notRated = "Sin evaluar"
    override val detailExtraction = "Extracción"
    override val extractionPhase = "Fase de extracción"
    override val waterMilkDoNotChangeRatio = "El agua y la leche no cambian el ratio de extracción."
    override val temperatureC = "Temperatura · °C"
    override val dialContext = "Contexto para ajustar espresso"
    override val linkedAttempt = "Nuevo intento vinculado a una extracción anterior."
    override val recipeCopied = "¡Receta copiada al portapapeles!"
    override val copyRecipe = "Copiar receta / resumen"
    override val saveAsRecipe = "Guardar como receta"
    override val adjustNextExtraction = "Ajustar próxima extracción"
    override val removeFavorite = "Quitar favorito"
    override val markFavorite = "Marcar favorito"
    override val editShot = "Editar shot"
    override val deleteShotQuestion = "¿Querés eliminar este shot?"
    override val yesDelete = "Sí, eliminar"
    override val deleteShot = "Eliminar shot"
    override val beanPhoto = "Foto del café"
    override val adjustEspresso = "Ajustar espresso"
    override val hideDialContext = "Ocultar contexto de ajuste"
    override val showDialContext = "Contexto para ajustar espresso (opcional)"
    override val editShotTaste = "Editar shot / evaluar sabor"
    override val nextExtractionTitle = "Próxima extracción"
    override val noteGrindInForm = "Anotá el nuevo ajuste de molienda en el formulario."
    override val measureAndLogAttempt = "Medí el tiempo y registrá cómo salió este nuevo intento."
    override val prepareNextAttempt = "Preparar próximo intento"
    override val compareFromPump = "Para comparar tiempos, medí desde la activación de la bomba."
    override val tasteNotTime =
        "Compará el sabor con el intento anterior. El tiempo y el ratio no " +
        "diagnostican por sí solos la extracción."
    override val comparableRefs = "Tus referencias comparables"
    override val refsWillAppear =
        "Las recetas favoritas o equilibradas con puntaje alto serán " +
        "referencias cuando coincidan café, equipo y contexto."
    override val evolution = "Evolución"
    override val previousAttemptGone = "El intento anterior ya no está disponible."
    override val viewAttempt = "Ver intento"
    override val compareTitle = "Comparar intentos"
    override val referenceHeading = "Referencia"
    override val compareHint =
        "Elegí 2 o 3 shots. El primero será la referencia; los cambios " +
        "aparecen resaltados."
    override val differentCoffeesNote =
        "Son cafés distintos: las diferencias de sabor no dependen solo de " +
        "los ajustes."
    override val descriptiveCompare =
        "Comparación descriptiva: tiempo y ratio no determinan por sí solos " +
        "la calidad. Agua y leche también cambian el sabor."
    override val scrollTable = "Deslizá la tabla hacia los lados para ver todos los intentos."
    override val preferThis = "Prefiero este"
    override val preferThisNote =
        "“Prefiero este” lo guarda como favorito para futuras referencias del " +
        "asistente; no modifica los demás favoritos."
    override val addShots = "Agregar shots"
    override val allCoffees = "Todos los cafés"
    override val favoritesOnly = "Solo favoritos"
    override val noShotsForFilters = "No hay shots para estos filtros."
    override val recipeTitle = "Receta"
    override val yourRecipe = "Tu receta"
    override val doseKeepsRatio =
        "Al cambiar dosis se conserva el ratio; al cambiar output se " +
        "recalcula el ratio."
    override val doseFieldG = "Dosis · g"
    override val extractionRatioField = "Ratio de extracción · 1:"
    override val outputFieldG = "Output · g"
    override val extraWaterOptional = "Agua extra · ml (opcional)"
    override val milkOptionalMl = "Leche · ml (opcional)"
    override val saveOrBrew = "Guardar o preparar"
    override val timeLoggedAgain =
        "El tiempo, la primera gota y el sabor se registran de nuevo al " +
        "preparar."
    override val checkRecipeFields = "Revisá nombre, molienda y cantidades."
    override val recipeAmountHint = "Usá dosis de 0,1 a 100 g y output de 0,1 a 1000 g."
    override val grindKind = "Tipo de molienda"
    override val veryFine = "Muy fino"
    override val veryCoarse = "Muy grueso"
    override val millAdjustment = "Ajuste del molino"
    override val grindHint = "Elegí un nivel o anotá el ajuste real: 18 clics, 2.3, 1.4.2…"
    override val hideProGrind = "Ocultar ajustes pro"
    override val showProGrind = "Mostrar ajustes pro"
    override val proGrindTitle = "Vueltas · Dial · Clics"
    override val proGrindHint =
        "Usá esta notación si tu molino la admite. Cada escala es propia del " +
        "equipo; no convierte fino o grueso a clics."
    override val rotations = "Vueltas"
    override val dial = "Dial"
    override val clicks = "Clics"
    override val tapToApplyPro =
        "Tocá un control para aplicar el ajuste pro; tu valor actual todavía " +
        "se conserva."
    override val adminReadOnly = "Solo lectura · datos sincronizados en el servidor"
    override val adminSignIn = "Iniciá sesión con la cuenta administradora."
    override val allAccounts = "Todas las cuentas"
    override val refresh = "Actualizar"
    override val noScoreYet = "sin datos"
    override val accountsAppearLater =
        "Las cuentas anteriores aparecen después de su próxima " +
        "sincronización. Los cambios sin subir no están incluidos."
    override val activity30Days = "Actividad · últimos 30 días (UTC)"
    override val noShotsInPeriod = "Sin shots en este período."
    override val topStyles = "Estilos más usados"
    override val accountsHeading = "Cuentas"
    override val firstPage = "Primera página"
    override val moreAccounts = "Más cuentas"
    override val filterStyle = "Filtrar estilo"
    override val fromDateUtc = "Desde · AAAA-MM-DD (UTC)"
    override val toDateUtc = "Hasta · AAAA-MM-DD (UTC)"
    override val scoreOptional = "Puntuación · 1 a 5 (opcional)"
    override val checkDatesAndScore = "Revisá las fechas y la puntuación."
    override val noAdminPermission = "Esta cuenta no tiene permiso de administrador."
    override val sessionExpired = "La sesión venció. Volvé a iniciar sesión."
    override val adminQueryFailed =
        "No pudimos consultar el panel. Reintentá; tus datos no se " +
        "modificaron."
    override val couldNotSaveDefault = "No pudimos guardar el predeterminado. Intentá otra vez."
    override val couldNotSaveFavorite = "No pudimos guardar el favorito. Reintentá."
    override val conflictChanges = "Hay cambios en conflicto. Resolvelos en Cuenta."
    override val couldNotSync = "No se pudo sincronizar."
    override val couldNotSaveRecipe = "No pudimos guardar la receta. Reintentá."
    override val couldNotDeleteRecipe = "No pudimos eliminar la receta. Reintentá."
    override val resolvingConflict = "Resolviendo el conflicto…"
    override val noPendingConflicts = "No hay conflictos pendientes."
    override val conflictKept = "No pudimos resolver el conflicto. Tus datos siguen guardados."
    override val couldNotSaveMachine = "No pudimos guardar la máquina."
    override val couldNotSaveBean = "No pudimos guardar el café."
    override val couldNotSaveGrinder = "No pudimos guardar el molinillo."
    override val couldNotSaveCup = "No pudimos guardar la taza."
    override val logShotToDial = "Registrá un shot y evaluá su sabor para empezar a ajustar."
    override val couldNotSaveContext = "No pudimos guardar el contexto. Reintentá."
    override val couldNotSaveShot =
        "No pudimos guardar el shot. Tus datos siguen acá; intentá otra vez."
    override val shotNotFound = "No encontramos este shot."
    override val appName = "Coffee Dial"
    override val today = "Hoy"
    override val yesterday = "Ayer"
    override val errBeanName = "Ingresá el nombre del café"
    override val errPhoto = "La foto no es válida."
    override val errBagWeight = "Ingresá un peso de bolsa válido en gramos"
    override val errMachineName = "Ingresá el nombre de la máquina"
    override val errYear = "Ingresá un año válido (ej. 2022)"
    override val errGrinderName = "Ingresá el nombre del molinillo"
    override val errGrinderType = "Elegí el tipo de molinillo"
    override val errCupName = "Ingresá el nombre de la taza"
    override val errWeight = "Ingresá un peso válido en gramos"
    override val errCapacity = "Ingresá una capacidad válida en cm³"
    override val errDose = "Usá entre 0,1 y 100 g"
    override val errOutput = "Usá entre 0,1 y 1000 g"
    override val errSeconds = "Usá entre 0,1 y 600 s"
    override val errFirstDrop = "La primera gota debe estar entre 0 y el tiempo total"
    override val errGrind = "Ingresá el ajuste de molienda"
    override val errTemperature = "Usá entre 1 y 100 °C o dejalo vacío"
    override val errMilk = "Usá entre 1 y 200 ml o dejalo vacío"
    override val errWater = "Usá entre 1 y 1000 ml o dejalo vacío"
    override val errStyleLength = "Usá hasta 100 caracteres"
    override val errDialIn = "Revisá los datos del ajuste"
    override val errTaste = "Usá valores de 1 a 5"
    override val errRating = "Elegí entre 1 y 5"
    override val adviceCheckContext = "Revisá el contexto"
    override val adviceCheckContextBody = "El nombre del molinillo admite hasta 200 caracteres."
    override val advicePickMethod = "Elegí la preparación"
    override val advicePickMethodBody = "Confirmá cómo preparaste este café."
    override val adviceEspressoOnly = "Por ahora, espresso"
    override val adviceEspressoOnlyBody =
        "Este asistente ajusta espresso. No aplica estas recetas a filtro o " +
        "moka."
    override val advicePickRoast = "Indicá el tueste"
    override val advicePickRoastBody = "Si no lo sabés, elegí Desconocido."
    override val advicePickGoal = "Elegí tu objetivo"
    override val advicePickGoalBody =
        "Podés buscar más intensidad, más suavidad o mantener el equilibrio."
    override val adviceLogTaste = "Primero, registrá el sabor"
    override val adviceLogTasteBody =
        "Editá el shot y elegí cómo salió en Perfil de sabor. El tiempo y el " +
        "ratio solos no alcanzan."
    override val adviceWhatTasted = "¿Qué probaste?"
    override val adviceWhatTastedBody =
        "Separá el sabor del espresso solo del de la bebida con agua o leche."
    override val adviceLogAdditions = "Registrá agua y leche"
    override val adviceLogAdditionsBody =
        "Completá las cantidades agregadas antes de ajustar la bebida final."
    override val adviceRepeatCompare = "Repetí y compará"
    override val adviceAtLimit =
        "La receta ya está en el límite de este ajuste. Repetí y evaluá el " +
        "sabor antes de cambiar otra variable."
    override val adviceKeepDose = "Mantené dosis, molienda y temperatura; medí el nuevo tiempo."
    override val adviceGoalClash = "Revisá sabor y objetivo"
    override val adviceGoalClashBody =
        "La valoración y la intensidad buscada apuntan en direcciones " +
        "opuestas. Confirmalas antes de cambiar la receta."
    override val adviceEspressoAlone = "Probá el espresso solo"
    override val adviceEspressoAloneBody =
        "El agua y la leche cambian la percepción. Repetí la receta y evaluá " +
        "el espresso antes de agregarlos; no hay base para diagnosticar la " +
        "extracción."
    override val adviceAdditionLimit =
        "La cantidad agregada está en el límite admitido. Probá el espresso " +
        "solo antes de modificar la extracción."
    override val adviceOneAddition =
        "Para cambiar la intensidad de la bebida final, ajustá primero una " +
        "sola adición. Conservá la receta del espresso."
    override val adviceKeepRecipe = "Conservá la receta"
    override val adviceBalancedDrink =
        "La bebida te resultó equilibrada. Repetila para comprobar " +
        "consistencia y marcala como favorita si la preferís."
    override val adviceCheckConsistency = "Revisá la consistencia"
    override val adviceHighAcidityBitter =
        "Acidez y amargor altos pueden tener varias causas. Repetí con " +
        "distribución pareja del café antes de cambiar la receta; no podemos " +
        "concluir subextracción o sobreextracción."
    override val adviceConfirmGrinder = "Confirmá el molinillo"
    override val adviceLongSour =
        "Con un yield largo y sabor ácido, conviene revisar distribución y " +
        "molienda. Indicá tu molinillo antes de probar un ajuste."
    override val adviceSlightlyFiner = "Probar un ajuste apenas más fino"
    override val adviceAlreadyLong =
        "Ya usás un yield largo. Si tu molinillo permite regularlo, cambiá un " +
        "paso pequeño hacia fino y mantené dosis y yield. Registrá el ajuste " +
        "real; no hay un número universal de clicks."
    override val adviceMoreYieldSour =
        "Como el sabor fue demasiado ácido, probá un poco más de yield. " +
        "También puede bajar la intensidad: priorizá evaluar el sabor en este " +
        "intento."
    override val adviceShortBitter =
        "Con un yield ya corto, revisá molienda y distribución. Indicá tu " +
        "molinillo antes de intentar cambiarlo."
    override val adviceSlightlyCoarser = "Probar un ajuste apenas más grueso"
    override val adviceAlreadyShort =
        "El yield ya es corto. Si podés regular la molienda, probá un paso " +
        "pequeño hacia grueso manteniendo dosis y yield; compará el sabor."
    override val adviceLessYieldBitter =
        "Probá un yield algo menor y compará el amargor. Esto puede aumentar " +
        "la concentración; no demuestra sobreextracción."
    override val adviceStrongerYield =
        "Para buscar más intensidad, probá menos yield con la misma dosis. " +
        "Compará también si aparece acidez."
    override val advicePersonalMilder =
        "Tus recetas preferidas comparables sirven como referencia para un " +
        "yield mayor."
    override val adviceMilderYield =
        "Para un espresso más suave, probá algo más de yield y compará si " +
        "aumenta el amargor."
    override val adviceKeepBalanced =
        "Lo registraste equilibrado y querés mantenerlo. Repetilo para " +
        "comprobar consistencia y marcá tu preferido como favorito."
    override val shotDetailTitle = "Detalle del shot"
    override val compareWithOthers = "Comparar con otros intentos"

    override fun extractionGuides() = listOf(
        GuideCard("Ristretto", "Hasta 1:1.5", "Shot corto. Más cuerpo y dulzor, menos amargor."),
        GuideCard(
            "Espresso",
            "Más de 1:1.5 y hasta 1:2.5",
            "La zona habitual. Cuerpo, acidez y dulzor en balance."
        ),
        GuideCard(
            "Espresso largo",
            "Más de 1:2.5 y hasta 1:3",
            "Un poco más de agua en la extracción. Más liviano."
        ),
        GuideCard("Lungo", "Más de 1:3", "Más agua durante la extracción. No es un americano."),
    )

    override fun timeGuides() = listOf(
        TimeGuide(
            "<15 s",
            "Muy rápido",
            "Muy subextraído",
            "Aguado, ácido, poco cuerpo",
            "Molienda mucho más fina"
        ),
        TimeGuide(
            "15-20 s",
            "Rápido",
            "Subextraído",
            "Ácido, poco dulce, final corto",
            "Molienda más fina"
        ),
        TimeGuide(
            "20-25 s",
            "Algo rápido",
            "Puede estar subextraído",
            "Ya puede ser rico, depende del grano",
            "Algo más fino si le falta cuerpo"
        ),
        TimeGuide(
            "25-30 s",
            "Normal",
            "Zona clásica",
            "Balance, dulzor y cuerpo",
            "Ajustar fino según el gusto"
        ),
        TimeGuide(
            "30-35 s",
            "Lento",
            "Puede estar perfecto",
            "Más cuerpo e intensidad",
            "Si está rico, no tocar nada"
        ),
        TimeGuide(
            "35-40 s",
            "Muy lento",
            "Tendencia a sobreextracción",
            "Amargo, seco, astringente",
            "Molienda algo más gruesa"
        ),
        TimeGuide(
            ">40 s",
            "Goteo",
            "Probable sobreextracción",
            "Muy amargo, seco, áspero",
            "Molienda más gruesa"
        ),
    )

    override fun grainGuides() = listOf(
        GuideCard(
            "Arábica y robusta",
            "Especies",
            "Arábica: más aroma, acidez y dulzor. Robusta: más cuerpo, crema y amargor."
        ),
        GuideCard(
            "Bourbon, Caturra, Catuaí",
            "Varietales clásicos",
            "Cuerpo sedoso, chocolate, caramelo, frutas rojas y acidez cítrica."
        ),
        GuideCard(
            "Geisha",
            "Varietal de alta gama",
            "Floral, acidez cítrica brillante, lima y cuerpo ligero."
        ),
        GuideCard(
            "Pacamara y Maragogipe",
            "Granos grandes",
            "Especias, chocolate amargo, frutos secos y acidez vinosa."
        ),
        GuideCard(
            "Colombia",
            "Origen",
            "Chocolate con leche, frutos rojos, caramelo y cuerpo redondo."
        ),
        GuideCard("Etiopía", "Origen", "Cítrico, jazmín, bergamota, té negro y acidez viva."),
        GuideCard("Brasil", "Origen", "Cuerpo denso, acidez baja, avellana, cacao y nuez."),
    )

    override fun flowAndTaste(flow: String, taste: String) = "Flujo: $flow. Sabor: $taste."

    override fun whatToTry(advice: String) = "Qué probar: $advice"

    override fun ratioParts(water: String, milk: String) = "1 : $water : $milk"

    override fun doseCoffeeExtraction(dose: String, coffee: String, ratio: String) =
        "$dose g de dosis · $coffee g de café · extracción 1:$ratio"

    override fun waterAndMilkMl(water: String, milk: String) =
        "$water ml de agua · $milk ml de leche"

    override fun knownDrinkName(name: String) = name

    override fun grindLevelLabel(canonical: String) = canonical

    override fun machineTypeLabel(canonical: String) = canonical

    override fun grinderTypeLabel(canonical: String) = canonical

    override fun conflictLabel(canonical: String) = canonical

    override fun firstDropAt(seconds: String) = "Primera gota: $seconds s"

    override fun backupNewCounts(
        beans: Int,
        shots: Int,
        machines: Int,
        grinders: Int,
        cups: Int,
        recipes: Int,
        defaults: Int
    ) =
        "$beans cafés · $shots shots · $machines máquinas · $grinders " +
            "molinillos · $cups tazas · $recipes recetas nuevas · $defaults " +
            "predeterminados"

    override fun backupDuplicates(count: Int) = "$count shots ya existentes."

    override fun backupImported(beans: Int, shots: Int, grinders: Int, cups: Int, recipes: Int) =
        "Importación completa: $beans cafés, $shots shots, $grinders " +
            "molinillos, $cups tazas y $recipes recetas procesados."

    override fun backupReplaced(
        beans: Int,
        shots: Int,
        machines: Int,
        grinders: Int,
        cups: Int,
        recipes: Int
    ) =
        "Reemplazo completo: $beans cafés, $shots shots, $machines máquinas, " +
            "$grinders molinillos, $cups tazas y $recipes recetas cargados desde " +
            "el backup."

    override fun thisDeviceValue(value: String) = "Este dispositivo: $value"

    override fun otherDeviceValue(value: String) = "El otro dispositivo: $value"

    override fun grindAdjustment(value: String) = "Ajuste del molino · $value"

    override fun prepLabel(label: String) = "Preparación · $label"

    override fun roastLabel(label: String) = "Tueste · $label"

    override fun doseYieldLine(dose: String, yield: String) = "Dosis: $dose g · Yield: $yield g"

    override fun milkLine(ml: String) = "Leche: $ml ml"

    override fun waterLine(ml: String) = "Agua extra: $ml ml"

    override fun referencesCount(count: Int) = "Tus referencias comparables ($count)"

    override fun attemptNumber(index: Int, favorite: Boolean) =
        "Intento $index" + if (favorite) " · favorito" else ""

    override fun nextAttemptLine(output: String, seconds: String) =
        "Siguiente intento: $output g · $seconds s"

    override fun tasteRating(taste: String, rating: Int) = "$taste · $rating/5"

    override fun selectedCount(count: Int) = "Seleccionados ($count/3)"

    override fun referenceName(name: String) = "Referencia: $name"

    override fun useAsReference(index: Int) = "$index: usar como referencia"

    override fun attemptColumn(index: Int) = "Intento $index"

    override fun temperatureLine(value: String) = "Temperatura: $value °C"

    override fun savedAdjustment(value: String) = "Ajuste guardado: $value"

    override fun decreaseLabel(label: String) = "Reducir $label"

    override fun increaseLabel(label: String) = "Aumentar $label"

    override fun accountsAndShots(accounts: Int, shots: Int) = "$accounts cuentas · $shots shots"

    override fun last7Days(count: Int) = "Últimos 7 días: $count shots"

    override fun averageScore(value: String) = "Puntuación media: $value"

    override fun styleCount(label: String, count: Int) = "$label: $count"

    override fun shotCount(count: Int) = "$count shots"

    override fun lastConfirm(whenText: String) = "Última confirmación: $whenText"

    override fun filteredShotCount(shown: Int, total: Int) = "$shown de $total shots"

    override fun weekdayShort(index: Int) =
        listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")[index]

    override fun ratingOfFive(rating: Int) = "Puntuación: $rating de 5"

    override fun rateWithCups(value: Int) = "Puntuar con $value de 5 cafés"

    override fun cupTotalDescription(total: String, dose: String, water: String, milk: String) =
        "Taza con total $total. Dosis $dose g, agua $water ml, leche $milk " +
            "ml."

    override fun foamSpoken(label: String) = " Espuma: $label."

    override fun foamRatioSpoken(ratio: String) = " Espuma : leche ≈ $ratio."

    override fun capacitySpoken(capacity: String) = " Capacidad $capacity cm³."

    override fun adviceTryYield(grams: String) = "Probar $grams g de espresso"

    override fun adviceTryAddition(amount: String, water: Boolean): String {
        val liquid = if (water) "agua" else "leche"
        return "Probar $amount ml de $liquid"
    }

    override fun advicePersonalStronger(count: Int) =
        "Uso el ratio central de $count recetas comparables que preferiste " +
            "como referencia, sin cambiar todo a la vez."

    override fun roastNote(roast: RoastLevel?) = when (roast) {
        RoastLevel.LIGHT -> "En tuestes claros puede haber acidez propia del café. "
        RoastLevel.DARK -> "En tuestes oscuros puede haber amargor propio del tueste. "
        RoastLevel.UNKNOWN -> "Sin conocer el tueste, este es un ensayo conservador. "
        else -> ""
    }

    override fun roastAge(days: Int) = when (days) {
        0 -> "Tostado hoy"
        1 -> "Tostado ayer"
        in 2..6 -> "Tostado hace $days días (desgasificando)"
        in 7..30 -> "Tostado hace $days días (ventana ideal ☕)"
        else -> "Tostado hace $days días"
    }

    override fun preInfusion(pre: String, extraction: String) =
        "Pre-infusión: $pre s · Extracción: $extraction s"

    override fun clearField(label: String) = "Borrar $label"

    override fun userMessage(raw: String?): String {
        val text = raw.orEmpty()
        val tail = text.substringAfter(':', "")
        return when {
            text.startsWith("i18n.invalid:") ->
                "No se pudieron validar los datos ($tail). Se conservan sin cambios."
            text.startsWith("i18n.backup_version:") ->
                "Esta versión de backup no es compatible ($tail)."
            text.startsWith("i18n.sync_status:") -> "No se pudo completar el sync ($tail)."
            text.startsWith("i18n.backup_invalid:") -> "El backup contiene datos inválidos: $tail"
            text.startsWith("i18n.conflict_count:") ->
                "Hay cambios distintos en $tail registros. Elegí qué copia conservar."
            else -> when (text) {
                "i18n.other_account" -> "Los datos locales pertenecen a otra cuenta."
                "i18n.other_account_long" ->
                    "Los datos locales pertenecen a otra cuenta. Volvé a esa cuenta para " +
                    "sincronizar, o exportá un backup antes de cambiar de cuenta."
                "i18n.sign_in" -> "Entrá a Cuenta para iniciar sesión y sincronizar."
                "i18n.resolve_sign_in" -> "Entrá a Cuenta para resolver el conflicto."
                "i18n.account_changed" ->
                    "La cuenta cambió. Volvé a sincronizar; no se reemplazaron tus datos " +
                    "locales."
                "i18n.incompatible" ->
                    "Actualizá Coffee Dial: el formato de sincronización no es " +
                    "compatible."
                "i18n.upgrade_sync" -> "Actualizá Coffee Dial para sincronizar."
                "i18n.resolve_failed" -> "No se pudo resolver el conflicto."
                "i18n.offline" ->
                    "No se pudo conectar. Tus datos siguen guardados; probá nuevamente."
                "i18n.sync_failed" -> "No se pudo sincronizar."
                "i18n.response" -> "Respuesta de sync incompatible."
                "i18n.missing_photo" ->
                    "Falta una foto local. Volvé a sincronizar antes de exportar."
                "i18n.renew" ->
                    "Necesitamos renovar tu acceso. Entrá a Cuenta y continuá con tu " +
                    "proveedor."
                "i18n.forbidden" -> "Esta cuenta no tiene acceso a la sincronización."
                "i18n.stale" ->
                    "Otro dispositivo guardó cambios. Volvé a sincronizar; tus datos se " +
                    "conservan."
                "i18n.upgrade_app" ->
                    "Actualizá esta app: la nube contiene datos de una versión más nueva."
                "i18n.too_big" ->
                    "El historial supera el límite de sync. Exportá un backup para " +
                    "conservarlo."
                "i18n.rate" -> "Hubo demasiados intentos. Esperá un momento y volvé a sincronizar."
                "i18n.unavailable" ->
                    "El servicio no está disponible ahora. Reintentá; no hace falta " +
                    "cerrar sesión."
                "i18n.not_backup" -> "El archivo no es un backup de Coffee Dial."
                "i18n.bad_version" -> "El backup no tiene una versión válida."
                "i18n.newer_backup" ->
                    "Este backup es de una versión más nueva. Actualizá Coffee Dial."
                "i18n.backup_damaged" -> "El backup está dañado o contiene campos no compatibles."
                "i18n.backup_invalid" -> "El backup contiene datos inválidos."
                "i18n.backup_photos" ->
                    "El backup no incluye todas las fotos. Sincronizá antes de exportar."
                "i18n.backup_size" -> "El backup supera el límite de 10 MB."
                "i18n.local_damaged" ->
                    "Los datos locales tienen un formato incompatible o dañado. No se " +
                    "modificaron."
                "i18n.other_tab" ->
                    "Los datos cambiaron en otra pestaña. Revisalos e intentá otra vez."
                "i18n.sync_changed" ->
                    "Los datos cambiaron durante el sync. Tus cambios están guardados; " +
                    "volvé a sincronizar."
                "i18n.tab_changed" -> "Otra pestaña cambió los datos. Volvé a sincronizar."
                "i18n.shot_gone" -> "Este shot ya no existe."
                "i18n.bean_gone" -> "El café ya no existe."
                "i18n.cup_gone" -> "La taza ya no existe."
                "i18n.grinder_gone" -> "El molinillo ya no existe."
                "i18n.machine_gone" -> "La máquina ya no existe."
                "i18n.history_changed" ->
                    "El historial cambió. Volvé a seleccionar el backup para revisarlo."
                "i18n.saved_session_other" ->
                    "La sesión guardada pertenece a otra cuenta. Volvé a iniciar sesión."
                "i18n.renew_google_keep" ->
                    "Volvé a continuar con Google para renovar el acceso. Tus datos se " +
                    "conservan."
                "i18n.renew_provider_keep" ->
                    "Volvé a continuar con tu proveedor para renovar el acceso. Tus datos " +
                    "se conservan."
                "i18n.sign_in_expired" -> "El intento de acceso venció. Volvé a intentarlo."
                "i18n.google_not_configured" ->
                    "Google todavía no está configurado para esta versión de Coffee Dial."
                "i18n.could_not_sign_in_google" ->
                    "No se pudo iniciar sesión con Google. Revisá la conexión y la " +
                    "configuración de acceso de la app."
                "i18n.could_not_open_browser" ->
                    "No se pudo abrir el navegador. Instalá o habilitá uno e intentá " +
                    "nuevamente."
                "i18n.could_not_complete_web" ->
                    "No se pudo completar el acceso web. Volvé a intentarlo."
                "i18n.apple_android_later" ->
                    "Apple en Android estará disponible cuando configuremos el servicio " +
                    "de acceso web. Podés seguir usando la app sin cuenta."
                "i18n.could_not_renew_google" ->
                    "No se pudo renovar Google. Revisá la conexión y probá de nuevo."
                "i18n.access_not_ready" -> "El acceso todavía no está disponible."
                "i18n.provider_identity" -> "El proveedor no devolvió una identidad válida."
                "i18n.could_not_verify_access" ->
                    "No se pudo verificar el acceso. Volvé a iniciar sesión."
                "i18n.could_not_open_google" ->
                    "No se pudo abrir Google. Habilitá el almacenamiento del navegador."
                "i18n.apple_web_unavailable" -> "Sign in con Apple no está configurado en Web"
                "i18n.could_not_open_photos" -> "No se pudo abrir el selector de fotos."
                "i18n.could_not_prepare_photo" -> "No se pudo preparar la foto. Elegí otra imagen."
                "i18n.could_not_open_files" -> "No se pudo abrir el selector de archivos."
                "i18n.could_not_save_file" -> "No se pudo guardar el archivo. Volvé a exportarlo."
                "i18n.could_not_read_backup" ->
                    "No se pudo leer el archivo. Elegí un backup UTF-8 de hasta 10 MB."
                "i18n.export_interrupted" -> "La exportación se interrumpió. Volvé a intentarlo."
                "i18n.backup_exported" -> "Backup exportado."
                "i18n.read_interrupted" ->
                    "La lectura se interrumpió. Volvé a seleccionar el archivo."
                "i18n.backup_download_ready" -> "Backup preparado para descargar."
                "i18n.could_not_download" -> "No se pudo descargar el backup."
                "i18n.photo_invalid" -> "Foto inválida."
                "i18n.photo_too_big" -> "La foto supera los 100 KB."
                "i18n.photo_jpeg" -> "Foto JPEG inválida."
                "i18n.photo_pick_limit" -> "Elegí una imagen de hasta 20 MB."
                "i18n.could_not_read_photo" ->
                    "No se pudo leer la foto. Probá con JPEG o PNG."
                "i18n.apple_not_configured" ->
                    "Apple todavía no está configurado para esta versión de " +
                    "Coffee Dial."
                "i18n.apple_invalid_credential" ->
                    "Apple no devolvió una credencial válida."
                "i18n.could_not_sign_in_apple" ->
                    "No se pudo iniciar sesión con Apple. Revisá la cuenta del " +
                    "dispositivo y volvé a intentar."
                "i18n.sign_in_google" -> "Iniciá sesión con Google."
                "i18n.sign_in_google_again" -> "Volvé a iniciar sesión con Google."
                "i18n.account_changed_admin" -> "La cuenta cambió. Volvé a abrir Admin."
                else -> text
            }
        }
    }
}

object EnglishStrings : AppStrings {
    override val languageName = "English"
    override val languageSystem = "System"
    override val languageSpanish = "Spanish"
    override val languageEnglish = "English"

    override val navBrew = "Brew"
    override val navShots = "Log"
    override val navEquipment = "Equipment"
    override val navAccount = "Account"
    override val navRecipes = "Recipes"
    override val navAnalysis = "Analytics"
    override val navDialIn = "Dial-in"

    override val actionSave = "Save"
    override val actionCancel = "Cancel"
    override val actionDelete = "Delete"
    override val actionEdit = "Edit"
    override val actionBack = "Back"
    override val actionRepeat = "Brew again"
    override val actionShare = "Share shot"
    override val actionConfirm = "Confirm"
    override val actionRetry = "Retry"
    override val couldNotReadHistory = "Could not load history."
    override val styleLabel = "Style"
    override val totalLabel = "Total"
    override val favoriteLabel = "Favorite"
    override fun preinfusionText(preinfusion: String, extraction: String) =
        "Pre-infusion: $preinfusion s · Extraction: $extraction s"
    override fun foamLabelText(foamLabel: String) = "Foam: $foamLabel"
    override fun cupLabelText(cupName: String) = "Cup: $cupName"

    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val themeSystem = "System"

    override val homeGreeting = "Hello!"
    override fun greeting(userName: String) = "Hello, $userName!"
    override fun historyHeader(count: Int) = "History ($count)"
    override fun shotsSavedOnDevice(count: Int) = "$count shots · saved on this device"
    override val homeSyncing = "Syncing with cloud…"
    override val homeBrewCoffee = "Brew coffee"
    override val homeViewRecipes = "View all recipes"
    override val homeRecentShots = "Recent shots"
    override val homeViewAllShots = "View all shots"
    override val homeNoShotsYet = "No shots recorded yet."
    override val homeLogFirstShot = "Log first shot"
    override val logAShot = "Log a shot"
    override val nextExtraction = "Your next extraction"
    override val logShotAndFlavor = "Log your measurements and coffee flavor profile."
    override val yourWay = "Your way"
    override val repeatOrNew = "Repeat a recipe or dial in a new extraction."
    override val myRecipesRepeat = "My recipes · repeat coffee"
    override val startShotTimer = "Start Shot Timer"
    override val ratiosAndStyles = "Ratios and coffee styles"
    override fun viewHistoryCount(count: Int) = "View history ($count)"
    override val filterAll = "All"
    override val filterNoStyle = "No style"
    override fun shotsFilterCount(filtered: Int, total: Int) = "$filtered of $total shots"
    override val noShotsWithStyle = "No shots with this style."

    override val dose = "Dose"
    override val yield = "Yield"
    override val ratio = "Ratio"
    override val grind = "Grind setting"
    override val grindFine = "Fine"
    override val grindMedium = "Medium"
    override val grindCoarse = "Coarse"
    override val time = "Total time"
    override val firstDrop = "First drop"
    override val addedWater = "Added water"
    override val milk = "Milk"
    override val milkFoam = "Milk foam"
    override val milkFoamOptional = "Milk foam (optional)"
    override val actionRemove = "Remove"
    override val notRecorded = "Not recorded"
    override val milkFoamNote = "Describes texture; does not add volume to total."
    override val rating = "Rating"
    override val notes = "Notes"
    override val tastingNotes = "Tasting notes"

    override val beans = "Beans"
    override val roaster = "Roaster"
    override val roastDate = "Roast date"
    override val origin = "Origin"
    override val process = "Process"
    override val bagWeight = "Bag size"
    override val machine = "Machine"
    override val grinder = "Grinder"
    override val cup = "Cup"
    override val capacity = "Capacity"
    override val emptyWeight = "Empty weight"
    override val defaultLabel = "Default"
    override val setDefault = "Set as default"
    override val repeatWithTimer = "Brew with timer"
    override val viewOrAdjust = "View / adjust"
    override val deleteRecipeQuestion = "Delete recipe?"
    override val shotsPreservedNote = "Recorded shots are kept."
    override val saveRecipe = "Save recipe"

    override val rowDose = "Dose (g)"
    override val rowYield = "Output (g)"
    override val rowRatio = "Extraction ratio"
    override val rowTime = "Time (s)"
    override val rowFirstDrop = "First drop (s)"
    override val rowTemperature = "Temperature (°C)"
    override val rowAddedWater = "Added water (ml)"
    override val rowMilk = "Milk (ml)"
    override val rowMilkFoam = "Milk foam"

    override val coffeeInNumbers = "Your coffee in numbers"
    override val averageRating = "Average rating"
    override val differentCoffees = "Distinct coffees"
    override val totalCoffeeUsed = "Total coffee used"
    override val averageRecipe = "Average recipe"
    override val averageRatio = "Average ratio per shot"
    override val shotBreakdownTitle = "Shot rating breakdown"
    override val topCoffees = "Top coffees used"
    override val trackPillarDesc = "Your coffee, equipment, and extractions."
    override val improvePillarDesc = "Compare results and find your next recipe."
    override val compareAttempts = "Compare attempts & favorites"
    override val yourRecords = "Your logs"
    override val exploreResults = "Explore your results"

    override val myBeans = "My Beans"
    override val addBean = "Add beans"
    override val editBean = "Edit beans"
    override val myMachines = "My Machines"
    override val addMachine = "Add machine"
    override val editMachine = "Edit machine"
    override val myGrinders = "My Grinders"
    override val addGrinder = "Add grinder"
    override val editGrinder = "Edit grinder"
    override val myCups = "My Cups"
    override val addCup = "Add cup"
    override val editCup = "Edit cup"
    override val nameField = "Name"

    override val newShotTitle = "New shot"
    override val editShotTitle = "Edit shot"
    override val saveShotLabel = "Save shot"
    override val manageBeans = "Manage beans"
    override val manageMachines = "Manage machines"
    override val manageGrinders = "Manage grinders"
    override val manageCups = "Manage cups"

    override val savedBeans = "Saved beans"
    override val deleteBeanNote = "Deleting beans does not delete recorded shots."
    override val noBeansYet = "No beans added yet."
    override fun roasterLabelText(roaster: String) = "Roaster: $roaster"
    override fun bagSizeLabelText(grams: String) = "Bag: $grams g"
    override val useByDefault = "Use as default"
    override val nameCoffee = "Coffee name"
    override val roasterOptional = "Roaster (optional)"
    override val originOptional = "Origin (optional)"
    override val processOptional = "Process (optional)"
    override val bagWeightOptional = "Bag size in g (optional)"
    override val addPhoto = "Add photo"
    override val changePhoto = "Change photo"
    override val removePhoto = "Remove photo"
    override val preparingPhoto = "Preparing photo…"
    override val signInForPhotos = "Sign in on Account to add photos."
    override val savedMachines = "Saved machines"
    override val noMachinesYet = "No machines added yet."
    override val savedGrinders = "Saved grinders"
    override val noGrindersYet = "No grinders added yet."
    override val savedCups = "Saved cups"
    override val noCupsYet = "No cups added yet."

    override val foamNone = "No foam"
    override val foamLow = "Low"
    override val foamMedium = "Medium"
    override val foamHigh = "High"

    override val cupSmall = "Small"
    override val cupMedium = "Medium"
    override val cupLarge = "Large"

    override val roastLight = "Light"
    override val roastMediumLight = "Medium-light"
    override val roastMedium = "Medium"
    override val roastMediumDark = "Medium-dark"
    override val roastDark = "Dark"
    override val roastUnknown = "Unknown"

    override val methodFilter = "Filter"
    override val methodMoka = "Moka"
    override val methodOther = "Other"
    override val goalStronger = "Stronger"
    override val goalBalanced = "Keep balanced"
    override val goalMilder = "Milder"
    override val basisEspresso = "Straight espresso"
    override val basisFinishedDrink = "Drink with water/milk"

    override val dialInTitle = "Dial-in Assistant"
    override val roastLevel = "Roast level"
    override val brewMethod = "Brew method"
    override val strengthGoal = "Extraction goal"
    override val diagnosis = "Diagnosis"
    override val suggestions = "Suggestions"
    override val grindFiner = "Grind finer"
    override val grindCoarser = "Grind coarser"

    override val flavorProfile = "Flavor profile"
    override val body = "Body"
    override val acidity = "Acidity"
    override val bitterness = "Bitterness"
    override val sweetness = "Sweetness"
    override val impressionTooSour = "Too sour"
    override val impressionTooBitter = "Too bitter"
    override val impressionWeak = "Weak"
    override val impressionBalanced = "Balanced"
    override val impressionTooStrong = "Too strong"
    override val tasteOptionalNote = "Optional · How was it?"
    override val removeTasteRating = "Remove taste rating"
    override val tasteScaleNote = "1 to 5 · Move only controls you want to record."
    override val bodyLight = "Light"
    override val bodyFull = "Full bodied"
    override val levelLow = "Low"
    override val levelHigh = "High"
    override val levelLowMasculine = "Low"
    override val levelHighMasculine = "High"
    override val actionRegister = "Record"

    override val myAccount = "My Account"
    override val appearance = "Appearance"
    override val language = "Language"
    override val savedOnDeviceOnly = "Saved on this device only."
    override val cloudSync = "Cloud Sync"
    override val syncNow = "Sync"
    override val restoreAndMerge = "Restore & merge"
    override val conflicts = "Conflicts"
    override val backupExportImport = "Backup: export / import"
    override val signInGoogle = "Continue with Google"
    override val signInApple = "Continue with Apple"
    override val signOut = "Sign out"
    override val backupExportImportText = "Backup: export / import"
    override val optionalAccessTitle = "Optional Sign-In"
    override val optionalAccessDesc =
        "Your coffee data remains saved on this device. Sign in to enable cloud synchronization."
    override val continueWithGoogleBrowser = "Continue with Google in browser"
    override val signingInTitle = "Signing in…"
    override val adminPanelButton = "Admin Panel"
    override val sessionStartedTitle = "Signed In"
    override fun userNameText(name: String) = "Name: $name"
    override fun userEmailText(email: String) = "Email: $email"
    override fun userProviderText(provider: String) = "Provider: $provider"
    override val cloudSyncTitle = "Cloud Sync"
    override val cloudSyncDesc =
        "Sync with Google without overwriting pending changes. Restore merges data; if there's a conflict, both copies are preserved."
    override val syncReadyState = "Status: Ready to sync"
    override val syncingState = "Syncing with cloud…"
    override fun syncSuccessState(count: Int) =
        "Last sync successful ($count shots synced)"
    override fun syncErrorState(message: String) =
        "Sync error: ${userMessage(message)}"
    override fun syncConflictState(count: Int) =
        "There are $count entries with conflicting changes. Choose which copy to keep. None were overwritten."
    override val reviewConflictsButton = "Review conflicts"
    override val importAndOverwriteCloudButton = "Import & overwrite from cloud"
    override val overwriteCloudButton = "Overwrite cloud data"

    override val inTheCup = "In the cup"
    override val coffeeWaterMilkRatio = "Coffee : water : milk"
    override val totalRatioPrefix = "Total ratio ≈ 1:"
    override val coffeeFromDose = "Coffee in cup brewed from"
    override val legendCoffee = "Coffee"
    override val legendWater = "Water"
    override val legendMilk = "Milk"
    override val legendFoam = "Foam"

    override fun foamIllustrativeText(foamLabel: String, interactive: Boolean) =
        if (interactive) "Foam: $foamLabel" else "Foam: $foamLabel · illustrative texture"

    override fun foamMilkRatioText(ratio: String) = "Foam : liquid milk ≈ $ratio"

    override fun foamBreakdownText(foamMl: String, liquidMl: String) =
        "Of the milk, ~$foamMl ml is foam and $liquidMl ml liquid."

    override fun coffeeFromDoseText(doseGrams: String) =
        "Coffee in cup brewed from $doseGrams g dose."

    override fun exceedsCapacityText(capacityCm3: String) =
        "Exceeds cup capacity ($capacityCm3 cm³)."

    override fun cupCapacityText(capacityCm3: String) =
        "$capacityCm3 cm³ cup."

    override val prepareTitle = "Brew a coffee"

    override fun maxSizeNotice(maxSize: String) =
        "Maximum size for this drink is $maxSize×."

    override val logWithMeasurements = "Log with these measurements"

    override fun sizeLabel(size: String) = "Size $size×"

    override fun foamHowToText(drinkName: String, foam: MilkFoam): String = when {
        drinkName == "Macchiato" && foam == MilkFoam.HIGH ->
            "A dollop of stiff foam over the espresso."

        foam == MilkFoam.HIGH ->
            "Stretch the milk for a thick layer, 1 to 2 cm."

        foam == MilkFoam.MEDIUM ->
            "Stretch for a thin layer, 0.5 to 1 cm, and integrate as you pour."

        foam == MilkFoam.LOW ->
            "Brief steam: silky milk with almost no foam on top."

        else -> "Warm the milk without stretching the steam."
    }

    override val tipsTitle = "Tips and styles"
    override val tabStyles = "Styles"
    override val tabTimes = "Times"
    override val tabGrains = "Beans"
    override val tipsSplitTitle = "Extraction and the drink are two different styles."
    override val tipsSplitBody =
        "The extraction ratio compares the dose with the coffee in the cup. " +
        "Water and milk are added afterwards and do not change it."
    override val extractionHeading = "Extraction"
    override val drinksHeading = "Drinks"
    override val drinksIntro =
        "The same drinks as Brew a coffee, at 1×. The 1 in coffee : water : " +
        "milk is the coffee in the cup, not the dose."
    override val otherMixesHeading = "Other mixes"
    override val otherMixesBody =
        "These help you recognize a shot. In the app, water and milk are " +
        "logged in ml; here 1 ml equals 1 g on 36 g of coffee."
    override val macchiatoFootnote =
        "Macchiato can also be 0.25 to 0.5 parts milk. Latte macchiato is a " +
        "different drink. Adding water after extraction does not turn an " +
        "espresso into a lungo."
    override val timeHeading = "Extraction time"
    override val timeIntro =
        "Measured from when the water starts until you stop the flow. It is a " +
        "clue, not a rule: taste decides."
    override val grainsHeading = "Beans"
    override val grainsIntro =
        "Variety and origin change aroma, acidity, and body. They are " +
        "starting points, not a spec sheet for the coffee you bought."
    override val shotTimerTitle = "Shot Timer"
    override val shotTimerHint =
        "Start when extraction begins and mark when the first drop appears."
    override val extractionRunning = "Extraction running"
    override val markFirstDrop = "Mark first drop"
    override val removeMark = "Remove mark"
    override val timerMaxReached = "The 10 minute maximum was reached."
    override val logTheExtraction = "Log the extraction"
    override val useTheseTimes = "Use these times"
    override val logShotWithTimes = "Log a shot with these times"
    override val timerIncludesFirstDrop =
        "Total time starts at the beginning and includes the time until the " +
        "first drop."
    override val timerPause = "Pause"
    override val timerStart = "Start"
    override val timerResume = "Continue"
    override val timerReset = "Reset"
    override val additionsHeading = "Additions"
    override val backupTitle = "Backup"
    override val backupIntro = "Save a copy of your coffees and shots, or restore them from a file."
    override val backupPrivacy =
        "The file includes your notes and is not encrypted. Choose where to " +
        "keep it."
    override val exportBackup = "Export backup"
    override val importBackup = "Import backup"
    override val reviewBeforeImport = "Review before import"
    override val mergeBackup = "Merge backup data"
    override val replaceAllBackup = "Replace everything with this backup"
    override val backupFailed = "We could not finish the operation. Your local data is unchanged."
    override val conflictsTitle = "Conflicts"
    override val conflictsBody =
        "Two devices changed the same record. Both copies stay saved until " +
        "you pick one. The other is not deleted quietly."
    override val keepThisCopy = "Keep this copy"
    override val keepOtherCopy = "Keep the other copy"
    override val beanNameField = "Coffee name"
    override val beanSection = "Coffee"
    override val addSavedBean = "+ Add a saved coffee"
    override val roasterOptionalField = "Roaster (optional)"
    override val extractionSection = "Extraction"
    override val extractionHint = "Dose, yield, and time from the start of extraction."
    override val measureWithTimer = "Time it with Shot Timer"
    override val firstDropOptional = "First drop · s (optional)"
    override val temperatureOptional = "Temperature · °C (optional)"
    override val yourDrink = "Your drink"
    override val drinkHint = "Style, water, and milk added after extraction."
    override val styleOptional = "Style (optional)"
    override val waterAfterExtraction =
        "Water added after extraction; it does not change the ratio."
    override val equipmentAndGrind = "Equipment and grind"
    override val orTypeMachine = "Or type a machine"
    override val machineOptional = "Machine (optional)"
    override val addSavedMachine = "+ Add a saved machine"
    override val orTypeGrinder = "Or type a grinder"
    override val addSavedGrinder = "+ Add a saved grinder"
    override val orTypeCup = "Or type a cup"
    override val cupOptional = "Cup (optional)"
    override val addSavedCup = "+ Add a saved cup"
    override val howItTasted = "How it tasted"
    override val notesOptional = "Notes (optional)"
    override val ratingQuestion = "How was it? · 1 to 5"
    override val nextAttempt = "Next attempt"
    override val checkMarkedFields = "Check the marked fields."
    override val doseIn = "Dose in"
    override val outputField = "Yield"
    override val yearField = "Year"
    override val grinderTypeElectric = "Electric"
    override val grinderTypeManual = "Manual"
    override val grinderOptionalField = "Grinder (optional)"
    override val logShot = "Log a shot"
    override val actionClose = "Close"
    override val notRated = "Not rated"
    override val detailExtraction = "Extraction"
    override val extractionPhase = "Extraction phase"
    override val waterMilkDoNotChangeRatio = "Water and milk do not change the extraction ratio."
    override val temperatureC = "Temperature · °C"
    override val dialContext = "Context for dialing in espresso"
    override val linkedAttempt = "New attempt linked to a previous extraction."
    override val recipeCopied = "Recipe copied to the clipboard!"
    override val copyRecipe = "Copy recipe / summary"
    override val saveAsRecipe = "Save as recipe"
    override val adjustNextExtraction = "Dial in the next extraction"
    override val removeFavorite = "Remove favorite"
    override val markFavorite = "Mark as favorite"
    override val editShot = "Edit shot"
    override val deleteShotQuestion = "Delete this shot?"
    override val yesDelete = "Yes, delete"
    override val deleteShot = "Delete shot"
    override val beanPhoto = "Coffee photo"
    override val adjustEspresso = "Dial in espresso"
    override val hideDialContext = "Hide dial-in context"
    override val showDialContext = "Context for dialing in espresso (optional)"
    override val editShotTaste = "Edit shot / rate the taste"
    override val nextExtractionTitle = "Next extraction"
    override val noteGrindInForm = "Write the new grind setting in the form."
    override val measureAndLogAttempt = "Time it and log how this new attempt tasted."
    override val prepareNextAttempt = "Brew the next attempt"
    override val compareFromPump = "To compare times, start when the pump turns on."
    override val tasteNotTime =
        "Compare the taste with the previous attempt. Time and ratio do not " +
        "diagnose the extraction on their own."
    override val comparableRefs = "Your comparable references"
    override val refsWillAppear =
        "Favorite or high-scoring balanced recipes become references when the " +
        "coffee, equipment, and context match."
    override val evolution = "History"
    override val previousAttemptGone = "The previous attempt is no longer available."
    override val viewAttempt = "View attempt"
    override val compareTitle = "Compare attempts"
    override val referenceHeading = "Reference"
    override val compareHint =
        "Pick 2 or 3 shots. The first one is the reference; changes are " +
        "highlighted."
    override val differentCoffeesNote =
        "These are different coffees: taste differences are not only from the " +
        "settings."
    override val descriptiveCompare =
        "Descriptive comparison: time and ratio do not decide quality on " +
        "their own. Water and milk also change the taste."
    override val scrollTable = "Scroll the table sideways to see every attempt."
    override val preferThis = "I prefer this one"
    override val preferThisNote =
        "“I prefer this one” saves it as a favorite for later assistant " +
        "references; it does not change your other favorites."
    override val addShots = "Add shots"
    override val allCoffees = "All coffees"
    override val favoritesOnly = "Favorites only"
    override val noShotsForFilters = "No shots match these filters."
    override val recipeTitle = "Recipe"
    override val yourRecipe = "Your recipe"
    override val doseKeepsRatio =
        "Changing dose keeps the ratio; changing yield recalculates the " +
        "ratio."
    override val doseFieldG = "Dose · g"
    override val extractionRatioField = "Extraction ratio · 1:"
    override val outputFieldG = "Yield · g"
    override val extraWaterOptional = "Added water · ml (optional)"
    override val milkOptionalMl = "Milk · ml (optional)"
    override val saveOrBrew = "Save or brew"
    override val timeLoggedAgain = "Time, first drop, and taste are logged again when you brew."
    override val checkRecipeFields = "Check the name, grind, and amounts."
    override val recipeAmountHint = "Use a dose of 0.1 to 100 g and a yield of 0.1 to 1000 g."
    override val grindKind = "Grind type"
    override val veryFine = "Very fine"
    override val veryCoarse = "Very coarse"
    override val millAdjustment = "Mill adjustment"
    override val grindHint = "Pick a level or write the real setting: 18 clicks, 2.3, 1.4.2…"
    override val hideProGrind = "Hide pro grind settings"
    override val showProGrind = "Show pro grind settings"
    override val proGrindTitle = "Rotations · Dial · Clicks"
    override val proGrindHint =
        "Use this notation if your grinder has it. Each scale belongs to that " +
        "grinder; it does not convert fine or coarse into clicks."
    override val rotations = "Rotations"
    override val dial = "Dial"
    override val clicks = "Clicks"
    override val tapToApplyPro =
        "Tap a control to apply the pro setting; your current value is still " +
        "kept."
    override val adminReadOnly = "Read only · data synced on the server"
    override val adminSignIn = "Sign in with the admin account."
    override val allAccounts = "All accounts"
    override val refresh = "Refresh"
    override val noScoreYet = "no data"
    override val accountsAppearLater =
        "Older accounts appear after their next sync. Changes that were not " +
        "uploaded are not included."
    override val activity30Days = "Activity · last 30 days (UTC)"
    override val noShotsInPeriod = "No shots in this period."
    override val topStyles = "Most used styles"
    override val accountsHeading = "Accounts"
    override val firstPage = "First page"
    override val moreAccounts = "More accounts"
    override val filterStyle = "Filter style"
    override val fromDateUtc = "From · YYYY-MM-DD (UTC)"
    override val toDateUtc = "To · YYYY-MM-DD (UTC)"
    override val scoreOptional = "Score · 1 to 5 (optional)"
    override val checkDatesAndScore = "Check the dates and the score."
    override val noAdminPermission = "This account does not have admin permission."
    override val sessionExpired = "The session expired. Sign in again."
    override val adminQueryFailed =
        "We could not load the panel. Try again; your data was not changed."
    override val couldNotSaveDefault = "We could not save the default. Try again."
    override val couldNotSaveFavorite = "We could not save the favorite. Try again."
    override val conflictChanges = "There are conflicting changes. Resolve them in Account."
    override val couldNotSync = "Could not sync."
    override val couldNotSaveRecipe = "We could not save the recipe. Try again."
    override val couldNotDeleteRecipe = "We could not delete the recipe. Try again."
    override val resolvingConflict = "Resolving the conflict…"
    override val noPendingConflicts = "There are no pending conflicts."
    override val conflictKept = "We could not resolve the conflict. Your data is still saved."
    override val couldNotSaveMachine = "We could not save the machine."
    override val couldNotSaveBean = "We could not save the coffee."
    override val couldNotSaveGrinder = "We could not save the grinder."
    override val couldNotSaveCup = "We could not save the cup."
    override val logShotToDial = "Log a shot and rate its taste to start dialing in."
    override val couldNotSaveContext = "We could not save the context. Try again."
    override val couldNotSaveShot =
        "We could not save the shot. Your data is still here; try again."
    override val shotNotFound = "We could not find this shot."
    override val appName = "Coffee Dial"
    override val today = "Today"
    override val yesterday = "Yesterday"
    override val errBeanName = "Enter the coffee name"
    override val errPhoto = "The photo is not valid."
    override val errBagWeight = "Enter a valid bag weight in grams"
    override val errMachineName = "Enter the machine name"
    override val errYear = "Enter a valid year (e.g. 2022)"
    override val errGrinderName = "Enter the grinder name"
    override val errGrinderType = "Choose the grinder type"
    override val errCupName = "Enter the cup name"
    override val errWeight = "Enter a valid weight in grams"
    override val errCapacity = "Enter a valid capacity in cm³"
    override val errDose = "Use between 0.1 and 100 g"
    override val errOutput = "Use between 0.1 and 1000 g"
    override val errSeconds = "Use between 0.1 and 600 s"
    override val errFirstDrop = "First drop must be between 0 and the total time"
    override val errGrind = "Enter the grind setting"
    override val errTemperature = "Use 1 to 100 °C or leave it empty"
    override val errMilk = "Use 1 to 200 ml or leave it empty"
    override val errWater = "Use 1 to 1000 ml or leave it empty"
    override val errStyleLength = "Use up to 100 characters"
    override val errDialIn = "Check the dial-in details"
    override val errTaste = "Use values from 1 to 5"
    override val errRating = "Choose from 1 to 5"
    override val adviceCheckContext = "Check the context"
    override val adviceCheckContextBody = "The grinder name can be up to 200 characters."
    override val advicePickMethod = "Choose the brew method"
    override val advicePickMethodBody = "Confirm how you brewed this coffee."
    override val adviceEspressoOnly = "Espresso for now"
    override val adviceEspressoOnlyBody =
        "This assistant dials in espresso. It does not apply these recipes to " +
        "filter or moka."
    override val advicePickRoast = "Set the roast"
    override val advicePickRoastBody = "If you don't know, choose Unknown."
    override val advicePickGoal = "Choose your goal"
    override val advicePickGoalBody =
        "You can aim for more intensity, a milder cup, or keep the balance."
    override val adviceLogTaste = "First, log the taste"
    override val adviceLogTasteBody =
        "Edit the shot and pick how it tasted in Flavor profile. Time and " +
        "ratio alone are not enough."
    override val adviceWhatTasted = "What did you taste?"
    override val adviceWhatTastedBody =
        "Separate the straight espresso from the drink with water or milk."
    override val adviceLogAdditions = "Log water and milk"
    override val adviceLogAdditionsBody =
        "Fill in the added amounts before adjusting the finished drink."
    override val adviceRepeatCompare = "Repeat and compare"
    override val adviceAtLimit =
        "This recipe is already at the limit of this adjustment. Repeat it " +
        "and taste before changing another variable."
    override val adviceKeepDose = "Keep dose, grind, and temperature; time the new shot."
    override val adviceGoalClash = "Check taste and goal"
    override val adviceGoalClashBody =
        "The taste rating and the intensity you want point in opposite " +
        "directions. Confirm them before changing the recipe."
    override val adviceEspressoAlone = "Taste the espresso on its own"
    override val adviceEspressoAloneBody =
        "Water and milk change how it tastes. Repeat the recipe and rate the " +
        "espresso before adding them; there is no basis to judge the " +
        "extraction."
    override val adviceAdditionLimit =
        "The added amount is at the allowed limit. Taste the espresso on its " +
        "own before changing the extraction."
    override val adviceOneAddition =
        "To change the strength of the finished drink, adjust one addition " +
        "first. Keep the espresso recipe."
    override val adviceKeepRecipe = "Keep the recipe"
    override val adviceBalancedDrink =
        "The drink tasted balanced. Repeat it to check consistency and mark " +
        "it as a favorite if you prefer it."
    override val adviceCheckConsistency = "Check consistency"
    override val adviceHighAcidityBitter =
        "High acidity and bitterness can have several causes. Repeat with an " +
        "even coffee bed before changing the recipe; we cannot call it under- " +
        "or over-extracted."
    override val adviceConfirmGrinder = "Confirm the grinder"
    override val adviceLongSour =
        "With a long yield and a sour taste, check distribution and grind. " +
        "Name your grinder before trying an adjustment."
    override val adviceSlightlyFiner = "Try a slightly finer setting"
    override val adviceAlreadyLong =
        "You are already using a long yield. If your grinder can be adjusted, " +
        "take a small step finer and keep dose and yield. Log the real " +
        "setting; there is no universal click count."
    override val adviceMoreYieldSour =
        "Because it tasted too sour, try a little more yield. That can also " +
        "lower intensity: focus on tasting this attempt."
    override val adviceShortBitter =
        "With an already short yield, check grind and distribution. Name your " +
        "grinder before trying to change it."
    override val adviceSlightlyCoarser = "Try a slightly coarser setting"
    override val adviceAlreadyShort =
        "The yield is already short. If you can adjust the grind, try a small " +
        "step coarser while keeping dose and yield; compare the taste."
    override val adviceLessYieldBitter =
        "Try a slightly lower yield and compare the bitterness. This can " +
        "raise concentration; it does not prove over-extraction."
    override val adviceStrongerYield =
        "For more intensity, try less yield with the same dose. Also check " +
        "whether acidity shows up."
    override val advicePersonalMilder =
        "Your preferred comparable recipes are a reference for a higher " +
        "yield."
    override val adviceMilderYield =
        "For a milder espresso, try a bit more yield and compare whether " +
        "bitterness increases."
    override val adviceKeepBalanced =
        "You logged it as balanced and want to keep it. Repeat it to check " +
        "consistency and mark the one you prefer as a favorite."
    override val shotDetailTitle = "Shot detail"
    override val compareWithOthers = "Compare with other attempts"

    override fun extractionGuides() = listOf(
        GuideCard(
            "Ristretto",
            "Up to 1:1.5",
            "Short shot. More body and sweetness, less bitterness."
        ),
        GuideCard(
            "Espresso",
            "Above 1:1.5 and up to 1:2.5",
            "The usual range. Body, acidity, and sweetness in balance."
        ),
        GuideCard(
            "Long espresso",
            "Above 1:2.5 and up to 1:3",
            "A little more water in the extraction. Lighter."
        ),
        GuideCard("Lungo", "Above 1:3", "More water during extraction. Not an americano."),
    )

    override fun timeGuides() = listOf(
        TimeGuide(
            "<15 s",
            "Very fast",
            "Very underextracted",
            "Thin, sour, little body",
            "Grind much finer"
        ),
        TimeGuide(
            "15-20 s",
            "Fast",
            "Underextracted",
            "Sour, not very sweet, short finish",
            "Grind finer"
        ),
        TimeGuide(
            "20-25 s",
            "A bit fast",
            "May be underextracted",
            "Can already taste good; it depends on the coffee",
            "A bit finer if it lacks body"
        ),
        TimeGuide(
            "25-30 s",
            "Normal",
            "Classic range",
            "Balance, sweetness, and body",
            "Fine-tune to taste"
        ),
        TimeGuide(
            "30-35 s",
            "Slow",
            "Can be perfect",
            "More body and intensity",
            "If it tastes good, leave it"
        ),
        TimeGuide(
            "35-40 s",
            "Very slow",
            "Trending overextracted",
            "Bitter, dry, astringent",
            "Grind a bit coarser"
        ),
        TimeGuide(
            ">40 s",
            "Dripping",
            "Likely overextracted",
            "Very bitter, dry, harsh",
            "Grind coarser"
        ),
    )

    override fun grainGuides() = listOf(
        GuideCard(
            "Arabica and robusta",
            "Species",
            "Arabica: more aroma, acidity, and sweetness. Robusta: more body, " +
                "crema, and bitterness."
        ),
        GuideCard(
            "Bourbon, Caturra, Catuaí",
            "Classic varieties",
            "Silky body, chocolate, caramel, red fruit, and citrus acidity."
        ),
        GuideCard(
            "Geisha",
            "High-end variety",
            "Floral, bright citrus acidity, lime, and a light body."
        ),
        GuideCard(
            "Pacamara and Maragogipe",
            "Large beans",
            "Spice, dark chocolate, nuts, and winey acidity."
        ),
        GuideCard("Colombia", "Origin", "Milk chocolate, red fruit, caramel, and a round body."),
        GuideCard(
            "Ethiopia",
            "Origin",
            "Citrus, jasmine, bergamot, black tea, and lively acidity."
        ),
        GuideCard("Brazil", "Origin", "Dense body, low acidity, hazelnut, cocoa, and walnut."),
    )

    override fun flowAndTaste(flow: String, taste: String) = "Flow: $flow. Taste: $taste."

    override fun whatToTry(advice: String) = "What to try: $advice"

    override fun ratioParts(water: String, milk: String) = "1 : $water : $milk"

    override fun doseCoffeeExtraction(dose: String, coffee: String, ratio: String) =
        "$dose g dose · $coffee g coffee · extraction 1:$ratio"

    override fun waterAndMilkMl(water: String, milk: String) = "$water ml water · $milk ml milk"

    override fun knownDrinkName(name: String) = when (name) {
        "Americano corto" -> "Short americano"
        "Americano suave" -> "Mild americano"
        "Latte más lechoso" -> "Milkier latte"
        "Americano con un toque de leche" -> "Americano with a splash of milk"
        "Agua y leche, más cremoso" -> "Water and milk, creamier"
        "Receta personal" -> "Custom recipe"
        "Sin agregados" -> "No additions"
        "Mezcla personal" -> "Custom mix"
        "Espresso largo" -> "Long espresso"
        else -> name
    }

    override fun grindLevelLabel(canonical: String) = when (canonical) {
        "Muy fino" -> "Very fine"
        "Fino" -> "Fine"
        "Medio fino" -> "Medium-fine"
        "Medio" -> "Medium"
        "Medio grueso" -> "Medium-coarse"
        "Grueso" -> "Coarse"
        "Muy grueso" -> "Very coarse"
        else -> canonical
    }

    override fun machineTypeLabel(canonical: String) = when (canonical) {
        "Cápsulas" -> "Capsules"
        "Filtro" -> "Filter"
        "Prensa" -> "Press"
        else -> canonical
    }

    override fun grinderTypeLabel(canonical: String) = when (canonical) {
        "Eléctrico" -> "Electric"
        "Manual" -> "Manual"
        else -> canonical
    }

    override fun conflictLabel(canonical: String) = when (canonical) {
        "Sin predeterminado" -> "No default"
        "Café" -> "Coffee"
        "Máquina" -> "Machine"
        "Taza" -> "Cup"
        "Molinillo" -> "Grinder"
        "Café predeterminado" -> "Default coffee"
        "Máquina predeterminada" -> "Default machine"
        "Molinillo predeterminado" -> "Default grinder"
        "Taza predeterminada" -> "Default cup"
        "Receta" -> "Recipe"
        "Shot" -> "Shot"
        "Selección" -> "Selection"
        "Dosis" -> "Dose"
        "Salida" -> "Yield"
        "Tiempo" -> "Time"
        "Molienda" -> "Grind"
        "Temperatura" -> "Temperature"
        "Leche" -> "Milk"
        "Espuma" -> "Foam"
        "Agua extra" -> "Added water"
        "Estilo" -> "Style"
        "Vínculo de máquina" -> "Machine link"
        "Vínculo de taza" -> "Cup link"
        "Vínculo de molinillo" -> "Grinder link"
        "Primera gota" -> "First drop"
        "Notas" -> "Notes"
        "Puntuación" -> "Score"
        "Sabor" -> "Taste"
        "Ajuste" -> "Dial-in"
        "Nombre" -> "Name"
        "Tostador" -> "Roaster"
        "Origen" -> "Origin"
        "Proceso" -> "Process"
        "Bolsa (g)" -> "Bag (g)"
        "Archivado" -> "Archived"
        "sí" -> "yes"
        "no" -> "no"
        "Tipo" -> "Type"
        "Año" -> "Year"
        "Peso" -> "Weight"
        "Capacidad" -> "Capacity"
        "Parámetros" -> "Parameters"
        "Estado" -> "Status"
        "Eliminado en este dispositivo" -> "Deleted on this device"
        "Conservado en este dispositivo" -> "Kept on this device"
        "Eliminado en el otro dispositivo" -> "Deleted on the other device"
        "Conservado en el otro dispositivo" -> "Kept on the other device"
        "Registro" -> "Record"
        "Distinto" -> "Different"
        "Elemento no disponible" -> "Item unavailable"
        else -> canonical
    }

    override fun firstDropAt(seconds: String) = "First drop: $seconds s"

    override fun backupNewCounts(
        beans: Int,
        shots: Int,
        machines: Int,
        grinders: Int,
        cups: Int,
        recipes: Int,
        defaults: Int
    ) =
        "$beans coffees · $shots shots · $machines machines · $grinders " +
            "grinders · $cups cups · $recipes new recipes · $defaults defaults"

    override fun backupDuplicates(count: Int) = "$count shots already saved."

    override fun backupImported(beans: Int, shots: Int, grinders: Int, cups: Int, recipes: Int) =
        "Import complete: $beans coffees, $shots shots, $grinders grinders, " +
            "$cups cups, and $recipes recipes processed."

    override fun backupReplaced(
        beans: Int,
        shots: Int,
        machines: Int,
        grinders: Int,
        cups: Int,
        recipes: Int
    ) =
        "Replace complete: $beans coffees, $shots shots, $machines machines, " +
            "$grinders grinders, $cups cups, and $recipes recipes loaded from the " +
            "backup."

    override fun thisDeviceValue(value: String) = "This device: $value"

    override fun otherDeviceValue(value: String) = "The other device: $value"

    override fun grindAdjustment(value: String) = "Mill adjustment · $value"

    override fun prepLabel(label: String) = "Brew · $label"

    override fun roastLabel(label: String) = "Roast · $label"

    override fun doseYieldLine(dose: String, yield: String) = "Dose: $dose g · Yield: $yield g"

    override fun milkLine(ml: String) = "Milk: $ml ml"

    override fun waterLine(ml: String) = "Added water: $ml ml"

    override fun referencesCount(count: Int) = "Your comparable references ($count)"

    override fun attemptNumber(index: Int, favorite: Boolean) =
        "Attempt $index" + if (favorite) " · favorite" else ""

    override fun nextAttemptLine(output: String, seconds: String) =
        "Next attempt: $output g · $seconds s"

    override fun tasteRating(taste: String, rating: Int) = "$taste · $rating/5"

    override fun selectedCount(count: Int) = "Selected ($count/3)"

    override fun referenceName(name: String) = "Reference: $name"

    override fun useAsReference(index: Int) = "$index: use as reference"

    override fun attemptColumn(index: Int) = "Attempt $index"

    override fun temperatureLine(value: String) = "Temperature: $value °C"

    override fun savedAdjustment(value: String) = "Saved setting: $value"

    override fun decreaseLabel(label: String) = "Decrease $label"

    override fun increaseLabel(label: String) = "Increase $label"

    override fun accountsAndShots(accounts: Int, shots: Int) = "$accounts accounts · $shots shots"

    override fun last7Days(count: Int) = "Last 7 days: $count shots"

    override fun averageScore(value: String) = "Average score: $value"

    override fun styleCount(label: String, count: Int) = "$label: $count"

    override fun shotCount(count: Int) = "$count shots"

    override fun lastConfirm(whenText: String) = "Last confirmation: $whenText"

    override fun filteredShotCount(shown: Int, total: Int) = "$shown of $total shots"

    override fun weekdayShort(index: Int) =
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index]

    override fun ratingOfFive(rating: Int) = "Score: $rating of 5"

    override fun rateWithCups(value: Int) = "Rate $value of 5 coffees"

    override fun cupTotalDescription(total: String, dose: String, water: String, milk: String) =
        "Cup with total $total. Dose $dose g, water $water ml, milk $milk ml."

    override fun foamSpoken(label: String) = " Foam: $label."

    override fun foamRatioSpoken(ratio: String) = " Foam : liquid milk ≈ $ratio."

    override fun capacitySpoken(capacity: String) = " Capacity $capacity cm³."

    override fun adviceTryYield(grams: String) = "Try $grams g of espresso"

    override fun adviceTryAddition(amount: String, water: Boolean): String {
        val liquid = if (water) "water" else "milk"
        return "Try $amount ml of $liquid"
    }

    override fun advicePersonalStronger(count: Int) =
        "Using the middle ratio of $count comparable recipes you preferred, " +
            "without changing everything at once."

    override fun roastNote(roast: RoastLevel?) = when (roast) {
        RoastLevel.LIGHT -> "Light roasts can have acidity that belongs to the coffee. "
        RoastLevel.DARK -> "Dark roasts can have bitterness that belongs to the roast. "
        RoastLevel.UNKNOWN -> "Without knowing the roast, this is a conservative try. "
        else -> ""
    }

    override fun roastAge(days: Int) = when (days) {
        0 -> "Roasted today"
        1 -> "Roasted yesterday"
        in 2..6 -> "Roasted $days days ago (degassing)"
        in 7..30 -> "Roasted $days days ago (ideal window ☕)"
        else -> "Roasted $days days ago"
    }

    override fun preInfusion(pre: String, extraction: String) =
        "Preinfusion: $pre s · Extraction: $extraction s"

    override fun clearField(label: String) = "Clear $label"

    override fun userMessage(raw: String?): String {
        val text = raw.orEmpty()
        val tail = text.substringAfter(':', "")
        return when {
            text.startsWith("i18n.invalid:") ->
                "Could not validate the data ($tail). It was kept unchanged."
            text.startsWith("i18n.backup_version:") ->
                "This backup version is not supported ($tail)."
            text.startsWith("i18n.sync_status:") -> "Could not finish sync ($tail)."
            text.startsWith("i18n.backup_invalid:") -> "The backup contains invalid data: $tail"
            text.startsWith("i18n.conflict_count:") ->
                "There are different changes in $tail records. Choose which copy to " +
                "keep."
            else -> when (text) {
                "i18n.other_account" -> "Local data belongs to another account."
                "i18n.other_account_long" ->
                    "Local data belongs to another account. Sign back into that account " +
                    "to sync, or export a backup before switching accounts."
                "i18n.sign_in" -> "Open Account to sign in and sync."
                "i18n.resolve_sign_in" -> "Open Account to resolve the conflict."
                "i18n.account_changed" ->
                    "The account changed. Sync again; your local data was not replaced."
                "i18n.incompatible" -> "Update Coffee Dial: the sync format is not compatible."
                "i18n.upgrade_sync" -> "Update Coffee Dial to sync."
                "i18n.resolve_failed" -> "Could not resolve the conflict."
                "i18n.offline" -> "Could not connect. Your data is still saved; try again."
                "i18n.sync_failed" -> "Could not sync."
                "i18n.response" -> "Incompatible sync response."
                "i18n.missing_photo" -> "A local photo is missing. Sync again before exporting."
                "i18n.renew" ->
                    "We need to renew your access. Open Account and continue with your " +
                    "provider."
                "i18n.forbidden" -> "This account cannot sync."
                "i18n.stale" -> "Another device saved changes. Sync again; your data is kept."
                "i18n.upgrade_app" -> "Update this app: the cloud has data from a newer version."
                "i18n.too_big" -> "The log exceeds the sync limit. Export a backup to keep it."
                "i18n.rate" -> "Too many attempts. Wait a moment and sync again."
                "i18n.unavailable" ->
                    "The service is unavailable right now. Try again; you do not need to " +
                    "sign out."
                "i18n.not_backup" -> "This file is not a Coffee Dial backup."
                "i18n.bad_version" -> "The backup does not have a valid version."
                "i18n.newer_backup" -> "This backup is from a newer version. Update Coffee Dial."
                "i18n.backup_damaged" -> "The backup is damaged or has incompatible fields."
                "i18n.backup_invalid" -> "The backup contains invalid data."
                "i18n.backup_photos" ->
                    "The backup does not include every photo. Sync before exporting."
                "i18n.backup_size" -> "The backup exceeds the 10 MB limit."
                "i18n.local_damaged" ->
                    "Local data has an incompatible or damaged format. It was not " +
                    "changed."
                "i18n.other_tab" -> "Data changed in another tab. Review it and try again."
                "i18n.sync_changed" ->
                    "Data changed during sync. Your changes are saved; sync again."
                "i18n.tab_changed" -> "Another tab changed the data. Sync again."
                "i18n.shot_gone" -> "This shot no longer exists."
                "i18n.bean_gone" -> "The coffee no longer exists."
                "i18n.cup_gone" -> "The cup no longer exists."
                "i18n.grinder_gone" -> "The grinder no longer exists."
                "i18n.machine_gone" -> "The machine no longer exists."
                "i18n.history_changed" -> "The log changed. Select the backup again to review it."
                "i18n.saved_session_other" ->
                    "The saved session belongs to another account. Sign in again."
                "i18n.renew_google_keep" ->
                    "Continue with Google again to renew access. Your data is kept."
                "i18n.renew_provider_keep" ->
                    "Continue with your provider again to renew access. Your data is " +
                    "kept."
                "i18n.sign_in_expired" -> "The sign-in attempt expired. Try again."
                "i18n.google_not_configured" ->
                    "Google is not configured for this version of Coffee Dial yet."
                "i18n.could_not_sign_in_google" ->
                    "Could not sign in with Google. Check the connection and the app's " +
                    "sign-in setup."
                "i18n.could_not_open_browser" ->
                    "Could not open the browser. Install or enable one and try again."
                "i18n.could_not_complete_web" -> "Could not finish web sign-in. Try again."
                "i18n.apple_android_later" ->
                    "Apple on Android will be available once web sign-in is set up. You " +
                    "can keep using the app without an account."
                "i18n.could_not_renew_google" ->
                    "Could not renew Google. Check the connection and try again."
                "i18n.access_not_ready" -> "Sign-in is not available yet."
                "i18n.provider_identity" -> "The provider did not return a valid identity."
                "i18n.could_not_verify_access" -> "Could not verify access. Sign in again."
                "i18n.could_not_open_google" -> "Could not open Google. Enable browser storage."
                "i18n.apple_web_unavailable" -> "Sign in with Apple is not set up on the web"
                "i18n.could_not_open_photos" -> "Could not open the photo picker."
                "i18n.could_not_prepare_photo" ->
                    "Could not prepare the photo. Choose another image."
                "i18n.could_not_open_files" -> "Could not open the file picker."
                "i18n.could_not_save_file" -> "Could not save the file. Export it again."
                "i18n.could_not_read_backup" ->
                    "Could not read the file. Choose a UTF-8 backup up to 10 MB."
                "i18n.export_interrupted" -> "The export was interrupted. Try again."
                "i18n.backup_exported" -> "Backup exported."
                "i18n.read_interrupted" -> "The read was interrupted. Select the file again."
                "i18n.backup_download_ready" -> "Backup ready to download."
                "i18n.could_not_download" -> "Could not download the backup."
                "i18n.photo_invalid" -> "Invalid photo."
                "i18n.photo_too_big" -> "The photo is over 100 KB."
                "i18n.photo_jpeg" -> "Invalid JPEG photo."
                "i18n.photo_pick_limit" -> "Choose an image up to 20 MB."
                "i18n.could_not_read_photo" ->
                    "Could not read the photo. Try JPEG or PNG."
                "i18n.apple_not_configured" ->
                    "Apple is not set up for this version of Coffee Dial yet."
                "i18n.apple_invalid_credential" ->
                    "Apple did not return a valid credential."
                "i18n.could_not_sign_in_apple" ->
                    "Could not sign in with Apple. Check the device account and " +
                    "try again."
                "i18n.sign_in_google" -> "Sign in with Google."
                "i18n.sign_in_google_again" -> "Sign in with Google again."
                "i18n.account_changed_admin" -> "The account changed. Open Admin again."
                else -> text
            }
        }
    }
}
