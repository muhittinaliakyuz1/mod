package com.example.mixin;

import com.example.PlayerDataManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerDeathMixin {

    @Shadow
    public abstract World getEntityWorld();

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        World world = getEntityWorld();

        // 🌍 Sadece sunucu tarafında çalışsın
        if (world.isClient()) return;

        // 🔒 Sadece ServerPlayerEntity için
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        UUID uuid = player.getUuid();
        int currentHearts = PlayerDataManager.getPlayerHeartCount(uuid);

        // 💔 Ölünce kalp azaltma
        if (currentHearts > 1) {
            PlayerDataManager.setPlayerHeartCount(uuid, currentHearts - 1);

            // 🩸 Yeni max sağlık
            double newMaxHealth = (currentHearts - 1) * 2.0;
            player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(newMaxHealth);

            // ❤️ Kalp itemi oluştur
            ItemStack heartItem = new ItemStack(Items.NETHER_STAR);
            heartItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§c ❤ Kalp"));

            // 💎 Kalbi düşür (ölüm konumuna)
            BlockPos deathPos = player.getBlockPos();
            ItemEntity itemEntity = new ItemEntity(
                    world,
                    deathPos.getX() + 0.5,
                    deathPos.getY(),
                    deathPos.getZ() + 0.5,
                    heartItem
            );
            world.spawnEntity(itemEntity);

            player.sendMessage(Text.literal("§c❤ Bir kalbiniz düştü! Kalan kalp sayınız: " + (currentHearts - 1)), true);
        } else {
            // ☠️ Hiç kalp kalmadıysa oyuncuyu banla
            PlayerDataManager.banPlayer(serverPlayer);
        }
    }
}
