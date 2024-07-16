@file:JvmName("PaperAgones")

package net.kubecloud

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.EventPriority
import dev.cubxity.libs.agones.AgonesSDK
import kotlinx.coroutines.runBlocking
import java.io.File

class PaperAgones : JavaPlugin, Listener {

	val config : AgonesConfig
	val service: LifecycleService

	constructor() : super() {
		val dataFolder = this.dataFolder
		if (!dataFolder.exists()) {
			dataFolder.mkdirs()
		}
		val configFile = File(dataFolder, "config.yml")
		config = AgonesConfig(configFile)
		service = LifecycleService(config)
	}

	override fun onLoad() {
		if (!config.enabled) {
			logger.info("Agones is not enabled. Disabling plugin...")
			server.pluginManager.disablePlugin(this)
			return
		}
		service.start()
		logger.info("Connected to Agones SDK")
	}

	override fun onEnable() {
		server.pluginManager.registerEvents(this, this)
		if (config.features.readyOnStart) {
			service.ready()
			logger.info("Agones Server ready")
		}
		if (config.features.playerCounter) {
			val players = server.maxPlayers.toLong()
			service.setCapacity(players)

			// Connect all online players
			for (player in server.onlinePlayers) {
				service.connectPlayer(player.uniqueId)
			}
		}
	}

	override fun onDisable() {
		service.shutdown()
		if (config.features.playerCounter) {
			// Disconnect all online players
			for (player in server.onlinePlayers) {
				service.disconnectPlayer(player.uniqueId)
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	fun onPlayerJoin(event: PlayerJoinEvent) {
		if (config.enabled && config.features.playerCounter) {
			service.connectPlayer(event.player.uniqueId)
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	fun onPlayerQuit(event: PlayerQuitEvent) {
		if (config.enabled && config.features.playerCounter) {
			service.disconnectPlayer(event.player.uniqueId)
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	fun onPlayerKick(event: PlayerKickEvent) {
		if (config.enabled && config.features.playerCounter) {
			service.disconnectPlayer(event.player.uniqueId)
		}
	}

}
