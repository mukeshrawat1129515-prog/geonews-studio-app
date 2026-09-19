package com.example.ui.youtube

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.model.LongVideoScript
import com.example.data.repository.ScriptGenState
import com.example.ui.components.MultiPlatformSeoDialog
import com.example.ui.components.copyToClipboard
import com.example.ui.components.openBrowserUrl
import com.example.ui.theme.*
import com.example.ui.viewmodel.GeoNewsViewModel

@Composable
fun YouTubeScreen(
    viewModel: GeoNewsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val longScript by viewModel.longScript.collectAsStateWithLifecycle()
    val genState by viewModel.longVideoGenState.collectAsStateWithLifecycle()
    val topStories by viewModel.topStories.collectAsStateWithLifecycle()

    val showSeoDialog by viewModel.showSeoDialog.collectAsStateWithLifecycle()
    val seoState by viewModel.seoGenState.collectAsStateWithLifecycle()
    val seoData by viewModel.currentSeoData.collectAsStateWithLifecycle()

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
                        text = "YouTube Long Video",
                        color = GeoTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "8–12 min In-Depth Documentary Script (Hindi/Hinglish)",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.generateLongVideo() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = GeoTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp).testTag("regenerate_long_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regenerate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Generating progress state
        if (genState is ScriptGenState.Generating) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GeoDarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFEF4444),
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

        // Error state
        if (genState is ScriptGenState.Error) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1619)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFEF4444))
                        Text(
                            text = (genState as ScriptGenState.Error).message,
                            color = GeoTextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.generateLongVideo() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // If no script generated yet
        if (longScript == null && genState !is ScriptGenState.Generating) {
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
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No Long Video Script Generated",
                            color = GeoTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Synthesizes today's 4 verified stories into a complete 10-chapter documentary script with citations and source transparency.",
                            color = GeoTextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.generateLongVideo() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = GeoTextPrimary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate 8–12 Min Script", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (longScript != null) {
            val script = longScript!!

            // Title and Copy action
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "DURATION: ${script.estimatedDuration}",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.generateMultiPlatformSeo(script.title, script.fullCombinedScript)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GeoSecondary.copy(alpha = 0.15f),
                                        contentColor = GeoSecondary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoSecondary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp).testTag("seo_long_script_btn")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SEO Pack", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        copyToClipboard(context, "YouTube Script", script.fullCombinedScript)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GeoPrimary,
                                        contentColor = GeoDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp).testTag("copy_long_script_btn")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = script.title,
                            color = GeoTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // 10 Mandatory Sections
            item {
                DocumentarySectionCard(
                    number = 1,
                    title = "Strong Opening Hook",
                    content = script.openingHook
                )
            }
            item {
                DocumentarySectionCard(
                    number = 2,
                    title = "What Happened?",
                    content = script.whatHappened
                )
            }
            item {
                DocumentarySectionCard(
                    number = 3,
                    title = "Background / Context",
                    content = script.backgroundContext
                )
            }
            item {
                DocumentarySectionCard(
                    number = 4,
                    title = "Timeline",
                    content = script.timeline
                )
            }
            item {
                DocumentarySectionCard(
                    number = 5,
                    title = "What Each Relevant Side Officially Says",
                    subtitle = "Attributed Official Claims",
                    content = script.officialClaims
                )
            }
            item {
                DocumentarySectionCard(
                    number = 6,
                    title = "What Is Independently Confirmed",
                    subtitle = "Verified Facts",
                    content = script.confirmedFacts
                )
            }
            item {
                DocumentarySectionCard(
                    number = 7,
                    title = "Why It Matters",
                    content = script.whyItMatters
                )
            }
            item {
                DocumentarySectionCard(
                    number = 8,
                    title = "Possible Implications",
                    subtitle = "Clearly Labeled as Analysis",
                    content = script.implicationsAnalysis
                )
            }
            item {
                DocumentarySectionCard(
                    number = 9,
                    title = "What Remains Uncertain",
                    subtitle = "Transparency & Open Questions",
                    content = script.whatRemainsUncertain
                )
            }
            item {
                DocumentarySectionCard(
                    number = 10,
                    title = "Conclusion",
                    content = script.conclusion
                )
            }

            // Sources List Section with Clickable URLs
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                    modifier = Modifier.fillMaxWidth().testTag("sources_section_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SOURCES",
                                color = GeoPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (topStories.isNotEmpty()) {
                            topStories.forEach { story ->
                                Surface(
                                    color = GeoDarkSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = story.sourceName,
                                                color = GeoSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = story.headline,
                                                color = GeoTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2
                                            )
                                            Text(
                                                text = "Published: ${story.publishedAt}",
                                                color = GeoTextMuted,
                                                fontSize = 10.sp
                                            )
                                        }

                                        IconButton(onClick = { openBrowserUrl(context, story.sourceUrl) }) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInBrowser,
                                                contentDescription = "Open Source",
                                                tint = GeoPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = script.sourcesText,
                                color = GeoTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
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
}

@Composable
fun DocumentarySectionCard(
    number: Int,
    title: String,
    subtitle: String? = null,
    content: String
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
        modifier = Modifier.fillMaxWidth().testTag("doc_section_$number")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(GeoPrimary.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, GeoPrimary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$number",
                            color = GeoPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = GeoTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                color = GeoSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = GeoTextMuted
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = GeoDarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = content.ifBlank { "Details loading..." },
                            color = GeoTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
