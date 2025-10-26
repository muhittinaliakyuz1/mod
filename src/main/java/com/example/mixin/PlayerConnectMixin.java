package com.example.mixin;

import com.example.PlayerDataManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class PlayerConnectMixin {
    
    @Inject(method = "onSpawn", at = @At("HEAD"))
    private void onPlayerConnect(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        
        // Oyuncu banlı mı kontrol et
        if (PlayerDataManager.isPlayerBanned(player.getUuid())) {
            player.networkHandler.disconnect(Text.literal("§cBanlısınız! Son kalbinizle öldürüldünüz."));
            return;
        }
        
        // Oyuncunun kalp sayısını PlayerDataManager'dan al ve ayarla
        int heartCount = PlayerDataManager.getPlayerHeartCount(player.getUuid());
        if (heartCount != 10) { // Default değilse
            player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH)
                .setBaseValue(heartCount * 2.0); // Kalp sayısı * 2 = health
            player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        }
    }
}
