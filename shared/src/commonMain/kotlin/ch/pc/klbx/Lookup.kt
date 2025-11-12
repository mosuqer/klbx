package ch.pc.klbx

interface Displayable {
    val name: String
}

data class Lookup<T>(
    val id: String,
    override val name: String
) : Displayable