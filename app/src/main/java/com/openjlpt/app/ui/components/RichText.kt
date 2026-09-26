package com.openjlpt.app.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/** Turns `<u>word</u>` markup from the question banks into underlined text. */
fun markupToAnnotated(text: String): AnnotatedString = buildAnnotatedString {
    var rest = text
    while (true) {
        val start = rest.indexOf("<u>")
        if (start < 0) {
            append(rest)
            break
        }
        val end = rest.indexOf("</u>", start)
        if (end < 0) {
            append(rest.replace("<u>", ""))
            break
        }
        append(rest.substring(0, start))
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold)) {
            append(rest.substring(start + 3, end))
        }
        rest = rest.substring(end + 4)
    }
}

fun stripMarkup(text: String): String = text.replace("<u>", "").replace("</u>", "")

@Composable
fun RichText(text: String, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    val annotated = remember(text) { markupToAnnotated(text) }
    Text(annotated, modifier = modifier, style = style)
}
