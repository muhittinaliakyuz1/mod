package com.example;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.text.Text;

public class HeartItemHandler {
    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            // Sadece sunucu tarafında işlem yap
            if (world.isClient()) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() == Items.NETHER_STAR) {
                // Özel isim kontrolü (isimde "❤" varsa)
                if (stack.getName() != null && stack.getName().getString().contains("❤")) {
                    if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

                    // Bir kalp ekle
                    java.util.UUID uuid = serverPlayer.getUuid();
                    int current = PlayerDataManager.getPlayerHeartCount(uuid);
                    PlayerDataManager.setPlayerHeartCount(uuid, current + 1);

                    // Max sağlığı güncelle (her kalp = 2 can)
                    try {
                        var attr = serverPlayer.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
                        if (attr != null) {
                            double newMax = (current + 1) * 2.0;
                            attr.setBaseValue(newMax);
                            float newHealth = (float) Math.min(serverPlayer.getHealth() + 2.0f, newMax);
                            serverPlayer.setHealth(newHealth);
                        }
                    } catch (Exception e) {
                        // Eğer attribute API farklıysa sadece canı arttır
                        serverPlayer.setHealth((float) Math.min(serverPlayer.getHealth() + 2.0f, serverPlayer.getMaxHealth() + 2.0f));
                    }

                    // Tüket
                    if (!serverPlayer.getAbilities().creativeMode) stack.decrement(1);

                    serverPlayer.sendMessage(Text.literal("§a❤ Kalp kullanıldı! Toplam kalp: " + PlayerDataManager.getPlayerHeartCount(uuid)), true);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }
}
