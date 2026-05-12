package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.presentation.viewmodel.ProjectEditUiState
import com.universe_st.quickwriter.presentation.viewmodel.ProjectEditViewModel
import com.universe_st.quickwriter.util.FileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditScreen(
    projectId: String,
    onBackPressed: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProjectEditViewModel
) {
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val formData by viewModel.formData.collectAsState()
    var genreMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.project_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ProjectEditUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                    )
                }
                is ProjectEditUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = formData.title,
                            onValueChange = { viewModel.updateTitle(it) },
                            label = { Text(stringResource(R.string.project_field_title)) },
                            isError = formData.titleError != null,
                            supportingText = formData.titleError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        OutlinedTextField(
                            value = formData.author,
                            onValueChange = { viewModel.updateAuthor(it) },
                            label = { Text(stringResource(R.string.project_field_author)) },
                            isError = formData.authorError != null,
                            supportingText = formData.authorError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        ExposedDropdownMenuBox(
                            expanded = genreMenuExpanded,
                            onExpandedChange = { genreMenuExpanded = !genreMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = formData.genre,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(stringResource(R.string.project_field_genre)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreMenuExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = genreMenuExpanded,
                                onDismissRequest = { genreMenuExpanded = false }
                            ) {
                                FileManager.NOVEL_GENRES.forEach { genre ->
                                    DropdownMenuItem(
                                        text = { Text(genre) },
                                        onClick = {
                                            viewModel.updateGenre(genre)
                                            genreMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = formData.description,
                            onValueChange = { viewModel.updateDescription(it) },
                            label = { Text(stringResource(R.string.project_field_description)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.updateProject() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = formData.title.isNotEmpty() &&
                                    formData.author.isNotEmpty() &&
                                    formData.titleError == null &&
                                    formData.authorError == null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(stringResource(R.string.project_edit_button))
                        }

                        if (uiState is ProjectEditUiState.Error) {
                            val context = LocalContext.current
                            val errorMessage = (uiState as ProjectEditUiState.Error).message
                            Text(
                                text = errorMessage.asString(context),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                is ProjectEditUiState.Error -> {
                    val context = LocalContext.current
                    val errorMessage = (uiState as ProjectEditUiState.Error).message
                    Column(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.Center)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = errorMessage.asString(context),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onNavigateBack) {
                            Text(stringResource(R.string.common_back))
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ProjectEditUiState.UpdateSuccess) {
            onNavigateBack()
        }
    }
}
