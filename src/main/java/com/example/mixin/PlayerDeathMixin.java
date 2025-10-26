package com.example.mixin;

import com.example.HeartRegistry;
import com.example.PlayerDataManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerDeathMixin {
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        
        // Sadece sunucu tarafında çalışsın
        if (player.getEntityWorld().isClient()) return;
        
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        
        // Ölüm nedenini kontrol et
        if (shouldDropHeart(damageSource)) {
            // Kalp sayısını kontrol et
            int currentHearts = (int)(player.getMaxHealth() / 2);
            
            if (currentHearts > 1) {
                // Kalp sayısını 1 azalt
                player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH)
                    .setBaseValue(player.getMaxHealth() - 2.0);
                
                // PlayerDataManager'da da güncelle
                PlayerDataManager.setPlayerHeartCount(player.getUuid(), currentHearts - 1);
                
                // Özel nether yıldızı oluştur (kalp)
                ItemStack heartItem = new ItemStack(Items.NETHER_STAR);
                
                // Kalp item'ını oyuncunun öldüğü yere düşür
                BlockPos deathPos = player.getBlockPos();
                player.getEntityWorld().spawnEntity(
                    new net.minecraft.entity.ItemEntity(
                        player.getEntityWorld(),
                        deathPos.getX() + 0.5,
                        deathPos.getY() + 0.5,
                        deathPos.getZ() + 0.5,
                        heartItem
                    )
                );
                
                // HeartRegistry'ye ekle
                HeartRegistry.add(player.getUuid(), 1);
                
                // Oyuncuya mesaj gönder
                player.sendMessage(Text.literal("§c❤ Bir kalbiniz düştü! Kalan kalp sayınız: " + (currentHearts - 1)), true);
                
            } else if (currentHearts == 1) {
                // Son kalp ile öldürüldü - ban sistemi
                PlayerDataManager.banPlayer(serverPlayer);
                player.sendMessage(Text.literal("§c§lSon kalbinizle öldürüldünüz! Sunucudan banlandınız."), true);
            }
        }
    }
    
    private boolean shouldDropHeart(DamageSource damageSource) {
        // Sadece oyuncu tarafından öldürülme durumunda kalp düşsün
        if (damageSource.getAttacker() instanceof PlayerEntity) {
            return true;
        }
        
        // Diğer ölüm nedenleri için false döndür
        return false;
    }
}
