package snd.komelia.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

// ponytail: top-level helper replaces abstract base class used by 21 repositories
@OptIn(InternalApi::class)
suspend fun <T> transaction(database: Database, statement: Transaction.() -> T): T {
    return withContext(Dispatchers.IO + NonCancellable) {
        suspendTransaction(db = database, statement = statement)
    }
}
