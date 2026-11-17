package com.mfouad.muslimtv.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusRequester
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.ChoiceRow
import com.mfouad.muslimtv.core.designsystem.component.Skeleton
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import com.mfouad.muslimtv.core.model.City
import java.util.Locale

data class CityPickerState(
    val query: String = "",
    val results: List<City> = emptyList(),
    val suggestions: List<City> = emptyList(),
    val loading: Boolean = true,
)

/** Search field plus a short list of cities. With an empty query it shows suggestions. */
@Composable
fun CityPicker(
    state: CityPickerState,
    selectedCityId: Long?,
    onQueryChange: (String) -> Unit,
    onPick: (City) -> Unit,
    modifier: Modifier = Modifier,
    searchRequester: FocusRequester = remember { FocusRequester() },
) {
    val arabic = isArabicUi()
    val locale = rememberTimeFormatter().locale
    val showingSuggestions = state.query.isBlank()
    val list = if (showingSuggestions) state.suggestions else state.results
    val listState = rememberLazyListState()
    val firstItem = remember { FocusRequester() }
    // Remote-first: start on the first suggested city, so OK alone completes this step.
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.suggestions.isNotEmpty()) {
        if (!focusedOnce && showingSuggestions && state.suggestions.isNotEmpty()) {
            focusedOnce = firstItem.requestFocusSafely() || searchRequester.requestFocusSafely()
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(MtvSpacing.m)) {
        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            hint = stringResource(R.string.city_search_hint),
            fieldRequester = searchRequester,
        )
        Text(
            stringResource(if (showingSuggestions) R.string.city_suggested else R.string.city_results),
            style = MtvTheme.type.label,
            color = MtvTheme.colors.onBackgroundMuted,
        )
        when {
            state.loading && list.isEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(MtvSpacing.s)) {
                repeat(3) { Skeleton(Modifier.fillMaxWidth().height(72.dp)) }
            }
            !showingSuggestions && list.isEmpty() -> Text(
                stringResource(R.string.city_no_results, state.query),
                style = MtvTheme.type.body,
                color = MtvTheme.colors.onBackgroundMuted,
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().focusRestorer(),
                verticalArrangement = Arrangement.spacedBy(MtvSpacing.s),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
            ) {
                itemsIndexed(list, key = { _, c -> c.id }) { index, city ->
                    ChoiceRow(
                        title = city.displayName(arabic),
                        trailing = countryName(city.countryCode, locale),
                        selected = city.id == selectedCityId,
                        onClick = { onPick(city) },
                        modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
                    )
                }
            }
        }
    }
}

fun countryName(code: String, locale: Locale): String =
    if (code.isBlank()) "" else Locale("", code).getDisplayCountry(locale).ifBlank { code }
