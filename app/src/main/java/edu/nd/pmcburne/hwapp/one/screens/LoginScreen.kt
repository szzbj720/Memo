package edu.nd.pmcburne.hwapp.one.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import edu.nd.pmcburne.hwapp.one.ui.theme.BrownText
import edu.nd.pmcburne.hwapp.one.ui.theme.CreamBackground
import edu.nd.pmcburne.hwapp.one.ui.theme.FieldBorder
import edu.nd.pmcburne.hwapp.one.ui.theme.SoftCream
import edu.nd.pmcburne.hwapp.one.ui.theme.WarmOrange
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.Center

        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(110.dp)
                    .background(SoftCream, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✈",
                    fontSize = 36.sp,
                    color = BrownText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "memo",
                style = MaterialTheme.typography.headlineLarge,
                color = BrownText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome back to your travel journal",
                style = MaterialTheme.typography.bodyMedium,
                color = BrownText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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

                    when {
                        trimmedEmail.isBlank() || password.isBlank() -> {
                            errorMessage = "Please fill in all fields."
                        }
                        else -> {
                            isLoading = true
                            errorMessage = ""

                            auth.signInWithEmailAndPassword(trimmedEmail, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false

                                    if (task.isSuccessful) {
                                        onLoginClick()
                                    } else {
                                        errorMessage = task.exception?.localizedMessage
                                            ?: "Login failed. Please try again."
                                    }
                                }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .width(170.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmOrange,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isLoading) "Loading..." else "Log In",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onSignUpClick,
                enabled = !isLoading
            ) {
                Text(
                    text = "Don't have an account yet? Sign up",
                    color = BrownText
                )
            }
        }
    }
}