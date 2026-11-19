package com.example.ledger.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wishes")
public class WishItem {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String need;
    public String category;
    public long createdAt;
}
