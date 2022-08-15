package io.github.tofodroid.mods.mimi.common.item;

import javax.annotation.Nonnull;

import io.github.tofodroid.mods.mimi.client.gui.ClientGuiWrapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemFileCaster extends Item {
    public static final String REGISTRY_NAME = "filecaster";

    public ItemFileCaster() {
        super(new Properties().tab(ModItems.ITEM_GROUP).stacksTo(1));
    }
    
    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        final ItemStack heldItem = playerIn.getItemInHand(handIn);

        if(worldIn.isClientSide && !playerIn.isCrouching()) {
            ClientGuiWrapper.openPlaylistGui(
                worldIn, 
                playerIn, 
                InteractionHand.MAIN_HAND.equals(handIn) ? playerIn.getInventory().selected : Inventory.SLOT_OFFHAND
            );
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, heldItem);
        }

        return new InteractionResultHolder<>(InteractionResult.PASS, heldItem);
    }
}
