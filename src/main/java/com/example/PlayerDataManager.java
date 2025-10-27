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

    public static class PlayerData {
        public int heartCount;
        public boolean isBanned;
        public long banTime;

        public PlayerData(int heartCount, boolean isBanned, long banTime) {
            this.heartCount = heartCount;
            this.isBanned = isBanned;
            this.banTime = banTime;
        }

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("heartCount", heartCount);
            nbt.putBoolean("isBanned", isBanned);
            nbt.putLong("banTime", banTime);
            return nbt;
        }

        public static PlayerData fromNbt(NbtCompound nbt) {
            if (nbt == null) return new PlayerData(10, false, 0);
            int heart = nbt.getInt("heartCount").orElse(10);
            boolean banned = nbt.getBoolean("isBanned").orElse(false);
            long banTime = nbt.getLong("banTime").orElse(0L);
            return new PlayerData(heart, banned, banTime);
        }
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
        return playerData.getOrDefault(id, new PlayerData(10, false, 0));
    }

    public static int getPlayerHeartCount(UUID id) {
        return getPlayerData(id).heartCount;
    }

    public static void setPlayerHeartCount(UUID id, int hearts) {
        PlayerData pd = playerData.getOrDefault(id, new PlayerData(10, false, 0));
        pd.heartCount = hearts;
        playerData.put(id, pd);
        saveData();
    }

    public static void setPlayerData(UUID id, PlayerData data) {
        playerData.put(id, data);
        saveData();
    }

    public static void banPlayer(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        PlayerData pd = getPlayerData(id);
        pd.heartCount = 0;
        pd.isBanned = true;
        pd.banTime = System.currentTimeMillis();
        setPlayerData(id, pd);
        try {
            player.networkHandler.disconnect(Text.literal("§cSon kalbinizle öldünüz; sunucudan atıldınız."));
        } catch (Exception e) {
            ExampleMod.LOGGER.warn("Ban sırasında disconnect başarısız: ", e);
        }
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