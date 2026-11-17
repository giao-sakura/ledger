package com.example.ledger.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE occurredAt >= :start AND occurredAt < :endExclusive ORDER BY occurredAt DESC, id DESC")
    List<Transaction> listBetween(long start, long endExclusive);

    @Insert
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);
}
