package ch.pc.klbx.client

import kotlinx.coroutines.delay


// A starter that is only used for testing the KlbxClient class.
// all it does is login and call echo every 2 seconds.
// with specific timing this tests the bearer token handling as well as interception with refresh token


suspend fun main(args: Array<String>) {
    val client = KlbxClient("localhost", 8079)
    client.login("admin", "admin")
    val body = "hello world"
    while(true) {
        val echoedMessage = client.echo(body)
        echoedMessage.onSuccess { println("echoed: $it") }
        if(echoedMessage.status.value >= 300) println("ERROR: response body: "+ echoedMessage.errorMessage)
        delay(2000)
    }
}