package com.example.mixin;

import com.example.PlayerDataManager;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public class HeartUseMixin {

    /**
     * Injects into ServerPlayerEntity's interactItem method to handle custom heart item usage.
     */
    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void onUseHeart(Hand hand, CallbackInfoReturnable<com.example.mixin.TypedActionResult<ItemStack>> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        ItemStack stack = player.getStackInHand(hand);
        String stackName = stack.getName() != null ? stack.getName().getString() : "";
        if (stack.isOf(Items.NETHER_STAR) && (stackName.contains("❤") || stackName.contains("♥") || stackName.equals("♥ Kalp") || stackName.equals("❤ Kalp"))) {
            UUID uuid = player.getUuid();
            int currentHearts = PlayerDataManager.getPlayerHeartCount(uuid);

            // Increment heart count
            PlayerDataManager.setPlayerHeartCount(uuid, currentHearts + 1);

            // Update max health (each heart = 2 health points)
            EntityAttributeInstance maxHealthAttribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            if (maxHealthAttribute != null) {
                double newMaxHealth = (currentHearts + 1) * 2.0;
                maxHealthAttribute.setBaseValue(newMaxHealth);
                player.setHealth((float) newMaxHealth);
            } else {
                player.sendMessage(Text.literal("§cError: Could not update max health!"), true);
            }

            // Consume the item
            stack.decrement(1);

            // Send feedback to player
            player.sendMessage(Text.literal("§a♥ Kalp kullanıldı! +1 kalp eklendi. Toplam: " + (currentHearts + 1)), true);

            // Return success to cancel default item usage
            cir.setReturnValue(com.example.mixin.TypedActionResult.success(stack));
        }
    }
}
