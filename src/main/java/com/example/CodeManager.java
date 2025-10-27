package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CodeManager {
    public enum EffectType { INVISIBILITY, STRENGTH, PREVENT_HUNGER }

    private static final Map<String, EffectType> activeCodes = new ConcurrentHashMap<>();

    public static void register(MinecraftServer server) {
        // Server tick: maintain hunger prevention and ensure status effects persist
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
                // Only apply to players who have any active code (we assume Kynexis_ uses codes)
                for (Map.Entry<String, EffectType> e : activeCodes.entrySet()) {
                    EffectType type = e.getValue();
                    switch (type) {
                        case PREVENT_HUNGER -> {
                            try {
                                // keep food level full
                                player.getHungerManager().setFoodLevel(20);
                                player.getHungerManager().setSaturationLevel(20.0F);
                            } catch (Exception ignored) {}
                        }
                        case INVISIBILITY -> {
                            try {
                                if (!player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, true, false, false));
                                }
                            } catch (Exception ignored) {}
                        }
                        case STRENGTH -> {
                            try {
                                if (!player.hasStatusEffect(StatusEffects.STRENGTH)) {
                                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 2, true, false, false));
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        });
    }

    public static boolean addCode(String code, EffectType type) {
        if (code == null || code.length() != 4) return false;
        activeCodes.put(code, type);
        return true;
    }

    public static boolean removeCode(String code) {
        return activeCodes.remove(code) != null;
    }

    public static Set<String> getActiveCodes() {
        return activeCodes.keySet();
    }

    public static EffectType getEffect(String code) {
        return activeCodes.get(code);
    }
}
