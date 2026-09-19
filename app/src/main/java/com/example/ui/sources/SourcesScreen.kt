package com.example.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FactStatusBadge
import com.example.ui.components.openBrowserUrl
import com.example.ui.theme.*
import com.example.ui.viewmodel.GeoNewsViewModel

@Composable
fun SourcesScreen(
    viewModel: GeoNewsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val topStories = viewModel.topStories.collectAsStateWithLifecycle().value
    val sources = remember(topStories) { viewModel.getAllSourcesList() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = GeoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Source Verification & Citations",
                        color = GeoTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Strict factual verification. Click any entry to inspect the original reporting.",
                    color = GeoTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Neutrality banner
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = GeoSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "GeoNews Studio never invents quotes, facts, or statistics. All summaries retain direct links to primary reporting.",
                        color = GeoTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        if (sources.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No sources available yet.",
                            color = GeoTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(sources) { sourceItem ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openBrowserUrl(context, sourceItem.url) }
                        .testTag("source_card_${sourceItem.sourceName.lowercase().replace(' ', '_')}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = GeoPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = sourceItem.sourceType.uppercase(),
                                    color = GeoPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            FactStatusBadge(status = sourceItem.factStatus)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = sourceItem.headline,
                            color = GeoTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "📰 Source: ${sourceItem.sourceName}",
                            color = GeoSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "🗓️ Published: ${sourceItem.publishedAt}",
                            color = GeoTextMuted,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "🔗 Related: ${sourceItem.relatedSources}",
                            color = GeoTextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = GeoPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = sourceItem.url,
                                color = GeoPrimary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium
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
}
