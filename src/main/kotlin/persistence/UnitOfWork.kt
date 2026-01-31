package persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class UnitOfWork {
    suspend fun <T> transaction(block: suspend () -> T): T {
        return suspendTransaction { withContext(Dispatchers.IO) { block() } }
    }
}
