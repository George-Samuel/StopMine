package com.george_samusevich.stopmine

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAppScanScreen(
    appScanner: AppScanner,
    packageName: String,
    onBackClick: () -> Unit
) {
    var scanResult by remember { mutableStateOf<AppScanResult?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }

    LaunchedEffect(packageName) {
        val result = withContext(Dispatchers.IO) {
            appScanner.scanSingleApp(packageName)
        }
        scanResult = result
        isScanning = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Scan Result") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scanning selected app...")
                    }
                }
            } else {
                scanResult?.let { result ->
                    AppScanResultItem( // Убрали параметр modifier
                        result = result,
                        permissionManager = permissionManager
                    )
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("App not found or cannot be scanned")
                    }
                }
            }
        }
    }
}