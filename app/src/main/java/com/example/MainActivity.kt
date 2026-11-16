package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.configureAppCheck
import com.example.auth.signOutUser
import com.example.data.AgentMemoryRepository
import com.example.ui.ApexMindViewModel
import com.example.ui.screens.AdaptiveAnalyticsScreen
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.ResearchLabScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldTelemetry
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class ApexDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ARENA("arena", R.string.nav_arena, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    RESEARCH("research", R.string.nav_research, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    MEMORY("memory", R.string.nav_memory, Icons.Filled.Memory, Icons.Outlined.Memory),
    ADAPTIVE("adaptive", R.string.nav_adaptive, Icons.Filled.Speed, Icons.Outlined.Speed)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge
        enableEdgeToEdge()

        // STEP 3: Render Compose UI with Auth Gating
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val firebaseAuth: FirebaseAuth? = remember(context) {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            runCatching { Firebase.auth }.getOrNull()
        } else {
            null
        }
    }

    if (firebaseAuth == null) {
        FirebaseSetupRequiredScreen(packageName = context.packageName)
        return
    }

    val currentUser by firebaseAuth.authStateFlow().collectAsStateWithLifecycle(initialValue = firebaseAuth.currentUser)

    AnimatedContent(
        targetState = currentUser,
        transitionSpec = {
            fadeIn(tween(240, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(180))
        },
        contentKey = { it?.uid },
        label = "auth_gate_transition"
    ) { user ->
        if (user == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            val uid = user.uid
            key(uid) {
                ApexMindMainShell(
                    currentUserId = uid,
                    userEmail = user.email ?: "Operator"
                )
            }
        }
    }
}

@Composable
private fun FirebaseSetupRequiredScreen(packageName: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firebase_setup_required_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "Configuration required",
                            tint = ElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Firebase Config Missing",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "This APK was compiled without app/google-services.json, so Firebase Auth, Firestore Memory, and Gemini AI Logic could not initialize.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "To build a working APK on GitHub Actions:\n" +
                            "1. Copy app/google-services.json from AI Studio into GitHub Repo Settings -> Secrets -> Actions as GOOGLE_SERVICES_JSON (or commit app/google-services.json).\n" +
                            "2. Copy debug.keystore.base64 into a DEBUG_KEYSTORE_BASE64 secret so Google Sign-In SHA-1 matches ($packageName).\n" +
                            "3. Re-run the Build Android APK workflow and install the new APK.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApexMindMainShell(
    currentUserId: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: ApexMindViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ApexMindViewModel(
                    repository = AgentMemoryRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    var currentTab by rememberSaveable { mutableStateOf(ApexDestination.ARENA) }

    if (currentTab != ApexDestination.ARENA) {
        BackHandler {
            currentTab = ApexDestination.ARENA
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBackground,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldTelemetry)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = ElectricCyan
                                    )
                                }
                                Text(
                                    text = "NEURAL ENGINE • $userEmail",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SlateSurface
                        )
                    )
                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.7f))
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    Column {
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.7f))
                        NavigationBar(
                            containerColor = SlateSurface,
                            tonalElevation = 0.dp
                        ) {
                            ApexDestination.entries.forEach { dest ->
                                val selected = currentTab == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(dest.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = ElectricCyan.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = SlateSurface
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ApexDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                    VerticalDivider(color = BorderSubtle.copy(alpha = 0.7f))
                }

                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                initialOffsetY = { it / 28 }
                            )) togetherWith fadeOut(animationSpec = tween(140))
                    },
                    label = "main_tab_transition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { tab ->
                    when (tab) {
                        ApexDestination.ARENA -> ArenaScreen(
                            viewModel = viewModel,
                            onNavigateToResearch = { currentTab = ApexDestination.RESEARCH },
                            onNavigateToMemory = { currentTab = ApexDestination.MEMORY }
                        )
                        ApexDestination.RESEARCH -> ResearchLabScreen(
                            viewModel = viewModel,
                            onLaunchGameAfterResearch = { currentTab = ApexDestination.ARENA }
                        )
                        ApexDestination.MEMORY -> MemoryVaultScreen(
                            viewModel = viewModel
                        )
                        ApexDestination.ADAPTIVE -> AdaptiveAnalyticsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
