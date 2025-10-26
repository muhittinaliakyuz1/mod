package com.example;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandRegistry {
    private static final Set<String> immortalPlayers = new HashSet<>();

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 🔮 /immortalize <player> — oyuncuya ölümsüzlük ver/al
            dispatcher.register(CommandManager.literal("immortalize")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> {
                        ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();

                        // Yetki kontrolü
                        if (!"Kynexis_".equals(executor.getName().getString())) {
                            executor.sendMessage(Text.literal("§cBu komutu kullanamazsın!"), true);
                            return 0;
                        }

                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                        String name = target.getName().getString();

                        // Ölümsüzlük aç/kapat
                        if (immortalPlayers.contains(name)) {
                            immortalPlayers.remove(name);
                            target.getAbilities().invulnerable = false;
                            target.sendAbilitiesUpdate();
                            executor.sendMessage(Text.literal("§c" + name + " artık ölümsüz değil!"), true);
                            target.sendMessage(Text.literal("§cÖlümsüzlüğün kaldırıldı!"), true);
                        } else {
                            immortalPlayers.add(name);
                            target.getAbilities().invulnerable = true;
                            target.sendAbilitiesUpdate();
                            executor.sendMessage(Text.literal("§d" + name + " artık §5ölümsüz!"), true);
                            target.sendMessage(Text.literal("§dArtık §5ölümsüzsün!"), true);
                        }
                        return 1;
                    }))
            );

            // 📜 /immortallist — ölümsüz oyuncuları listeler
            dispatcher.register(CommandManager.literal("immortallist")
                .executes(context -> {
                    ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();

                    if (!"Kynexis_".equals(executor.getName().getString())) {
                        executor.sendMessage(Text.literal("§cBu komutu kullanamazsın!"), true);
                        return 0;
                    }

                    if (immortalPlayers.isEmpty()) {
                        executor.sendMessage(Text.literal("§7Şu anda ölümsüz oyuncu yok."), true);
                    } else {
                        executor.sendMessage(Text.literal("§dÖlümsüz Oyuncular: §f" + String.join(", ", immortalPlayers)), true);
                    }
                    return 1;
                })
            );

            // 🔢 /numpad — test komutu
            dispatcher.register(CommandManager.literal("numpad")
                .executes(context -> {
                    ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();

                    if (!"Kynexis_".equals(executor.getName().getString())) {
                        executor.sendMessage(Text.literal("§cBu komutu kullanamazsın!"), true);
                        return 0;
                    }

                    executor.sendMessage(Text.literal("§dNumpad ekranı açıldı! (F3+T tuşlarına basın)"), true);
                    return 1;
                })
            );

            // ❤️ /kalp ver [miktar] — kalp verir (azaltır ve item düşürür)
            dispatcher.register(CommandManager.literal("kalp")
                .then(CommandManager.literal("ver")
                    .executes(context -> giveHearts(context.getSource().getPlayerOrThrow(), 1))
                    .then(CommandManager.argument("miktar", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int miktar = IntegerArgumentType.getInteger(context, "miktar");
                            return giveHearts(context.getSource().getPlayerOrThrow(), miktar);
                        })
                    )
                )
            );
        });
    }

    /**
     * Oyuncunun kalbini düşürüp envanterine "❤ Kalp" itemi verir.
     */
    private static int giveHearts(ServerPlayerEntity executor, int amount) {
        UUID uuid = executor.getUuid();
        int heartCount = PlayerDataManager.getPlayerHeartCount(uuid);

        if (heartCount <= amount) {
            executor.sendMessage(Text.literal("§cYeterli kalbiniz yok! En az " + (amount + 1) + " kalp olmalı."), true);
            return 0;
        }

        // Kalp azalt
        PlayerDataManager.setPlayerHeartCount(uuid, heartCount - amount);

        // Max sağlık güncelle
        double newMaxHealth = (heartCount - amount) * 2.0;
        executor.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        executor.setHealth((float) Math.min(executor.getHealth(), newMaxHealth));

        // Kalp itemini oluştur ve ver
        for (int i = 0; i < amount; i++) {
            ItemStack heartItem = new ItemStack(Items.NETHER_STAR);
            heartItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§c ❤ Kalp"));
            executor.getInventory().offerOrDrop(heartItem);
        }

        executor.sendMessage(Text.literal("§c❤ " + amount + " kalbinizi verdiniz! Kalan kalp sayınız: " + (heartCount - amount)), true);
        return 1;
    }
}
