package com.example.ledger.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE occurredAt >= :start AND occurredAt < :endExclusive ORDER BY occurredAt DESC, id DESC")
    List<DiaryEntry> listBetween(long start, long endExclusive);

    @Insert
    long insert(DiaryEntry entry);

    @Update
    void update(DiaryEntry entry);

    @Delete
    void delete(DiaryEntry entry);
}
