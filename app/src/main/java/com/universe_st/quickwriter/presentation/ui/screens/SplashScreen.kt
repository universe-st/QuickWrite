package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universe_st.quickwriter.R

/**
 * 闪屏界面。
 *
 * **注意**：此组件被设计为纯静态展示组件，内部不包含任何 [remember]、[LaunchedEffect]、
 * [Animatable] 或无限动画，以规避 Compose 在条件分支切换时可能触发的重组作用域 edge case。
 * 所有过渡动画由调用方（如 [Crossfade]）统一处理。
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    statusText: String = ""
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 应用 Logo 图标
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 应用名称
            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 副标题/标语
            Text(
                text = stringResource(R.string.splash_slogan),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 加载状态文字（静态，不使用旋转动画以避免重组作用域异常）
            Text(
                text = statusText,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}
