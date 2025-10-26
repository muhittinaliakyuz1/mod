package com.example;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private static File dataFile;

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
            if (nbt == null) {
                return new PlayerData(10, false, 0);
            }

            // Handle Optional return types for Fabric environment
            int heart = nbt.getInt("heartCount").orElse(10);
            boolean banned = nbt.getBoolean("isBanned").orElse(false);
            long banTime = nbt.getLong("banTime").orElse(0L);

            return new PlayerData(heart, banned, banTime);
        }
    }

    public static void initialize(MinecraftServer server) {
        dataFile = server.getRunDirectory().resolve("player_data.nbt").toFile();
        loadData();
    }

    private static void loadData() {
        if (dataFile == null || !dataFile.exists()) return;

        try {
            // Correct for Fabric 1.21.1: NbtIo.read returns NbtCompound
            NbtCompound root = NbtIo.read(dataFile.toPath());
            if (root == null) return;

            for (String key : root.getKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    // Handle Optional<NbtCompound> return type
                    NbtCompound playerNbt = root.getCompound(key).orElse(null);
                    PlayerData data = PlayerData.fromNbt(playerNbt);
                    playerData.put(playerId, data);
                } catch (IllegalArgumentException e) {
                    System.err.println("Geçersiz UUID formatı: " + key + ", hata: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Player data yüklenirken hata: " + e.getMessage());
        }
    }

    public static void saveData() {
        if (dataFile == null) return;

        try {
            NbtCompound root = new NbtCompound();
            for (Map.Entry<UUID, PlayerData> entry : playerData.entrySet()) {
                root.put(entry.getKey().toString(), entry.getValue().toNbt());
            }
            NbtIo.write(root, dataFile.toPath());
        } catch (IOException e) {
            System.err.println("Player data kaydedilirken hata: " + e.getMessage());
        }
    }

    public static PlayerData getPlayerData(UUID playerId) {
        return playerData.getOrDefault(playerId, new PlayerData(10, false, 0));
    }

    public static void setPlayerData(UUID playerId, PlayerData data) {
        playerData.put(playerId, data);
        saveData();
    }

    public static void banPlayer(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        PlayerData data = getPlayerData(playerId);

        data.heartCount = 0;
        data.isBanned = true;
        data.banTime = System.currentTimeMillis();

        setPlayerData(playerId, data);

        player.networkHandler.disconnect(Text.literal("§cSon kalbinizle öldürüldünüz! Sunucudan banlandınız."));
    }

    public static void unbanPlayer(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        data.isBanned = false;
        data.heartCount = 10;
        data.banTime = 0;
        setPlayerData(playerId, data);
    }

    public static boolean isPlayerBanned(UUID playerId) {
        return getPlayerData(playerId).isBanned;
    }

    public static int getPlayerHeartCount(UUID playerId) {
        return getPlayerData(playerId).heartCount;
    }

    public static void setPlayerHeartCount(UUID playerId, int heartCount) {
        PlayerData data = getPlayerData(playerId);
        data.heartCount = heartCount;
        setPlayerData(playerId, data);
    }
}