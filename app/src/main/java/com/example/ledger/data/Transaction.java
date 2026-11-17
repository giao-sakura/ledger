package com.example.ledger.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions", indices = {@Index("occurredAt")})
public class Transaction {
    public static final int EXPENSE = 0;
    public static final int INCOME = 1;

    @PrimaryKey(autoGenerate = true)
    public long id;
    public int type;
    public long amountCents;
    public String categoryKey;
    public String note;
    public long occurredAt;

    public Transaction copy() {
        Transaction copy = new Transaction();
        copy.id = id;
        copy.type = type;
        copy.amountCents = amountCents;
        copy.categoryKey = categoryKey;
        copy.note = note == null ? "" : note;
        copy.occurredAt = occurredAt;
        return copy;
    }
}
