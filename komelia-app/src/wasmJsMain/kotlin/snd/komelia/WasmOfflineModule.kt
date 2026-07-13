package snd.komelia

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import snd.komelia.db.NoopTransactionTemplate
import snd.komelia.db.TransactionTemplate
import snd.komelia.komga.api.model.KomeliaBook
import snd.komelia.offline.OfflineDependencies
import snd.komelia.offline.OfflineRepositories
import snd.komelia.offline.action.OfflineActions
import snd.komelia.offline.api.OfflineBookApi
import snd.komelia.offline.api.OfflineCollectionsApi
import snd.komelia.offline.api.OfflineFileSystemApi
import snd.komelia.offline.api.OfflineKomgaApi
import snd.komelia.offline.api.OfflineLibraryApi
import snd.komelia.offline.api.OfflineReadListApi
import snd.komelia.offline.api.OfflineReferentialApi
import snd.komelia.offline.api.OfflineSeriesApi
import snd.komelia.offline.api.OfflineSettingsApi
import snd.komelia.offline.api.OfflineTaskApi
import snd.komelia.offline.api.OfflineUserApi
import snd.komelia.offline.api.OfflineActuatorApi
import snd.komelia.offline.api.OfflineAnnouncementsApi
import snd.komelia.offline.api.repository.OfflineBookDtoRepository
import snd.komelia.offline.api.repository.OfflineReferentialRepository
import snd.komelia.offline.api.repository.OfflineSeriesDtoRepository
import snd.komelia.offline.book.actions.BookKomgaImportAction
import snd.komelia.offline.book.model.OfflineBook
import snd.komelia.offline.book.model.OfflineBookMetadata
import snd.komelia.offline.book.model.OfflineThumbnailBook
import snd.komelia.offline.book.repository.OfflineBookMetadataAggregationRepository
import snd.komelia.offline.book.repository.OfflineBookMetadataRepository
import snd.komelia.offline.book.repository.OfflineBookRepository
import snd.komelia.offline.book.repository.OfflineThumbnailBookRepository
import snd.komelia.offline.library.actions.LibraryKomgaImportAction
import snd.komelia.offline.library.model.OfflineLibrary
import snd.komelia.offline.library.repository.OfflineLibraryRepository
import snd.komelia.offline.media.model.OfflineMedia
import snd.komelia.offline.media.repository.OfflineMediaRepository
import snd.komelia.offline.mediacontainer.BookContentExtractors
import snd.komelia.offline.mediacontainer.DivinaExtractor
import snd.komelia.offline.mediacontainer.EpubExtractor
import snd.komelia.offline.readprogress.OfflineReadProgress
import snd.komelia.offline.readprogress.OfflineReadProgressRepository
import snd.komelia.offline.series.actions.SeriesKomgaImportAction
import snd.komelia.offline.series.model.OfflineBookMetadataAggregation
import snd.komelia.offline.series.model.OfflineSeries
import snd.komelia.offline.series.model.OfflineSeriesMetadata
import snd.komelia.offline.series.model.OfflineThumbnailSeries
import snd.komelia.offline.series.repository.OfflineSeriesMetadataRepository
import snd.komelia.offline.series.repository.OfflineSeriesRepository
import snd.komelia.offline.series.repository.OfflineThumbnailSeriesRepository
import snd.komelia.offline.server.actions.MediaServerSaveAction
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
import snd.komelia.offline.tasks.OfflineTaskEmitter
import snd.komelia.offline.tasks.model.TaskEntry
import snd.komelia.offline.tasks.repository.OfflineTasksRepository
import snd.komelia.offline.user.actions.UserKomgaImportAction
import snd.komelia.offline.user.model.OfflineUser
import snd.komelia.offline.user.repository.OfflineUserRepository
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
import snd.komga.client.sse.KomgaEvent
import snd.komga.client.user.KomgaUserId
import snd.komga.client.user.KomgaUser
import kotlin.time.Instant

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
        transactionTemplate = NoopTransactionTemplate(),
        tasksRepository = WasmTasksRepository(),
        offlineSettingsRepository = WasmOfflineSettingsRepository(),
    )

    val komgaEvents = MutableSharedFlow<KomgaEvent>(replay = 0)
    val bookDownloadEvents = MutableSharedFlow<DownloadEvent>(replay = 0)
    val taskEmitter = OfflineTaskEmitter(tasksRepository = repos.tasksRepository, tasksFlow = MutableSharedFlow())
    val fileService = BookContentExtractors(emptyList(), WasmEpubExtractor())

    val actions = OfflineActions(
        listOf(
            UserKomgaImportAction(repos.userRepository, repos.transactionTemplate),
            MediaServerSaveAction(repos.mediaServerRepository, repos.transactionTemplate),
            LibraryKomgaImportAction(repos.libraryRepository, repos.mediaServerRepository, repos.logJournalRepository, repos.transactionTemplate),
            SeriesKomgaImportAction(repos.seriesRepository, repos.seriesMetadataRepository, repos.thumbnailSeriesRepository, repos.bookMetadataAggregationRepository, repos.logJournalRepository, komgaClientFactory.seriesClient(), repos.transactionTemplate),
            BookKomgaImportAction(repos.bookRepository, repos.bookMetadataRepository, repos.thumbnailBookRepository, repos.readProgressRepository, repos.mediaRepository, repos.logJournalRepository, komgaClientFactory.bookClient(), taskEmitter, repos.transactionTemplate, komgaEvents),
        )
    )

    val offlineUserId = MutableStateFlow(OfflineUser.ROOT)
    val offlineServerFlow = MutableStateFlow<OfflineMediaServer?>(null)

    val komgaApi = OfflineKomgaApi(
        actuatorApi = OfflineActuatorApi(),
        announcementsApi = OfflineAnnouncementsApi(),
        bookApi = OfflineBookApi(repos.mediaRepository, repos.bookDtoRepository, repos.bookRepository, repos.thumbnailBookRepository, repos.readProgressRepository, actions, fileService, offlineUserId),
        collectionsApi = OfflineCollectionsApi(),
        fileSystemApi = OfflineFileSystemApi(),
        libraryApi = OfflineLibraryApi(repos.libraryRepository, offlineServerFlow, offlineUserId, actions),
        readListApi = OfflineReadListApi(),
        referentialApi = OfflineReferentialApi(repos.referentialRepository),
        seriesApi = OfflineSeriesApi(actions, repos.seriesDtoRepository, repos.thumbnailSeriesRepository, repos.seriesRepository, repos.libraryRepository, repos.bookRepository, repos.thumbnailBookRepository, offlineUserId),
        settingsApi = OfflineSettingsApi(),
        tasksApi = OfflineTaskApi(),
        userApi = OfflineUserApi(offlineUserId, repos.userRepository),
        komgaEvents = komgaEvents,
    )

    return OfflineDependencies(
        actions = actions,
        taskEmitter = taskEmitter,
        komgaEvents = komgaEvents,
        bookDownloadEvents = bookDownloadEvents,
        downloadService = BookDownloadService(
            libraryDownloadPath = emptyFlow(),
            bookClient = komgaClientFactory.bookClient(),
            seriesClient = komgaClientFactory.seriesClient(),
            libraryClient = komgaClientFactory.libraryClient(),
            userClient = komgaClientFactory.userClient(),
            saveUserAction = actions.get(),
            saveServerAction = actions.get(),
            libraryImportAction = actions.get(),
            seriesImportAction = actions.get(),
            bookImportAction = actions.get(),
            onlineServerUrl = onlineServerUrl,
        ),
        repositories = repos,
        fileService = fileService,
        komgaApi = komgaApi,
    )
}
