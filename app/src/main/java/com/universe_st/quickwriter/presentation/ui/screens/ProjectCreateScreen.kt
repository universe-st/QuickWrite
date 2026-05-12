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
import com.universe_st.quickwriter.presentation.viewmodel.ProjectCreateUiState
import com.universe_st.quickwriter.presentation.viewmodel.ProjectCreateViewModel
import com.universe_st.quickwriter.util.FileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCreateScreen(
    onBackPressed: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProjectCreateViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val formData by viewModel.formData.collectAsState()
    var genreMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建项目") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                is ProjectCreateUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                    )
                }
                else -> {
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
                            label = { Text("项目标题 *") },
                            isError = formData.titleError != null,
                            supportingText = formData.titleError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        OutlinedTextField(
                            value = formData.author,
                            onValueChange = { viewModel.updateAuthor(it) },
                            label = { Text("作者名称 *") },
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
                                label = { Text("小说类型") },
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
                            label = { Text("项目描述") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.createProject() },
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
                            Text("创建项目")
                        }

                        if (uiState is ProjectCreateUiState.Error) {
                            val context = LocalContext.current
                            val errorMessage = (uiState as ProjectCreateUiState.Error).message
                            Text(
                                text = errorMessage.asString(context),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ProjectCreateUiState.Success) {
            onNavigateBack()
        }
    }
}