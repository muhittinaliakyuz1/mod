package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CodeManager {
    public enum EffectType { INVISIBILITY, STRENGTH, SPEED, FIRE_RESISTANCE, PREVENT_HUNGER }

    private static final Map<String, CodeEntry> activeCodes = new ConcurrentHashMap<>();

    private static class CodeEntry {
        final EffectType effect;
        final String ownerName; // player name who created it
        final boolean isPrivate; // if true, only owner sees it
        final boolean showParticles; // whether status effect particles are shown

        CodeEntry(EffectType effect, String ownerName, boolean isPrivate, boolean showParticles) {
            this.effect = effect;
            this.ownerName = ownerName;
            this.isPrivate = isPrivate;
            this.showParticles = showParticles;
        }
    }

    public static void register(MinecraftServer server) {
        // Server tick: maintain hunger prevention and ensure status effects persist
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            for (Map.Entry<String, CodeEntry> e : activeCodes.entrySet()) {
                CodeEntry entry = e.getValue();
                // find the player by name (owner) and apply effect only to them
                for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
                    if (!player.getName().getString().equals(entry.ownerName)) continue;
                    switch (entry.effect) {
                        case PREVENT_HUNGER -> {
                            try {
                                player.getHungerManager().setFoodLevel(20);
                                player.getHungerManager().setSaturationLevel(20.0F);
                            } catch (Exception ignored) {}
                        }
                        case INVISIBILITY -> {
                            try {
                                if (!player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, true, !entry.showParticles, false));
                                }
                            } catch (Exception ignored) {}
                        }
                        case STRENGTH -> {
                            try {
                                if (!player.hasStatusEffect(StatusEffects.STRENGTH)) {
                                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, true, !entry.showParticles, false));
                                }
                            } catch (Exception ignored) {}
                        }
                        case SPEED -> {
                            try {
                                if (!player.hasStatusEffect(StatusEffects.SPEED)) {
                                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 1, true, !entry.showParticles, false));
                                }
                            } catch (Exception ignored) {}
                        }
                        case FIRE_RESISTANCE -> {
                            try {
                                if (!player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, !entry.showParticles, false));
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        });
    }

    public static boolean addCode(String code, EffectType type, String ownerName, boolean isPrivate) {
        // default: showParticles = true
        return addCode(code, type, ownerName, isPrivate, true);
    }

    public static boolean addCode(String code, EffectType type, String ownerName, boolean isPrivate, boolean showParticles) {
        if (code == null || code.length() != 4) return false;
        activeCodes.put(code, new CodeEntry(type, ownerName == null ? "" : ownerName, isPrivate, showParticles));
        return true;
    }

    public static boolean removeCode(String code) {
        return activeCodes.remove(code) != null;
    }

    /**
     * Remove the code and clear its effect from the owner if they are online.
     */
    public static boolean removeCode(MinecraftServer server, String code) {
        CodeEntry removed = activeCodes.remove(code);
        if (removed == null) return false;

        // Clear status effect from owner player(s)
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.getName().getString().equals(removed.ownerName)) continue;
            try {
                switch (removed.effect) {
                    case INVISIBILITY -> player.removeStatusEffect(StatusEffects.INVISIBILITY);
                    case STRENGTH -> player.removeStatusEffect(StatusEffects.STRENGTH);
                    case SPEED -> player.removeStatusEffect(StatusEffects.SPEED);
                    case FIRE_RESISTANCE -> player.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
                    case PREVENT_HUNGER -> {
                        // no status effect to remove; we set hunger directly each tick
                    }
                }
            } catch (Exception ignored) {}
        }
        return true;
    }

    /** Returns all codes visible to the given viewer (player name). */
    public static Set<String> getVisibleCodes(String viewerName) {
        return activeCodes.entrySet().stream()
                .filter(e -> {
                    CodeEntry ce = e.getValue();
                    if (!ce.isPrivate) return true;
                    if (viewerName == null) return false;
                    return viewerName.equals(ce.ownerName);
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static Set<String> getActiveCodes() {
        return activeCodes.keySet();
    }

    public static EffectType getEffect(String code) {
        CodeEntry e = activeCodes.get(code);
        return e == null ? null : e.effect;
    }
}
