package com.example.ledger.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Transaction.class, DiaryEntry.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `diary_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `body` TEXT, `occurredAt` INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_occurredAt` ON `diary_entries` (`occurredAt`)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `diary_entries_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `body` TEXT, `occurredAt` INTEGER NOT NULL)");
            db.execSQL("INSERT INTO `diary_entries_new` (`id`, `body`, `occurredAt`) SELECT `id`, `body`, `occurredAt` FROM `diary_entries`");
            db.execSQL("DROP TABLE `diary_entries`");
            db.execSQL("ALTER TABLE `diary_entries_new` RENAME TO `diary_entries`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_occurredAt` ON `diary_entries` (`occurredAt`)");
        }
    };

    public abstract TransactionDao transactionDao();

    public abstract DiaryDao diaryDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "ledger.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();
                }
            }
        }
        return instance;
    }
}
