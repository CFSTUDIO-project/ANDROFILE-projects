package app.androfile.manager.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class SyntaxHighlighter(private val extension: String) : VisualTransformation {
    private val tokenPattern = Regex(
        """(?s)(//[^\n]*|/\*.*?\*/|<!--.*?-->|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|#[^\n]*|</?[A-Za-z][^>]*>|\b(?:fun|val|var|class|object|interface|return|if|else|when|for|while|in|is|as|import|package|public|private|protected|override|suspend|const|let|async|await|function|export|default|new|true|false|null|None|def|from|try|catch|throw|extends|implements|this|super|static|final|void|int|String|boolean)\b|\b\d+(?:\.\d+)?\b)"""
    )

    override fun filter(text: AnnotatedString): TransformedText {
        if (extension.lowercase() in setOf("txt", "log", "md", "csv")) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val highlighted = buildAnnotatedString {
            var cursor = 0
            tokenPattern.findAll(text.text).forEach { match ->
                if (match.range.first > cursor) append(text.subSequence(cursor, match.range.first))
                val token = match.value
                pushStyle(SpanStyle(color = tokenColour(token)))
                append(token)
                pop()
                cursor = match.range.last + 1
            }
            if (cursor < text.length) append(text.subSequence(cursor, text.length))
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }

    private fun tokenColour(token: String): Color {
        val lower = token.lowercase()
        return when {
            token.startsWith("//") || token.startsWith("/*") || token.startsWith("<!--") || token.startsWith("#") -> Color(0xFF8794A5)
            token.startsWith("\"") || token.startsWith("'") -> Color(0xFF9BD6A8)
            token.startsWith("<") -> Color(0xFF71C9E8)
            token.firstOrNull()?.isDigit() == true -> Color(0xFFF0B66A)
            lower in KEYWORDS -> Color(0xFFC6A0F6)
            else -> Color(0xFFE6EDF5)
        }
    }

    companion object {
        private val KEYWORDS = setOf(
            "fun", "val", "var", "class", "object", "interface", "return", "if", "else",
            "when", "for", "while", "in", "is", "as", "import", "package", "public",
            "private", "protected", "override", "suspend", "const", "let", "async",
            "await", "function", "export", "default", "new", "true", "false", "null",
            "none", "def", "from", "try", "catch", "throw", "extends", "implements",
            "this", "super", "static", "final", "void", "int", "string", "boolean"
        )
    }
}
