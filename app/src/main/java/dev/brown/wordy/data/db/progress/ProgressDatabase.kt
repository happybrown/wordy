package dev.brown.wordy.data.db.progress

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/** 사용자 학습 데이터. 콘텐츠 DB 와 분리되어 콘텐츠 갱신 시에도 절대 삭제되지 않는다. */
@Database(
    entities = [CardEntity::class, ReviewLogEntity::class, WordStatusEntity::class, DailyStatEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun cards(): CardDao
    abstract fun reviewLogs(): ReviewLogDao
    abstract fun wordStatus(): WordStatusDao
    abstract fun dailyStats(): DailyStatDao

    companion object {
        const val FILE_NAME = "progress.db"

        fun open(context: Context): ProgressDatabase =
            Room.databaseBuilder<ProgressDatabase>(context = context, name = context.getDatabasePath(FILE_NAME).absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
