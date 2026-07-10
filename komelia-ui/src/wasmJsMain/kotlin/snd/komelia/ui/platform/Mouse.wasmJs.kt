package snd.komelia.ui.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

actual fun Modifier.cursorForMove() =
    this.pointerHoverIcon(PointerIcon.Hand)

actual fun Modifier.cursorForHand(): Modifier =
    this.pointerHoverIcon(PointerIcon.Hand)
