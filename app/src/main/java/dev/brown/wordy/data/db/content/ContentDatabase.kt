package dev.brown.wordy.data.db.content

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

@Database(
    entities = [WordEntity::class, SenseEntity::class, UnitEntity::class, ContentMetaEntity::class],
    version = ContentDatabase.VERSION,
    exportSchema = true,
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun dao(): ContentDao

    companion object {
        const val VERSION = 1
        const val FILE_NAME = "content.db"
        const val ASSET_DB = "database/content.db"
        const val ASSET_VERSION = "database/content.version"
        private const val INSTALLED_MARKER = "content.db.installed"

        /**
         * assets 의 프리패키지 DB 를 앱 DB 디렉터리로 복사(버전이 바뀐 경우에만)한 뒤 Room 으로 연다.
         * Room 은 첫 열기에서 테이블 구조를 검증하고 room_master_table 을 기록한다.
         */
        fun open(context: Context): ContentDatabase {
            installFromAssets(context)
            return Room.databaseBuilder<ContentDatabase>(context = context, name = context.getDatabasePath(FILE_NAME).absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        /** assets/database/content.version 의 문자열. 자산이 없으면 null (콘텐츠 미빌드 상태). */
        fun assetVersion(context: Context): String? = runCatching {
            context.assets.open(ASSET_VERSION).bufferedReader().use { it.readText().trim() }
        }.getOrNull()

        private fun installFromAssets(context: Context) {
            val version = assetVersion(context) ?: return
            val dbFile = context.getDatabasePath(FILE_NAME)
            val marker = File(dbFile.parentFile, INSTALLED_MARKER)
            val installed = marker.takeIf { it.exists() }?.readText()?.trim()
            if (installed == version && dbFile.exists()) return
            dbFile.parentFile?.mkdirs()
            for (suffix in listOf("", "-wal", "-shm", "-journal")) File(dbFile.path + suffix).delete()
            context.assets.open(ASSET_DB).use { input -> dbFile.outputStream().use { input.copyTo(it) } }
            marker.writeText(version)
        }
    }
}
