package com.coffeedial.ui

import com.coffeedial.domain.AppLanguage
import com.coffeedial.domain.CupProfile
import com.coffeedial.domain.MilkFoam
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanguageTest {

    @Test
    fun systemLanguageResolvesToPlatformDefault() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.SYSTEM.resolve(AppLanguage.ENGLISH))
        assertEquals(AppLanguage.SPANISH, AppLanguage.SYSTEM.resolve(AppLanguage.SPANISH))
    }

    @Test
    fun explicitLanguageChoiceOverridesPlatformDefault() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.ENGLISH.resolve(AppLanguage.SPANISH))
        assertEquals(AppLanguage.SPANISH, AppLanguage.SPANISH.resolve(AppLanguage.ENGLISH))
    }

    @Test
    fun englishStringsUseAccurateCoffeeSlangAndJargon() {
        val strings = AppStrings.forLanguage(AppLanguage.ENGLISH)

        // Specialty Coffee Terms & Slang
        assertEquals("Dose", strings.dose)
        assertEquals("Yield", strings.yield)
        assertEquals("Ratio", strings.ratio)
        assertEquals("Grind setting", strings.grind)
        assertEquals("Total time", strings.time)
        assertEquals("First drop", strings.firstDrop)
        assertEquals("Milk foam", strings.milkFoam)
        assertEquals("Beans", strings.beans)
        assertEquals("Roaster", strings.roaster)
        assertEquals("Roast date", strings.roastDate)
        assertEquals("Flavor profile", strings.flavorProfile)
        assertEquals("Dial-in Assistant", strings.dialInTitle)
        assertEquals("Grind finer", strings.grindFiner)
        assertEquals("Grind coarser", strings.grindCoarser)
        assertEquals("Brew again", strings.actionRepeat)
        assertEquals("Brew coffee", strings.homeBrewCoffee)
        assertEquals("In the cup", strings.inTheCup)
        assertEquals("Generic view: the drink fills the cup", strings.genericCupFill)
        assertEquals("Coffee : water : milk", strings.coffeeWaterMilkRatio)
        assertEquals("Tips and styles", strings.tipsTitle)
        assertEquals("Long espresso", strings.knownDrinkName("Espresso largo"))
        assertEquals("Enter the coffee name", strings.errBeanName)
        assertEquals("Fine", strings.grindLevelLabel("Fino"))
        assertEquals(
            "Local data belongs to another account.",
            strings.userMessage("i18n.other_account")
        )

        assertTrue(strings is EnglishStrings)
    }

    @Test
    fun spanishStringsPreserveStandardTerminology() {
        val strings = AppStrings.forLanguage(AppLanguage.SPANISH)

        assertEquals("Dosis", strings.dose)
        assertEquals("Extracción", strings.yield)
        assertEquals("Ratio", strings.ratio)
        assertEquals("Molienda", strings.grind)
        assertEquals("Tiempo total", strings.time)
        assertEquals("Primera gota", strings.firstDrop)
        assertEquals("Espuma de leche", strings.milkFoam)
        assertEquals("Granos", strings.beans)
        assertEquals("Tostador", strings.roaster)
        assertEquals("Fecha de tueste", strings.roastDate)
        assertEquals("Perfil de sabor", strings.flavorProfile)
        assertEquals("Asistente de Dial-in", strings.dialInTitle)
        assertEquals("Molienda más fina", strings.grindFiner)
        assertEquals("Molienda más gruesa", strings.grindCoarser)
        assertEquals("Repetir shot", strings.actionRepeat)
        assertEquals("Tips y estilos", strings.tipsTitle)
        assertEquals(
            "Vista genérica: el contenido llena la taza",
            strings.genericCupFill
        )
        assertEquals("Espresso largo", strings.knownDrinkName("Espresso largo"))
        assertEquals("Ingresá el nombre del café", strings.errBeanName)

        assertTrue(strings is SpanishStrings)
    }

    @Test
    fun enumLabelsTranslateCorrectly() {
        val en = EnglishStrings
        val es = SpanishStrings

        assertEquals("No foam", MilkFoam.NONE.label(en))
        assertEquals("Sin espuma", MilkFoam.NONE.label(es))
        assertEquals("High", MilkFoam.HIGH.label(en))
        assertEquals("Abundante", MilkFoam.HIGH.label(es))

        assertEquals("Small", CupProfile.SMALL.label(en))
        assertEquals("Chica", CupProfile.SMALL.label(es))
        assertEquals("Extra", CupProfile.EXTRA.label(en))
        assertEquals("Extra", CupProfile.EXTRA.label(es))
    }
}
