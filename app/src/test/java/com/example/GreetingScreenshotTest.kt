package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.example.data.model.NewsStory
import com.example.ui.components.NewsStoryCard
import com.example.ui.theme.GeoNewsStudioTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun news_card_render_test() {
    val sampleStory = NewsStory(
      id = "test_1",
      rank = 1,
      headline = "Global Diplomatic Summit Reaches Framework Agreement",
      summary = "Delegations from twenty nations concluded discussions on regional maritime security and economic cooperation.",
      countryRegion = "Indo-Pacific",
      sourceName = "Reuters",
      publishedAt = "10 mins ago",
      sourceUrl = "https://reuters.com",
      categoryTag = "Diplomacy",
      factStatus = "CONFIRMED",
      isShortCandidate = true,
      relatedSources = "Associated Press, BBC"
    )

    composeTestRule.setContent {
      GeoNewsStudioTheme {
        NewsStoryCard(story = sampleStory, onReadSource = {})
      }
    }

    composeTestRule.onNodeWithTag("news_card_1").assertIsDisplayed()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/news_card.png")
  }
}
