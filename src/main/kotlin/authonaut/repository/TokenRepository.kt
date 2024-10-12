package authonaut.repository

import jakarta.inject.Singleton
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction

@Singleton
class TokenRepository {

    fun addToken(shortUUID: String, jwtToken: String, timestamp: Long) {
        transaction {
            Tokens.insert {
                it[Tokens.shortUUID] = shortUUID
                it[Tokens.jwtToken] = jwtToken
                it[Tokens.timestamp] = timestamp
            }
        }
    }

    fun getJWTByToken(shortUUID: String): String? = transaction {
        Tokens.selectAll()
                .where { Tokens.shortUUID eq shortUUID }
                .map { it[Tokens.jwtToken] }
                .singleOrNull()
    }

    fun deleteExpiredTokens(olderThan: Long) {
        transaction { Tokens.deleteWhere { Tokens.timestamp less olderThan } }
    }
}
