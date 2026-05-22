package com.hrm.breeze.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hrm.breeze.ui.theme.BreezeTheme

@Composable
fun BreezePageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
    ) {
        if (showBackButton && onBack != null) {
            BreezeHeaderBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        if (trailingContent != null) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd),
                content = trailingContent,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = typography.titleLarge,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = spacing.xxs),
                )
            }
        }
    }
}

@Composable
fun BreezeHeaderBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val typography = BreezeTheme.typography

    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = shapes.pill,
        colors = ButtonDefaults.textButtonColors(
            containerColor = scheme.surface,
            contentColor = scheme.onSurface,
        ),
    ) {
        Text(
            text = "<",
            style = typography.titleMedium,
        )
    }
}
