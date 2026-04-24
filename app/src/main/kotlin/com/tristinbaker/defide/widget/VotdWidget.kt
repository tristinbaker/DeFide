package com.tristinbaker.defide.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tristinbaker.defide.MainActivity
import com.tristinbaker.defide.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class VotdWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication<VotdWidgetEntryPoint>(context)
        val translationId = ep.prefsRepository().preferences.first().bibleTranslationId
        val result = ep.bibleRepository().getVerseOfDay(translationId)

        provideContent {
            val colors = GlanceTheme.colors
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(colors.surface)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp),
                ) {
                    Text(
                        text = context.getString(R.string.verse_of_the_day),
                        style = TextStyle(
                            color = colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    if (result != null) {
                        val (verse, book) = result
                        Text(
                            text = "“${verse.text}”",
                            style = TextStyle(
                                color = colors.onSurface,
                                fontSize = 13.sp,
                            ),
                            maxLines = 4,
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "— ${book.fullName} ${verse.chapter}:${verse.verse}",
                            style = TextStyle(
                                color = colors.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                            ),
                        )
                    } else {
                        Text(
                            text = context.getString(R.string.loading),
                            style = TextStyle(
                                color = colors.onSurfaceVariant,
                                fontSize = 13.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}
