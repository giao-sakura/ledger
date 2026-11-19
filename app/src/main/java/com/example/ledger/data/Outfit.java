package com.example.ledger.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "outfits")
public class Outfit {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public long createdAt;
}
