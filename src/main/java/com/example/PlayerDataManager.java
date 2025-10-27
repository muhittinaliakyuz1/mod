package com.example;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple, robust PlayerDataManager: keeps heart counts in memory and persists
 * them to a single NBT file under the server run directory.
 */
public class PlayerDataManager {
    private static final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private static Path dataPath = null;
    public static final int DEFAULT_HEARTS = 10;

    public static class PlayerData {
        public int heartCount;
        public boolean isBanned;
        public long banTime;
        public String lastKnownName;

        public PlayerData(int heartCount, boolean isBanned, long banTime, String lastKnownName) {
            this.heartCount = heartCount;
            this.isBanned = isBanned;
            this.banTime = banTime;
            this.lastKnownName = lastKnownName;
        }

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("heartCount", heartCount);
            nbt.putBoolean("isBanned", isBanned);
            nbt.putLong("banTime", banTime);
            if (lastKnownName != null) nbt.putString("lastKnownName", lastKnownName);
            return nbt;
        }

        public static PlayerData fromNbt(NbtCompound nbt) {
            if (nbt == null) return new PlayerData(DEFAULT_HEARTS, false, 0, null);
            int heart = safeGetInt(nbt, "heartCount", DEFAULT_HEARTS);
            boolean banned = safeGetBoolean(nbt, "isBanned", false);
            long banTime = safeGetLong(nbt, "banTime", 0L);
            String name = safeGetString(nbt, "lastKnownName", null);
            return new PlayerData(heart, banned, banTime, name);
        }
    }

    // Helpers for mappings where getX may return Optional<T> or raw T depending on mapping
    private static int safeGetInt(NbtCompound nbt, String key, int def) {
        try {
            java.lang.reflect.Method m = NbtCompound.class.getMethod("getInt", String.class);
            Object res = m.invoke(nbt, key);
            if (res instanceof Integer) return (Integer) res;
            if (res instanceof java.util.Optional) {
                java.util.Optional<?> o = (java.util.Optional<?>) res;
                if (o.isPresent()) return ((Number) o.get()).intValue();
            }
            // try orElse via reflection
            try {
                java.lang.reflect.Method orElse = res.getClass().getMethod("orElse", Object.class);
                Object v = orElse.invoke(res, def);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return def;
    }

    private static boolean safeGetBoolean(NbtCompound nbt, String key, boolean def) {
        try {
            java.lang.reflect.Method m = NbtCompound.class.getMethod("getBoolean", String.class);
            Object res = m.invoke(nbt, key);
            if (res instanceof Boolean) return (Boolean) res;
            if (res instanceof java.util.Optional) {
                java.util.Optional<?> o = (java.util.Optional<?>) res;
                if (o.isPresent()) return (Boolean) o.get();
            }
            try {
                java.lang.reflect.Method orElse = res.getClass().getMethod("orElse", Object.class);
                Object v = orElse.invoke(res, def);
                if (v instanceof Boolean) return (Boolean) v;
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return def;
    }

    private static long safeGetLong(NbtCompound nbt, String key, long def) {
        try {
            java.lang.reflect.Method m = NbtCompound.class.getMethod("getLong", String.class);
            Object res = m.invoke(nbt, key);
            if (res instanceof Long) return (Long) res;
            if (res instanceof java.util.Optional) {
                java.util.Optional<?> o = (java.util.Optional<?>) res;
                if (o.isPresent()) return ((Number) o.get()).longValue();
            }
            try {
                java.lang.reflect.Method orElse = res.getClass().getMethod("orElse", Object.class);
                Object v = orElse.invoke(res, def);
                if (v instanceof Number) return ((Number) v).longValue();
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return def;
    }

    private static String safeGetString(NbtCompound nbt, String key, String def) {
        try {
            java.lang.reflect.Method m = NbtCompound.class.getMethod("getString", String.class);
            Object res = m.invoke(nbt, key);
            if (res instanceof String) return (String) res;
            if (res instanceof java.util.Optional) {
                java.util.Optional<?> o = (java.util.Optional<?>) res;
                if (o.isPresent()) return (String) o.get();
            }
            try {
                java.lang.reflect.Method orElse = res.getClass().getMethod("orElse", Object.class);
                Object v = orElse.invoke(res, def);
                if (v instanceof String) return (String) v;
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return def;
    }

    public static void initialize(MinecraftServer server) {
        try {
            dataPath = server.getRunDirectory().resolve("player_data.nbt");
            loadData();
        } catch (Exception e) {
            ExampleMod.LOGGER.error("PlayerDataManager initialize hata: ", e);
        }
    }

    private static void loadData() {
        if (dataPath == null || !dataPath.toFile().exists()) return;
        try {
            NbtCompound root = NbtIo.read(dataPath);
            if (root == null) return;
            for (String key : root.getKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    java.util.Optional<NbtCompound> maybe = root.getCompound(key);
                    NbtCompound pd = maybe.orElse(null);
                    playerData.put(id, PlayerData.fromNbt(pd));
                } catch (IllegalArgumentException ex) {
                    ExampleMod.LOGGER.warn("Geçersiz UUID kaydı atlandı: {}", key);
                }
            }
            ExampleMod.LOGGER.info("PlayerDataManager: {} oyuncu yüklendi.", playerData.size());
        } catch (IOException e) {
            ExampleMod.LOGGER.error("PlayerDataManager yükleme hatası:", e);
        }
    }

    public static void saveData() {
        if (dataPath == null) return;
        try {
            NbtCompound root = new NbtCompound();
            for (Map.Entry<UUID, PlayerData> e : playerData.entrySet()) {
                root.put(e.getKey().toString(), e.getValue().toNbt());
            }
            NbtIo.write(root, dataPath);
        } catch (IOException e) {
            ExampleMod.LOGGER.error("PlayerDataManager kaydetme hatası:", e);
        }
    }

    public static PlayerData getPlayerData(UUID id) {
        return playerData.getOrDefault(id, new PlayerData(DEFAULT_HEARTS, false, 0, null));
    }

    public static int getPlayerHeartCount(UUID id) {
        return getPlayerData(id).heartCount;
    }

    public static void setPlayerHeartCount(UUID id, int hearts) {
        PlayerData pd = playerData.getOrDefault(id, new PlayerData(DEFAULT_HEARTS, false, 0, null));
        pd.heartCount = hearts;
        playerData.put(id, pd);
        saveData();
    }

    public static void setPlayerData(UUID id, PlayerData data) {
        playerData.put(id, data);
        saveData();
    }

    /** Reset all known players to default heart count. Only changes in-memory and persists. */
    public static void resetAllToDefault() {
        for (Map.Entry<UUID, PlayerData> e : playerData.entrySet()) {
            e.getValue().heartCount = DEFAULT_HEARTS;
        }
        saveData();
    }

    public static void banPlayer(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        PlayerData pd = getPlayerData(id);
        pd.heartCount = 0;
        pd.isBanned = true;
        pd.banTime = System.currentTimeMillis();
        pd.lastKnownName = player.getName().getString();
        setPlayerData(id, pd);
        addToReviveList(id, pd.lastKnownName);
        try {
            player.networkHandler.disconnect(Text.literal("§cSon kalbinizle öldünüz; sunucudan atıldınız."));
        } catch (Exception e) {
            ExampleMod.LOGGER.warn("Ban sırasında disconnect başarısız: ", e);
        }
    }

    // Revive list management
    private static final java.util.Set<UUID> reviveList = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public static void addToReviveList(UUID id, String name) {
        reviveList.add(id);
        PlayerData pd = playerData.getOrDefault(id, new PlayerData(DEFAULT_HEARTS, true, System.currentTimeMillis(), name));
        if (name != null) pd.lastKnownName = name;
        playerData.put(id, pd);
        saveData();
    }

    public static void removeFromReviveList(UUID id) {
        reviveList.remove(id);
    }

    public static java.util.List<String> getReviveNames() {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (UUID id : reviveList) {
            PlayerData pd = playerData.get(id);
            if (pd != null && pd.lastKnownName != null) names.add(pd.lastKnownName);
            else names.add(id.toString());
        }
        return names;
    }

    public static boolean isInReviveList(UUID id) {
        return reviveList.contains(id);
    }

    public static boolean isPlayerBanned(UUID id) {
        return getPlayerData(id).isBanned;
    }

    public static void unbanPlayer(UUID id) {
        PlayerData pd = getPlayerData(id);
        pd.isBanned = false;
        pd.heartCount = 10;
        pd.banTime = 0;
        setPlayerData(id, pd);
    }
}