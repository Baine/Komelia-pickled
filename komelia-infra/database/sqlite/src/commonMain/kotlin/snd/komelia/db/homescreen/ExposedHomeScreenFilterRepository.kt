package snd.komelia.db.homescreen

import kotlinx.serialization.SerializationException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import snd.komelia.db.tables.HomeScreenFiltersTable
import snd.komelia.homefilters.HomeScreenFilter

class ExposedHomeScreenFilterRepository(
    private val database: Database
) {

    suspend fun getFilters(): List<HomeScreenFilter>? {
        return try {
            transaction(database) {
                HomeScreenFiltersTable.selectAll()
                    .firstOrNull()?.get(HomeScreenFiltersTable.filters)
                    ?.sortedBy { it.order }
            }
        } catch (_: SerializationException) {
            null
        }
    }

    suspend fun putFilters(filters: List<HomeScreenFilter>) {
        transaction(database) {
            HomeScreenFiltersTable.upsert {
                it[this.version] = 1
                it[this.filters] = filters
            }
        }
    }
}