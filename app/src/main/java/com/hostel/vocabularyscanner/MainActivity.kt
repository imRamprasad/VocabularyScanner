package com.hostel.vocabularyscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.hostel.vocabularyscanner.data.local.WordDatabase
import com.hostel.vocabularyscanner.data.repository.AuthRepository
import com.hostel.vocabularyscanner.data.repository.WordRepository
import com.hostel.vocabularyscanner.ui.screens.HomeScreen
import com.hostel.vocabularyscanner.ui.screens.LoginScreen
import com.hostel.vocabularyscanner.ui.screens.SavedWordsScreen
import com.hostel.vocabularyscanner.ui.screens.ScannerScreen

enum class Screen {
    HOME, SCANNER, SAVED_WORDS
}

class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()
    private lateinit var wordRepository: WordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = WordDatabase.getDatabase(applicationContext)
        wordRepository = WordRepository(database.wordDao())

        setContent {
            MaterialTheme {
                var currentUser by remember {
                    mutableStateOf(authRepository.currentUser)
                }

                var currentScreen by remember {
                    mutableStateOf(Screen.HOME)
                }

                Surface {
                    val user = currentUser
                    if (user == null) {
                        LoginScreen(
                            authRepository = authRepository,
                            onLoginSuccess = {
                                currentUser = authRepository.currentUser
                                currentScreen = Screen.HOME
                            }
                        )
                    } else {
                        when (currentScreen) {
                            Screen.HOME -> HomeScreen(
                                userEmail = user.email ?: user.uid,
                                onScanClick = {
                                    currentScreen = Screen.SCANNER
                                },
                                onSavedWordsClick = {
                                    currentScreen = Screen.SAVED_WORDS
                                },
                                onSignOutClick = {
                                    authRepository.signOut()
                                    currentUser = null
                                    currentScreen = Screen.HOME
                                }
                            )

                            Screen.SCANNER -> ScannerScreen(
                                userId = user.uid,
                                wordRepository = wordRepository,
                                onBackClick = {
                                    currentScreen = Screen.HOME
                                }
                            )

                            Screen.SAVED_WORDS -> SavedWordsScreen(
                                userId = user.uid,
                                wordRepository = wordRepository,
                                onBackClick = {
                                    currentScreen = Screen.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
