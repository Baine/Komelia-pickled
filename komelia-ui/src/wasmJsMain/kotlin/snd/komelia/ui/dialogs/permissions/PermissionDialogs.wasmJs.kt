package snd.komelia.ui.dialogs.permissions

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

@Composable
actual fun DownloadNotificationRequestDialog(onComplete: (granted: Boolean) -> Unit) {}

@Composable
actual fun StoragePermissionRequestDialog(onComplete: (directory: PlatformFile?) -> Unit) {}