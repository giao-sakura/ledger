package com.example.ledger.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WardrobeDao {
    @Query("SELECT * FROM clothes ORDER BY createdAt DESC, id DESC")
    List<ClothingItem> clothes();

    @Insert
    long insert(ClothingItem item);

    @Update
    void update(ClothingItem item);

    @Delete
    void delete(ClothingItem item);

    @Query("DELETE FROM outfit_items WHERE itemId = :itemId")
    void clearItem(long itemId);

    @Transaction
    default void deleteClothing(ClothingItem item) {
        clearItem(item.id);
        delete(item);
    }

    @Query("SELECT * FROM outfits ORDER BY createdAt DESC, id DESC")
    List<Outfit> outfits();

    @Insert
    long insert(Outfit outfit);

    @Delete
    void delete(Outfit outfit);

    @Query("SELECT * FROM outfit_items")
    List<OutfitLink> links();

    @Insert
    void insert(OutfitLink link);

    @Query("DELETE FROM outfit_items WHERE outfitId = :outfitId")
    void clearOutfit(long outfitId);

    @Query("DELETE FROM outfit_plans WHERE outfitId = :outfitId")
    void deletePlansForOutfit(long outfitId);

    @Transaction
    default void deleteOutfitFully(Outfit outfit) {
        clearOutfit(outfit.id);
        deletePlansForOutfit(outfit.id);
        delete(outfit);
    }

    @Query("SELECT * FROM outfit_plans ORDER BY planDate ASC")
    List<OutfitPlan> plans();

    @Insert
    long insert(OutfitPlan plan);

    @Query("DELETE FROM outfit_plans WHERE planDate = :date")
    void deletePlanOn(String date);

    @Transaction
    default void replacePlan(OutfitPlan plan) {
        deletePlanOn(plan.planDate);
        insert(plan);
    }

    @Query("SELECT * FROM wishes ORDER BY createdAt DESC, id DESC")
    List<WishItem> wishes();

    @Insert
    long insert(WishItem item);

    @Delete
    void delete(WishItem item);
}
