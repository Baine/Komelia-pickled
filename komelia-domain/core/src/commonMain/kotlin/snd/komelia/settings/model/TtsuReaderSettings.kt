package snd.komelia.settings.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import snd.komelia.settings.model.TtsuBlurMode.AFTER_TOC

// ponytail: one generic serializer replaces 5 identical inner classes

inline fun <reified E : Enum<E>> enumStringSerializer(
    serialName: String,
    crossinline resolve: E.() -> String,
): KSerializer<E> = object : KSerializer<E> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): E = enumValueOf<E>(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: E) = encoder.encodeString(value.resolve())
}

@Serializable
data class TtsuReaderSettings(
    val theme: String = "light-theme",
    val customThemes: Map<String, TtsuThemeOption> = emptyMap(),
    val multiplier: Int = 20,
    val serifFontFamily: TtsuFont = TtsuFont(
        displayName = "Noto Sans CJK JP",
        familyName = "Noto Sans CJK JP"
    ),
    val sansFontFamily: TtsuFont = TtsuFont(
        displayName = "Noto Sans CJK JP",
        familyName = "Noto Sans CJK JP"
    ),
    val fontSize: Int = 20,
    val lineHeight: Float = 1.65f,
    val hideSpoilerImage: Boolean = true,
    val hideSpoilerImageMode: TtsuBlurMode = AFTER_TOC,
    val hideFurigana: Boolean = false,
    val furiganaStyle: TtsuFuriganaStyle = TtsuFuriganaStyle.Partial,
    val writingMode: TtsuWritingMode = TtsuWritingMode.HORIZONTAL_TB,
    val enableReaderWakeLock: Boolean = false,
    val showCharacterCounter: Boolean = true,
    val viewMode: TtsuViewMode = TtsuViewMode.Continuous,
    val secondDimensionMaxValue: Int? = 0,
    val firstDimensionMargin: Int? = 0,
    val swipeThreshold: Int = 10,
    val disableWheelNavigation: Boolean = false,
    val autoPositionOnResize: Boolean = true,
    val avoidPageBreak: Boolean = false,
    val customReadingPointEnabled: Boolean = false,
    val selectionToBookmarkEnabled: Boolean = false,
    val confirmClose: Boolean = false,
    val manualBookmark: Boolean = false,
    val autoBookmark: Boolean = true,
    val autoBookmarkTime: Int = 3,
    val pageColumns: Int = 0,
    val verticalCustomReadingPosition: Int = 100,
    val horizontalCustomReadingPosition: Int = 0,
    val userFonts: List<TtsuUserFont> = emptyList(),
)

@Serializable
data class TtsuThemeOption(
    val fontColor: String,
    val backgroundColor: String,
    val selectionFontColor: String,
    val selectionBackgroundColor: String,
    val hintFuriganaShadowColor: String,
    val hintFuriganaFontColor: String,
    val tooltipTextFontColor: String,
)

@Serializable
data class TtsuFont(
    val displayName: String,
    val familyName: String,
)

@Serializable
data class TtsuUserFont(
    val displayName: String,
    val familyName: String,
    val path: String,
    val fileName: String,
)

@Serializable(with = TtsuBlurMode.Serializer::class)
enum class TtsuBlurMode(val value: String) {
    ALL("all"),
    AFTER_TOC("afterToc");

    internal object Serializer : KSerializer<TtsuBlurMode> by enumStringSerializer("TtsuBlurMode", TtsuBlurMode::value)
}

@Serializable(with = TtsuFuriganaStyle.Serializer::class)
enum class TtsuFuriganaStyle(val value: String) {
    Hide("hide"),
    Partial("partial"),
    Toggle("toggle"),
    Full("full");

    internal object Serializer : KSerializer<TtsuFuriganaStyle> by enumStringSerializer("TtsuFuriganaStyle", TtsuFuriganaStyle::value)
}

@Serializable(with = TtuTheme.Serializer::class)
enum class TtuTheme(val value: String) {
    LIGHT("light-theme"),
    DARK("dark-theme");

    internal object Serializer : KSerializer<TtuTheme> by enumStringSerializer("TtuTheme", TtuTheme::value)
}

@Serializable(with = TtsuWritingMode.Serializer::class)
enum class TtsuWritingMode(val value: String) {
    HORIZONTAL_TB("horizontal-tb"),
    VERTICAL_RL("vertical-rl");

    internal object Serializer : KSerializer<TtsuWritingMode> by enumStringSerializer("TtsuWritingMode", TtsuWritingMode::value)
}

@Serializable(with = TtsuViewMode.Serializer::class)
enum class TtsuViewMode(val value: String) {
    Continuous("continuous"),
    Paginated("paginated");

    internal object Serializer : KSerializer<TtsuViewMode> by enumStringSerializer("TtsuViewMode", TtsuViewMode::value)
}
