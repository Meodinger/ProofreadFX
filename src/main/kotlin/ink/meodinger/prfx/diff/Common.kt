package ink.meodinger.prfx.diff

import java.util.*
import java.util.regex.Pattern


/**
 * Author: Meodinger
 * Date: 2021/10/25
 * Have fun with my code!
 */

fun diff(ori: String, dst: String): List<Difference> {
    // Obviously
    if (ori == dst) return emptyList()

    val prefix = commonPrefix(ori, dst)
    val suffix = commonSuffix(ori, dst)

    val prefixString = ori.substring(0, prefix)
    val suffixString = ori.substring(ori.length - suffix, ori.length)
    val processedOri = if (prefix + suffix == ori.length + 1) "" else ori.substring(prefix, ori.length - suffix)
    val processedDst = if (prefix + suffix == dst.length + 1) "" else dst.substring(prefix, dst.length - suffix)

    val diffs: LinkedList<Difference> = LinkedList<Difference>()

    // One text is empty: Single Insertion/Deletion
    if (processedOri.isEmpty()) {
        // Just insert some text
        if (prefixString.isNotEmpty()) diffs.add(Equal(prefixString))
        diffs.add(Insert(processedDst))
        if (suffixString.isNotEmpty()) diffs.add(Equal(suffixString))
        return diffs
    }
    if (processedDst.isEmpty()) {
        // Just delete some text
        if (prefixString.isNotEmpty()) diffs.add(Equal(prefixString))
        diffs.add(Delete(processedOri))
        if (suffixString.isNotEmpty()) diffs.add(Equal(suffixString))
        return diffs
    }

    // One text in another: Two Insertion/Deletion
    val longText = if (processedOri.length > processedDst.length) processedOri else processedDst
    val shortText = if (processedOri.length > processedDst.length) processedDst else processedOri
    val i = longText.indexOf(shortText)
    if (i != -1) {
        if (prefixString.isNotEmpty()) diffs.add(Equal(prefixString))
        // Shorter text is inside the longer text
        if (processedOri.length > processedDst.length) {
            diffs.add(Delete(longText.substring(0, i)))
            diffs.add(Equal(shortText))
            diffs.add(Delete(longText.substring(i + shortText.length)))
        } else {
            diffs.add(Insert(longText.substring(0, i)))
            diffs.add(Equal(shortText))
            diffs.add(Insert(longText.substring(i + shortText.length)))
        }
        if (suffixString.isNotEmpty()) diffs.add(Equal(suffixString))
        return diffs
    }

    // Two text have something the same: Two <Deletion, Insertion> pairs
    val halfMatch = halfMatch(processedOri, processedDst)
    if (halfMatch.isNotEmpty()) {
        // A half-match was found, sort out the return data
        val oriA = halfMatch[0]
        val oriB = halfMatch[1]
        val dstA = halfMatch[2]
        val dstB = halfMatch[3]
        val midC = halfMatch[4]

        // Send both pairs off for separate processing
        val diffsA = diff(oriA, dstA)
        val diffsB = diff(oriB, dstB)

        // Merge the results.
        if (prefixString.isNotEmpty()) diffs.add(Equal(prefixString))
        diffs.addAll(diffsA)
        diffs.add(Equal(midC))
        diffs.addAll(diffsB)
        if (suffixString.isNotEmpty()) diffs.add(Equal(suffixString))

        return diffs
    }

    // Use lcs
    if (prefixString.isNotEmpty()) diffs.add(Equal(prefixString))
    diffs.addAll(lcs(processedOri, processedDst))
    if (suffixString.isNotEmpty()) diffs.add(Equal(suffixString))

    // merge
    merge(diffs)

    // cleanup
    // cleanupSemanticLossless(diffs)

    return diffs
}

// Since binary searches are the least efficient at their extreme points,
// and it is not uncommon in the real-world to have zero commonality,
// it makes sense to do a quick check of the first (or last) character before starting the search
private fun commonPrefix(ori: String, dst: String): Int {
    // empty strings handle
    if (ori.isEmpty() || dst.isEmpty() || ori[0] != dst[0]) return 0

    // Binary search
    var pointerMin = 0
    var pointerMax = ori.length.coerceAtMost(dst.length)
    var pointerMid = pointerMax
    var pointerStart = 0

    while (pointerMin < pointerMid) {
        if (ori.regionMatches(pointerStart, dst, pointerStart, pointerMid - pointerStart)) {
            pointerMin = pointerMid
            pointerStart = pointerMin
        } else {
            pointerMax = pointerMid
        }
        pointerMid = (pointerMax - pointerMin) / 2 + pointerMin
    }

    return pointerMid
}
private fun commonSuffix(ori: String, dst: String): Int {
    // Quick check for common null cases.
    if (ori.isEmpty() || dst.isEmpty() || ori[ori.length -1] != dst[dst.length - 1]) return 0

    // Binary search.
    var pointerMin = 0
    var pointerMax = ori.length.coerceAtMost(dst.length)
    var pointerMid = pointerMax
    var pointerEnd = 0

    while (pointerMin < pointerMid) {
        if (ori.regionMatches(ori.length - pointerMid, dst, dst.length - pointerMid, pointerMid - pointerEnd)) {
            pointerMin = pointerMid
            pointerEnd = pointerMin
        } else {
            pointerMax = pointerMid
        }
        pointerMid = (pointerMax - pointerMin) / 2 + pointerMin
    }

    return pointerMid
}

// If a substring exists in both texts which is at least half the length of the longer text,
// then it is guaranteed to be common. In this case the texts can be split in two pairs of
// <Deletion, Insertion> and a middle Equal
private fun halfMatch(ori: String, dst: String): List<String> {
    // Do the two texts share a substring which is at least half the length of the longer text?
    val longText = if (ori.length > dst.length) ori else dst
    val shortText = if (ori.length > dst.length) dst else ori

    // pointless
    if (longText.length < 10 || shortText.isEmpty()) return emptyList()

    // First check if the second quarter is the seed for a half-match
    val hm1 = halfMatchI(longText, shortText, (longText.length + 3) / 4)
    val hm2 = halfMatchI(longText, shortText, (longText.length + 1) / 2)

    val hm = if (hm1.isEmpty() && hm2.isEmpty()) emptyList()
        else if (hm2.isEmpty()) hm1
        else if (hm1.isEmpty()) hm2
        else if (hm1[4].length > hm2[4].length) hm1 else hm2

    if (hm.isEmpty()) return hm

    return if (ori.length > dst.length) hm else listOf(hm[2], hm[3], hm[0], hm[1], hm[4])
}
private fun halfMatchI(longText: String, shortText: String, i: Int): List<String> {
    // Start with a 1/4 length substring at position i as a seed.
    val seed = longText.substring(i, i + longText.length / 4)
    var j = shortText.indexOf(seed, 0)

    var bestCommon = ""
    var bestLongTextA = ""
    var bestLongTextB = ""
    var bestShortTextA = ""
    var bestShortTextB = ""

    while (j != -1) {
        val prefix = commonPrefix(longText.substring(i), shortText.substring(j))
        val suffix = commonSuffix(longText.substring(0, i), shortText.substring(0, j))

        if (bestCommon.length < suffix + prefix) {
            bestCommon = shortText.substring(j - suffix, j) + shortText.substring(j, j + prefix)
            bestLongTextA = longText.substring(0, i - suffix)
            bestLongTextB = longText.substring(i + prefix)
            bestShortTextA = shortText.substring(0, j - suffix)
            bestShortTextB = shortText.substring(j + prefix)
        }

        j = shortText.indexOf(seed, j + 1)
    }

    return if (bestCommon.length >= longText.length / 2) {
        listOf(bestLongTextA, bestLongTextB, bestShortTextA, bestShortTextB, bestCommon)
    } else {
        emptyList()
    }
}

// LCS Algorithm
private enum class Direction { EQUAL, LEFT_UP, UP, LEFT, NULL }
private class Matrix(private val value: Array<IntArray>) {

    val row: Int get() = value.size
    val col: Int get() = value[0].size

    constructor(row: Int, col: Int, init: (Int, Int) -> Int) : this(Array(row) { r -> IntArray(col) { c -> init(r, c) } })

    operator fun get(row: Int, col: Int): Int {
        return this.value[row][col]
    }
    operator fun set(row: Int, col: Int, value: Int) {
        this.value[row][col] = value
    }

    override fun toString(): String {
        val builder = StringBuilder()

        for (i in 0 until row) {
            builder.append(this[i, 0])
            for (j in 1 until col) builder.append(", ").append(this[i, j])
            builder.append("\n")
        }

        return builder.toString()
    }
}
fun lcs(ori: String, dst: String): List<Difference> {

    val matrix = Matrix(ori.length + 1, dst.length + 1) { r, c ->
        if (r == 0) return@Matrix c
        if (c == 0) return@Matrix r
        0
    }

    // If a[r] == b[c] -> LD(r + 1, c + 1) = LD(r, c)
    // If a[r] != b[c] -> LD(r + 1, c + 1) = Min(LD(r, c), LD(r, c + 1), LD(r + 1, c)) + 1
    for (r in ori.indices) for (c in dst.indices) matrix[r + 1, c + 1] =
        if (ori[r] == dst[c]) matrix[r, c]
        else matrix[r, c].coerceAtMost(matrix[r, c + 1]).coerceAtMost(matrix[r + 1, c]) + 1

    var row = matrix.row - 1
    var col = matrix.col - 1

    val diffs = LinkedList<Difference>()
    var lastDirection: Direction = Direction.NULL

    // If a[r] == b[c] -> Left Up (Equal)
    // If a[r] != b[c] -> Min(Left-Up, Up, Left)
    while (row != 0 || col != 0) {
        val canUp = row > 0
        val canLeft   = col > 0
        val shiftedRow = if (canUp) row - 1 else 0
        val shiftedCol = if (canLeft) col - 1 else 0

        if (ori[shiftedRow] == dst[shiftedCol] && canUp && canLeft) {
            row = shiftedRow
            col = shiftedCol

            val equal = Equal(ori[row].toString())
            if (lastDirection == Direction.EQUAL) {
                equal.content += diffs[0].content
                diffs.removeFirst()
            }

            diffs.addFirst(equal)
            lastDirection = Direction.EQUAL
        } else {
            val leftUp = if (canLeft && canUp) matrix[shiftedRow, shiftedCol] else Int.MAX_VALUE
            val up     = if (canUp)            matrix[shiftedRow, col]        else Int.MAX_VALUE
            val left   = if (canLeft)          matrix[row, shiftedCol]        else Int.MAX_VALUE

            when (leftUp.coerceAtMost(left).coerceAtMost(up)) {
                leftUp -> {
                    row = shiftedRow
                    col = shiftedCol

                    val delete = Delete(ori[row].toString())
                    val insert = Insert(dst[col].toString())
                    if (lastDirection == Direction.LEFT_UP) {
                        delete.content += diffs[0].content
                        insert.content += diffs[1].content
                        diffs.removeFirst()
                        diffs.removeFirst()
                    }

                    diffs.addFirst(insert)
                    diffs.addFirst(delete)
                    lastDirection = Direction.LEFT_UP
                }
                up -> {
                    row = shiftedRow

                    val delete = Delete(ori[row].toString())
                    if (lastDirection == Direction.UP) {
                        delete.content += diffs[0].content
                        diffs.removeFirst()
                    }

                    diffs.addFirst(delete)
                    lastDirection = Direction.UP
                }
                left -> {
                    col = shiftedCol

                    val insert = Insert(dst[col].toString())
                    if (lastDirection == Direction.LEFT) {
                        insert.content += diffs[0].content
                        diffs.removeFirst()
                    }

                    diffs.addFirst(insert)
                    lastDirection = Direction.LEFT
                }
                else -> throw IllegalStateException()
            }
        }
    }

    return diffs
}

// Merge same difference
private fun merge(diffs: LinkedList<Difference>) {
    if (diffs.size > 2) {
        var prevDiff = diffs[0]
        var thisPointer = 1

        do {
            while (diffs[thisPointer]::class == prevDiff::class) {
                prevDiff.content += diffs[thisPointer].content
                diffs.removeAt(thisPointer)
            }
            prevDiff = diffs[thisPointer]
            thisPointer++
        } while (thisPointer < diffs.size)
    }
}

/**
 * @see <a href="https://neil.fraser.name/writing/diff/">Diff Strategies</a>
 */
// Look for single edits surrounded on both sides by equalities
// which can be shifted sideways to align the edit to a word boundary.
// e.g: The c<ins>at c</ins>ame. -> The <ins>cat </ins>came.
// Look for single edits surrounded on both sides by equalities
// which can be shifted sideways to align the edit to a word boundary.
// e.g: The c<ins>at c</ins>ame. -> The <ins>cat </ins>came.
private fun cleanupSemanticLossless(diffs: LinkedList<Difference>) {
    var equality1: String
    var edit: String
    var equality2: String
    var commonString: String
    var commonOffset: Int
    var score: Int
    var bestScore: Int
    var bestEquality1: String
    var bestEdit: String
    var bestEquality2: String
    // Create a new iterator at the start.
    // Create a new iterator at the start.
    val pointer: MutableListIterator<Difference> = diffs.listIterator()
    var prevDiff = if (pointer.hasNext()) pointer.next() else null
    var thisDiff = if (pointer.hasNext()) pointer.next() else null
    var nextDiff = if (pointer.hasNext()) pointer.next() else null
    // Intentionally ignore the first and last element (don't need checking).
    // Intentionally ignore the first and last element (don't need checking).
    while (nextDiff != null) {
        if (prevDiff!! is Equal && nextDiff is Equal) {
            // This is a single edit surrounded by equalities.
            equality1 = prevDiff.content
            edit = thisDiff!!.content
            equality2 = nextDiff.content

            // First, shift the edit as far left as possible.
            commonOffset = commonSuffix(equality1, edit)
            if (commonOffset != 0) {
                commonString = edit.substring(edit.length - commonOffset)
                equality1 = equality1.substring(0, equality1.length - commonOffset)
                edit = commonString + edit.substring(0, edit.length - commonOffset)
                equality2 = commonString + equality2
            }

            // Second, step character by character right, looking for the best fit.
            bestEquality1 = equality1
            bestEdit = edit
            bestEquality2 = equality2
            bestScore = cleanupSemanticScore(equality1, edit) + cleanupSemanticScore(edit, equality2)
            while (edit.isNotEmpty() && equality2.isNotEmpty() && edit[0] == equality2[0]) {
                equality1 += edit[0]
                edit = edit.substring(1) + equality2[0]
                equality2 = equality2.substring(1)
                score = cleanupSemanticScore(equality1, edit) + cleanupSemanticScore(edit, equality2)
                // The >= encourages trailing rather than leading whitespace on edits.
                if (score >= bestScore) {
                    bestScore = score
                    bestEquality1 = equality1
                    bestEdit = edit
                    bestEquality2 = equality2
                }
            }
            if (prevDiff.content != bestEquality1) {
                // We have an improvement, save it back to the diff.
                if (bestEquality1.isNotEmpty()) {
                    prevDiff.content = bestEquality1
                } else {
                    pointer.previous() // Walk past nextDiff.
                    pointer.previous() // Walk past thisDiff.
                    pointer.previous() // Walk past prevDiff.
                    pointer.remove() // Delete prevDiff.
                    pointer.next() // Walk past thisDiff.
                    pointer.next() // Walk past nextDiff.
                }
                thisDiff.content = bestEdit
                if (bestEquality2.isNotEmpty()) {
                    nextDiff.content = bestEquality2
                } else {
                    pointer.remove() // Delete nextDiff.
                    nextDiff = thisDiff
                    thisDiff = prevDiff
                }
            }
        }
        prevDiff = thisDiff
        thisDiff = nextDiff
        nextDiff = if (pointer.hasNext()) pointer.next() else null
    }
}
private fun cleanupSemanticScore(one: String, two: String): Int {
    // Given two strings, compute a score representing whether the internal
    // boundary falls on logical boundaries
    // Scores range from 5 (best) to 0 (worst)

    // Edges are the best
    if (one.isEmpty() || two.isEmpty()) return 5

    // Each port of this function behaves slightly differently due to
    // subtle differences in each language's definition of things like
    // 'whitespace'. Since this function's purpose is largely cosmetic,
    // the choice has been made to use each language's native features
    // rather than force total conformity
    var score = 0
    // One point for non-alphanumeric.
    if (!Character.isLetterOrDigit(one[one.length - 1]) || !Character.isLetterOrDigit(two[0])) {
        score++
        // Two points for whitespace.
        if (Character.isWhitespace(one[one.length - 1]) || Character.isWhitespace(two[0])) {
            score++
            // Three points for line breaks.
            if (Character.getType(one[one.length - 1]) == Character.CONTROL.toInt() || Character.getType(two[0]) == Character.CONTROL.toInt()) {
                score++
                // Four points for blank lines.
                if (BLANK_LINE_END.matcher(one).find() || BLANK_LINE_START.matcher(two).find()) {
                    score++
                }
            }
        }
    }
    return score
}
private val BLANK_LINE_END: Pattern = Pattern.compile("\\n\\r?\\n\\Z", Pattern.DOTALL)
private val BLANK_LINE_START: Pattern = Pattern.compile("\\A\\r?\\n\\r?\\n", Pattern.DOTALL)