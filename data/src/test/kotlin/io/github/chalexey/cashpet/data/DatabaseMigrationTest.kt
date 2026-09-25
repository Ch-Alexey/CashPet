package io.github.chalexey.cashpet.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.chalexey.cashpet.core.model.Slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Обновление приложения не теряет сохранения: база версии 1 — ровно по схеме из data/schemas —
 * открывается новой версией, профиль и настройки на месте.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = context.getDatabasePath(DB_NAME)

    @Before
    fun setUp() {
        file.parentFile?.mkdirs()
        file.delete()
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun `версия 1 → 2 — настройки и профиль сохраняются, демо выключено`() = runBlocking {
        createVersion1 { db ->
            db.execSQL("INSERT INTO settings (id, sound, animations, seen_hints) VALUES (0, 0, 1, '[\"shop\"]')")
            db.execSQL("INSERT INTO game_state (slot, json, schema_version, updated_at) VALUES ('CHILD', '{}', 1, 0)")
        }

        val db = Room.databaseBuilder(context, CashPetDatabase::class.java, DB_NAME).allowMainThreadQueries().build()
        val settings = SettingsRepository(db.settingsDao()).settings.first()
        val child = db.gameStateDao().get(Slot.CHILD)
        db.close()

        assertEquals(AppSettings(sound = false, animations = true, seenHints = setOf("shop"), demoActive = false), settings)
        assertEquals("{}", child?.json)
    }

    /** База версии 1 так, как её создал бы Room: таблицы и отметка схемы из 1.json. */
    private fun createVersion1(fill: (SQLiteDatabase) -> Unit) {
        val schema = Json.parseToJsonElement(File(SCHEMA_V1).readText()).jsonObject.getValue("database").jsonObject
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        schema.getValue("entities").jsonArray.forEach { entity ->
            val e = entity.jsonObject
            val table = e.getValue("tableName").jsonPrimitive.content
            db.execSQL(e.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
        }
        schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
        db.version = 1
        fill(db)
        db.close()
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
        const val SCHEMA_V1 = "schemas/io.github.chalexey.cashpet.data.CashPetDatabase/1.json"
    }
}
