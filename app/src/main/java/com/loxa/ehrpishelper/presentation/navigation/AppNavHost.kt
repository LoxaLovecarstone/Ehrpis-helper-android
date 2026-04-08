package com.loxa.ehrpishelper.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.loxa.ehrpishelper.presentation.coupon.CouponScreen

@Composable
fun AppNavHost(initialDestination: Any = CouponList) { // 파라미터 추가
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentDest?.hasRoute(CouponList::class) == true,
                    onClick = {
                        navController.navigate(CouponList) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("쿠폰") }
                )
                NavigationBarItem(
                    selected = currentDest?.hasRoute(CharacterList::class) == true,
                    onClick = {
                        navController.navigate(CharacterList) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("캐릭터") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialDestination, // 주입받은 목적지 사용
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<CouponList> {
                CouponScreen()
            }
            composable<CharacterList> {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(innerPadding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("캐릭터 정보 (준비 중)")
                }
            }
        }
    }
}