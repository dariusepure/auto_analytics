package com.dariusepure.caractivitylog.ui.cars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.ui.common.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarHealthScreen(
    carId: String,
    onBack: () -> Unit,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(carId) {
        viewModel.loadCarData(carId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.car_health_check)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            CarDetailsUiState.Loading -> LoadingState()
            is CarDetailsUiState.Error -> ErrorState(s.message, onRetry = { viewModel.loadCarData(carId) })
            is CarDetailsUiState.Success -> {
                val analysis = s.aiAnalysis
                val carAccentColor = Color(0xFF2196F3)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HealthScoreHeader(
                            score = analysis?.healthScore ?: 0,
                            isAnalyzing = s.isAnalyzing,
                            accentColor = carAccentColor,
                            onAnalyzeClick = { viewModel.analyzeCarHealth(carId) }
                        )
                    }

                    // Car Basic Info Summary
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "${s.car.make} ${s.car.model}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${s.car.year} \u00B7 ${s.car.engineSize} ${s.car.fuelType}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Icon(Icons.Default.DirectionsCar, null, tint = carAccentColor, modifier = Modifier.size(32.dp))
                            }
                        }
                    }

                    if (analysis != null && !s.isAnalyzing) {
                        item {
                            Text(
                                text = stringResource(R.string.car_health_ai_summary),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = analysis.summary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        if (analysis.recommendations.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.car_health_recommendations),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(analysis.recommendations) { rec ->
                                RecommendationItem(rec)
                            }
                        }
                        
                        item {
                            val lastUpdate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(analysis.analyzedAt)
                            Text(
                                text = stringResource(R.string.car_health_last_update, lastUpdate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (!s.isAnalyzing) {
                        item {
                            EmptyState(
                                title = stringResource(R.string.car_health_ai_summary),
                                subtitle = stringResource(R.string.car_health_empty_subtitle),
                                icon = Icons.Default.HealthAndSafety
                            )
                        }
                    }
                    
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun HealthScoreHeader(
    score: Int,
    isAnalyzing: Boolean,
    accentColor: Color,
    onAnalyzeClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { if (isAnalyzing) 1f else score / 100f },
                modifier = Modifier.fillMaxSize(),
                color = if (score > 80) Color(0xFF4CAF50) else if (score > 50) Color(0xFFFF9800) else Color(0xFFF44336),
                strokeWidth = 12.dp,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp),
                    color = accentColor
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onAnalyzeClick,
            enabled = !isAnalyzing,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (score == 0) stringResource(R.string.car_health_analyze_btn) 
                else stringResource(R.string.car_health_refresh_btn)
            )
        }
    }
}

@Composable
fun RecommendationItem(text: String) {
    val accentColor = Color(0xFF2196F3)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = accentColor)
            }
            Spacer(Modifier.width(16.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
