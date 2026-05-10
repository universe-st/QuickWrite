package com.universe_st.quickwriter.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.util.ChapterPattern
import com.universe_st.quickwriter.util.FileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxtImportDialog(
    defaultTitle: String,
    onConfirm: (title: String, author: String, genre: String, patterns: Set<ChapterPattern>, customRegex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(defaultTitle) }
    var author by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("其他") }
    var selectedPatterns by remember { mutableStateOf(setOf(ChapterPattern.CN_CHAPTER)) }
    var customRegex by remember { mutableStateOf("") }
    var genreExpanded by remember { mutableStateOf(false) }

    val isValid = title.isNotBlank() && author.isNotBlank() && selectedPatterns.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.txt_import_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.txt_import_label_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.txt_import_label_author)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = genreExpanded,
                    onExpandedChange = { genreExpanded = it }
                ) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.txt_import_label_genre)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = genreExpanded,
                        onDismissRequest = { genreExpanded = false }
                    ) {
                        FileManager.NOVEL_GENRES.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    genre = g
                                    genreExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.txt_import_label_patterns),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ChapterPattern.values().forEach { pattern ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPatterns = if (pattern in selectedPatterns) {
                                    selectedPatterns - pattern
                                } else {
                                    selectedPatterns + pattern
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = pattern in selectedPatterns,
                            onCheckedChange = {
                                selectedPatterns = if (it) {
                                    selectedPatterns + pattern
                                } else {
                                    selectedPatterns - pattern
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(pattern.displayNameKey),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (ChapterPattern.CUSTOM in selectedPatterns) {
                    OutlinedTextField(
                        value = customRegex,
                        onValueChange = { customRegex = it },
                        label = { Text("Regex") },
                        placeholder = { Text(stringResource(R.string.txt_import_custom_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, author, genre, selectedPatterns, customRegex) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.txt_import_btn_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
