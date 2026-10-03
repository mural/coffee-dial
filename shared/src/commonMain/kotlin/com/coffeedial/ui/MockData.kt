package com.coffeedial.ui

import com.coffeedial.domain.Bean
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft

internal val mockBean = Bean(
    id = "1",
    name = "Bourbon Rosado",
    roaster = "Café Fuego"
)

internal val mockShot = Shot(
    id = "1",
    bean = mockBean,
    dose = 18.0,
    output = 36.0,
    seconds = 28.0,
    grind = "14.5",
    temperature = 93.0,
    milk = 60.0,
    rating = 5,
    notes = "Cuerpo medio, acidez brillante con notas a durazno y miel.",
    createdAt = 1710000000000L
)

internal val mockDraft = ShotDraft(
    beanName = "Geisha Panama",
    roaster = "Puerto Blest",
    dose = "18",
    output = "40",
    seconds = "26",
    grind = "12",
    temperature = "92",
    milk = "60",
    notes = "Muy floral",
    rating = 4
)
