package org.rsmod.api.db

public data class DatabaseConfig(
    val scheme: String,
    val path: String,
    val user: String?,
    val password: String?,
) {
    public val url: String
        get() = "$scheme$path"

    public companion object {
        public fun createSqlite(): DatabaseConfig {
            // Allow a per-world database file via env (e.g. world 2 -> game_w2.db) so multiple
            // worlds can run side-by-side without sharing one SQLite file. Default unchanged.
            val path = System.getenv("RSMOD_DB") ?: ".data/saves/game.db"
            return DatabaseConfig("jdbc:sqlite:", path, null, null)
        }
    }
}
