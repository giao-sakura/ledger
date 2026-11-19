package com.example.ledger.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Transaction.class, DiaryEntry.class, ClothingItem.class, Outfit.class, OutfitLink.class, OutfitPlan.class, WishItem.class}, version = 4, exportSchema = false)
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

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `clothes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `imagePath` TEXT, `category` TEXT, `color` TEXT, `seasons` INTEGER NOT NULL, `style` TEXT, `status` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `outfits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `createdAt` INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `outfit_items` (`outfitId` INTEGER NOT NULL, `itemId` INTEGER NOT NULL, PRIMARY KEY(`outfitId`, `itemId`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `outfit_plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `outfitId` INTEGER NOT NULL, `planDate` TEXT)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_outfit_plans_planDate` ON `outfit_plans` (`planDate`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `wishes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `need` TEXT, `category` TEXT, `createdAt` INTEGER NOT NULL)");
        }
    };

    public abstract TransactionDao transactionDao();

    public abstract DiaryDao diaryDao();

    public abstract WardrobeDao wardrobeDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "ledger.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build();
                }
            }
        }
        return instance;
    }
}
