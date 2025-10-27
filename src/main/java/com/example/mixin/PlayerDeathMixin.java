package com.example.mixin;

import com.example.ExampleMod;
import com.example.PlayerDataManager;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerDeathMixin {

    @Inject(method = "onDeath", at = @At("RETURN"))
    private void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // ✅ Doğru: 1.21.10'da PlayerEntity -> getEntityWorld() var
        World world = player.getEntityWorld();

        if (world.isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        UUID uuid = player.getUuid();
        int currentHearts = PlayerDataManager.getPlayerHeartCount(uuid);

        // Determine attacker: only transfer on PvP kills. Ignore fall/mob deaths.
        net.minecraft.entity.Entity src = damageSource.getAttacker();
        ServerPlayerEntity killer = null;
        if (src instanceof ServerPlayerEntity sp) {
            killer = sp;
        } else if (src instanceof PersistentProjectileEntity proj && proj.getOwner() instanceof ServerPlayerEntity owner) {
            killer = owner;
        } else {
            // try damageSource.getSource() as a fallback (some damage types store the projectile in source)
            var dsSource = damageSource.getSource();
            if (dsSource instanceof PersistentProjectileEntity proj2 && proj2.getOwner() instanceof ServerPlayerEntity owner2) {
                killer = owner2;
            } else if (dsSource instanceof ServerPlayerEntity sp2) {
                killer = sp2;
            }
        }

        if (killer != null && !killer.getUuid().equals(uuid)) {
            // PvP kill: transfer one heart from victim to killer
            ExampleMod.LOGGER.info("PlayerDeathMixin: victim={} killer={}", uuid, killer.getUuid());
            int transfer = 1;
            int victimNew = Math.max(0, currentHearts - transfer);
            PlayerDataManager.setPlayerHeartCount(uuid, victimNew);

            // Give killer a heart
            java.util.UUID kId = killer.getUuid();
            int kHearts = PlayerDataManager.getPlayerHeartCount(kId);
            PlayerDataManager.setPlayerHeartCount(kId, kHearts + transfer);

            // Update attributes (use GENERIC_MAX_HEALTH)
            try {
                var victimAttr = serverPlayer.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (victimAttr != null) victimAttr.setBaseValue(victimNew * 2.0);
            } catch (Exception ignored) {}

            try {
                var killerAttr = killer.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (killerAttr != null) killerAttr.setBaseValue((kHearts + transfer) * 2.0);
            } catch (Exception ignored) {}

            // Notify players
            serverPlayer.sendMessage(Text.literal("§cBir kalbiniz yok oldu ve öldürüldünüz! Kalan kalp: " + victimNew), true);
            killer.sendMessage(Text.literal("§aBaşarılı öldürme! Bir kalp kazandınız. Toplam: " + PlayerDataManager.getPlayerHeartCount(kId)), true);

            // If victim reached 0 hearts, ban
            if (victimNew <= 0) {
                PlayerDataManager.banPlayer(serverPlayer);
            }
        }
    }
}
