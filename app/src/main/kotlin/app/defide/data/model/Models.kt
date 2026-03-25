package app.defide.data.model

data class Translation(
    val id: String,
    val name: String,
    val language: String,
    val license: String,
)

data class Book(
    val id: Int,
    val translationId: String,
    val bookNumber: Int,
    val testament: String,
    val shortName: String,
    val fullName: String,
    val drName: String,
)

data class Verse(
    val id: Int,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
)

data class CccSection(
    val id: Int,
    val part: Int?,
    val section: Int?,
    val chapter: Int?,
    val article: Int?,
    val heading: String?,
    val body: String,
)

data class Prayer(
    val id: String,
    val title: String,
    val body: String,
    val source: String?,
    val category: String,
    val tags: List<String> = emptyList(),
)

data class Novena(
    val id: String,
    val title: String,
    val description: String?,
    val totalDays: Int,
    val feastDay: String?,
)

data class NovenaDay(
    val id: Int,
    val novenaId: String,
    val dayNumber: Int,
    val title: String?,
    val body: String,
)

data class Mystery(
    val id: String,
    val name: String,
    val traditionalDays: String?,
)

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
