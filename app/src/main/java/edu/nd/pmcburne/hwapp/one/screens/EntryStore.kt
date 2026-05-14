// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/screens/EntryStore.kt
package edu.nd.pmcburne.hwapp.one.screens

import androidx.compose.runtime.mutableStateListOf

object EntryStore {
    val homeEntries = mutableStateListOf<HomeEntry>()

    val draftEntries = mutableStateListOf<HomeEntry>()

    fun findEntry(entryId: String): HomeEntry? {
        return homeEntries.find { it.id == entryId }
            ?: draftEntries.find { it.id == entryId }
    }

    fun deleteEntry(entryId: String) {
        homeEntries.removeAll { it.id == entryId }
        draftEntries.removeAll { it.id == entryId }
    }
}