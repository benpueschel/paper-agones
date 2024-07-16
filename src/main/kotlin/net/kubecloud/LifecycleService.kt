@file:JvmName("LifecycleService")
package net.kubecloud

import dev.cubxity.libs.agones.AgonesSDK
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

class LifecycleService {

    private val agones: AgonesSDK
    private val thread: Thread
	private val config: AgonesConfig
	private val mutex = Mutex()

	private var running = true
	private var ready = false
	private var capacity = -1L

	private var connectedPlayers = mutableSetOf<String>()
	private var disconnectedPlayers = mutableSetOf<String>()

    public constructor(config: AgonesConfig) {
		this.config = config
        this.agones = AgonesSDK()
        this.thread = Thread(this::run)
    }

	fun start() {
		thread.start()
	}

    fun run() = runBlocking {
		var previousReady = false
		var oldCapacity = -1L
		launch {
			while (running) {
				health()
				delay(1000)
			}
		}
		launch {
			while (running) {
				mutex.lock()
				if (ready && !previousReady) {
					agones.ready()
					previousReady = true
				}
				mutex.unlock()

				if (config.features.playerCounter) {
					mutex.lock()

					if (capacity != -1L && capacity != oldCapacity) {
						agones.alpha.setPlayerCapacity(capacity)
						oldCapacity = capacity
					}

					// Copy player lists to avoid locking for too long
					val connected = connectedPlayers.toSet()
					val disconnected = disconnectedPlayers.toSet()
					connectedPlayers.clear()
					disconnectedPlayers.clear()
					mutex.unlock()

					for (player in connected) {
						agones.alpha.playerConnect(player)
					}
					for (player in disconnected) {
						agones.alpha.playerDisconnect(player)
					}
				}
				delay(100)
			}
		}
    }

	public fun setCapacity(_capacity: Long) = runBlocking {
		mutex.lock()
		capacity = _capacity
		mutex.unlock()
	}

	public fun connectPlayer(player: UUID) = runBlocking {
		mutex.lock()
		val id = player.toString()
		if (!disconnectedPlayers.remove(id)) {
			// only add if not already in the disconnected list
			// this is to prevent the player from being added to both lists
			// in case they rapidly connect and disconnect (which shouldn't
			// really ever happen, but just in case)
			connectedPlayers += id
		}
		mutex.unlock()
	}

	public fun disconnectPlayer(player: UUID) = runBlocking {
		mutex.lock()
		val id = player.toString()
		if (!connectedPlayers.remove(id)) {
			// only add if not already in the connected list
			// this is to prevent the player from being added to both lists
			// in case they rapidly disconnect and reconnect (which shouldn't
			// really ever happen, but just in case)
			disconnectedPlayers += id
		}
		mutex.unlock()
	}

    public fun ready() = runBlocking {
		mutex.lock()
		ready = true
		mutex.unlock()
	}

    public fun shutdown() = runBlocking {
		mutex.lock()
		running = false
		mutex.unlock()

		thread.join()
		agones.close()
	}

	private suspend fun health() {
		agones.health(flow {
			emit(Unit)
		})
	}
}
