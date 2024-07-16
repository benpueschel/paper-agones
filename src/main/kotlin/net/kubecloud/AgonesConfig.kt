@file:JvmName("AgonesConfig")
package net.kubecloud

import java.io.File
import org.bukkit.configuration.file.YamlConfiguration

class AgonesConfig {
    val config: YamlConfiguration

    public val enabled: Boolean
    public val features: AgonesFeatures

    constructor(file: File) {
        if (!file.exists()) {
            config = defaultConfig()
            config.save(file)
        } else {
            config = YamlConfiguration.loadConfiguration(file)
        }

        enabled = config.getBoolean("enabled")
        features = AgonesFeatures(config)
    }

    fun defaultConfig(): YamlConfiguration {
        val config = YamlConfiguration()
        config.set("enabled", true)
        config.set(
                "features",
                hashMapOf(
                        "readyOnStart" to true,
                        "playerCounter" to true,
                )
        )
        return config
    }
}

class AgonesFeatures {
    val readyOnStart: Boolean
    val playerCounter: Boolean

    constructor(config: YamlConfiguration) {
        readyOnStart = config.getBoolean("features.readyOnStart")
        playerCounter = config.getBoolean("features.playerCounter")
    }
}
