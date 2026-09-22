package com.example.ui.navigation

sealed class Screen(val route: String, val title: String, val iconName: String) {
    object Dashboard : Screen("dashboard", "الرئيسية", "dashboard")
    object Groups : Screen("groups", "المجموعات", "groups")
    object GroupDetail : Screen("group_detail/{groupId}", "تفاصيل المجموعة", "group")
    object Students : Screen("students", "الطلاب", "school")
    object StudentProfile : Screen("student_profile/{studentId}", "ملف الطالب", "person")
    object Attendance : Screen("attendance", "الحضور", "check_circle")
    object Payments : Screen("payments", "المدفوعات", "payments")
    object Exams : Screen("exams", "الامتحانات", "quiz")
    object Certificates : Screen("certificates", "الشهادات", "military_tech")
    object Reports : Screen("reports", "التقارير", "assessment")
    object CashDrawer : Screen("cash_drawer", "الخزينة", "account_balance_wallet")
    object ParentPortal : Screen("parent_portal", "بوابة ولي الأمر", "family_restroom")
    object Audit : Screen("audit", "سجل الحركات", "history")
    object Settings : Screen("settings", "الإعدادات", "settings")
    object Login : Screen("login", "تسجيل الدخول", "login")
}

val BottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Groups,
    Screen.Students,
    Screen.Attendance,
    Screen.Payments
)

val DrawerNavItems = listOf(
    Screen.Dashboard,
    Screen.Groups,
    Screen.Students,
    Screen.Attendance,
    Screen.Payments,
    Screen.Exams,
    Screen.Certificates,
    Screen.Reports,
    Screen.CashDrawer,
    Screen.ParentPortal,
    Screen.Audit,
    Screen.Settings
)
