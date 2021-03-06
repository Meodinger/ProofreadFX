package ink.meodinger.prfx

import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.options.Options

import javafx.application.Application
import kotlin.system.exitProcess

/**
 * Author: Meodinger
 * Date: 2021/10/3
 * Location: info.meodinger.prfx
 */

/**
 * Launcher
 */
fun main() {
    Options.init(".prfx")

    Logger.start()
    Application.launch(ProofreadFX::class.java)
    Logger.stop()

    exitProcess(0)
}