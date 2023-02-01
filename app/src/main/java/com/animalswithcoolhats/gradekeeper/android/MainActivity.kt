package com.animalswithcoolhats.gradekeeper.android

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.animalswithcoolhats.gradekeeper.android.ui.theme.GradekeeperTheme
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import java.text.SimpleDateFormat
import java.util.*

var format = SimpleDateFormat("MMM d, y", Locale.US)

class MainActivity : ComponentActivity() {
    private var token: String? = null

    private val viewModel: MainViewModel by viewModels()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            2 -> {
                try {
                    val credential = viewModel.oneTapClient!!.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    when {
                        idToken != null -> {
                            // Got an ID token from Google. Use it to authenticate
                            // with your backend.
                            Log.d(TAG, "Got ID token: ${idToken}")
                            viewModel.acceptToken(idToken, this)
                            token = idToken
                        }
                        else -> {
                            // Shouldn't happen.
                            Log.d(TAG, "No ID token!")
                        }
                    }
                } catch (e: ApiException) {
                    // ...
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.oneTapClient = Identity.getSignInClient(this);
        viewModel.signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId("115621609543-a4lnfteodutupmrv72prub28bgdeffe7.apps.googleusercontent.com")
                    // Show all accounts on the device.
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .setAutoSelectEnabled(true)
            .build()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val token = getPreferences(Context.MODE_PRIVATE)?.getString("TOKEN", "")
        if (!token.isNullOrEmpty()) {
            viewModel.userToken = token
            Log.i("token", "token fetched from cache: $token")
            viewModel.redownload()
        }

        setContent {
            GradekeeperTheme {
                if (viewModel.userIsAuthenticated) {
                    Root(viewModel)
                } else {
                    Button({
                        viewModel.oneTapClient!!.beginSignIn(viewModel.signUpRequest!!).addOnSuccessListener(this) {
                            try {
                                startIntentSenderForResult(it.pendingIntent.intentSender, 2, null, 0, 0, 0)
                            } catch (e: IntentSender.SendIntentException) {
                                Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                            }
                        }.addOnFailureListener(this) { e ->
                            Log.i(TAG, e.localizedMessage)
                        }
                    }, modifier=Modifier.padding(80.dp)) {
                        Text("Sign in")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(viewModel: MainViewModel){
    var menuIsExpanded by remember { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        title = {
                Text("Gradekeeper")
        },
        navigationIcon = {},
        actions = {
            IconButton(onClick = { menuIsExpanded=true }) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Localized description"
                )
            }
            DropdownMenu(expanded = menuIsExpanded, onDismissRequest = { menuIsExpanded=false }) {
                DropdownMenuItem(text = {Text("Logout")}, onClick = { viewModel.logout() })
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Root(viewModel: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(topBar = {TopBar(viewModel)}, bottomBar = {
        NavigationBar {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBarItem(
                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                label = { Text("Courses") },
                selected = currentRoute == "courses", onClick = {navController.navigate("courses")})
            NavigationBarItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Home") },
                label = { Text("Settings") },
                selected = currentRoute == "settings", onClick = {navController.navigate("settings")})
        }
    }, content = {
        Box(modifier = Modifier.padding(it)) {
            NavHost(navController = navController, startDestination = "courses") {
                composable("courses") { Courses(viewModel) }
                composable("settings") { SettingsPage() }
            }
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Courses(viewModel: MainViewModel) {
    Column(modifier = Modifier.padding(14.dp)) {
        viewModel.studyBlocks?.sortedByDescending { a ->
            a.startDate
        }?.forEach { studyBlock ->
            Card(modifier=Modifier.padding(bottom = 20.dp)) {
                Column() {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            studyBlock.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${format.format(studyBlock.startDate)} — ${format.format(studyBlock.endDate)}")
                    }

                    Column {
                        studyBlock.subjects.forEachIndexed { idx, it ->
                            ListItem({
                                Text(it.longName)
                            }, leadingContent = {
                                Text("${it.courseCodeName} ${it.courseCodeNumber}")
                            })
                            if (idx != studyBlock.subjects.size-1) Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPage() {
    Text("hello")
}

@Composable
fun CourseCard(tag: String, name: String) {
    var isClicked by remember { mutableStateOf(false) }
    Card(
        modifier= Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.padding(16.dp)) {
                Text(fontWeight = FontWeight.Bold, fontSize = 20.sp, text = tag)
                Text(fontSize = 16.sp, text = name)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A+")
                Text("96.25%")
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GradekeeperTheme {
        Greeting("Android")
    }
}