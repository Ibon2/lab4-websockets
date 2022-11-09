@file:Suppress("NoWildcardImports")
package websockets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.util.concurrent.CountDownLatch
import javax.websocket.*

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ElizaServerTest {
    private val possibleResponses =
            arrayListOf(
                    "Tell me more about such feelings.",
                    "Do you often feel sad?",
                    "Do you enjoy feeling sad?",
                    "Why do you feel that way?"
    )
    private lateinit var container: WebSocketContainer

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setup() {
        container = ContainerProvider.getWebSocketContainer()
    }

    @Test
    fun onOpen() {
        val latch = CountDownLatch(3)
        val list = mutableListOf<String>()

        val client = ElizaOnOpenMessageHandler(list, latch)
        container.connectToServer(client, URI("ws://localhost:$port/eliza"))
        latch.await()
        assertEquals(3, list.size)
        assertEquals("The doctor is in.", list[0])
    }

    @Test
    fun onChat() {
        val latch = CountDownLatch(4)
        val list = mutableListOf<String>()

        val client = ElizaOnOpenMessageHandlerToComplete(list, latch)
        container.connectToServer(client, URI("ws://localhost:$port/eliza"))
        latch.await()
        assertEquals(4, list.size)
        assertEquals(possibleResponses[possibleResponses.indexOf(list[3])], list[3])
    }
}

@ClientEndpoint
class ElizaOnOpenMessageHandler(private val list: MutableList<String>, private val latch: CountDownLatch) {
    @OnMessage
    fun onMessage(message: String) {
        list.add(message)
        latch.countDown()
    }
}

@ClientEndpoint
class ElizaOnOpenMessageHandlerToComplete(private val list: MutableList<String>, private val latch: CountDownLatch) {

    @OnMessage
    fun onMessage(message: String, session: Session) {
        list.add(message)
        latch.countDown()

        if (message.equals("What's on your mind?")) {
            synchronized(session) {
                with(session.basicRemote) {
                    sendText("i feel sad")
                }
            }
        }
    }
}
