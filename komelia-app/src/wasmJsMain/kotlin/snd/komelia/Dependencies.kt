package snd.komelia

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import snd.komelia.api.RemoteActuatorApi
import snd.komelia.api.RemoteAnnouncementsApi
import snd.komelia.api.RemoteApi
import snd.komelia.api.RemoteBookApi
import snd.komelia.api.RemoteCollectionsApi
import snd.komelia.api.RemoteFileSystemApi
import snd.komelia.api.RemoteLibraryApi
import snd.komelia.api.RemoteReadListApi
import snd.komelia.api.RemoteReferentialApi
import snd.komelia.api.RemoteSeriesApi
import snd.komelia.api.RemoteSettingsApi
import snd.komelia.api.RemoteTaskApi
import snd.komelia.api.RemoteUserApi
import snd.komelia.db.SettingsStateWrapper
import Database
import snd.komelia.db.color.IDBBookColorCorrectionRepository
import snd.komelia.db.color.IDBColorCurvesPresetRepository
import snd.komelia.db.color.IDBColorLevelsPresetRepository
import snd.komelia.db.getIndexedDb
import snd.komelia.db.repository.EpubReaderSettingsRepositoryWrapper
import snd.komelia.db.repository.HomeScreenFilterRepositoryWrapper
import snd.komelia.db.repository.KomfSettingsRepositoryWrapper
import snd.komelia.db.repository.ReaderSettingsRepositoryWrapper
import snd.komelia.db.repository.SettingsRepositoryWrapper
import snd.komelia.db.settings.LocalStorageSettingsRepository
import snd.komelia.db.settings.NoopFontsRepository
import snd.komelia.homefilters.homeScreenDefaultFilters
import snd.komelia.image.BookImageLoader
import snd.komelia.image.WasmReaderImageFactory
import snd.komelia.image.coil.BlobFetcher
import snd.komelia.image.coil.CoilAwareDecoder
import snd.komelia.image.coil.CoilDecoder
import snd.komelia.image.coil.FileMapper
import snd.komelia.image.coil.KomeliaFetcherFactory
import snd.komelia.image.processing.ColorCorrectionStep
import snd.komelia.image.processing.ImageProcessingPipeline
import snd.komelia.image.wasm.client.WorkerImageDecoder
import snd.komelia.komga.api.KomgaApi
import snd.komelia.offline.book.repository.OfflineBookRepository
import snd.komelia.settings.CookieStoreSecretsRepository
import snd.komelia.ui.DependencyContainer
import snd.komelia.ui.strings.EnStrings
import snd.komf.client.KomfClientFactory
import snd.komga.client.KomgaClientFactory
import snd.komga.client.sse.KomgaEvent
import snd.komga.client.user.KomgaUser

suspend fun initDependencies(stateFlowScope: CoroutineScope): DependencyContainer {
    val workerDecoder = WorkerImageDecoder()
    workerDecoder.init()

    val localStorageRepository = LocalStorageSettingsRepository()
    val appSettingsRepository = SettingsRepositoryWrapper(
        SettingsStateWrapper(
            localStorageRepository.getSettings(),
            localStorageRepository::saveAppSettings
        )
    )
    val imageReaderSettingsRepository = ReaderSettingsRepositoryWrapper(
        SettingsStateWrapper(
            localStorageRepository.getImageReaderSettings(),
            localStorageRepository::saveImageReaderSettings
        )
    )
    val epubReaderSettingsRepository = EpubReaderSettingsRepositoryWrapper(
        SettingsStateWrapper(
            localStorageRepository.getEpubReaderSettings(),
            localStorageRepository::saveEpubReaderSettings
        )
    )
    val komfSettingsRepository = KomfSettingsRepositoryWrapper(
        SettingsStateWrapper(
            localStorageRepository.getKomfSettings(),
            localStorageRepository::saveKomfSettings
        )
    )
    val secretsRepository = CookieStoreSecretsRepository()

    val idb = getIndexedDb()
    val bookColorCorrectionRepository = IDBBookColorCorrectionRepository(idb)
    val curvePresetsRepository = IDBColorCurvesPresetRepository(idb)
    val levelsPresetsRepository = IDBColorLevelsPresetRepository(idb)

    val appRepositories = AppRepositories(
        settingsRepository = appSettingsRepository,
        epubReaderSettingsRepository = epubReaderSettingsRepository,
        imageReaderSettingsRepository = imageReaderSettingsRepository,
        fontsRepository = NoopFontsRepository(),
        colorCurvesPresetsRepository = curvePresetsRepository,
        colorLevelsPresetRepository = levelsPresetsRepository,
        bookColorCorrectionRepository = bookColorCorrectionRepository,
        secretsRepository = secretsRepository,
        komfSettingsRepository = komfSettingsRepository,
        homeScreenFilterRepository = HomeScreenFilterRepositoryWrapper(
            SettingsStateWrapper(homeScreenDefaultFilters) {}
        )
    )

    val baseUrl = appSettingsRepository.getServerUrl().stateIn(stateFlowScope)
    val komfUrl = komfSettingsRepository.getKomfUrl().stateIn(stateFlowScope)
    overrideFetch { baseUrl.value }

    val ktorClient = HttpClient(Js) {
        defaultRequest { url(baseUrl.value) }
        expectSuccess = true
        followRedirects = false
    }

    val komgaClientFactory = KomgaClientFactory.Builder()
        .ktor(ktorClient)
        .baseUrl { baseUrl.value }
        .build()

    val komfClientFactory = KomfClientFactory.Builder()
        .baseUrl { komfUrl.value }
        .ktor(ktorClient)
        .build()

    val offlineBookRepo = WasmOfflineBookRepository()
    val isOffline = MutableStateFlow(false)
    val currentUserFlow = MutableStateFlow<KomgaUser?>(null)

    val komgaApiFlow = MutableStateFlow<KomgaApi>(
        RemoteApi(
            actuatorApi = RemoteActuatorApi(komgaClientFactory.actuatorClient()),
            announcementsApi = RemoteAnnouncementsApi(komgaClientFactory.announcementClient()),
            bookApi = RemoteBookApi(komgaClientFactory.bookClient(), offlineBookRepo),
            collectionsApi = RemoteCollectionsApi(komgaClientFactory.collectionClient()),
            fileSystemApi = RemoteFileSystemApi(komgaClientFactory.fileSystemClient()),
            libraryApi = RemoteLibraryApi(komgaClientFactory.libraryClient()),
            readListApi = RemoteReadListApi(komgaClientFactory.readListClient(), offlineBookRepo),
            referentialApi = RemoteReferentialApi(komgaClientFactory.referentialClient()),
            seriesApi = RemoteSeriesApi(komgaClientFactory.seriesClient()),
            settingsApi = RemoteSettingsApi(komgaClientFactory.settingsClient()),
            tasksApi = RemoteTaskApi(komgaClientFactory.taskClient()),
            userApi = RemoteUserApi(komgaClientFactory.userClient()),
            komgaClientFactory = komgaClientFactory,
            offlineEvents = MutableSharedFlow()
        )
    )

    val komgaSharedState = KomgaAuthenticationState(
        userApi = komgaApiFlow.map { it.userApi }.stateIn(stateFlowScope),
        libraryApi = komgaApiFlow.map { it.libraryApi }.stateIn(stateFlowScope),
        currentUserFlow = currentUserFlow,
        serverUrl = baseUrl
    )

    val colorCorrectionStep = ColorCorrectionStep(bookColorCorrectionRepository)
    val imagePipeline = ImageProcessingPipeline()
    imagePipeline.addStep(colorCorrectionStep)

    val coil = createCoil(komgaApiFlow, ktorClient, workerDecoder)
    SingletonImageLoader.setSafe { coil }

    val komgaEvents = ManagedKomgaEvents(
        komgaApi = komgaApiFlow,
        komgaSharedState = komgaSharedState,
        memoryCache = coil.memoryCache,
        diskCache = null,
        libraryApi = komgaApiFlow.map { it.libraryApi },
    )

    val readerImageFactory = WasmReaderImageFactory(
        imageDecoder = workerDecoder,
        downSamplingKernel = imageReaderSettingsRepository.getDownsamplingKernel().stateIn(stateFlowScope),
        upsamplingMode = imageReaderSettingsRepository.getUpsamplingMode().stateIn(stateFlowScope),
        linearLightDownSampling = imageReaderSettingsRepository.getLinearLightDownsampling().stateIn(stateFlowScope),
        processingPipeline = imagePipeline,
        stretchImages = imageReaderSettingsRepository.getStretchToFit().stateIn(stateFlowScope),
    )

    val readerImageLoader = BookImageLoader(
        bookClient = komgaApiFlow.map { it.bookApi }.stateIn(stateFlowScope),
        imageDecoder = workerDecoder,
        readerImageFactory = readerImageFactory,
        diskCache = null
    )

    val offlineDeps = createWasmOfflineDependencies(
        komgaClientFactory = komgaClientFactory,
        authenticatedUser = currentUserFlow,
        onlineServerUrl = baseUrl,
        isOffline = isOffline,
    )

    return DependencyContainer(
        appStrings = MutableStateFlow(EnStrings),
        appRepositories = appRepositories,
        komgaApi = komgaApiFlow,
        isOffline = isOffline,
        komfClientFactory = komfClientFactory,
        appNotifications = AppNotifications(),
        komgaSharedState = komgaSharedState,
        komgaEvents = komgaEvents,
        appUpdater = null,
        coilContext = PlatformContext.INSTANCE,
        coilImageLoader = coil,
        imageDecoder = workerDecoder,
        bookImageLoader = readerImageLoader,
        readerImageFactory = readerImageFactory,
        windowState = BrowserWindowState(),
        colorCorrectionStep = colorCorrectionStep,
        onnxRuntimeInstaller = null,
        onnxModelDownloader = null,
        onnxRuntime = null,
        upscaler = null,
        panelDetector = null,
        offlineDependencies = offlineDeps,
    )
}

private fun createCoil(
    komgaApi: kotlinx.coroutines.flow.StateFlow<KomgaApi>,
    ktorClient: HttpClient,
    imageDecoder: WorkerImageDecoder,
): ImageLoader {
    val coilAwareDecoder = CoilAwareDecoder(imageDecoder)
    return ImageLoader.Builder(PlatformContext.INSTANCE)
        .components {
            add(FileMapper())
            add(CoilDecoder.Factory(coilAwareDecoder))
            add(KomeliaFetcherFactory(komgaApi, coilAwareDecoder))
            add(BlobFetcher.Factory())
        }
        .memoryCache(
            MemoryCache.Builder()
                .maxSizeBytes(64 * 1024 * 1024)
                .build()
        )
        .build()
}

private fun overrideFetch(komgaUrl: () -> String) {
    js("""
    window.originalFetch = window.fetch;
    window.fetch = function (resource, init) {
        init = Object.assign({}, init);
        if(typeof resource =='string' && resource.startsWith(komgaUrl())) {
            init.headers = Object.assign( { 'X-Requested-With' : 'XMLHttpRequest' }, init.headers)
            init.credentials = 'include';
        }
        return window.originalFetch(resource, init);
    };
""")
}
