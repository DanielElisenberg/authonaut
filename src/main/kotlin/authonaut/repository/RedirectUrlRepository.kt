package authonaut.repository

import authonaut.models.RedirectUrl
import jakarta.inject.Singleton
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Singleton
class RedirectUrlRepository {

    fun getAll(): List<RedirectUrl> = transaction {
        RedirectUrls.selectAll().map {
            RedirectUrl(it[RedirectUrls.id], it[RedirectUrls.url], it[RedirectUrls.isValid])
        }
    }

    fun getRedirectUrlId(url: String): Int = transaction {
        RedirectUrls.selectAll()
                .where { RedirectUrls.url eq url }
                .map { it[RedirectUrls.id] }
                .first()
    }

    fun add(url: String, isValid: Boolean): RedirectUrl {
        transaction {
            val exists = RedirectUrls.selectAll().where { RedirectUrls.url eq url }.count() > 0
            if (!exists) {
                RedirectUrls.insert {
                    it[RedirectUrls.url] = url
                    it[RedirectUrls.isValid] = isValid
                }
            }
        }
        return transaction {
            RedirectUrls.selectAll()
                    .where { RedirectUrls.url eq url }
                    .map {
                        RedirectUrl(
                                it[RedirectUrls.id],
                                it[RedirectUrls.url],
                                it[RedirectUrls.isValid]
                        )
                    }
                    .first()
        }
    }

    fun update(url: String, isValid: Boolean) = transaction {
        RedirectUrls.update({ RedirectUrls.url eq url }) { it[RedirectUrls.isValid] = isValid }
    }

    fun delete(id: Int) = transaction { RedirectUrls.deleteWhere { RedirectUrls.id eq id } }

    fun verify(url: String): Boolean = transaction {
        RedirectUrls.selectAll()
                .where { RedirectUrls.url eq url and RedirectUrls.isValid }
                .count() > 0
    }
}
