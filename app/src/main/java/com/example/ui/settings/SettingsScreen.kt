package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSettings
import com.example.ui.theme.*
import com.example.ui.viewmodel.GeoNewsViewModel

@Composable
fun SettingsScreen(
    viewModel: GeoNewsViewModel,
    modifier: Modifier = Modifier
) {
    val currentSettings by viewModel.settings.collectAsStateWithLifecycle()

    var newsApiKey by remember { mutableStateOf(currentSettings.newsApiKey) }
    var newsApiEndpoint by remember { mutableStateOf(currentSettings.newsApiEndpoint) }
    var geminiApiKey by remember { mutableStateOf(currentSettings.geminiApiKey) }
    var geminiModel by remember { mutableStateOf(currentSettings.geminiModel) }
    var aiProvider by remember { mutableStateOf(currentSettings.aiProvider) }
    var openRouterApiKey by remember { mutableStateOf(currentSettings.openRouterApiKey) }
    var openRouterModel by remember { mutableStateOf(currentSettings.openRouterModel) }
    var preferredLanguage by remember { mutableStateOf(currentSettings.preferredLanguage) }
    var shortsTargetDuration by remember { mutableStateOf(currentSettings.shortsTargetDuration) }
    var refreshHours by remember { mutableStateOf(currentSettings.autoRefreshHours) }
    var longVideoDuration by remember { mutableStateOf(currentSettings.longVideoDuration) }
    var isDarkTheme by remember { mutableStateOf(currentSettings.isDarkTheme) }

    var showNewsKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenRouterKey by remember { mutableStateOf(false) }

    LaunchedEffect(currentSettings) {
        newsApiKey = currentSettings.newsApiKey
        newsApiEndpoint = currentSettings.newsApiEndpoint
        geminiApiKey = currentSettings.geminiApiKey
        geminiModel = currentSettings.geminiModel
        aiProvider = currentSettings.aiProvider
        openRouterApiKey = currentSettings.openRouterApiKey
        openRouterModel = currentSettings.openRouterModel
        preferredLanguage = currentSettings.preferredLanguage
        shortsTargetDuration = currentSettings.shortsTargetDuration
        refreshHours = currentSettings.autoRefreshHours
        longVideoDuration = currentSettings.longVideoDuration
        isDarkTheme = currentSettings.isDarkTheme
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = "Studio Configuration",
                    color = GeoTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure AI engine, script duration, and API keys",
                    color = GeoTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Security Notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoDarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = GeoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "API keys are encrypted in local private storage or injected via BuildConfig. OpenRouter automatically falls back to Gemini if unavailable.",
                        color = GeoTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Primary AI Provider Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "PRIMARY AI SCRIPT GENERATOR",
                        color = GeoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Choose your preferred LLM provider for Shorts and Documentary scripts:",
                        color = GeoTextSecondary,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Gemini", "OpenRouter").forEach { provider ->
                            val isSelected = aiProvider.equals(provider, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { aiProvider = provider },
                                label = {
                                    Text(
                                        if (provider == "Gemini") "Google Gemini" else "OpenRouter (Multi-Model)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = GeoDarkSurfaceVariant,
                                    labelColor = GeoTextSecondary,
                                    selectedContainerColor = GeoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = GeoPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = GeoCardBorder,
                                    selectedBorderColor = GeoPrimary,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }
            }
        }

        // OpenRouter Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (aiProvider == "OpenRouter") GeoPrimary else GeoCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "OPENROUTER CONFIGURATION",
                            color = GeoSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (aiProvider == "OpenRouter") {
                            Surface(
                                color = GeoPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = GeoPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Access Llama 3.3 70B, Mistral, and Claude via openrouter.ai. Generates scripts and high-CTR SEO for YouTube, Instagram Reels & Bilibili.",
                        color = GeoTextSecondary,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = openRouterApiKey,
                        onValueChange = { openRouterApiKey = it },
                        label = { Text("OpenRouter API Key (sk-or-v1-...)") },
                        placeholder = { Text("Enter OpenRouter Key") },
                        visualTransformation = if (showOpenRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showOpenRouterKey = !showOpenRouterKey }) {
                                Icon(
                                    imageVector = if (showOpenRouterKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = GeoTextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoSecondary,
                            unfocusedBorderColor = GeoCardBorder,
                            focusedTextColor = GeoTextPrimary,
                            unfocusedTextColor = GeoTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("openrouter_key_input")
                    )

                    OutlinedTextField(
                        value = openRouterModel,
                        onValueChange = { openRouterModel = it },
                        label = { Text("Model ID") },
                        placeholder = { Text("meta-llama/llama-3.3-70b-instruct:free") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoSecondary,
                            unfocusedBorderColor = GeoCardBorder,
                            focusedTextColor = GeoTextPrimary,
                            unfocusedTextColor = GeoTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("openrouter_model_input")
                    )
                }
            }
        }

        // Gemini AI Configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (aiProvider == "Gemini") GeoPrimary else GeoCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GEMINI AI CONFIGURATION (PRIMARY / FALLBACK)",
                            color = GeoPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (aiProvider == "Gemini") {
                            Surface(
                                color = GeoPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = GeoPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = { geminiApiKey = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("Masked (e.g. AIzaSy...****)") },
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    imageVector = if (showGeminiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = GeoTextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimary,
                            unfocusedBorderColor = GeoCardBorder,
                            focusedTextColor = GeoTextPrimary,
                            unfocusedTextColor = GeoTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("gemini_key_input")
                    )

                    OutlinedTextField(
                        value = geminiModel,
                        onValueChange = { geminiModel = it },
                        label = { Text("Model Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoPrimary,
                            unfocusedBorderColor = GeoCardBorder,
                            focusedTextColor = GeoTextPrimary,
                            unfocusedTextColor = GeoTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("gemini_model_input")
                    )
                }
            }
        }

        // News API Configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "NEWS API CONFIGURATION (OPTIONAL)",
                        color = GeoSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "GeoNews Studio uses verified primary RSS wires (BBC, Al Jazeera, Guardian, DW, UN). You can also provide an optional NewsAPI key:",
                        color = GeoTextSecondary,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = newsApiKey,
                        onValueChange = { newsApiKey = it },
                        label = { Text("NewsAPI Key (Optional)") },
                        visualTransformation = if (showNewsKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNewsKey = !showNewsKey }) {
                                Icon(
                                    imageVector = if (showNewsKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = GeoTextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoSecondary,
                            unfocusedBorderColor = GeoCardBorder,
                            focusedTextColor = GeoTextPrimary,
                            unfocusedTextColor = GeoTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("news_key_input")
                    )

                    OutlinedTextField(
                        value = newsApiEndpoint,
                        onValueChange = { newsApiEndpoint = it },
                        label = { Text("Endpoint URL") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoSecondary,
                            unfocusedBorderColor = GeoCardBorder,
                            focusedTextColor = GeoTextPrimary,
                            unfocusedTextColor = GeoTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Script Preferences
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "SCRIPTING PREFERENCES",
                        color = GeoTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Preferred Language for Video Scripts:",
                            color = GeoTextSecondary,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Hinglish", "Hindi", "English").forEach { lang ->
                                val isSelected = preferredLanguage == lang
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { preferredLanguage = lang },
                                    label = { Text(lang, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = GeoDarkSurfaceVariant,
                                        labelColor = GeoTextSecondary,
                                        selectedContainerColor = GeoPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = GeoPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = GeoCardBorder,
                                        selectedBorderColor = GeoPrimary,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Refresh Check Interval:",
                            color = GeoTextSecondary,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(24, 12, 6).forEach { hours ->
                                val isSelected = refreshHours == hours
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { refreshHours = hours },
                                    label = { Text("$hours Hours", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = GeoDarkSurfaceVariant,
                                        labelColor = GeoTextSecondary,
                                        selectedContainerColor = GeoSecondary.copy(alpha = 0.2f),
                                        selectedLabelColor = GeoSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = GeoCardBorder,
                                        selectedBorderColor = GeoSecondary,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Shorts Script Target Duration:",
                            color = GeoTextSecondary,
                            fontSize = 12.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(10, 15, 20, 30, 45, 60).forEach { dur ->
                                val isSelected = shortsTargetDuration == dur
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { shortsTargetDuration = dur },
                                    label = { Text("${dur}s", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = GeoDarkSurfaceVariant,
                                        labelColor = GeoTextSecondary,
                                        selectedContainerColor = GeoPrimary.copy(alpha = 0.25f),
                                        selectedLabelColor = GeoPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = GeoCardBorder,
                                        selectedBorderColor = GeoPrimary,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Long-Video Target Duration:",
                            color = GeoTextSecondary,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("8-12 min", "5-8 min", "12-15 min").forEach { dur ->
                                val isSelected = longVideoDuration == dur
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { longVideoDuration = dur },
                                    label = { Text(dur, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = GeoDarkSurfaceVariant,
                                        labelColor = GeoTextSecondary,
                                        selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFFEF4444)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = GeoCardBorder,
                                        selectedBorderColor = Color(0xFFEF4444),
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Dark Interface Mode",
                            color = GeoTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isDarkTheme = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GeoPrimary,
                                checkedTrackColor = GeoPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Save and Clear Cache Actions
        item {
            Button(
                onClick = {
                    viewModel.updateSettings(
                        currentSettings.copy(
                            newsApiKey = newsApiKey.trim(),
                            newsApiEndpoint = newsApiEndpoint.trim(),
                            geminiApiKey = geminiApiKey.trim(),
                            geminiModel = geminiModel.trim(),
                            aiProvider = aiProvider,
                            openRouterApiKey = openRouterApiKey.trim(),
                            openRouterModel = openRouterModel.trim(),
                            preferredLanguage = preferredLanguage,
                            shortsTargetDuration = shortsTargetDuration,
                            autoRefreshHours = refreshHours,
                            longVideoDuration = longVideoDuration,
                            isDarkTheme = isDarkTheme
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeoPrimary,
                    contentColor = GeoDarkBg
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("save_settings_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configuration", fontWeight = FontWeight.Bold)
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.clearCache() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("clear_cache_btn")
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Cached News & Scripts", fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
