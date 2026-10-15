package com.desconectado.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.formatearDuracion
import com.desconectado.app.domain.model.UserPreferences
import kotlin.math.roundToInt

@Composable
fun SelectorMetaDiaria(
    selectedMinutes: Int?,
    error: Boolean,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    titleResource: Int = R.string.registro_meta_titulo,
) {
    val initialDailyGoal = selectedMinutes?.let(UserPreferences::metaDiariaDesdeSemanal)
        ?: UserPreferences.META_DIARIA_MINIMA
    var dailyGoal by remember(selectedMinutes) { mutableIntStateOf(initialDailyGoal) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(titleResource),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (selectedMinutes == null) {
                stringResource(R.string.selector_meta_diaria_pendiente)
            } else {
                stringResource(R.string.perfil_meta_diaria_seleccionada, formatearDuracion(dailyGoal))
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("meta_diaria_seleccionada"),
        )
        Slider(
            value = dailyGoal.toFloat(),
            onValueChange = { value ->
                dailyGoal = ((value.roundToInt() + UserPreferences.INCREMENTO_META / 2) /
                    UserPreferences.INCREMENTO_META) * UserPreferences.INCREMENTO_META
                onSelect(UserPreferences.metaSemanalDesdeDiaria(dailyGoal))
            },
            valueRange = UserPreferences.META_DIARIA_MINIMA.toFloat()..UserPreferences.META_DIARIA_MAXIMA.toFloat(),
            steps = (UserPreferences.META_DIARIA_MAXIMA - UserPreferences.META_DIARIA_MINIMA) /
                UserPreferences.INCREMENTO_META - 1,
            modifier = Modifier.fillMaxWidth().testTag("selector_meta_diaria"),
        )
        Text(
            text = if (selectedMinutes == null) {
                stringResource(R.string.selector_meta_semanal_pendiente)
            } else {
                stringResource(
                    R.string.perfil_meta_semanal_generada,
                    formatearDuracion(UserPreferences.metaSemanalDesdeDiaria(dailyGoal)),
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (error) {
            Text(text = stringResource(R.string.registro_meta_error), color = MaterialTheme.colorScheme.error)
        }
    }
}