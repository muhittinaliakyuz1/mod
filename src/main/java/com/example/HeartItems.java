package com.example;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class HeartItems {
    // Use vanilla Nether Star as a safe fallback visual for the heart item.
    public static final Item HEART_ITEM = Items.NETHER_STAR;

    public static void register() {
        // No-op: registration is not required when using a vanilla item fallback.
    }
}
