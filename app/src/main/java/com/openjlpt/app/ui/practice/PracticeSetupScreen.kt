package com.openjlpt.app.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.ui.components.BackTopBar
import com.openjlpt.app.ui.components.LoadingBox
import com.openjlpt.app.ui.components.rememberBank
import com.openjlpt.core.format.JlptFormat
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.QuestionType
import com.openjlpt.core.model.Section

private val countOptions = listOf(10, 20, 30, 0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeSetupScreen(
    container: AppContainer,
    level: JlptLevel,
    section: Section,
    onBack: () -> Unit,
    onStart: (QuestionType?, Int) -> Unit,
) {
    val bank by rememberBank(container, level)
    // Selected type name, or "" for every type in the section.
    var typeName by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableIntStateOf(10) }

    Scaffold(topBar = { BackTopBar("${level.label} · ${section.english}", onBack) }) { padding ->
        val loaded = bank
        if (loaded == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        val types = JlptFormat.typesFor(level).filter { it.section == section }.distinct()
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(section.japanese, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Choose a question type", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    TypeRow(
                        title = "All types",
                        subtitle = "Mixed practice",
                        count = loaded.bySection(section).size,
                        selected = typeName.isEmpty(),
                        onSelect = { typeName = "" },
                    )
                    types.forEach { type ->
                        TypeRow(
                            title = type.japanese,
                            subtitle = type.english,
                            count = loaded.byType(type).size,
                            selected = typeName == type.name,
                            onSelect = { typeName = type.name },
                        )
                    }
                }
            }
            Text("Number of questions", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                countOptions.forEach { option ->
                    FilterChip(
                        selected = count == option,
                        onClick = { count = option },
                        label = { Text(if (option == 0) "All" else option.toString()) },
                    )
                }
            }
            Text(
                "You'll see whether each answer is right straight away, with an explanation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val selectedType = types.firstOrNull { it.name == typeName }
            val available = if (selectedType != null) loaded.byType(selectedType).size else loaded.bySection(section).size
            Button(
                onClick = { onStart(selectedType, if (count == 0) available else count) },
                enabled = available > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (available > 0) "Start practice" else "No questions yet")
            }
        }
    }
}

@Composable
private fun TypeRow(title: String, subtitle: String, count: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("$count", style = MaterialTheme.typography.labelLarge)
    }
}
