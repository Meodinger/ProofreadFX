package info.meodinger.prfx.diff

import org.junit.Test

/**
 * Author: Meodinger
 * Date: 2021/10/25
 * Have fun with my code!
 */

class CommonKtTest {

    companion object {
        const val text1 = "The cat in the hat"
        const val text2 = "The dog in the hat"
        const val text3 = "The furry cat in the hat"
        const val text4 = "The fat cat in the black hat"
        const val text5 = "The big cat in the yellow hat"

        const val textTrans = "为了让你集中精力上课…"
        const val textProof = "我提醒你一下要集中精力上课…"
    }

    @Test
    fun diff() {
        println(diff(text1, text2))

        println(diff(text1, text3))
        println(diff(text3, text1))

        println(diff(text1, text4))
        println(diff(text4, text1))

        println(diff(text4, text5))

        println(diff(textTrans, textProof))
    }
}