package com.example.ledger.data;

public final class CategoryTotal {
    public final String key;
    public final long cents;

    public CategoryTotal(String key, long cents) {
        this.key = key;
        this.cents = cents;
    }
}
