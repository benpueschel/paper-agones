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
	private var mutex = Mutex()

	private var running = true
	private var ready = false

	private var connectedPlayers = mutableSetOf<String>()
	private var disconnectedPlayers = mutableSetOf<String>()

    public constructor() {
        agones = AgonesSDK()
        thread = Thread(this::run)
    }

	fun start() {
		thread.start()
	}

    fun run() = runBlocking {
		var previousReady = false
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
				for (player in connectedPlayers) {
					agones.alpha.playerConnect(player)
				}
				for (player in disconnectedPlayers) {
					agones.alpha.playerDisconnect(player)
				}
				mutex.unlock()
				delay(100)
			}
		}
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
