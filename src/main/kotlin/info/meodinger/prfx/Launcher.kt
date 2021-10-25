package info.meodinger.prfx

import info.meodinger.lpfx.options.Logger
import info.meodinger.lpfx.options.Options

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