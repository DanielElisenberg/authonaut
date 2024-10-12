package authonaut.repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val needsPasswordUpdate = bool("needs_password_update").default(false)
    val role = varchar("role", 20).default("default")
    val twoFactorSecret = varchar("two_factor_secret", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}

object RedirectUrls : Table() {
    val id = integer("id").autoIncrement()
    val url = varchar("url", 255).uniqueIndex()
    val isValid = bool("is_valid").default(false)
    override val primaryKey = PrimaryKey(id)
}

object Tokens : Table() {
    val id = integer("id").autoIncrement()
    val shortUUID = varchar("short_uuid", 36)
    val jwtToken = text("jwt_token")
    val timestamp = long("timestamp")
    override val primaryKey = PrimaryKey(id)
}

object LoginLogs : Table() {
    val id = integer("id").autoIncrement()
    val user = reference("user", Users.id)
    val redirect = reference("redirect", RedirectUrls.id).nullable()
    val timestamp = long("timestamp")
    val successful = bool("successful")
    override val primaryKey = PrimaryKey(id)
}

fun initDatabase() {
    Database.connect("jdbc:sqlite:./auth.db", "org.sqlite.JDBC")
    transaction { SchemaUtils.create(Users, RedirectUrls, Tokens, LoginLogs) }
}
