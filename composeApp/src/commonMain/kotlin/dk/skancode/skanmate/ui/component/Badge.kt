package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.util.ProvideContentColorTextStyle

@Composable
fun Badge(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentStyle: TextStyle = MaterialTheme.typography.labelLarge,
    contentAlign: TextAlign = TextAlign.Center,
    shape: Shape = RoundedCornerShape(4.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    content: @Composable () -> Unit,
) {
    val contentStyle = contentStyle.merge(textAlign = contentAlign)

    val boxModifier = Modifier
        .clip(shape)
        .background(color)
        .padding(contentPadding)
    Box(
        modifier = boxModifier
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        ProvideContentColorTextStyle(
            contentColor = contentColor,
            textStyle = contentStyle,
            content = content,
        )
    }
}