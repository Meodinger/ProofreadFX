package info.meodinger.prfx

import info.meodinger.lpfx.component.common.CTextSlider
import info.meodinger.lpfx.options.Logger
import info.meodinger.lpfx.options.Options

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

/**
 * Author: Meodinger
 * Date: 2021/10/3
 * Location: info.meodinger.prfx
 */
class ProofreadFX : Application() {

    init {
        Options.load()
    }

    override fun start(primaryStage: Stage) {
        primaryStage.scene = Scene(CTextSlider(),400.0, 400.0)
        primaryStage.show()
        Logger.info("App start", "Application")
    }
}