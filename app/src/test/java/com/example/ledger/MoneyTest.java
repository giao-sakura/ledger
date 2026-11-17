package com.example.ledger;

import static org.junit.Assert.assertEquals;

import com.example.ledger.data.Money;
import com.example.ledger.data.Transaction;

import org.junit.Test;

public class MoneyTest {
    @Test
    public void parsesYuan() {
        assertEquals(123456L, Money.parseYuan("1234.56"));
        assertEquals(1000L, Money.parseYuan("10"));
        assertEquals(1050L, Money.parseYuan("10.5"));
        assertEquals(1000L, Money.parseYuan("10."));
        assertEquals(1L, Money.parseYuan("0.01"));
        assertEquals(100050L, Money.parseYuan("1,000.50"));
        assertEquals(100050L, Money.parseYuan("¥1,000.50"));
        assertEquals(1001L, Money.parseYuan("10.005"));
        assertEquals(-1L, Money.parseYuan("0"));
        assertEquals(-1L, Money.parseYuan(""));
        assertEquals(-1L, Money.parseYuan("abc"));
        assertEquals(-1L, Money.parseYuan("99999999.999"));
    }

    @Test
    public void formatsCents() {
        assertEquals("¥1,234.50", Money.format(123450L));
        assertEquals("¥0.00", Money.format(0L));
        assertEquals("\u2212¥12.30", Money.format(-1230L));
        assertEquals("+¥10.00", Money.formatChange(Transaction.INCOME, 1000L));
        assertEquals("\u2212¥10.00", Money.formatChange(Transaction.EXPENSE, 1000L));
    }

    @Test
    public void plainOmitsGrouping() {
        assertEquals("10.00", Money.plain(1000L));
        assertEquals("10.10", Money.plain(1010L));
        assertEquals("0.01", Money.plain(1L));
    }
}
