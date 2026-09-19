package com.example.ui.news

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.NewsRefreshState
import com.example.ui.components.NewsStoryCard
import com.example.ui.components.StageLoadingCard
import com.example.ui.components.openBrowserUrl
import com.example.ui.theme.*
import com.example.ui.viewmodel.GeoNewsViewModel

@Composable
fun NewsScreen(
    viewModel: GeoNewsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val topStories by viewModel.topStories.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Geopolitics", "Conflict", "Diplomacy", "Economy", "Defence", "International Relations")

    val filteredStories = remember(topStories, selectedCategory) {
        if (selectedCategory == "All") topStories
        else topStories.filter { it.categoryTag.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "Geopolitical Intel",
                        color = GeoTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "4 verified stories curated from international wires",
                        color = GeoTextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { viewModel.refreshNews() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = GeoPrimary
                    )
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GeoDarkSurface,
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

        if (refreshState is NewsRefreshState.Loading) {
            item {
                StageLoadingCard(message = (refreshState as NewsRefreshState.Loading).stageMessage)
            }
        }

        items(
            items = filteredStories,
            key = { story -> story.id }
        ) { story ->
            NewsStoryCard(
                story = story,
                onReadSource = { url -> openBrowserUrl(context, url) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
