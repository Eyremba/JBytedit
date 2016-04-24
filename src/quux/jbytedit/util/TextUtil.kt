package quux.jbytedit.util

import java.util.regex.Pattern

object TextUtil {

    val patterns = arrayOf(
            Pattern.compile("&"),
            Pattern.compile("<"),
            Pattern.compile(">")
    )
    val replacements = arrayOf(
            "&amp;",
            "&lt;",
            "&gt;"
    )

    fun escapeHTML(input: String): String {
        var result = input
        for (i in 1..patterns.size - 1)
            result = patterns[i].matcher(result).replaceAll(replacements[i]);
        return result;
    }

    fun toHtml(str: String?): String {
        return "<html>" + toBlack(str)
    }

    fun addTag(str: String?, tag: String?): String {
        return "<$tag>$str</${tag?.split(" ")?.first()}>"
        //return "<font color=$color>$str</font>"
    }

    fun toLighter(str: String?): String {
        return addTag(str, "font color=#999999")
    }

    fun toBold(str: String?): String {
        return addTag(str, "b")
    }

    fun toBlack(str: String?): String {
        return addTag(str, "font color=#000000")
    }

}
