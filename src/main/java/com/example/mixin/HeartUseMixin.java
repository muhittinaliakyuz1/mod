package com.example.mixin;

import com.example.PlayerDataManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public class HeartUseMixin {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onUseHeart(Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity player = (PlayerEntity)(Object)this;

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isOf(Items.NETHER_STAR) && stack.getName().getString().contains("❤ Kalp")) {

            // Sadece server tarafında
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

            UUID uuid = serverPlayer.getUuid();
            int currentHearts = PlayerDataManager.getPlayerHeartCount(uuid);

            PlayerDataManager.setPlayerHeartCount(uuid, currentHearts + 1);

            double newMaxHealth = (currentHearts + 1) * 2.0;
            serverPlayer.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(newMaxHealth);
            serverPlayer.setHealth((float)newMaxHealth);

            stack.decrement(1);

            serverPlayer.sendMessage(Text.literal("§a❤ Kalp kullanıldı! +1 kalp eklendi. Toplam: " + (currentHearts + 1)), true);

            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
