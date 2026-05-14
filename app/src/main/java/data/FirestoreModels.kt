// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/data/FirestoreModels.kt
package edu.nd.pmcburne.hwapp.one.data

data class FirestoreEntry(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val title: String = "",
    val location: String = "",
    val date: String = "",
    val previewText: String = "",
    val tags: List<String> = emptyList(),
    val public: Boolean = true,
    val createdAt: Long = 0L,
    val photoUrl: String = ""
)

data class FirestoreComment(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)