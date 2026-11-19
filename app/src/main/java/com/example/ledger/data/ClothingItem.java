package com.example.ledger.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clothes")
public class ClothingItem {
    public static final int READY = 0;
    public static final int LAUNDRY = 1;
    public static final int WASHING = 2;
    public static final int BACK = 3;

    public static final int SPRING = 1;
    public static final int SUMMER = 2;
    public static final int AUTUMN = 4;
    public static final int WINTER = 8;

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String imagePath;
    public String category;
    public String color;
    public int seasons;
    public String style;
    public int status;
    public String note;
    public long createdAt;

    public boolean wearable() {
        return status != WASHING;
    }

    public ClothingItem copy() {
        ClothingItem copy = new ClothingItem();
        copy.id = id;
        copy.imagePath = imagePath;
        copy.category = category;
        copy.color = color;
        copy.seasons = seasons;
        copy.style = style;
        copy.status = status;
        copy.note = note;
        copy.createdAt = createdAt;
        return copy;
    }
}
