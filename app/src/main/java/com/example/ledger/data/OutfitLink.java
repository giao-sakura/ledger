package com.example.ledger.data;

import androidx.room.Entity;

@Entity(tableName = "outfit_items", primaryKeys = {"outfitId", "itemId"})
public class OutfitLink {
    public long outfitId;
    public long itemId;
}
