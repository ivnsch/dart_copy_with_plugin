package action

class CopyWithMethodGenerator {
    private val fieldLineRegex = """^\s*(?:final)?\s*(\w+)\s*(\w+);$""".toRegex()
    private val classNameLineRegex = """^.*(?:class)\s*(\w+).*$""".toRegex()

    private val indentation = "  "

    fun generate(fileContent: CharSequence): String? {
        val lines = fileContent.lines()
        val classLineIndex = lines.indexOfFirst { classNameLineRegex.matches(it) }
        if (classLineIndex == -1) return null.also { println("No class found") }
        val className = extractClassName(lines[classLineIndex]) ?: return null.also { println("Couldn't find class name") }
        return generateCopyWithMethodString(
            className,
            extractLeadingFields(lines.subList(classLineIndex + 1, lines.size))
        )
    }

    private fun generateCopyWithMethodString(className: String, fields: List<Field>): String =
        CopyWithMethodFormatter(CopyWithMethod(className, fields), indentation).format()

    private fun extractClassName(line: String): String? {
        val result = classNameLineRegex.find(line) ?: return null
        return if (result.groupValues.size == 2) {
            result.groupValues[1]
        } else { // No match
            null
        }
    }

    /// Extracts leading (from the beginning until first non-field and non-blank line) field declarations.
    private fun extractLeadingFields(lines: List<String>): List<Field> =
        lines.takeWhile { toField(it) != null || it.isBlank() }.mapNotNull { toField(it) }

    private fun toField(line: String): Field? {
        val result = fieldLineRegex.find(line) ?: return null
        return if (result.groupValues.size == 3) {
            Field(result.groupValues[1], result.groupValues[2])
        } else { // No match
            null
        }
    }
}

private data class Field(val type: String, val name: String)
private data class CopyWithMethod(val clazz: String, val fields: List<Field>)

private class CopyWithMethodFormatter(val copyWithMethod: CopyWithMethod, val indentation: String) {

    fun format(): String = copyWithMethod.toFormattedLines().joinToString("\n")

    private fun CopyWithMethod.toFormattedLines(): List<String> = listOf(
        "$clazz copyWith({${toMethodParametersString(fields)}}) {",
        "${indentation.repeat(2)}return $clazz("
    ) + copyWithFallbackLines().map {
        "${indentation.repeat(3)}$it"
    } + listOf(
        "${indentation.repeat(2)});",
        "$indentation}"
    )

    private fun CopyWithMethod.copyWithFallbackLines() = fields.map { toCopyWithFallbackLine(it) }

    private fun toMethodParametersString(fields: List<Field>): String =
        fields.joinToString(", ") { field -> "${field.type} ${field.name}" }

    private fun toCopyWithFallbackLine(field: Field): String =
        "${field.name}: ${field.name} ?? this.${field.name},"
}
