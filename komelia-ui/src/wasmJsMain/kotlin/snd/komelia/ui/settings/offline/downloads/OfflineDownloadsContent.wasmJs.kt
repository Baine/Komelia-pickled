package snd.komelia.ui.settings.offline.downloads

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import coil3.PlatformContext

@Composable
internal actual fun rememberStorageLabel(file: PlatformFile): String = ""

internal actual fun getDefaultInternalDownloadsDir(platformContent: PlatformContext): DefaultDownloadStorageLocation {
    throw NotImplementedError("Not supported on wasmJs")
}