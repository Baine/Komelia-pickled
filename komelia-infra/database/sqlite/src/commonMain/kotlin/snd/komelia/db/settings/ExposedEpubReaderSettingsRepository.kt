package snd.komelia.db.settings

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import snd.komelia.db.EpubReaderSettings
import snd.komelia.db.defaultBookId
import snd.komelia.db.tables.EpubReaderSettingsTable
import snd.komelia.settings.model.EpubReaderType

class ExposedEpubReaderSettingsRepository(private val database: Database) {

    suspend fun get(): EpubReaderSettings? {
        return transaction(database) {
            EpubReaderSettingsTable.selectAll()
                .where { EpubReaderSettingsTable.bookId.eq(defaultBookId) }
                .firstOrNull()
                ?.let {
                    EpubReaderSettings(
                        readerType = EpubReaderType.valueOf(it[EpubReaderSettingsTable.readerType]),
                        komgaReaderSettings = it[EpubReaderSettingsTable.komgaSettingsJson],
                        ttsuReaderSettings = it[EpubReaderSettingsTable.ttsuSettingsJson]
                    )
                }
        }
    }

    suspend fun save(settings: EpubReaderSettings) {
        transaction(database) {
            EpubReaderSettingsTable.upsert {
                it[bookId] = defaultBookId
                it[readerType] = settings.readerType.name
                it[komgaSettingsJson] = settings.komgaReaderSettings
                it[ttsuSettingsJson] = settings.ttsuReaderSettings
            }
        }
    }
}