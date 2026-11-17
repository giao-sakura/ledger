package com.example.ledger.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Category {
    private static final int[] CUSTOM_COLORS = new int[]{
            0xFFB45309,
            0xFF0369A1,
            0xFF6D28D9,
            0xFFBE185D,
            0xFF0F766E,
            0xFF9F1239,
            0xFF3F6212,
            0xFF1D4ED8
    };

    private static final Category[] EXPENSE = new Category[]{
            new Category("food", "餐饮", Transaction.EXPENSE, 0xFFC4622D, "餐", false),
            new Category("transit", "交通", Transaction.EXPENSE, 0xFF3D6F99, "行", false),
            new Category("shopping", "购物", Transaction.EXPENSE, 0xFFA85D86, "购", false),
            new Category("home", "居住", Transaction.EXPENSE, 0xFF5E7A45, "居", false),
            new Category("fun", "娱乐", Transaction.EXPENSE, 0xFFC45C5C, "娱", false),
            new Category("medical", "医疗", Transaction.EXPENSE, 0xFF3E8E86, "医", false),
            new Category("education", "教育", Transaction.EXPENSE, 0xFF8A7040, "学", false),
            new Category("other", "其他", Transaction.EXPENSE, 0xFF7A736C, "其", false)
    };

    private static final Category[] INCOME = new Category[]{
            new Category("salary", "工资", Transaction.INCOME, 0xFF2F7D52, "薪", false),
            new Category("bonus", "奖金", Transaction.INCOME, 0xFF8C6A1E, "奖", false),
            new Category("invest", "理财", Transaction.INCOME, 0xFF2A8A84, "理", false),
            new Category("gift", "红包", Transaction.INCOME, 0xFFC4543C, "红", false),
            new Category("other_income", "其他", Transaction.INCOME, 0xFF7A736C, "其", false)
    };

    public final String key;
    public final String name;
    public final int type;
    public final int color;
    public final String mark;
    public final boolean custom;

    public Category(String key, String name, int type, int color, String mark, boolean custom) {
        this.key = key;
        this.name = name;
        this.type = type;
        this.color = color;
        this.mark = mark;
        this.custom = custom;
    }

    public static List<Category> builtins() {
        List<Category> all = new ArrayList<>(EXPENSE.length + INCOME.length);
        Collections.addAll(all, EXPENSE);
        Collections.addAll(all, INCOME);
        return all;
    }

    public static List<Category> ofType(List<Category> all, int type) {
        List<Category> result = new ArrayList<>();
        if (all == null) {
            return result;
        }
        for (int i = 0; i < all.size(); i++) {
            Category category = all.get(i);
            if (category.type == type) {
                result.add(category);
            }
        }
        return result;
    }

    public static Category find(List<Category> all, String key) {
        if (key != null && all != null) {
            for (int i = 0; i < all.size(); i++) {
                Category category = all.get(i);
                if (key.equals(category.key)) {
                    return category;
                }
            }
        }
        if (key != null) {
            for (Category category : EXPENSE) {
                if (category.key.equals(key)) {
                    return category;
                }
            }
            for (Category category : INCOME) {
                if (category.key.equals(key)) {
                    return category;
                }
            }
        }
        return EXPENSE[EXPENSE.length - 1];
    }

    public static Category custom(int type, String name, int customCount) {
        int color = CUSTOM_COLORS[Math.floorMod(customCount, CUSTOM_COLORS.length)];
        String mark = name.substring(0, name.offsetByCodePoints(0, 1));
        String key = "c" + System.nanoTime();
        return new Category(key, name, type, color, mark, true);
    }
}
