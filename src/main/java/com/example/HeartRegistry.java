package com.example;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class HeartRegistry {
    private static final Map<UUID, Integer> heartCredits = new HashMap<>();

    public static void init() {
        ExampleMod.LOGGER.info("Kalp sistemi başlatılıyor...");
    }

    public static boolean consume(UUID playerId) {
        int credits = heartCredits.getOrDefault(playerId, 0);
        if (credits > 0) {
            heartCredits.put(playerId, credits - 1);
            return true;
        }
        return false;
    }

    public static int giveHearts(ServerPlayerEntity player, int amount) {
        // Her kalp 2 can değerine eşit
        int currentHearts = PlayerDataManager.getPlayerHeartCount(player.getUuid());
        int newHearts = currentHearts + amount;
        if (newHearts < 1) newHearts = 1; // minimum 1 kalp

        PlayerDataManager.setPlayerHeartCount(player.getUuid(), newHearts);

        double newMaxHealth = newHearts * 2.0;
        try {
            var attr = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(newMaxHealth);
        } catch (Exception e) {
            // Fallback: sadece current health arttır
        }

        // Oyuncunun canını doldurmaya çalış
        player.setHealth((float)Math.min(player.getHealth() + (amount * 2), newMaxHealth));

        // Oyuncuya bilgi mesajı
        String message = amount > 0 ?
            "§d+" + amount + " kalp kazandın! §7(Toplam: §c" + newHearts + "§7)" :
            "§c" + Math.abs(amount) + " kalp kaybettin! §7(Toplam: §c" + newHearts + "§7)";
        player.sendMessage(Text.literal(message), true);

        return 1;
    }

    public static int removeHearts(ServerPlayerEntity player, int amount) {
        return giveHearts(player, -amount);
    }
}