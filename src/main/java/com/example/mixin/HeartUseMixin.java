package com.example.mixin;

import com.example.PlayerDataManager;
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

@Mixin(ServerPlayerEntity.class)
public class HeartUseMixin {

    @Inject(method = "method_7907", at = @At("HEAD"), cancellable = true) // method_7907 = useItem/interactItem
    private void onUseHeart(Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isOf(Items.NETHER_STAR) && stack.getName().getString().contains("❤ Kalp")) {

            UUID uuid = player.getUuid();
            int currentHearts = PlayerDataManager.getPlayerHeartCount(uuid);

            PlayerDataManager.setPlayerHeartCount(uuid, currentHearts + 1);

            double newMaxHealth = (currentHearts + 1) * 2.0;
            player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(newMaxHealth);
            player.setHealth((float)newMaxHealth);

            stack.decrement(1);

            player.sendMessage(Text.literal("§a❤ Kalp kullanıldı! +1 kalp eklendi. Toplam: " + (currentHearts + 1)), true);

            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
