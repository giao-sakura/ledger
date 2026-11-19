package com.example.ledger.wardrobe;

import com.example.ledger.data.ClothingItem;
import com.example.ledger.data.Outfit;
import com.example.ledger.data.OutfitPlan;

import java.util.List;

public final class WardrobeUi {
    public final List<ClothingItem> clothes;
    public final List<OutfitCard> outfits;
    public final List<com.example.ledger.data.WishItem> wishes;
    public final String city;
    public final boolean loaded;

    public WardrobeUi(List<ClothingItem> clothes, List<OutfitCard> outfits, List<com.example.ledger.data.WishItem> wishes, String city, boolean loaded) {
        this.clothes = clothes;
        this.outfits = outfits;
        this.wishes = wishes;
        this.city = city;
        this.loaded = loaded;
    }

    public static final class OutfitCard {
        public final Outfit outfit;
        public final List<ClothingItem> items;
        public final OutfitPlan plan;
        public final String weather;
        public final int fitLevel;

        public OutfitCard(Outfit outfit, List<ClothingItem> items, OutfitPlan plan, String weather, int fitLevel) {
            this.outfit = outfit;
            this.items = items;
            this.plan = plan;
            this.weather = weather;
            this.fitLevel = fitLevel;
        }

        public boolean hasWashing() {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).status == ClothingItem.WASHING) {
                    return true;
                }
            }
            return false;
        }
    }
}
