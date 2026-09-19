package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.MultiPlatformSeo
import com.example.data.model.PlatformSeoData
import com.example.data.repository.ScriptGenState
import com.example.ui.theme.GeoCardBorder
import com.example.ui.theme.GeoDarkBg
import com.example.ui.theme.GeoDarkSurface
import com.example.ui.theme.GeoDarkSurfaceVariant
import com.example.ui.theme.GeoPrimary
import com.example.ui.theme.GeoSecondary
import com.example.ui.theme.GeoTextMuted
import com.example.ui.theme.GeoTextPrimary
import com.example.ui.theme.GeoTextSecondary

// Platform Brand Colors
val ColorYouTube = Color(0xFFFF0000)
val ColorInstagram = Color(0xFFE1306C)
val ColorBilibili = Color(0xFF23ADE5)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiPlatformSeoDialog(
    isOpen: Boolean,
    seoState: ScriptGenState,
    seoData: MultiPlatformSeo?,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val platforms = listOf("YouTube", "Instagram", "Bilibili")

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GeoSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "AI Multi-Platform SEO",
                            color = GeoTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Powered by OpenRouter API",
                            color = GeoTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = GeoTextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (seoState) {
                    is ScriptGenState.Generating -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = GeoSecondary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Analyzing script with OpenRouter...",
                                    color = GeoTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tailoring viral hooks, tags & descriptions for YouTube, Instagram Reels & Bilibili",
                                    color = GeoTextMuted,
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    is ScriptGenState.Error -> {
                        Surface(
                            color = Color(0xFF3F1D24),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "SEO Generation Failed",
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = seoState.message,
                                    color = GeoTextPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        if (seoData != null) {
                            // Tabs for YouTube, Instagram, Bilibili
                            ScrollableTabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = GeoDarkSurfaceVariant,
                                contentColor = GeoTextPrimary,
                                edgePadding = 4.dp,
                                indicator = { tabPositions ->
                                    val tabColor = when (selectedTab) {
                                        0 -> ColorYouTube
                                        1 -> ColorInstagram
                                        else -> ColorBilibili
                                    }
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                        color = tabColor,
                                        height = 3.dp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GeoCardBorder, RoundedCornerShape(8.dp))
                            ) {
                                platforms.forEachIndexed { idx, platform ->
                                    val isSelected = selectedTab == idx
                                    val brandColor = when (idx) {
                                        0 -> ColorYouTube
                                        1 -> ColorInstagram
                                        else -> ColorBilibili
                                    }
                                    Tab(
                                        selected = isSelected,
                                        onClick = { selectedTab = idx },
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(brandColor, CircleShape)
                                                )
                                                Text(
                                                    text = platform,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) GeoTextPrimary else GeoTextSecondary
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            val activePlatformData: PlatformSeoData = when (selectedTab) {
                                0 -> seoData.youtube
                                1 -> seoData.instagram
                                else -> seoData.bilibili
                            }

                            val currentBrandColor = when (selectedTab) {
                                0 -> ColorYouTube
                                1 -> ColorInstagram
                                else -> ColorBilibili
                            }

                            // Platform Detail Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = GeoDarkSurfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, currentBrandColor.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. Optimized Title
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ALGORITHM-OPTIMIZED TITLE",
                                            color = currentBrandColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        IconButton(
                                            onClick = {
                                                copyToClipboard(context, "${activePlatformData.platformName} Title", activePlatformData.title)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Title", tint = GeoTextMuted, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Text(
                                        text = activePlatformData.title,
                                        color = GeoTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 19.sp
                                    )

                                    // 2. High-CTR Description / Caption
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (selectedTab) {
                                                0 -> "YOUTUBE DESCRIPTION & TIMESTAMPS"
                                                1 -> "INSTAGRAM REEL CAPTION & HOOK"
                                                else -> "BILIBILI DYNAMIC POST & DESCRIPTION (动态)"
                                            },
                                            color = currentBrandColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        IconButton(
                                            onClick = {
                                                copyToClipboard(context, "${activePlatformData.platformName} Description", activePlatformData.description)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Description", tint = GeoTextMuted, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Surface(
                                        color = GeoDarkBg,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, GeoCardBorder)
                                    ) {
                                        Text(
                                            text = activePlatformData.description,
                                            color = GeoTextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    // 3. Hashtags
                                    if (activePlatformData.hashtags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "TRENDING HASHTAGS",
                                                color = GeoSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            )
                                            TextButton(
                                                onClick = {
                                                    copyToClipboard(context, "Hashtags", activePlatformData.hashtags.joinToString(" "))
                                                }
                                            ) {
                                                Text("Copy All", fontSize = 10.sp, color = GeoSecondary)
                                            }
                                        }
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            activePlatformData.hashtags.forEach { tag ->
                                                Surface(
                                                    color = GeoSecondary.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = BorderStroke(1.dp, GeoSecondary.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = tag,
                                                        color = GeoSecondary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 4. Algorithm Tags
                                    if (activePlatformData.tags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedTab == 2) "BILIBILI PARTITION TAGS (分区标签)" else "SEARCH ALGORITHM TAGS",
                                                color = GeoPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            )
                                            TextButton(
                                                onClick = {
                                                    copyToClipboard(context, "Tags", activePlatformData.tags.joinToString(", "))
                                                }
                                            ) {
                                                Text("Copy CSV", fontSize = 10.sp, color = GeoPrimary)
                                            }
                                        }
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            activePlatformData.tags.forEach { tag ->
                                                Surface(
                                                    color = GeoPrimary.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Icon(Icons.Default.Tag, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(10.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = tag,
                                                            color = GeoPrimary,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 5. Call To Action & Growth Tip
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = GeoDarkBg,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, GeoCardBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, tint = currentBrandColor, modifier = Modifier.size(12.dp))
                                                Text("RECOMMENDED CALL TO ACTION:", color = currentBrandColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = activePlatformData.callToAction,
                                                color = GeoTextPrimary,
                                                fontSize = 11.sp
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GeoSecondary, modifier = Modifier.size(12.dp))
                                                Text("ENGAGEMENT STRATEGY TIP:", color = GeoSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = activePlatformData.engagementTips,
                                                color = GeoTextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (seoData != null && seoState !is ScriptGenState.Generating) {
                val currentP = when (selectedTab) {
                    0 -> seoData.youtube
                    1 -> seoData.instagram
                    else -> seoData.bilibili
                }
                Button(
                    onClick = {
                        val fullBundle = buildString {
                            appendLine("=== ${currentP.platformName.uppercase()} METADATA & SEO ===")
                            appendLine("TITLE: ${currentP.title}")
                            appendLine("\nDESCRIPTION / CAPTION:\n${currentP.description}")
                            appendLine("\nHASHTAGS:\n${currentP.hashtags.joinToString(" ")}")
                            appendLine("\nTAGS (CSV):\n${currentP.tags.joinToString(", ")}")
                            appendLine("\nCTA: ${currentP.callToAction}")
                            appendLine("ENGAGEMENT TIP: ${currentP.engagementTips}")
                        }
                        copyToClipboard(context, "${currentP.platformName} SEO Bundle", fullBundle)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedTab) {
                            0 -> ColorYouTube
                            1 -> ColorInstagram
                            else -> ColorBilibili
                        },
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("copy_platform_seo_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Copy All for ${platforms[selectedTab]}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = GeoTextSecondary)
            }
        }
    )
}
