// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/data/FirestoreRepository.kt
package edu.nd.pmcburne.hwapp.one.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import edu.nd.pmcburne.hwapp.one.screens.HomeEntry

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val photoUrl: String = ""
)

object FirestoreRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private const val ENTRIES = "entries"
    private const val LIKES = "likes"
    private const val USERS = "users"

    fun listenToPublicEntries(
        onSuccess: (List<HomeEntry>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection(ENTRIES)
            .whereEqualTo("public", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load entries.")
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val entry = doc.toObject(FirestoreEntry::class.java) ?: return@mapNotNull null
                    entry.copy(id = doc.id).toHomeEntry()
                }

                onSuccess(entries)
            }
    }

    fun listenToUserDraftEntries(
        userId: String,
        onSuccess: (List<HomeEntry>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection(ENTRIES)
            .whereEqualTo("userId", userId)
            .whereEqualTo("public", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load drafts.")
                    return@addSnapshotListener
                }

                val drafts = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val entry = doc.toObject(FirestoreEntry::class.java) ?: return@mapNotNull null
                    entry.copy(id = doc.id).toHomeEntry()
                }

                onSuccess(drafts)
            }
    }

    fun listenToEntry(
        entryId: String,
        onSuccess: (FirestoreEntry?) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection(ENTRIES)
            .document(entryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load entry.")
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    onSuccess(null)
                    return@addSnapshotListener
                }

                val entry = snapshot.toObject(FirestoreEntry::class.java)?.copy(id = snapshot.id)
                onSuccess(entry)
            }
    }

    fun createEntry(
        entry: FirestoreEntry,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val docRef = if (entry.id.isBlank()) {
            db.collection(ENTRIES).document()
        } else {
            db.collection(ENTRIES).document(entry.id)
        }

        val payload = entry.copy(id = docRef.id)

        docRef.set(payload)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { ex ->
                onError(ex.localizedMessage ?: "Failed to create entry.")
            }
    }

    fun updateEntry(
        entry: FirestoreEntry,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (entry.id.isBlank()) {
            onError("Missing entry id.")
            return
        }

        db.collection(ENTRIES)
            .document(entry.id)
            .set(entry)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { ex ->
                onError(ex.localizedMessage ?: "Failed to update entry.")
            }
    }

    fun deleteEntry(
        entryId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection(ENTRIES)
            .document(entryId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { ex ->
                onError(ex.localizedMessage ?: "Failed to delete entry.")
            }
    }

    fun listenToLikes(
        entryId: String,
        currentUserId: String,
        onSuccess: (likeCount: Int, isLiked: Boolean) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection(ENTRIES)
            .document(entryId)
            .collection(LIKES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load likes.")
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents.orEmpty()
                val likeCount = docs.size
                val isLiked = currentUserId.isNotBlank() && docs.any { it.id == currentUserId }

                onSuccess(likeCount, isLiked)
            }
    }

    fun toggleLike(
        entryId: String,
        userId: String,
        username: String,
        isCurrentlyLiked: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val likeRef = db.collection(ENTRIES)
            .document(entryId)
            .collection(LIKES)
            .document(userId)

        if (isCurrentlyLiked) {
            likeRef.delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { ex ->
                    onError(ex.localizedMessage ?: "Failed to remove like.")
                }
        } else {
            val payload = mapOf(
                "userId" to userId,
                "username" to username,
                "createdAt" to System.currentTimeMillis()
            )

            likeRef.set(payload)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { ex ->
                    onError(ex.localizedMessage ?: "Failed to like post.")
                }
        }
    }

    fun listenToUserProfile(
        userId: String,
        onSuccess: (UserProfile?) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection(USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load profile.")
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    onSuccess(null)
                    return@addSnapshotListener
                }

                val profile = snapshot.toObject(UserProfile::class.java)
                onSuccess(profile)
            }
    }

    fun upsertUserProfile(
        userId: String,
        fullName: String,
        username: String,
        photoUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val payload = UserProfile(
            userId = userId,
            fullName = fullName,
            username = username,
            photoUrl = photoUrl
        )

        db.collection(USERS)
            .document(userId)
            .set(payload)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { ex ->
                onError(ex.localizedMessage ?: "Failed to save profile.")
            }
    }

    fun updateUserProfileFields(
        userId: String,
        fullName: String,
        username: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection(USERS)
            .document(userId)
            .set(
                mapOf(
                    "fullName" to fullName,
                    "username" to username
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { ex ->
                onError(ex.localizedMessage ?: "Failed to update profile.")
            }
    }

    fun updateUserPhotoUrl(
        userId: String,
        photoUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection(USERS)
            .document(userId)
            .set(mapOf("photoUrl" to photoUrl), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { ex ->
                onError(ex.localizedMessage ?: "Failed to save profile photo.")
            }
    }

    private fun FirestoreEntry.toHomeEntry(): HomeEntry? {
        if (id.isBlank() || title.isBlank() || location.isBlank()) return null

        return HomeEntry(
            id = id,
            title = title,
            location = location,
            date = date,
            previewText = previewText,
            tags = tags,
            photoUrl = photoUrl
        )
    }
}