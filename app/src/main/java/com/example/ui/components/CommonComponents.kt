package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NewsStory
import com.example.ui.theme.*

@Composable
fun FactStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, icon, label) = when (status.uppercase()) {
        "CONFIRMED" -> Quad(
            StatusConfirmed.copy(alpha = 0.15f),
            StatusConfirmed,
            Icons.Default.CheckCircle,
            "CONFIRMED"
        )
        "OFFICIAL CLAIM" -> Quad(
            StatusOfficialClaim.copy(alpha = 0.15f),
            StatusOfficialClaim,
            Icons.Default.Info,
            "OFFICIAL CLAIM"
        )
        "REPORTING" -> Quad(
            StatusReporting.copy(alpha = 0.15f),
            StatusReporting,
            Icons.Default.Public,
            "REPORTING"
        )
        "ALLEGATION" -> Quad(
            StatusAllegation.copy(alpha = 0.15f),
            StatusAllegation,
            Icons.Default.Warning,
            "ALLEGATION"
        )
        else -> Quad(
            StatusUnclear.copy(alpha = 0.15f),
            StatusUnclear,
            Icons.AutoMirrored.Filled.HelpOutline,
            "UNCLEAR / UNVERIFIED"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.35f)),
        modifier = modifier.testTag("fact_status_${status.lowercase().replace(' ', '_')}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun CategoryChip(category: String, modifier: Modifier = Modifier) {
    val tagColor = when (category.lowercase()) {
        "conflict" -> TagConflict
        "diplomacy" -> TagDiplomacy
        "economy" -> TagEconomy
        "defence" -> TagDefence
        "international relations" -> TagRelations
        else -> TagGeopolitics
    }

    Surface(
        color = tagColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tagColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Text(
            text = category.uppercase(),
            color = tagColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StageLoadingCard(message: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = GeoDarkSurfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeoPrimary.copy(alpha = alpha)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("stage_loading_card")
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = GeoPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "AI News Engine Active",
                    color = GeoPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    color = GeoTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun NewsStoryCard(
    story: NewsStory,
    modifier: Modifier = Modifier,
    onReadSource: (String) -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("news_card_${story.rank}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Rank badge, Category tag, Fact Status
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
                            .size(26.dp)
                            .background(GeoPrimary.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, GeoPrimary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${story.rank}",
                            color = GeoPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    CategoryChip(category = story.categoryTag)
                }

                FactStatusBadge(status = story.factStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Headline
            Text(
                text = story.headline,
                color = GeoTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2-3 line summary
            Text(
                text = story.summary,
                color = GeoTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row: Country/Region & Source + Published Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "📍 ${story.countryRegion}",
                        color = GeoTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${story.sourceName} • ${story.publishedAt}",
                        color = GeoTextMuted,
                        fontSize = 11.sp
                    )
                }

                if (story.isShortCandidate) {
                    Surface(
                        color = GeoSecondary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoSecondary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GeoSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "SHORTS PICK",
                                color = GeoSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Source URL bar
            Surface(
                color = GeoDarkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReadSource(story.sourceUrl) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = GeoPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = story.sourceUrl,
                        color = GeoTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Open Source Link",
                        tint = GeoPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (story.relatedSources.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Cross-referenced with: ${story.relatedSources}",
                    color = GeoSecondary.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Read Source Button
            OutlinedButton(
                onClick = { onReadSource(story.sourceUrl) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GeoPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("read_source_btn_${story.rank}")
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open Original Report (${story.sourceName})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}

fun openBrowserUrl(context: Context, url: String) {
    try {
        val uri = if (url.startsWith("http://") || url.startsWith("https://")) {
            Uri.parse(url)
        } else {
            Uri.parse("https://$url")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open source link", Toast.LENGTH_SHORT).show()
    }
}
