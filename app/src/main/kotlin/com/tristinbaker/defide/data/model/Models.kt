package com.tristinbaker.defide.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Translation(
    val id: String,
    val name: String,
    val language: String,
    val license: String,
)

@Immutable
data class Book(
    val id: Int,
    val translationId: String,
    val bookNumber: Int,
    val testament: String,
    val shortName: String,
    val fullName: String,
    val drName: String,
)

@Immutable
data class Verse(
    val id: Int,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
)

@Immutable
data class Prayer(
    val id: String,
    val title: String,
    val body: String,
    val source: String?,
    val category: String,
    val tags: List<String> = emptyList(),
)

@Immutable
data class Novena(
    val id: String,
    val title: String,
    val description: String?,
    val totalDays: Int,
    val feastDay: String?,
)

@Immutable
data class NovenaDay(
    val id: Int,
    val novenaId: String,
    val dayNumber: Int,
    val title: String?,
    val body: String,
)

@Immutable
data class Mystery(
    val id: String,
    val name: String,
    val traditionalDays: String?,
)

@Immutable
data class MysteryBead(
    val id: Int,
    val mysteryId: String,
    val position: Int,
    val prayerId: String?,
    val mysteryNumber: Int?,
    val mysteryTitle: String?,
    val mysteryScripture: String?,
    val mysteryMeditation: String?,
)

@Immutable
data class Saint(
    val id: String,
    val name: String,
    val feastDate: String?,
    val shortBio: String,
    val fullBio: String,
    val patronage: String?,
    val category: String,
)
