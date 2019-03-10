package action

class CopyWithMethodGenerator {
    private val dartFieldLineRegex = """^\s*(?:final)?\s*(\w+)\s*(\w+);$""".toRegex()
    private val classNameLineRegex = """^.*(?:class)\s*(\w+).*$""".toRegex()

    private val intendation = "  "

    fun generate(fileContent: CharSequence): String? {
        val lines = fileContent.lines()
        val classLineIndex = lines.indexOfFirst { classNameLineRegex.matches(it) }
        val className = extractClassName(lines[classLineIndex]) ?: return null.also { println("Couldn't find class name") }
        return generateCopyWithMethodString(
            className,
            extractLeadingFields(lines.subList(classLineIndex + 1, lines.size))
        )
    }

    private fun generateCopyWithMethodString(className: String, fields: List<DartField>): String =
        CopyWithMethodFormatter(CopyWithMethod(className, fields), intendation).format()

    private fun extractClassName(line: String): String? {
        val result = classNameLineRegex.find(line) ?: return null
        return if (result.groupValues.size == 2) {
            result.groupValues[1]
        } else { // No match
            null
        }
    }

    /// Extracts leading (from the beginning until first non-field and non-blank line) field declarations.
    private fun extractLeadingFields(lines: List<String>): List<DartField> =
        lines.takeWhile { toField(it) != null || it.isBlank() }.mapNotNull { toField(it) }

    private fun toField(line: String): DartField? {
        val result = dartFieldLineRegex.find(line) ?: return null
        return if (result.groupValues.size == 3) {
            DartField(result.groupValues[1], result.groupValues[2])
        } else { // No match
            null
        }
    }
}

private data class DartField(val type: String, val name: String)
private data class CopyWithMethod(val clazz: String, val fields: List<DartField>)

private class CopyWithMethodFormatter(val copyWithMethod: CopyWithMethod, val intendation: String) {

    fun format(): String = copyWithMethod.toFormattedLines().joinToString("\n")

    private fun CopyWithMethod.toFormattedLines(): List<String> = listOf(
        "$clazz copyWith({${toMethodParametersString(fields)}}) {",
        "${intendation.repeat(2)}return $clazz("
    ) + copyWithFallbackLines().map {
        "${intendation.repeat(3)}$it"
    } + listOf(
        "${intendation.repeat(2)});",
        "$intendation}"
    )

    private fun CopyWithMethod.copyWithFallbackLines() = fields.map { toCopyWithFallbackLine(it) }

    private fun toMethodParametersString(fields: List<DartField>): String =
        fields.joinToString(", ") { field -> "${field.type} ${field.name}" }

    private fun toCopyWithFallbackLine(field: DartField): String =
        "${field.name}: ${field.name} ?? this.${field.name},"
}
