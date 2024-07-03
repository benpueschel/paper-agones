package net.kubecloud

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import dev.cubxity.libs.agones.AgonesSDK
import kotlinx.coroutines.runBlocking
import java.io.File

class PaperAgones : JavaPlugin(), Listener {

	val service = LifecycleService()

	override fun onEnable() {
		service.start()
	}

	override fun onDisable() {
		service.shutdown()
	}

	@EventHandler
	fun onPlayerJoin(event: PlayerJoinEvent) {
		service.connectPlayer(event.player.uniqueId)
	}

	@EventHandler
	fun onPlayerQuit(event: PlayerQuitEvent) {
		service.disconnectPlayer(event.player.uniqueId)
	}

}
