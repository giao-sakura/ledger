package com.example.ledger.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "outfit_plans", indices = {@Index(value = "planDate", unique = true)})
public class OutfitPlan {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long outfitId;
    public String planDate;
}
