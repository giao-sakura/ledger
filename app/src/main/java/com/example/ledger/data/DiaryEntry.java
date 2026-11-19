package com.example.ledger.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "diary_entries", indices = {@Index("occurredAt")})
public class DiaryEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String body;
    public long occurredAt;

    public DiaryEntry copy() {
        DiaryEntry copy = new DiaryEntry();
        copy.id = id;
        copy.body = body == null ? "" : body;
        copy.occurredAt = occurredAt;
        return copy;
    }
}
