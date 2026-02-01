package service

import model.User
import model.UserId
import repository.UserRepository
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class UserCache(
    private val userRepository: UserRepository,
    private val ttl: kotlin.time.Duration = 5.minutes
) {
    private data class CachedUser(
        val user: User,
        val cachedAt: Instant
    )

    private val cache = ConcurrentHashMap<UserId, CachedUser>()

    suspend fun getActiveUser(id: UserId): User? {
        val now = Clock.System.now()
        val cached = cache[id]

        if (cached != null && (now - cached.cachedAt) < ttl) {
            return cached.user.takeIf { it.isActive }
        }

        val user = userRepository.findById(id)
        if (user != null) {
            cache[id] = CachedUser(user, now)
        }
        else {
            cache.remove(id)
        }

        return user?.takeIf { it.isActive }
    }
}
