package com.example.asking

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

// Menu Principal
@Composable
fun BakingScreen() {
    var currentScreen by remember { mutableStateOf("menu") }
    var score by remember { mutableStateOf(0) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userName = currentUser?.displayName ?: "Usuario no autenticado"

    when (currentScreen) {
        "menu" -> MainMenu(userName, onNavigate = { currentScreen = it })
        "game" -> QuizScreen(onGameEnd = { finalScore ->
            score = finalScore
            saveScoreToFirebase(userName, finalScore)
            currentScreen = "menu"
        })
        "records" -> RecordsScreen(onBack = { currentScreen = "menu" })
        "options" -> UserOptionsScreen(userName, onBack = { currentScreen = "menu" })
    }
}

@Composable
fun MainMenu(userName: String, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bienvenido a Asking, $userName",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                MenuButton("🎮 Nuevo Juego", onClick = { onNavigate("game") })
                Spacer(modifier = Modifier.height(16.dp))

                MenuButton("🏆 Ver Récords", onClick = { onNavigate("records") })
                Spacer(modifier = Modifier.height(16.dp))

                MenuButton("⚙ Opciones", onClick = { onNavigate("options") })
            }
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.8f),
        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}


// Juego de Preguntas

@Composable
fun QuizScreen(onGameEnd: (Int) -> Unit) {
    val questions = remember { getRandomQuestions(5) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }

    if (currentQuestionIndex < questions.size) {
        val question = questions[currentQuestionIndex]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            // Número de pregunta
            Text(
                text = "Pregunta ${currentQuestionIndex + 1} de ${questions.size}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Tarjeta con la pregunta
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = question.text,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            }

            // Opciones de respuesta
            question.options.forEach { option ->
                Button(
                    onClick = {
                        if (option == question.correctAnswer) score++
                        currentQuestionIndex++ // Pasa a la siguiente pregunta
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = option, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    } else {
        // Pantalla de finalización del juego
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val gold = Color(0xFFFFD700)
            Text("🎉 ¡Juego Terminado!", style = MaterialTheme.typography.headlineMedium.copy(color = gold))
            Text("🏆 Puntuación: $score", style = MaterialTheme.typography.headlineMedium.copy(color = gold))
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onGameEnd(score) },
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🏠 Volver al Menú", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// 🟢 Preguntas aleatorias
data class Question(val text: String, val options: List<String>, val correctAnswer: String)

fun getRandomQuestions(count: Int): List<Question> {
    val allQuestions = listOf(
        Question("¿Quién pintó la Mona Lisa?", listOf("Leonardo da Vinci", "Pablo Picasso", "Vincent van Gogh"), "Leonardo da Vinci"),
        Question("¿Cuál es el planeta más grande del sistema solar?", listOf("Marte", "Júpiter", "Saturno"), "Júpiter"),
        Question("¿Cuántos corazones tiene un pulpo?", listOf("Uno", "Dos", "Tres"), "Tres"),
        Question("¿Cuál es el animal terrestre más rápido?", listOf("Guepardo", "León", "Caballo"), "Guepardo"),
        Question("¿Cuál es el río más largo del mundo?", listOf("Amazonas", "Nilo", "Yangtsé"), "Amazonas"),
        Question("¿Quién escribió 'Don Quijote de la Mancha'?", listOf("Cervantes", "Shakespeare", "Borges"), "Cervantes"),
        Question("¿Cuál es el metal más ligero?", listOf("Aluminio", "Litio", "Hierro"), "Litio"),
        Question("¿En qué año llegó el hombre a la Luna?", listOf("1965", "1969", "1972"), "1969"),
        Question("¿Qué gas respiramos principalmente?", listOf("Oxígeno", "Nitrógeno", "Dióxido de carbono"), "Oxígeno"),
        Question("¿Qué país tiene forma de bota?", listOf("España", "Italia", "Francia"), "Italia"),
        Question("¿Cuál es el océano más grande del mundo?", listOf("Atlántico", "Pacífico", "Índico"), "Pacífico"),
        Question("¿Cuántos continentes hay en la Tierra?", listOf("5", "6", "7"), "7"),
        Question("¿Qué inventor creó la bombilla eléctrica?", listOf("Nikola Tesla", "Thomas Edison", "Albert Einstein"), "Thomas Edison"),
        Question("¿Cuál es el hueso más largo del cuerpo humano?", listOf("Fémur", "Radio", "Húmero"), "Fémur"),
        Question("¿Cuál es la capital de Australia?", listOf("Sídney", "Melbourne", "Canberra"), "Canberra")
    )

    return allQuestions.shuffled().take(count)
}

// 🟢 Guardar Puntuación en Firebase
fun saveScoreToFirebase(user: String, score: Int) {
    val db = FirebaseDatabase.getInstance("https://asking-ffe15-default-rtdb.firebaseio.com/").reference
    val newEntry = db.child("scores").push()
    newEntry.setValue(mapOf("Usuario" to user, "Puntuación" to score))
}


// 🟢 Pantalla de Récords (Corrigiendo Firebase)

@Composable
fun RecordsScreen(onBack: () -> Unit) {
    var scoresList by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance("https://asking-ffe15-default-rtdb.firebaseio.com/")
        val scoresRef = database.getReference("scores")

        scoresRef.get().addOnSuccessListener { dataSnapshot ->
            val scores = mutableListOf<Pair<String, Int>>()
            dataSnapshot.children.forEach { child ->
                val user = child.child("Usuario").getValue(String::class.java) ?: "Desconocido"
                val points = child.child("Puntuación").getValue(Int::class.java) ?: 0
                scores.add(user to points)
            }
            scoresList = scores.sortedByDescending { it.second } // Ordenar por puntuación
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏆 Tabla de Récords 🏆",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxHeight(0.8f), // Limitar la altura para mejor estética
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scoresList) { (user, points) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = user, style = MaterialTheme.typography.bodyLarge)
                        Text(text = "$points pts", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Volver al Menú")
        }
    }
}

// 🟢 Opciones (Solo Editar Nombre)

@Composable
fun UserOptionsScreen(currentUser: String, onBack: () -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue(currentUser)) }
    var isUpdating by remember { mutableStateOf(false) } // Para indicar que estamos en proceso de actualización
    val context = LocalContext.current // Obtener el contexto

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Editar Nombre",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isUpdating = true // Empezamos la actualización
                        FirebaseAuth.getInstance().currentUser?.updateProfile(
                            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(name.text)
                                .build()
                        )?.addOnCompleteListener { task ->
                            isUpdating = false // Terminamos la actualización
                            // Aquí ya no usas un @Composable
                            if (task.isSuccessful) {
                                // Actualización exitosa
                                Toast.makeText(context, "Nombre actualizado exitosamente", Toast.LENGTH_SHORT).show()
                                onBack() // Volver al menú
                            } else {
                                // Hubo un error
                                Toast.makeText(context, "Error al actualizar el nombre", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
                    enabled = !isUpdating // Deshabilitar el botón durante la actualización
                ) {
                    Text("Guardar Cambios")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
        ) {
            Text("⬅ Volver")
        }
    }
}

