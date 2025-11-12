package ch.pc.klbx.shared

data class User(
    val username: String,
    val id: String,
    val firstName: String,
    val middleName: String,
    val lastName: String,
) : Displayable {
    override val name: String
        get() = "$firstName $middleName $lastName"
}