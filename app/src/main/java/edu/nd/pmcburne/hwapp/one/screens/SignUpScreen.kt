package edu.nd.pmcburne.hwapp.one.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import edu.nd.pmcburne.hwapp.one.ui.theme.BrownText
import edu.nd.pmcburne.hwapp.one.ui.theme.CreamBackground
import edu.nd.pmcburne.hwapp.one.ui.theme.FieldBorder
import edu.nd.pmcburne.hwapp.one.ui.theme.SoftCream
import edu.nd.pmcburne.hwapp.one.ui.theme.WarmOrange

private const val PROFILE_PREFS = "memo_profile_prefs"
private const val KEY_SHOW_PHOTO_MODAL = "show_photo_modal"
private const val KEY_SHOW_LOCATION_MODAL = "show_location_modal"
private const val KEY_LOCATION_GRANTED = "location_granted"
private const val KEY_LOCATION_PROMPT_SHOWN = "location_prompt_shown"
private const val KEY_PROFILE_FULL_NAME = "profile_full_name"
private const val KEY_PROFILE_USERNAME = "profile_username"
private const val KEY_PROFILE_EMAIL = "profile_email"

@Composable
fun SignUpScreen(
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
    }

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showLocationPermissionDialog by rememberSaveable { mutableStateOf(false) }

    fun finishAccountCreation(locationGranted: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SHOW_LOCATION_MODAL, false)
            .putBoolean(KEY_SHOW_PHOTO_MODAL, true)
            .putBoolean(KEY_LOCATION_GRANTED, locationGranted)
            .putBoolean(KEY_LOCATION_PROMPT_SHOWN, true)
            .apply()

        showLocationPermissionDialog = false
        onCreateAccountClick()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        finishAccountCreation(locationGranted = granted)
    }

    if (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                finishAccountCreation(locationGranted = false)
            },
            containerColor = SoftCream,
            title = {
                Text(
                    text = "Enable location?",
                    color = BrownText
                )
            },
            text = {
                Text(
                    text = "Allow location access so Memo can use trip location features and in-app maps.",
                    color = BrownText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Allow", color = WarmOrange)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        finishAccountCreation(locationGranted = false)
                    }
                ) {
                    Text("Not now", color = BrownText)
                }
            }
        )
    }

    Scaffold(
        containerColor = CreamBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CreamBackground)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .background(SoftCream, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧳",
                    fontSize = 32.sp,
                    color = BrownText
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Memo",
                style = MaterialTheme.typography.headlineLarge,
                color = BrownText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create your travel journal account",
                style = MaterialTheme.typography.bodyMedium,
                color = BrownText
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("First and last name", color = BrownText.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FieldBorder,
                    unfocusedBorderColor = FieldBorder,
                    focusedTextColor = BrownText,
                    unfocusedTextColor = BrownText,
                    cursorColor = BrownText,
                    focusedContainerColor = SoftCream,
                    unfocusedContainerColor = SoftCream
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Email", color = BrownText.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FieldBorder,
                    unfocusedBorderColor = FieldBorder,
                    focusedTextColor = BrownText,
                    unfocusedTextColor = BrownText,
                    cursorColor = BrownText,
                    focusedContainerColor = SoftCream,
                    unfocusedContainerColor = SoftCream
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Password", color = BrownText.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FieldBorder,
                    unfocusedBorderColor = FieldBorder,
                    focusedTextColor = BrownText,
                    unfocusedTextColor = BrownText,
                    cursorColor = BrownText,
                    focusedContainerColor = SoftCream,
                    unfocusedContainerColor = SoftCream
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Re-enter password", color = BrownText.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FieldBorder,
                    unfocusedBorderColor = FieldBorder,
                    focusedTextColor = BrownText,
                    unfocusedTextColor = BrownText,
                    cursorColor = BrownText,
                    focusedContainerColor = SoftCream,
                    unfocusedContainerColor = SoftCream
                )
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFB3261E),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    val trimmedFullName = fullName.trim()
                    val derivedUsername = trimmedEmail.substringBefore("@").trim()

                    when {
                        trimmedFullName.isBlank() ||
                                trimmedEmail.isBlank() ||
                                password.isBlank() ||
                                confirmPassword.isBlank() -> {
                            errorMessage = "Please fill in all fields."
                        }

                        password != confirmPassword -> {
                            errorMessage = "Passwords do not match."
                        }

                        else -> {
                            isLoading = true
                            errorMessage = ""

                            auth.createUserWithEmailAndPassword(trimmedEmail, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false

                                    if (task.isSuccessful) {
                                        prefs.edit()
                                            .putBoolean(KEY_SHOW_LOCATION_MODAL, true)
                                            .putBoolean(KEY_SHOW_PHOTO_MODAL, true)
                                            .putBoolean(KEY_LOCATION_GRANTED, false)
                                            .putString(KEY_PROFILE_FULL_NAME, trimmedFullName)
                                            .putString(KEY_PROFILE_USERNAME, derivedUsername)
                                            .putString(KEY_PROFILE_EMAIL, trimmedEmail)
                                            .apply()

                                        val alreadyPrompted = prefs.getBoolean(KEY_LOCATION_PROMPT_SHOWN, false)
                                        val alreadyGranted = hasLocationPermission(context)

                                        when {
                                            alreadyGranted -> {
                                                finishAccountCreation(locationGranted = true)
                                            }
                                            !alreadyPrompted -> {
                                                showLocationPermissionDialog = true
                                            }
                                            else -> {
                                                onCreateAccountClick()
                                            }
                                        }
                                    } else {
                                        errorMessage = task.exception?.localizedMessage
                                            ?: "Account creation failed. Please try again."
                                    }
                                }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .width(220.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmOrange,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isLoading) "Creating..." else "Create Account",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onLoginClick,
                enabled = !isLoading
            ) {
                Text(
                    text = "Already have an account? Log in",
                    color = BrownText
                )
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fineGranted || coarseGranted
}