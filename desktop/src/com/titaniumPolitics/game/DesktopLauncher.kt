package com.titaniumPolitics.game

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
object DesktopLauncher
{
    @JvmStatic
    fun main(arg: Array<String>)
    {
        val config = Lwjgl3ApplicationConfiguration()
        config.setForegroundFPS(60)
        config.setWindowedMode(1500, 800)
        config.setTitle("titaniumPolitics")
        Lwjgl3Application(com.titaniumPolitics.game.EntryClass(), config)
    }
}
