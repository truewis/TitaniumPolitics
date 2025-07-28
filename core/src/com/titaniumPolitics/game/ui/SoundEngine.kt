package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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