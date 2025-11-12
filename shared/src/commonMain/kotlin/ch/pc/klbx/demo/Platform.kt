package ch.pc.klbx.demo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform