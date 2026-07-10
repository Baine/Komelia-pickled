package snd.komelia

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import snd.komelia.komga.api.model.KomeliaBook
import snd.komelia.offline.OfflineDependencies
import snd.komelia.offline.OfflineModule
import snd.komelia.offline.OfflineRepositories
import snd.komelia.offline.api.repository.OfflineBookDtoRepository
import snd.komelia.offline.api.repository.OfflineReferentialRepository
import snd.komelia.offline.api.repository.OfflineSeriesDtoRepository
import snd.komelia.offline.book.model.OfflineBook
import snd.komelia.offline.book.model.OfflineBookMetadata
import snd.komelia.offline.book.model.OfflineThumbnailBook
import snd.komelia.offline.book.repository.OfflineBookMetadataAggregationRepository
import snd.komelia.offline.book.repository.OfflineBookMetadataRepository
import snd.komelia.offline.book.repository.OfflineBookRepository
import snd.komelia.offline.book.repository.OfflineThumbnailBookRepository
import snd.komelia.offline.library.model.OfflineLibrary
import snd.komelia.offline.library.repository.OfflineLibraryRepository
import snd.komelia.offline.media.model.OfflineMedia
import snd.komelia.offline.media.repository.OfflineMediaRepository
import snd.komelia.offline.mediacontainer.DivinaExtractor
import snd.komelia.offline.mediacontainer.EpubExtractor
import snd.komelia.offline.readprogress.OfflineReadProgress
import snd.komelia.offline.readprogress.OfflineReadProgressRepository
import snd.komelia.offline.series.model.OfflineBookMetadataAggregation
import snd.komelia.offline.series.model.OfflineSeries
import snd.komelia.offline.series.model.OfflineSeriesMetadata
import snd.komelia.offline.series.model.OfflineThumbnailSeries
import snd.komelia.offline.series.repository.OfflineSeriesMetadataRepository
import snd.komelia.offline.series.repository.OfflineSeriesRepository
import snd.komelia.offline.series.repository.OfflineThumbnailSeriesRepository
import snd.komelia.offline.server.model.OfflineMediaServer
import snd.komelia.offline.server.model.OfflineMediaServerId
import snd.komelia.offline.server.repository.OfflineMediaServerRepository
import snd.komelia.offline.settings.OfflineSettingsRepository
import snd.komelia.offline.sync.BookDownloadService
import snd.komelia.offline.sync.PlatformDownloadManager
import snd.komelia.offline.sync.model.DownloadEvent
import snd.komelia.offline.sync.model.LogEntryId
import snd.komelia.offline.sync.model.OfflineLogEntry
import snd.komelia.offline.sync.repository.LogJournalRepository
import snd.komelia.offline.tasks.model.TaskEntry
import snd.komelia.offline.tasks.repository.OfflineTasksRepository
import snd.komelia.offline.user.model.OfflineUser
import snd.komelia.offline.user.repository.OfflineUserRepository
import snd.komelia.db.TransactionTemplate
import snd.komga.client.KomgaClientFactory
import snd.komga.client.book.KomgaBookId
import snd.komga.client.book.KomgaBookSearch
import snd.komga.client.collection.KomgaCollectionId
import snd.komga.client.common.KomgaAuthor
import snd.komga.client.common.KomgaPageRequest
import snd.komga.client.common.KomgaThumbnailId
import snd.komga.client.common.Page
import snd.komga.client.library.KomgaLibraryId
import snd.komga.client.readlist.KomgaReadListId
import snd.komga.client.series.KomgaSeries
import snd.komga.client.series.KomgaSeriesId
import snd.komga.client.series.KomgaSeriesSearch
import snd.komga.client.user.KomgaUserId
import snd.komga.client.user.KomgaUser
import kotlin.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class WasmOfflineBookRepository : OfflineBookRepository {
    override suspend fun save(book: OfflineBook) {}
    override suspend fun find(id: KomgaBookId): OfflineBook? = null
    override suspend fun exists(id: KomgaBookId): Boolean = false
    override suspend fun findIn(ids: Collection<KomgaBookId>): List<OfflineBook> = emptyList()
    override suspend fun findFirstIdInSeriesOrNull(seriesId: KomgaSeriesId): KomgaBookId? = null
    override suspend fun findLastIdInSeriesOrNull(seriesId: KomgaSeriesId): KomgaBookId? = null
    override suspend fun findFirstUnreadIdInSeriesOrNull(seriesId: KomgaSeriesId, userId: KomgaUserId): KomgaBookId? = null
    override suspend fun findAllBySeriesIds(seriesIds: List<KomgaSeriesId>): List<OfflineBook> = emptyList()
    override suspend fun findAllIdsBySeriesId(seriesId: KomgaSeriesId): List<KomgaBookId> = emptyList()
    override suspend fun findAllIdsByLibraryId(libraryId: KomgaLibraryId): List<KomgaBookId> = emptyList()
    override suspend fun get(id: KomgaBookId): OfflineBook = error("offline not available")
    override suspend fun findAll(id: KomgaSeriesId): List<OfflineBook> = emptyList()
    override suspend fun findAllNotDeleted(id: KomgaSeriesId): List<OfflineBook> = emptyList()
    override suspend fun delete(id: KomgaBookId) {}
    override suspend fun delete(ids: Collection<KomgaBookId>) {}
}

private class WasmMediaRepository : OfflineMediaRepository {
    override suspend fun save(media: OfflineMedia) {}
    override suspend fun find(id: KomgaBookId): OfflineMedia? = null
    override suspend fun findAll(ids: List<KomgaBookId>): List<OfflineMedia> = emptyList()
    override suspend fun get(id: KomgaBookId): OfflineMedia = error("offline not available")
    override suspend fun delete(id: KomgaBookId) {}
    override suspend fun delete(bookIds: List<KomgaBookId>) {}
}

private class WasmLibraryRepository : OfflineLibraryRepository {
    override suspend fun save(library: OfflineLibrary) {}
    override suspend fun get(id: KomgaLibraryId): OfflineLibrary = error("offline not available")
    override suspend fun find(id: KomgaLibraryId): OfflineLibrary? = null
    override suspend fun findAll(): List<OfflineLibrary> = emptyList()
    override suspend fun findAllByMediaServer(mediaServerId: OfflineMediaServerId): List<OfflineLibrary> = emptyList()
    override suspend fun delete(id: KomgaLibraryId) {}
}

private class WasmBookMetadataRepository : OfflineBookMetadataRepository {
    override suspend fun save(metadata: OfflineBookMetadata) {}
    override suspend fun find(id: KomgaBookId): OfflineBookMetadata? = null
    override suspend fun findAllByIds(bookIds: List<KomgaBookId>): List<OfflineBookMetadata> = emptyList()
    override suspend fun get(id: KomgaBookId): OfflineBookMetadata = error("offline not available")
    override suspend fun delete(id: KomgaBookId) {}
    override suspend fun delete(bookIds: List<KomgaBookId>) {}
}

private class WasmBookMetadataAggregationRepository : OfflineBookMetadataAggregationRepository {
    override suspend fun save(metadata: OfflineBookMetadataAggregation) {}
    override suspend fun find(seriesId: KomgaSeriesId): OfflineBookMetadataAggregation? = null
    override suspend fun get(seriesId: KomgaSeriesId): OfflineBookMetadataAggregation = error("offline not available")
    override suspend fun delete(seriesId: KomgaSeriesId) {}
    override suspend fun delete(seriesIds: List<KomgaSeriesId>) {}
}

private class WasmSeriesRepository : OfflineSeriesRepository {
    override suspend fun save(series: OfflineSeries) {}
    override suspend fun get(id: KomgaSeriesId): OfflineSeries = error("offline not available")
    override suspend fun find(id: KomgaSeriesId): OfflineSeries? = null
    override suspend fun findAllByLibraryId(libraryId: KomgaLibraryId): List<OfflineSeries> = emptyList()
    override suspend fun delete(id: KomgaSeriesId) {}
    override suspend fun delete(seriesids: List<KomgaSeriesId>) {}
}

private class WasmSeriesMetadataRepository : OfflineSeriesMetadataRepository {
    override suspend fun save(metadata: OfflineSeriesMetadata) {}
    override suspend fun find(id: KomgaSeriesId): OfflineSeriesMetadata? = null
    override suspend fun delete(id: KomgaSeriesId) {}
    override suspend fun delete(seriesIds: List<KomgaSeriesId>) {}
}

private class WasmThumbnailBookRepository : OfflineThumbnailBookRepository {
    override suspend fun save(thumbnail: OfflineThumbnailBook) {}
    override suspend fun find(id: KomgaThumbnailId): OfflineThumbnailBook? = null
    override suspend fun findSelectedByBookId(bookId: KomgaBookId): OfflineThumbnailBook? = null
    override suspend fun findAllByBookId(bookId: KomgaBookId): List<OfflineThumbnailBook> = emptyList()
    override suspend fun findAllByBookIdAndType(bookId: KomgaBookId, type: Collection<OfflineThumbnailBook.Type>): List<OfflineThumbnailBook> = emptyList()
    override suspend fun markSelected(thumbnail: OfflineThumbnailBook) {}
    override suspend fun delete(id: KomgaThumbnailId) {}
    override suspend fun deleteByBookIdAndType(id: KomgaBookId, type: OfflineThumbnailBook.Type) {}
    override suspend fun deleteAllBy(id: KomgaBookId) {}
    override suspend fun deleteByBookIds(bookIds: Collection<KomgaBookId>) {}
}

private class WasmThumbnailSeriesRepository : OfflineThumbnailSeriesRepository {
    override suspend fun save(thumbnail: OfflineThumbnailSeries) {}
    override suspend fun find(thumbnailId: KomgaThumbnailId): OfflineThumbnailSeries? = null
    override suspend fun findSelectedBySeriesId(seriesId: KomgaSeriesId): OfflineThumbnailSeries? = null
    override suspend fun findAllBySeriesId(seriesId: KomgaSeriesId): Collection<OfflineThumbnailSeries> = emptyList()
    override suspend fun findAllBySeriesIdAndType(seriesId: KomgaSeriesId, type: OfflineThumbnailSeries.Type): List<OfflineThumbnailSeries> = emptyList()
    override suspend fun markSelected(thumbnail: OfflineThumbnailSeries) {}
    override suspend fun delete(thumbnailSeriesId: KomgaThumbnailId) {}
    override suspend fun deleteBySeriesId(seriesId: KomgaSeriesId) {}
    override suspend fun deleteBySeriesIds(seriesIds: List<KomgaSeriesId>) {}
}

private class WasmUserRepository : OfflineUserRepository {
    override suspend fun save(user: OfflineUser) {}
    override suspend fun get(id: KomgaUserId): OfflineUser = error("offline not available")
    override suspend fun find(id: KomgaUserId): OfflineUser? = error("offline not available")
    override suspend fun findAll(): List<OfflineUser> = error("offline not available")
    override suspend fun findAllByServer(serverId: OfflineMediaServerId): List<OfflineUser> = error("offline not available")
    override suspend fun delete(id: KomgaUserId) {}
}

private class WasmReadProgressRepository : OfflineReadProgressRepository {
    override suspend fun save(readProgress: OfflineReadProgress) {}
    override suspend fun saveAll(readProgress: List<OfflineReadProgress>) {}
    override suspend fun find(bookId: KomgaBookId, userId: KomgaUserId): OfflineReadProgress? = null
    override suspend fun findAllByBookIdsAndUserId(bookIds: List<KomgaBookId>, userId: KomgaUserId): List<OfflineReadProgress> = emptyList()
    override suspend fun findAllModifiedAfter(timestamp: Instant, userId: KomgaUserId, serverId: OfflineMediaServerId): List<OfflineReadProgress> = emptyList()
    override suspend fun findAllByServer(userId: KomgaUserId, serverId: OfflineMediaServerId): List<OfflineReadProgress> = emptyList()
    override suspend fun deleteByUserId(userId: KomgaUserId) {}
    override suspend fun deleteByBookIdsAndUserId(bookIds: List<KomgaBookId>, userId: KomgaUserId) {}
    override suspend fun deleteBySeriesIds(seriesIds: List<KomgaSeriesId>) {}
    override suspend fun deleteByBookIds(bookIds: List<KomgaBookId>) {}
    override suspend fun delete(bookId: KomgaBookId, userId: KomgaUserId) {}
    override suspend fun deleteAllBy(bookId: KomgaBookId) {}
}

private class WasmMediaServerRepository : OfflineMediaServerRepository {
    override suspend fun save(server: OfflineMediaServer) {}
    override suspend fun get(id: OfflineMediaServerId): OfflineMediaServer = error("offline not available")
    override suspend fun find(id: OfflineMediaServerId): OfflineMediaServer? = null
    override suspend fun findAll(): List<OfflineMediaServer> = emptyList()
    override suspend fun findByUrl(url: String): OfflineMediaServer? = error("offline not available")
    override suspend fun findByUserId(userId: KomgaUserId): OfflineMediaServer? = null
    override suspend fun delete(id: OfflineMediaServerId) {}
}

private class WasmBookDtoRepository : OfflineBookDtoRepository {
    override suspend fun findAll(userId: KomgaUserId, pageRequest: KomgaPageRequest): Page<KomeliaBook> = error("offline not available")
    override suspend fun findAll(userId: KomgaUserId, search: KomgaBookSearch, pageRequest: KomgaPageRequest): Page<KomeliaBook> = error("offline not available")
    override suspend fun get(bookId: KomgaBookId, userId: KomgaUserId): KomeliaBook = error("offline not available")
    override suspend fun findByIdOrNull(bookId: KomgaBookId, userId: KomgaUserId): KomeliaBook? = null
    override suspend fun findPreviousInSeriesOrNull(bookId: KomgaBookId, userId: KomgaUserId): KomeliaBook? = null
    override suspend fun findNextInSeriesOrNull(bookId: KomgaBookId, userId: KomgaUserId): KomeliaBook? = null
    override suspend fun findAllOnDeck(userId: KomgaUserId, filterOnLibraryIds: Collection<KomgaLibraryId>?, pageRequest: KomgaPageRequest): Page<KomeliaBook> = error("offline not available")
}

private class WasmReferentialRepository : OfflineReferentialRepository {
    override suspend fun findAllAuthorsByName(search: String): List<KomgaAuthor> = error("offline not available")
    override suspend fun findAllAuthorsByNameAndLibrary(search: String, libraryId: KomgaLibraryId): List<KomgaAuthor> = error("offline not available")
    override suspend fun findAllAuthorsByNameAndCollection(search: String, collectionId: KomgaCollectionId): List<KomgaAuthor> = error("offline not available")
    override suspend fun findAllAuthorsByNameAndSeries(search: String, seriesId: KomgaSeriesId): List<KomgaAuthor> = error("offline not available")
    override suspend fun findAllAuthorsNamesByName(search: String): List<String> = error("offline not available")
    override suspend fun findAllAuthorsRoles(): List<String> = error("offline not available")
    override suspend fun findAllAuthorsByName(search: String?, role: String?, pageRequest: KomgaPageRequest): Page<KomgaAuthor> = error("offline not available")
    override suspend fun findAllAuthorsByNameAndLibraries(search: String?, role: String?, libraryIds: List<KomgaLibraryId>, pageRequest: KomgaPageRequest): Page<KomgaAuthor> = error("offline not available")
    override suspend fun findAllAuthorsByNameAndCollection(search: String?, role: String?, collectionId: KomgaCollectionId, pageRequest: KomgaPageRequest): Page<KomgaAuthor> = error("offline not available")
    override suspend fun findAllAuthorsByNameAndSeries(search: String?, role: String?, seriesId: KomgaSeriesId, pageRequest: KomgaPageRequest): Page<KomgaAuthor> = error("offline not available")
    override suspend fun findAllAuthorsByNameAndReadList(search: String?, role: String?, readListId: KomgaReadListId, pageRequest: KomgaPageRequest): Page<KomgaAuthor> = error("offline not available")
    override suspend fun findAllGenres(): List<String> = error("offline not available")
    override suspend fun findAllGenresByLibraries(libraryIds: List<KomgaLibraryId>): List<String> = error("offline not available")
    override suspend fun findAllGenresByCollection(collectionId: KomgaCollectionId): List<String> = error("offline not available")
    override suspend fun findAllSeriesAndBookTags(): List<String> = error("offline not available")
    override suspend fun findAllSeriesAndBookTagsByLibraries(libraryIds: List<KomgaLibraryId>): List<String> = error("offline not available")
    override suspend fun findAllSeriesAndBookTagsByCollection(collectionId: KomgaCollectionId): List<String> = error("offline not available")
    override suspend fun findAllSeriesTags(): List<String> = error("offline not available")
    override suspend fun findAllSeriesTagsByLibrary(libraryId: KomgaLibraryId): List<String> = error("offline not available")
    override suspend fun findAllSeriesTagsByCollection(collectionId: KomgaCollectionId): List<String> = error("offline not available")
    override suspend fun findAllBookTags(): List<String> = error("offline not available")
    override suspend fun findAllBookTagsBySeries(seriesId: KomgaSeriesId): List<String> = error("offline not available")
    override suspend fun findAllBookTagsByReadList(readListId: KomgaReadListId): List<String> = error("offline not available")
    override suspend fun findAllLanguages(): List<String> = error("offline not available")
    override suspend fun findAllLanguagesByLibraries(libraryIds: List<KomgaLibraryId>): List<String> = error("offline not available")
    override suspend fun findAllLanguagesByCollection(collectionId: KomgaCollectionId): List<String> = error("offline not available")
    override suspend fun findAllPublishers(): List<String> = error("offline not available")
    override suspend fun findAllPublishers(pageable: KomgaPageRequest): Page<String> = error("offline not available")
    override suspend fun findAllPublishersByLibraries(libraryIds: List<KomgaLibraryId>): List<String> = error("offline not available")
    override suspend fun findAllPublishersByCollection(collectionId: KomgaCollectionId): List<String> = error("offline not available")
    override suspend fun findAllAgeRatings(): List<Int?> = error("offline not available")
    override suspend fun findAllAgeRatingsByLibraries(libraryIds: List<KomgaLibraryId>): List<Int?> = error("offline not available")
    override suspend fun findAllAgeRatingsByCollection(collectionId: KomgaCollectionId): List<Int?> = error("offline not available")
    override suspend fun findAllSeriesReleaseDates(): List<LocalDate> = error("offline not available")
    override suspend fun findAllSeriesReleaseDatesByLibraries(libraryIds: List<KomgaLibraryId>): List<LocalDate> = error("offline not available")
    override suspend fun findAllSeriesReleaseDatesByCollection(collectionId: KomgaCollectionId): List<LocalDate> = error("offline not available")
    override suspend fun findAllSharingLabels(): List<String> = error("offline not available")
    override suspend fun findAllSharingLabelsByLibraries(libraryIds: List<KomgaLibraryId>): List<String> = error("offline not available")
    override suspend fun findAllSharingLabelsByCollection(collectionId: KomgaCollectionId): List<String> = error("offline not available")
}

private class WasmSeriesDtoRepository : OfflineSeriesDtoRepository {
    override suspend fun get(seriesId: KomgaSeriesId, userId: KomgaUserId): KomgaSeries = error("offline not available")
    override suspend fun find(seriesId: KomgaSeriesId, userId: KomgaUserId): KomgaSeries? = null
    override suspend fun findAll(userId: KomgaUserId, pageRequest: KomgaPageRequest): Page<KomgaSeries> = error("offline not available")
    override suspend fun findAll(search: KomgaSeriesSearch, userId: KomgaUserId, pageRequest: KomgaPageRequest): Page<KomgaSeries> = error("offline not available")
    override suspend fun findAllRecentlyUpdated(search: KomgaSeriesSearch, userId: KomgaUserId, pageRequest: KomgaPageRequest): Page<KomgaSeries> = error("offline not available")
}

private class WasmLogJournalRepository : LogJournalRepository {
    override suspend fun save(entry: OfflineLogEntry) {}
    override suspend fun get(id: LogEntryId): OfflineLogEntry = error("offline not available")
    override suspend fun findAll(limit: Int, offset: Long): Page<OfflineLogEntry> = error("offline not available")
    override suspend fun findAllByType(type: OfflineLogEntry.Type, limit: Int, offset: Long): Page<OfflineLogEntry> = error("offline not available")
    override suspend fun deleteAll() {}
}

private class WasmTransactions : TransactionTemplate {
    override suspend fun <T> execute(statement: suspend () -> T): T = statement()
}

private class WasmTasksRepository : OfflineTasksRepository {
    override suspend fun takeNew(): TaskEntry? = null
    override suspend fun save(entry: TaskEntry) {}
    override suspend fun save(tasks: Collection<TaskEntry>) {}
    override suspend fun delete(taskId: String) {}
    override suspend fun resetAllRunning(): Int = 0
}

private class WasmOfflineSettingsRepository : OfflineSettingsRepository {
    private val offlineMode = MutableStateFlow(false)
    private val userId = MutableStateFlow(KomgaUserId("0"))
    private val readProgressSyncDate = MutableStateFlow<Instant?>(null)
    private val dataSyncDate = MutableStateFlow<Instant?>(null)

    override fun getOfflineMode(): Flow<Boolean> = offlineMode.asStateFlow()
    override suspend fun putOfflineMode(offline: Boolean) { offlineMode.value = offline }
    override fun getUserId(): Flow<KomgaUserId> = userId.asStateFlow()
    override suspend fun putUserId(userId: KomgaUserId) { this.userId.value = userId }
    override fun getReadProgressSyncDate(): Flow<Instant?> = readProgressSyncDate.asStateFlow()
    override suspend fun putReadProgressSyncDate(timestamp: Instant) { readProgressSyncDate.value = timestamp }
    override fun getDataSyncDate(): Flow<Instant?> = dataSyncDate.asStateFlow()
    override suspend fun putDataSyncDate(timestamp: Instant) { dataSyncDate.value = timestamp }
    override fun getDownloadDirectory(): Flow<PlatformFile> = emptyFlow()
    override suspend fun putDownloadDirectory(path: PlatformFile) {}
}

private class WasmDivinaExtractor : DivinaExtractor {
    override fun mediaTypes(): List<String> = emptyList()
    override fun getEntryBytes(file: PlatformFile, entryName: String): ByteArray = error("not supported on wasm")
}

private class WasmEpubExtractor : EpubExtractor {
    override fun getEntryBytes(file: PlatformFile, entryName: String): ByteArray = error("not supported on wasm")
}

private class WasmDownloadManager : PlatformDownloadManager {
    override suspend fun launchBookDownload(bookId: KomgaBookId) {}
    override suspend fun cancelBookDownload(bookId: KomgaBookId) {}
}

private class WasmOfflineModuleImpl(
    repositories: OfflineRepositories,
    authenticatedUser: StateFlow<KomgaUser?>,
    onlineServerUrl: StateFlow<String>,
    isOffline: StateFlow<Boolean>,
    komgaClientFactory: KomgaClientFactory,
) : OfflineModule(repositories, authenticatedUser, onlineServerUrl, isOffline, komgaClientFactory) {
    override fun createDivinaExtractors(): List<DivinaExtractor> = emptyList()
    override fun createEpubExtractor(): EpubExtractor = WasmEpubExtractor()
    override fun createPlatformDownloadManager(
        downloadService: BookDownloadService,
        logJournalRepository: LogJournalRepository,
        events: MutableSharedFlow<DownloadEvent>,
    ): PlatformDownloadManager = WasmDownloadManager()
}

suspend fun createWasmOfflineDependencies(
    komgaClientFactory: KomgaClientFactory,
    authenticatedUser: MutableStateFlow<KomgaUser?>,
    onlineServerUrl: StateFlow<String>,
    isOffline: StateFlow<Boolean>,
): OfflineDependencies {
    val repos = OfflineRepositories(
        mediaServerRepository = WasmMediaServerRepository(),
        mediaRepository = WasmMediaRepository(),
        bookRepository = WasmOfflineBookRepository(),
        bookMetadataRepository = WasmBookMetadataRepository(),
        bookMetadataAggregationRepository = WasmBookMetadataAggregationRepository(),
        libraryRepository = WasmLibraryRepository(),
        readProgressRepository = WasmReadProgressRepository(),
        seriesMetadataRepository = WasmSeriesMetadataRepository(),
        seriesRepository = WasmSeriesRepository(),
        thumbnailBookRepository = WasmThumbnailBookRepository(),
        thumbnailSeriesRepository = WasmThumbnailSeriesRepository(),
        userRepository = WasmUserRepository(),
        bookDtoRepository = WasmBookDtoRepository(),
        referentialRepository = WasmReferentialRepository(),
        seriesDtoRepository = WasmSeriesDtoRepository(),
        logJournalRepository = WasmLogJournalRepository(),
        transactionTemplate = WasmTransactions(),
        tasksRepository = WasmTasksRepository(),
        offlineSettingsRepository = WasmOfflineSettingsRepository(),
    )

    val module = WasmOfflineModuleImpl(
        repositories = repos,
        authenticatedUser = authenticatedUser,
        onlineServerUrl = onlineServerUrl,
        isOffline = isOffline,
        komgaClientFactory = komgaClientFactory,
    )

    return module.initDependencies()
}
