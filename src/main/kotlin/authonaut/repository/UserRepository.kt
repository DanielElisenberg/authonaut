package authonaut.repository

import authonaut.models.User
import jakarta.inject.Singleton
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Singleton
class UserRepository {
    fun getAllUsers(): List<User> {
        return transaction {
            Users.selectAll().map {
                User(
                        id = it[Users.id],
                        username = it[Users.username],
                        role = it[Users.role],
                        needsPasswordUpdate = it[Users.needsPasswordUpdate],
                        hasTwoFactorAuth = it[Users.twoFactorSecret] != null
                )
            }
        }
    }

    fun getUser(id: Int): User {
        return transaction {
            Users.selectAll()
                    .where { Users.id eq id }
                    .map {
                        User(
                                id = it[Users.id],
                                username = it[Users.username],
                                role = it[Users.role],
                                needsPasswordUpdate = it[Users.needsPasswordUpdate],
                                hasTwoFactorAuth = it[Users.twoFactorSecret] != null
                        )
                    }
                    .first()
        }
    }
    fun getUserByUsername(username: String): User {
        return transaction {
            Users.selectAll()
                    .where { Users.username eq username }
                    .map {
                        User(
                                id = it[Users.id],
                                username = it[Users.username],
                                role = it[Users.role],
                                needsPasswordUpdate = it[Users.needsPasswordUpdate],
                                hasTwoFactorAuth = it[Users.twoFactorSecret] != null
                        )
                    }
                    .first()
        }
    }

    fun getUserId(username: String): Int {
        return transaction {
            Users.selectAll().where { Users.username eq username }.map { it[Users.id] }.first()
        }
    }
    fun getRole(username: String): String {
        return transaction {
            Users.selectAll().where { Users.username eq username }.map { it[Users.role] }.first()
        }
    }

    fun get2FactorSecret(username: String): String? {
        return transaction {
            Users.selectAll()
                    .where { Users.username eq username }
                    .map { it[Users.twoFactorSecret] }
                    .firstOrNull()
        }
    }

    fun getAdminCount(): Int {
        return transaction { Users.selectAll().where { Users.role eq "admin" }.count() }.toInt()
    }

    fun insertUser(
            username: String,
            passwordHash: String,
            role: String,
            needsPasswordUpdate: Boolean
    ): User {
        transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.passwordHash] = passwordHash
                it[Users.role] = role
                it[Users.needsPasswordUpdate] = needsPasswordUpdate
            }
        }
        return transaction {
            Users.selectAll()
                    .where { Users.username eq username }
                    .map {
                        User(
                                id = it[Users.id],
                                username = it[Users.username],
                                role = it[Users.role],
                                needsPasswordUpdate = it[Users.needsPasswordUpdate],
                                hasTwoFactorAuth = it[Users.twoFactorSecret] != null
                        )
                    }
                    .first()
        }
    }

    fun updateUsername(userId: Int, newUsername: String) {
        transaction { Users.update({ Users.id eq userId }) { it[Users.username] = newUsername } }
    }

    fun deleteUser(id: Int) {
        transaction { Users.deleteWhere { Users.id eq id } }
    }

    fun updateUserRole(userId: Int, newRole: String) {
        transaction { Users.update({ Users.id eq userId }) { it[Users.role] = newRole } }
    }

    fun reset2fa(userId: Int) {
        transaction { Users.update({ Users.id eq userId }) { it[Users.twoFactorSecret] = null } }
    }

    fun updateUserPassword(userId: Int, newPasswordHash: String) {
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.passwordHash] = newPasswordHash
                it[Users.needsPasswordUpdate] = false
            }
        }
    }

    fun updateTwoFactorAuth(username: String, twoFactorSecret: String) {
        transaction {
            Users.update({ Users.username eq username }) {
                it[Users.twoFactorSecret] = twoFactorSecret
            }
        }
    }

    fun getHashedPassword(username: String): String {
        return transaction {
            Users.selectAll()
                    .where { Users.username eq username }
                    .map { it[Users.passwordHash] }
                    .first()
        }
    }
}
