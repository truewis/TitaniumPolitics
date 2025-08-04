package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx

class SoundEngine {
    companion object {
        private var soundEnabled = true

        fun isSoundEnabled(): Boolean {
            return soundEnabled
        }

        fun setSoundEnabled(enabled: Boolean) {
            soundEnabled = enabled
        }

        fun playSound(soundName: String) {
            if (soundEnabled) {
                val sound =
                    Gdx.audio.newSound(Gdx.files.internal("data/sounds/$soundName"))
                sound.play()
            }
        }
    }
}