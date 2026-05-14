// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/data/StorageRepository.kt
package edu.nd.pmcburne.hwapp.one.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

object StorageRepository {
    private val storage = FirebaseStorage.getInstance()

    fun uploadProfilePhoto(
        userId: String,
        fileUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = storage.reference.child("profile_photos/$userId.jpg")

        ref.putFile(fileUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Profile photo upload failed.")
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                onSuccess(downloadUri.toString())
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Profile photo upload failed.")
            }
    }

    fun uploadEntryPhoto(
        userId: String,
        entryId: String,
        fileUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = storage.reference.child("entry_photos/$userId/$entryId.jpg")

        ref.putFile(fileUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Entry photo upload failed.")
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                onSuccess(downloadUri.toString())
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Entry photo upload failed.")
            }
    }
}