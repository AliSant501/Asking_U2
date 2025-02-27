package com.example.asking

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.asking.ui.theme.AskingTheme
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var signInClient: SignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        signInClient = Identity.getSignInClient(this)

        setContent {
            AskingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (auth.currentUser == null) {
                        LoginScreen { signIn() }
                    } else {
                        BakingScreen()
                    }
                }
            }
        }
    }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    try {
                        val credential = signInClient.getSignInCredentialFromIntent(data)
                        val idToken = credential.googleIdToken
                        if (idToken != null) {
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("GoogleSignIn", "Autenticación exitosa")
                                        setContent {
                                            AskingTheme {
                                                BakingScreen()
                                            }
                                        }
                                    } else {
                                        Log.e("GoogleSignIn", "Error en autenticación", task.exception)
                                    }
                                }
                        } else {
                            Log.e("GoogleSignIn", "ID Token es nulo")
                        }
                    } catch (e: Exception) {
                        Log.e("GoogleSignIn", "Error al obtener credenciales", e)
                    }
                } else {
                    Log.e("GoogleSignIn", "Intent de autenticación es nulo")
                }
            } else {
                Log.e("GoogleSignIn", "Autenticación cancelada o fallida")
            }
        }

    private fun signIn() {
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("963542333547-v0439luqghga05vmutct2kkl1cgr7fqe.apps.googleusercontent.com")
                    .setFilterByAuthorizedAccounts(false) // Cambiar a true si solo quieres cuentas usadas antes
                    .build()
            )
            .build()

        signInClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    signInLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent).build())
                } catch (e: Exception) {
                    Log.e("GoogleSignIn", "Error al iniciar IntentSender", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("GoogleSignIn", "Error en signIn", e)
            }
    }
}

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido a Asking")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLoginClick) {
            Text("Iniciar sesión con Google")
        }
    }
}
