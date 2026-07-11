package snd.komelia.db.color

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import snd.komelia.color.ColorCurvePoints
import snd.komelia.color.ColorCurvePreset
import snd.komelia.color.repository.ColorCurvePresetRepository
import snd.komelia.db.tables.ColorCurvePresetsTable
import snd.komelia.db.tables.ColorCurvePresetsTable.name

class ExposedColorCurvesPresetRepository(private val database: Database) :  ColorCurvePresetRepository {

    override suspend fun getPresets(): List<ColorCurvePreset> {
        return transaction(database) {
            ColorCurvePresetsTable.selectAll()
                .map {
                    val points = ColorCurvePoints(
                        colorCurvePoints = it[ColorCurvePresetsTable.colorCurvePoints],
                        redCurvePoints = it[ColorCurvePresetsTable.redCurvePoints],
                        greenCurvePoints = it[ColorCurvePresetsTable.greenCurvePoints],
                        blueCurvePoints = it[ColorCurvePresetsTable.blueCurvePoints],
                    )
                    ColorCurvePreset(
                        name = it[name],
                        points = points
                    )
                }
        }
    }

    override suspend fun savePreset(preset: ColorCurvePreset) {
        transaction(database) {
            ColorCurvePresetsTable.upsert {
                it[name] = preset.name
                it[colorCurvePoints] = preset.points.colorCurvePoints
                it[redCurvePoints] = preset.points.redCurvePoints
                it[greenCurvePoints] = preset.points.greenCurvePoints
                it[blueCurvePoints] = preset.points.blueCurvePoints
            }
        }
    }

    override suspend fun deletePreset(preset: ColorCurvePreset) {
        transaction(database) { ColorCurvePresetsTable.deleteWhere { name.eq(preset.name) } }
    }
}