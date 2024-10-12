package authonaut.repository

import authonaut.models.LoginLog
import jakarta.inject.Singleton
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

@Singleton
class LoginLogRepository {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun longToISO(timestamp: Long): String {
        val localDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        return localDateTime.format(formatter)
    }

    fun getAll(page: Int, pageSize: Int): List<LoginLog> = transaction {
        val offset = (page - 1) * pageSize
        (LoginLogs innerJoin Users leftJoin RedirectUrls)
                .selectAll()
                .orderBy(LoginLogs.timestamp, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map {
                    LoginLog(
                            id = it[LoginLogs.id],
                            username = it[Users.username],
                            redirectUrl = it.getOrNull(RedirectUrls.url) ?: "",
                            timestamp = longToISO(it[LoginLogs.timestamp]),
                            successful = it[LoginLogs.successful]
                    )
                }
    }

    fun getForUser(userid: Int, pageSize: Int): List<LoginLog> = transaction {
        val username = Users.selectAll().where { Users.id eq userid }.first()[Users.username]
        (LoginLogs leftJoin RedirectUrls)
                .selectAll()
                .where { LoginLogs.user eq userid }
                .limit(pageSize)
                .map {
                    LoginLog(
                            id = it[LoginLogs.id],
                            username = username,
                            redirectUrl = it.getOrNull(RedirectUrls.url) ?: "",
                            timestamp = longToISO(it[LoginLogs.timestamp]),
                            successful = it[LoginLogs.successful]
                    )
                }
    }

    fun getAmountofPages(pageSize: Int): Long = transaction {
        val amount = LoginLogs.selectAll().count()
        (amount / pageSize) + 1
    }

    fun add(userid: Int, redirectId: Int?, timestamp: Long, successful: Boolean) {
        transaction {
            LoginLogs.insert {
                it[LoginLogs.user] = userid
                it[LoginLogs.redirect] = redirectId
                it[LoginLogs.timestamp] = timestamp
                it[LoginLogs.successful] = successful
            }
        }
    }
}
