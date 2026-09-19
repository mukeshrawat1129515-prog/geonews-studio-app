package com.example.ui.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShortScript
import com.example.data.repository.ScriptGenState
import com.example.ui.components.MultiPlatformSeoDialog
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.*
import com.example.ui.viewmodel.GeoNewsViewModel

@Composable
fun ShortsScreen(
    viewModel: GeoNewsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shortScripts by viewModel.shortScripts.collectAsStateWithLifecycle()
    val genState by viewModel.shortsGenState.collectAsStateWithLifecycle()
    val visualPromptShort by viewModel.visualPromptDialogShort.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val showSeoDialog by viewModel.showSeoDialog.collectAsStateWithLifecycle()
    val seoState by viewModel.seoGenState.collectAsStateWithLifecycle()
    val seoData by viewModel.currentSeoData.collectAsStateWithLifecycle()

    val selectedDuration = settings.shortsTargetDuration

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Shorts Creator Studio",
                        color = GeoTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedDuration}s Viral Factual Scripts (${settings.preferredLanguage})",
                        color = GeoSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { viewModel.generateShorts(styleVariant = "standard", targetDuration = selectedDuration) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeoSecondary,
                        contentColor = GeoDarkBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Regen All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Duration Selector Chips (10s, 15s, 20s, 30s, 45s, 60s)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Target Duration:",
                    color = GeoTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(10, 15, 20, 30, 45, 60).forEach { dur ->
                        val isSelected = selectedDuration == dur
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateSettings(settings.copy(shortsTargetDuration = dur))
                            },
                            label = { Text("${dur}s", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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

        // Generating progress card
        if (genState is ScriptGenState.Generating) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GeoDarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = GeoSecondary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = (genState as ScriptGenState.Generating).message,
                            color = GeoTextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // If no shorts generated yet
        if (shortScripts.isEmpty() && genState !is ScriptGenState.Generating) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = GeoSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Shorts Generated",
                            color = GeoTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Generate 2 high-retention ${selectedDuration}-second Hindi/Hinglish scripts with camera directions and AI visual prompts.",
                            color = GeoTextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.generateShorts(styleVariant = "standard", targetDuration = selectedDuration) },
                            colors = ButtonDefaults.buttonColors(containerColor = GeoSecondary, contentColor = GeoDarkBg)
                        ) {
                            Text("Generate 2 Shorts Scripts (${selectedDuration}s)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            itemsIndexed(
                items = shortScripts,
                key = { _, short -> short.id }
            ) { index, short ->
                ShortCard(
                    index = index + 1,
                    short = short,
                    onCopy = {
                        val fullPackage = buildString {
                            appendLine("TITLE: ${short.viralTitle}")
                            appendLine("SHORTS TITLE: ${short.shortsTitle}")
                            appendLine("THUMBNAIL: ${short.thumbnailText}")
                            appendLine("\n${short.targetDurationSeconds}-SECOND SCRIPT:\n${short.scriptText}")
                            appendLine("\nVISUAL DIRECTION:\n${short.visualDirection}")
                            appendLine("\nCAPTIONS:\n${short.captionsText}")
                            appendLine("\nSOURCE: ${short.sourceName} (${short.sourceUrl})")
                        }
                        copyToClipboard(context, "Short ${index + 1} Script", fullPackage)
                    },
                    onRegenerate = { viewModel.generateShorts(styleVariant = "standard", storyId = short.storyId, targetDuration = short.targetDurationSeconds) },
                    onMoreHuman = { viewModel.generateShorts(styleVariant = "more_human", storyId = short.storyId, targetDuration = short.targetDurationSeconds) },
                    onMorePunchy = { viewModel.generateShorts(styleVariant = "more_punchy", storyId = short.storyId, targetDuration = short.targetDurationSeconds) },
                    onVisualPrompt = { viewModel.showVisualPrompt(short) },
                    onSeo = { viewModel.generateMultiPlatformSeo(short.viralTitle, short.scriptText) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // AI Multi-Platform SEO Dialog (YouTube, Instagram, Bilibili via OpenRouter)
    MultiPlatformSeoDialog(
        isOpen = showSeoDialog,
        seoState = seoState,
        seoData = seoData,
        onDismiss = { viewModel.dismissSeoDialog() }
    )

    // Visual Prompt Generator Dialog
    if (visualPromptShort != null) {
        val s = visualPromptShort!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissVisualPrompt() },
            containerColor = GeoDarkSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = GeoSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "AI Video Prompt",
                            color = GeoTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { viewModel.dismissVisualPrompt() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GeoTextMuted)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Cinematic documentary prompt ready for AI video generation (Runway / Pika / Sora / Midjourney / Veo):",
                        color = GeoTextSecondary,
                        fontSize = 12.sp
                    )
                    Surface(
                        color = GeoDarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder)
                    ) {
                        Text(
                            text = s.visualPrompt,
                            color = GeoTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        text = "• Described only documented facts & realistic news lighting.\n• No fabricated personal actions.",
                        color = GeoTextMuted,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        copyToClipboard(context, "AI Video Prompt", s.visualPrompt)
                        viewModel.dismissVisualPrompt()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GeoSecondary, contentColor = GeoDarkBg)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Visual Prompt", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissVisualPrompt() }) {
                    Text("Close", color = GeoTextSecondary)
                }
            }
        )
    }
}

@Composable
fun ShortCard(
    index: Int,
    short: ShortScript,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onMoreHuman: () -> Unit,
    onMorePunchy: () -> Unit,
    onVisualPrompt: () -> Unit,
    onSeo: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
        modifier = Modifier.fillMaxWidth().testTag("short_card_$index")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Short 1 / Short 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(GeoSecondary.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, GeoSecondary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$index",
                            color = GeoSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Short $index",
                        color = GeoTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = GeoDarkSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder)
                ) {
                    Text(
                        text = "${short.targetDurationSeconds} SECONDS",
                        color = GeoSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Viral Title
            Text(
                text = "🔥 ${short.viralTitle}",
                color = GeoTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Shorts / Instagram Title
            Text(
                text = "📱 ${short.shortsTitle}",
                color = GeoPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Thumbnail Text
            Surface(
                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
            ) {
                Text(
                    text = "🖼️ THUMBNAIL: ${short.thumbnailText}",
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Second Spoken Script
            Text(
                text = "🎙️ SPOKEN SCRIPT (${short.targetDurationSeconds}s):",
                color = GeoTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = GeoDarkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = short.scriptText,
                    color = GeoTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Direction
            Text(
                text = "🎬 Visual Direction: ${short.visualDirection}",
                color = GeoTextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Captions
            Text(
                text = "💬 On-Screen Captions: ${short.captionsText}",
                color = GeoTextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Source
            Text(
                text = "📰 Source: ${short.sourceName}",
                color = GeoTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row 1: Copy & Regenerate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeoPrimary,
                        contentColor = GeoDarkBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp).testTag("copy_script_$index")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRegenerate,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoTextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp).testTag("regenerate_short_$index")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Regenerate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row 2: More Human, More Punchy, Visual Prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onMoreHuman,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoTextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("more_human_$index")
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("More Human", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onMorePunchy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoSecondary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("more_punchy_$index")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("More Punchy", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onVisualPrompt,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeoDarkSurfaceVariant,
                        contentColor = GeoPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("visual_prompt_$index")
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Visual Prompt", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button Row 3: Multi-Platform SEO (YouTube, Instagram, Bilibili) via OpenRouter
            Button(
                onClick = onSeo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeoSecondary.copy(alpha = 0.15f),
                    contentColor = GeoSecondary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoSecondary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("seo_btn_short_$index")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Generate SEO (YouTube • Instagram • Bilibili)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
