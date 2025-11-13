package ch.pc.klbx

enum class ApplicationRoles {
    ADMIN,
    USER
}

fun List<ApplicationRoles>.encode(): List<String> = map { it.name }
