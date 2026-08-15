package com.gelengeden.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gelengeden.app.R

/**
 * App monogram logo. Use [size] for a square logo, or [width]/[height] to shrink
 * only vertically (e.g. home hero) while keeping the same horizontal size.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    width: Dp = size,
    height: Dp = size,
    cornerRadius: Dp? = null,
    contentScale: ContentScale = if (width == height) ContentScale.Fit else ContentScale.FillWidth
) {
    val shape = if (cornerRadius != null) {
        RoundedCornerShape(cornerRadius)
    } else {
        CircleShape
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.main_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .size(width = width * 0.72f, height = height * 0.72f),
            contentScale = contentScale
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppLogo() {
    MaterialTheme {
        AppLogo(
            size = 100.dp,
            cornerRadius = 20.dp
        )
    }
}
