package com.example.mixin;

import com.example.PlayerDataManager;
import net.minecraft.entity.attribute.EntityAttributes;
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
        
        if (PlayerDataManager.isPlayerBanned(player.getUuid())) {
            player.networkHandler.disconnect(Text.literal("§cBanlısınız! Son kalbinizle öldürüldünüz."));
            return;
        }
        
        int heartCount = PlayerDataManager.getPlayerHeartCount(player.getUuid());
        if (heartCount != 10) {
            player.getAttributeInstance(EntityAttributes.MAX_HEALTH)
                .setBaseValue(heartCount * 2.0);
            player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        }
    }
}