package im.molly.monero.demo.ui.component

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CopyableText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Text(
        text = text,
        style = style,
        modifier = modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            },
        ),
    )
}
