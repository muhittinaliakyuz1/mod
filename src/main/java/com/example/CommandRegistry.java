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
import com.mojang.brigadier.suggestion.SuggestionProvider;

public class CommandRegistry {
    private static final Set<String> immortalPlayers = new HashSet<>();

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // immortalize <player>
            var immortalizeRoot = CommandManager.literal("immortalize").requires(src -> {
                try {
                    return src.getPlayerOrThrow().getName().getString().equals("Kynexis_");
                } catch (Exception ignored) {
                    return false;
                }
            });
            var immortalizeArg = CommandManager.argument("player", EntityArgumentType.player()).executes(context -> {
                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                if (!"Kynexis_".equals(executor.getName().getString())) {
                    executor.sendMessage(Text.literal("§cBu komutu kullanamazsın!"), true);
                    return 0;
                }
                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                String name = target.getName().getString();
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
                    target.sendMessage(Text.literal("§dArtık §5ölümsün!"), true);
                }
                return 1;
            });
            dispatcher.register(immortalizeRoot.then(immortalizeArg));

            // immortallist
            dispatcher.register(CommandManager.literal("immortallist").requires(src -> {
                try { return src.getPlayerOrThrow().getName().getString().equals("Kynexis_"); } catch (Exception ignored) { return false; }
            }).executes(context -> {
                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                if (immortalPlayers.isEmpty()) {
                    executor.sendMessage(Text.literal("§7Şu anda ölümsüz oyuncu yok."), true);
                } else {
                    executor.sendMessage(Text.literal("§dÖlümsüz Oyuncular: §f" + String.join(", ", immortalPlayers)), true);
                }
                return 1;
            }));

            // (numpad kaldırıldı) - Numpad ekranı artık kullanılmıyor; /code kullanın

            // kalp ver [miktar]
            dispatcher.register(CommandManager.literal("kalp").then(
                CommandManager.literal("ver").executes(context -> giveHearts(context.getSource().getPlayerOrThrow(), 1)).then(
                    CommandManager.argument("miktar", IntegerArgumentType.integer(1)).executes(context -> {
                        int miktar = IntegerArgumentType.getInteger(context, "miktar");
                        return giveHearts(context.getSource().getPlayerOrThrow(), miktar);
                    })
                )
            ));

            // /kalp al <oyuncu> <miktar>  - admin-only (Kynexis_)
            dispatcher.register(CommandManager.literal("kalp").then(
                CommandManager.literal("al").then(
                    CommandManager.argument("player", EntityArgumentType.player()).then(
                        CommandManager.argument("miktar", IntegerArgumentType.integer(1)).executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            if (!"Kynexis_".equals(executor.getName().getString())) {
                                executor.sendMessage(Text.literal("§cBu komutu sadece Kynexis_ kullanabilir."), true);
                                return 0;
                            }
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                            int miktar = IntegerArgumentType.getInteger(context, "miktar");
                            java.util.UUID tid = target.getUuid();
                            int current = PlayerDataManager.getPlayerHeartCount(tid);
                            int newVal = Math.max(0, current - miktar);
                            PlayerDataManager.setPlayerHeartCount(tid, newVal);
                            // Update attribute if possible
                            try {
                                var attr = target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
                                if (attr != null) attr.setBaseValue(newVal * 2.0);
                            } catch (Exception ignored) {}
                            executor.sendMessage(Text.literal("§aBaşarılı. " + target.getName().getString() + " artık " + newVal + " kalbe sahip."), true);
                            target.sendMessage(Text.literal("§cBir admin tarafından kalbiniz alındı. Kalan: " + newVal), true);
                            if (newVal <= 0) PlayerDataManager.banPlayer(target);
                            return 1;
                        })
                    )
                )
            ));

            // /kod (kök): sadece Kynexis_ görecek
            var kodRoot = CommandManager.literal("kod").requires(src -> {
                try { return src.getPlayerOrThrow().getName().getString().equals("Kynexis_"); } catch (Exception ignored) { return false; }
            });

            // /kod (no-arg) -> listele
            kodRoot.executes(context -> {
                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                var codes = CodeManager.getVisibleCodes(executor.getName().getString());
                if (codes.isEmpty()) {
                    executor.sendMessage(Text.literal("§7Aktif kod yok."), true);
                    return 1;
                }
                executor.sendMessage(Text.literal("§eAktif Kodlar:"), true);
                for (String c : codes) {
                    var effect = CodeManager.getEffect(c);
                    String label = effect != null ? mapEffectLabel(effect) : "Bilinmiyor";
                    executor.sendMessage(Text.literal("§6" + c + " §7(" + label + ")"), true);
                }
                return 1;
            });
            SuggestionProvider<net.minecraft.server.command.ServerCommandSource> codeSuggestions = (context, builder) -> {
                String viewer = null;
                try {
                    viewer = context.getSource().getPlayerOrThrow().getName().getString();
                } catch (Exception ignored) {}
                // Only suggest codes to Kynexis_ (per request)
                if (!"Kynexis_".equals(viewer)) return builder.buildFuture();
                for (String c : CodeManager.getVisibleCodes(viewer)) {
                    if (c.startsWith(builder.getRemaining())) builder.suggest(c);
                }
                // also suggest some example codes for Kynexis_
                if ("1234".startsWith(builder.getRemaining())) builder.suggest("1234");
                if ("0000".startsWith(builder.getRemaining())) builder.suggest("0000");
                return builder.buildFuture();
            };

            // /kod kullan <4haneli>
            dispatcher.register(kodRoot.then(
                CommandManager.literal("kullan").then(
                    CommandManager.argument("arg", com.mojang.brigadier.arguments.StringArgumentType.word()).suggests(codeSuggestions).executes(context -> {
                        ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                        String code = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "arg");
                        if (!(code.length() == 4 && code.matches("\\d{4}"))) {
                            executor.sendMessage(Text.literal("§cGeçersiz kod. 4 rakamlı bir kod girin."), true);
                            return 0;
                        }
                        // Map specific codes to effects
                        CodeManager.EffectType effect;
                        switch (code) {
                            case "0000" -> effect = CodeManager.EffectType.INVISIBILITY;
                            case "1111" -> effect = CodeManager.EffectType.STRENGTH;
                            case "2222" -> effect = CodeManager.EffectType.SPEED;
                            case "4444" -> effect = CodeManager.EffectType.FIRE_RESISTANCE;
                            default -> effect = CodeManager.EffectType.INVISIBILITY;
                        }
                        CodeManager.addCode(code, effect, executor.getName().getString(), true);
                        executor.sendMessage(Text.literal("§aKod etkinleştirildi: " + code + " (" + mapEffectLabel(effect) + ", gizli)"), true);
                        return 1;
                    })
                )
            ));

            // /kod iptal <4haneli>
            dispatcher.register(kodRoot.then(
                CommandManager.literal("iptal").then(
                    CommandManager.argument("k", com.mojang.brigadier.arguments.StringArgumentType.word()).suggests(codeSuggestions).executes(context -> {
                        ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                        String k = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "k");
                        if (!(k.length() == 4 && k.matches("\\d{4}"))) {
                            executor.sendMessage(Text.literal("§cGeçersiz kod."), true);
                            return 0;
                        }
                        if (CodeManager.removeCode(k)) {
                            executor.sendMessage(Text.literal("§aKod iptal edildi: " + k), true);
                            return 1;
                        } else {
                            executor.sendMessage(Text.literal("§cBöyle bir aktif kod yok: " + k), true);
                            return 0;
                        }
                    })
                )
            ));

            // /kod bilgi <4haneli>
            dispatcher.register(kodRoot.then(
                CommandManager.literal("bilgi").then(
                    CommandManager.argument("k2", com.mojang.brigadier.arguments.StringArgumentType.word()).suggests(codeSuggestions).executes(context -> {
                        ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                        String k2 = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "k2");
                        if (!(k2.length() == 4 && k2.matches("\\d{4}"))) {
                            executor.sendMessage(Text.literal("§cGeçersiz kod."), true);
                            return 0;
                        }
                        var effect = CodeManager.getEffect(k2);
                        if (effect != null) {
                            executor.sendMessage(Text.literal("§eKod: " + k2 + " -> " + mapEffectLabel(effect)), true);
                            return 1;
                        } else {
                            executor.sendMessage(Text.literal("§cBöyle bir aktif kod yok: " + k2), true);
                            return 0;
                        }
                    })
                )
            ));
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

    private static String mapEffectLabel(CodeManager.EffectType effect) {
        return switch (effect) {
            case INVISIBILITY -> "Görünmezlik";
            case STRENGTH -> "Güç";
            case SPEED -> "Hız";
            case FIRE_RESISTANCE -> "Ateş Direnci";
            case PREVENT_HUNGER -> "Açlığı Engelle";
        };
    }
}
