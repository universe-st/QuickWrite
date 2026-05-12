package com.universe_st.quickwriter.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProjectCoverImage(
    project: ProjectEntity,
    coverImagePath: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    var effectivePath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(project.id, coverImagePath) {
        val path = withContext(Dispatchers.IO) {
            coverImagePath
                ?: project.coverImagePath
                ?: run {
                    val coverFile = File(project.storagePath, "cover.jpg")
                    if (coverFile.exists()) coverFile.absolutePath else null
                }
        }
        effectivePath = path
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
    ) {
        if (!effectivePath.isNullOrEmpty()) {
            val coverFile = File(effectivePath!!)
            val lastMod = remember(coverFile) { coverFile.lastModified() }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverFile)
                    .memoryCacheKey("${effectivePath}_$lastMod")
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.project_cover_content_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = project.title.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
