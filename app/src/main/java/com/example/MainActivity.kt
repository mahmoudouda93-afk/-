package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.MainViewModel
import com.example.ui.navigation.BottomNavItems
import com.example.ui.navigation.DrawerNavItems
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                AlMusaedApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlMusaedApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authState by viewModel.authState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val isAuthScreen = currentRoute == Screen.Login.route || currentRoute == Screen.ParentPortal.route
    val showBars = !isAuthScreen

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showBars,
        drawerContent = {
            if (showBars) {
                ModalDrawerSheet(
                    drawerContainerColor = NavySurface,
                    drawerContentColor = Color.White,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(Color(0xFF0F1E36), NavySurface))
                            )
                            .padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(BluePrimary)
                                .border(2.dp, GoldAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📚", fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "المساعد — Al-Musaed",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "مستر محمود عوده — التاريخ",
                            fontSize = 12.sp,
                            color = GoldAccent
                        )
                        Text(
                            text = "المستخدم: ${authState.userName} (${if (authState.role == "teacher") "المعلم" else "المساعد"})",
                            fontSize = 11.sp,
                            color = BlueLight
                        )
                    }

                    HorizontalDivider(color = NavyBorder)

                    Spacer(modifier = Modifier.height(8.dp))

                    DrawerNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = getScreenIcon(screen),
                                    contentDescription = null,
                                    tint = if (isSelected) GoldAccent else BlueLight
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else BlueLight,
                                    fontSize = 13.sp
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = NavyCard,
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Logout in Drawer
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = RedDanger) },
                        label = { Text("تسجيل الخروج", color = RedDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0)
                            }
                        },
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showBars) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "المساعد",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• التاريخ",
                                    fontSize = 13.sp,
                                    color = GoldAccent
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = BlueLight)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = NavySurface)
                    )
                }
            },
            bottomBar = {
                if (showBars) {
                    NavigationBar(
                        containerColor = NavySurface,
                        contentColor = BlueLight
                    ) {
                        BottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = getScreenIcon(screen),
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = GoldAccent,
                                    selectedTextColor = GoldAccent,
                                    indicatorColor = NavyCard,
                                    unselectedIconColor = BlueLight,
                                    unselectedTextColor = BlueLight
                                )
                            )
                        }
                    }
                }
            },
            containerColor = NavyDark
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (authState.isLoggedIn) Screen.Dashboard.route else Screen.Login.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToParentPortal = {
                            navController.navigate(Screen.ParentPortal.route)
                        }
                    )
                }

                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }

                composable(Screen.Groups.route) {
                    GroupsScreen(
                        viewModel = viewModel,
                        onNavigateToGroup = { groupId ->
                            navController.navigate("group_detail/$groupId")
                        }
                    )
                }

                composable(
                    route = Screen.GroupDetail.route,
                    arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val grpId = backStackEntry.arguments?.getString("groupId") ?: ""
                    GroupDetailScreen(
                        groupId = grpId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToStudent = { stId ->
                            navController.navigate("student_profile/$stId")
                        }
                    )
                }

                composable(Screen.Students.route) {
                    StudentsScreen(
                        viewModel = viewModel,
                        onNavigateToStudent = { stId ->
                            navController.navigate("student_profile/$stId")
                        }
                    )
                }

                composable(
                    route = Screen.StudentProfile.route,
                    arguments = listOf(navArgument("studentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val stId = backStackEntry.arguments?.getString("studentId") ?: ""
                    StudentProfileScreen(
                        studentId = stId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Attendance.route) {
                    AttendanceScreen(
                        viewModel = viewModel,
                        onNavigateToStudent = { stId ->
                            navController.navigate("student_profile/$stId")
                        }
                    )
                }

                composable(Screen.Payments.route) {
                    PaymentsScreen(
                        viewModel = viewModel,
                        onNavigateToStudent = { stId ->
                            navController.navigate("student_profile/$stId")
                        }
                    )
                }

                composable(Screen.Exams.route) {
                    ExamsScreen(viewModel = viewModel)
                }

                composable(Screen.Certificates.route) {
                    CertificatesScreen(viewModel = viewModel)
                }

                composable(Screen.Reports.route) {
                    ReportsScreen(
                        viewModel = viewModel,
                        onNavigateToStudent = { stId ->
                            navController.navigate("student_profile/$stId")
                        }
                    )
                }

                composable(Screen.CashDrawer.route) {
                    CashDrawerScreen(viewModel = viewModel)
                }

                composable(Screen.ParentPortal.route) {
                    ParentPortalScreen(viewModel = viewModel)
                }

                composable(Screen.Audit.route) {
                    AuditScreen(viewModel = viewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onLogout = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0)
                            }
                        }
                    )
                }
            }
        }
    }
}

fun getScreenIcon(screen: Screen): ImageVector {
    return when (screen) {
        Screen.Dashboard -> Icons.Default.Dashboard
        Screen.Groups -> Icons.Default.Groups
        Screen.GroupDetail -> Icons.Default.Groups
        Screen.Students -> Icons.Default.School
        Screen.StudentProfile -> Icons.Default.Person
        Screen.Attendance -> Icons.Default.CheckCircle
        Screen.Payments -> Icons.Default.Payments
        Screen.Exams -> Icons.Default.Quiz
        Screen.Certificates -> Icons.Default.MilitaryTech
        Screen.Reports -> Icons.Default.Assessment
        Screen.CashDrawer -> Icons.Default.AccountBalanceWallet
        Screen.ParentPortal -> Icons.Default.FamilyRestroom
        Screen.Audit -> Icons.Default.History
        Screen.Settings -> Icons.Default.Settings
        Screen.Login -> Icons.Default.Lock
    }
}
