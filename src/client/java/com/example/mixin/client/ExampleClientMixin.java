package com.example.mixin.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import com.example.HeartRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class ExampleClientMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUseItem(World world, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        ItemStack stack = player.getStackInHand(hand);

        if (stack.getItem() == Items.NETHER_STAR) {
            // Sunucudan /kalp ver komutuyla verilen haklar HeartRegistry'de tutuluyor.
            // Eğer oyuncunun kullanılmamış bir hakkı varsa, kullan ve itemi azalt.
            if (!world.isClient()) {
                if (HeartRegistry.consume(player.getUuid())) {
                    double currentMax = player.getMaxHealth();
                    double newMaxHealth = currentMax + 2.0;
                    player.getAbilities().setWalkSpeed(0.1f); // Can güncellemesi için gerekli
                    player.setHealth(Math.min(player.getHealth() + 2.0f, (float)newMaxHealth));
                    player.sendMessage(Text.literal("§a+1 kalp eklendi! Yeni maksimum can: " + (int)((currentMax + 2.0)/2) + " kalp"), true);
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                }
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
