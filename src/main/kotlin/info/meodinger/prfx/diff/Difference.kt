package info.meodinger.prfx.diff

/**
 * Author: Meodinger
 * Date: 2021/10/25
 * Have fun with my code!
 */

/**
 * An object shows a difference
 */
sealed class Difference(var content: String)

class Equal(content: String): Difference(content) {
    override fun toString(): String = "Equal: `$content`"
}
class Delete(content: String): Difference(content) {
    override fun toString(): String = "Delete: `$content`"
}
class Insert(content: String) : Difference(content) {
    override fun toString(): String = "Insert: `$content`"
}
