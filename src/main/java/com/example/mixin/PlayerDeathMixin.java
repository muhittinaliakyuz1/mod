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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerDeathMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // ✅ Doğru: 1.21.10'da PlayerEntity -> getEntityWorld() var
        World world = player.getEntityWorld();

        if (world.isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        UUID uuid = player.getUuid();
        int currentHearts = PlayerDataManager.getPlayerHeartCount(uuid);

        if (currentHearts > 1) {
            PlayerDataManager.setPlayerHeartCount(uuid, currentHearts - 1);

            double newMaxHealth = (currentHearts - 1) * 2.0;
            player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(newMaxHealth);

            ItemStack heartItem = new ItemStack(Items.NETHER_STAR);
            heartItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§c❤ Kalp"));

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
            PlayerDataManager.banPlayer(serverPlayer);
        }
    }
}
