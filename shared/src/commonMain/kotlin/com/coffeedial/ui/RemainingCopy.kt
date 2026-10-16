package com.coffeedial.ui

import com.coffeedial.domain.RoastLevel

/** User-facing copy that used to be hardcoded in screens. */
interface RemainingCopy {
    val tipsTitle: String
    val tabStyles: String
    val tabTimes: String
    val tabGrains: String
    val tipsSplitTitle: String
    val tipsSplitBody: String
    val extractionHeading: String
    val drinksHeading: String
    val drinksIntro: String
    val otherMixesHeading: String
    val otherMixesBody: String
    val macchiatoFootnote: String
    val timeHeading: String
    val timeIntro: String
    val grainsHeading: String
    val grainsIntro: String
    fun flowAndTaste(flow: String, taste: String): String
    fun whatToTry(advice: String): String
    fun ratioParts(water: String, milk: String): String
    fun doseCoffeeExtraction(dose: String, coffee: String, ratio: String): String
    fun waterAndMilkMl(water: String, milk: String): String
    fun extractionGuides(): List<GuideCard>
    fun timeGuides(): List<TimeGuide>
    fun grainGuides(): List<GuideCard>
    fun knownDrinkName(name: String): String

    val shotTimerTitle: String
    val shotTimerHint: String
    val extractionRunning: String
    val markFirstDrop: String
    val removeMark: String
    val timerMaxReached: String
    val logTheExtraction: String
    val useTheseTimes: String
    val logShotWithTimes: String
    val timerIncludesFirstDrop: String
    val timerPause: String
    val timerStart: String
    val timerResume: String
    val timerReset: String
    val additionsHeading: String
    fun firstDropAt(seconds: String): String

    val backupTitle: String
    val backupIntro: String
    val backupPrivacy: String
    val exportBackup: String
    val importBackup: String
    val reviewBeforeImport: String
    val mergeBackup: String
    val replaceAllBackup: String
    val backupFailed: String
    fun backupNewCounts(
        beans: Int,
        shots: Int,
        machines: Int,
        grinders: Int,
        cups: Int,
        recipes: Int,
        defaults: Int
    ): String
    fun backupDuplicates(count: Int): String
    fun backupImported(beans: Int, shots: Int, grinders: Int, cups: Int, recipes: Int): String
    fun backupReplaced(
        beans: Int,
        shots: Int,
        machines: Int,
        grinders: Int,
        cups: Int,
        recipes: Int
    ): String

    val conflictsTitle: String
    val conflictsBody: String
    fun thisDeviceValue(value: String): String
    fun otherDeviceValue(value: String): String
    val keepThisCopy: String
    val keepOtherCopy: String

    val beanNameField: String
    val beanSection: String
    val addSavedBean: String
    val roasterOptionalField: String
    val extractionSection: String
    val extractionHint: String
    val measureWithTimer: String
    val firstDropOptional: String
    val temperatureOptional: String
    val yourDrink: String
    val drinkHint: String
    val styleOptional: String
    val waterAfterExtraction: String
    val equipmentAndGrind: String
    val orTypeMachine: String
    val machineOptional: String
    val addSavedMachine: String
    val orTypeGrinder: String
    val addSavedGrinder: String
    val orTypeCup: String
    val cupOptional: String
    val addSavedCup: String
    val howItTasted: String
    val notesOptional: String
    val ratingQuestion: String
    val nextAttempt: String
    val checkMarkedFields: String
    val doseIn: String
    val outputField: String
    val yearField: String
    val grinderTypeElectric: String
    val grinderTypeManual: String
    val grinderOptionalField: String
    val logShot: String
    val actionClose: String
    val notRated: String

    val detailExtraction: String
    val extractionPhase: String
    val waterMilkDoNotChangeRatio: String
    fun grindAdjustment(value: String): String
    val temperatureC: String
    val dialContext: String
    val linkedAttempt: String
    val recipeCopied: String
    val copyRecipe: String
    val saveAsRecipe: String
    val adjustNextExtraction: String
    val removeFavorite: String
    val markFavorite: String
    val editShot: String
    val deleteShotQuestion: String
    val yesDelete: String
    val deleteShot: String
    val beanPhoto: String
    fun prepLabel(label: String): String
    fun roastLabel(label: String): String

    val adjustEspresso: String
    val hideDialContext: String
    val showDialContext: String
    val editShotTaste: String
    val nextExtractionTitle: String
    fun doseYieldLine(dose: String, yield: String): String
    fun milkLine(ml: String): String
    fun waterLine(ml: String): String
    val noteGrindInForm: String
    val measureAndLogAttempt: String
    val prepareNextAttempt: String
    val compareFromPump: String
    val tasteNotTime: String
    val comparableRefs: String
    fun referencesCount(count: Int): String
    val refsWillAppear: String
    val evolution: String
    val previousAttemptGone: String
    fun attemptNumber(index: Int, favorite: Boolean): String
    val viewAttempt: String
    fun nextAttemptLine(output: String, seconds: String): String
    fun tasteRating(taste: String, rating: Int): String

    val compareTitle: String
    val compareHint: String
    val referenceHeading: String
    fun selectedCount(count: Int): String
    fun referenceName(name: String): String
    fun useAsReference(index: Int): String
    val differentCoffeesNote: String
    val descriptiveCompare: String
    val scrollTable: String
    fun attemptColumn(index: Int): String
    val preferThis: String
    val preferThisNote: String
    val addShots: String
    val allCoffees: String
    val favoritesOnly: String
    val noShotsForFilters: String

    val recipeTitle: String
    val yourRecipe: String
    val doseKeepsRatio: String
    val doseFieldG: String
    val extractionRatioField: String
    val outputFieldG: String
    val extraWaterOptional: String
    val milkOptionalMl: String
    val saveOrBrew: String
    val timeLoggedAgain: String
    val checkRecipeFields: String
    val recipeAmountHint: String
    fun temperatureLine(value: String): String

    val grindKind: String
    val veryFine: String
    val veryCoarse: String
    val millAdjustment: String
    val grindHint: String
    val hideProGrind: String
    val showProGrind: String
    val grindModes: List<String>
    val grindModeHints: List<String>
    val proGrindTitle: String
    val proGrindHint: String
    val rotations: String
    val dial: String
    val clicks: String
    val tapToApplyPro: String
    fun savedAdjustment(value: String): String
    fun decreaseLabel(label: String): String
    fun increaseLabel(label: String): String
    fun grindLevelLabel(canonical: String): String
    fun machineTypeLabel(canonical: String): String
    fun grinderTypeLabel(canonical: String): String

    val adminReadOnly: String
    val adminSignIn: String
    val allAccounts: String
    val refresh: String
    fun accountsAndShots(accounts: Int, shots: Int): String
    fun last7Days(count: Int): String
    fun averageScore(value: String): String
    val noScoreYet: String
    val accountsAppearLater: String
    val activity30Days: String
    val noShotsInPeriod: String
    fun styleCount(label: String, count: Int): String
    val topStyles: String
    val accountsHeading: String
    fun shotCount(count: Int): String
    fun lastConfirm(whenText: String): String
    val firstPage: String
    val moreAccounts: String
    val filterStyle: String
    val fromDateUtc: String
    val toDateUtc: String
    val scoreOptional: String
    val checkDatesAndScore: String
    fun filteredShotCount(shown: Int, total: Int): String
    val noAdminPermission: String
    val sessionExpired: String
    val adminQueryFailed: String

    val couldNotSaveDefault: String
    val couldNotSaveFavorite: String
    val conflictChanges: String
    val couldNotSync: String
    val couldNotSaveRecipe: String
    val couldNotDeleteRecipe: String
    val resolvingConflict: String
    val noPendingConflicts: String
    val conflictKept: String
    val couldNotSaveMachine: String
    val couldNotSaveBean: String
    val couldNotSaveGrinder: String
    val couldNotSaveCup: String
    val logShotToDial: String
    val couldNotSaveContext: String
    val couldNotSaveShot: String
    val shotNotFound: String
    val appName: String

    val today: String
    val yesterday: String
    fun weekdayShort(index: Int): String
    fun ratingOfFive(rating: Int): String
    fun rateWithCups(value: Int): String
    fun cupTotalDescription(total: String, dose: String, water: String, milk: String): String
    fun foamSpoken(label: String): String
    fun foamRatioSpoken(ratio: String): String
    fun capacitySpoken(capacity: String): String

    val errBeanName: String
    val errPhoto: String
    val errBagWeight: String
    val errMachineName: String
    val errYear: String
    val errGrinderName: String
    val errGrinderType: String
    val errCupName: String
    val errWeight: String
    val errCapacity: String
    val errDose: String
    val errOutput: String
    val errSeconds: String
    val errFirstDrop: String
    val errGrind: String
    val errTemperature: String
    val editAmount: String
    val cancelAmount: String
    val acceptAmount: String
    val errMilk: String
    val errWater: String
    val errStyleLength: String
    val errDialIn: String
    val errTaste: String
    val errRating: String

    val adviceCheckContext: String
    val adviceCheckContextBody: String
    val advicePickMethod: String
    val advicePickMethodBody: String
    val adviceEspressoOnly: String
    val adviceEspressoOnlyBody: String
    val advicePickRoast: String
    val advicePickRoastBody: String
    val advicePickGoal: String
    val advicePickGoalBody: String
    val adviceLogTaste: String
    val adviceLogTasteBody: String
    val adviceWhatTasted: String
    val adviceWhatTastedBody: String
    val adviceLogAdditions: String
    val adviceLogAdditionsBody: String
    val adviceRepeatCompare: String
    val adviceAtLimit: String
    fun adviceTryYield(grams: String): String
    val adviceKeepDose: String
    val adviceGoalClash: String
    val adviceGoalClashBody: String
    val adviceEspressoAlone: String
    val adviceEspressoAloneBody: String
    val adviceAdditionLimit: String
    fun adviceTryAddition(amount: String, water: Boolean): String
    val adviceOneAddition: String
    val adviceKeepRecipe: String
    val adviceBalancedDrink: String
    val adviceCheckConsistency: String
    val adviceHighAcidityBitter: String
    fun roastNote(roast: RoastLevel?): String
    val adviceConfirmGrinder: String
    val adviceLongSour: String
    val adviceSlightlyFiner: String
    val adviceAlreadyLong: String
    val adviceMoreYieldSour: String
    val adviceShortBitter: String
    val adviceSlightlyCoarser: String
    val adviceAlreadyShort: String
    val adviceLessYieldBitter: String
    fun advicePersonalStronger(count: Int): String
    val adviceStrongerYield: String
    val advicePersonalMilder: String
    val adviceMilderYield: String
    val adviceKeepBalanced: String

    val shotDetailTitle: String
    val compareWithOthers: String
    fun preInfusion(pre: String, extraction: String): String
    fun roastAge(days: Int): String
    fun conflictLabel(canonical: String): String
    fun clearField(label: String): String

    fun userMessage(raw: String?): String
}
