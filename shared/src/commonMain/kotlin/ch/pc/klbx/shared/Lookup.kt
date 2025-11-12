package ch.pc.klbx.shared

data class Lookup<T>(
    val id: String,
    override val name: String
) : Displayable