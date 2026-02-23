package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music

class SoundEngine {
    companion object {
        var music: Music? = null
        private var soundEnabled = true

        fun isSoundEnabled(): Boolean {
            return soundEnabled
        }

        fun setSoundEnabled(enabled: Boolean) {
            soundEnabled = enabled
        }

        fun playSound(soundName: String, volume: Float = 1.0f) {
            if (soundEnabled) {
                val sound =
                    Gdx.audio.newSound(Gdx.files.internal("data/sounds/$soundName"))
                sound.play(volume)
            }
        }

        fun playMusic(musicName: String) {
            if (music?.isPlaying ?: false)
                music!!.stop() // Stop any currently playing music before starting new one
            if (soundEnabled) {
                try {
                    music = Gdx.audio.newMusic(Gdx.files.internal("data/music/$musicName"))
                    music!!.isLooping = true
                    music!!.play()
                } catch (e: Exception) {
                    println("Error playing music: $e")
                }
            }
        }

        fun stopMusic() {
            if (soundEnabled) {
                music?.stop()
            }
        }
    }
}
