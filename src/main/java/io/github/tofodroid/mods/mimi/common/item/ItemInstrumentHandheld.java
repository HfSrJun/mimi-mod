package io.github.tofodroid.mods.mimi.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.UUID;

import javax.annotation.Nonnull;

import io.github.tofodroid.mods.mimi.client.gui.ClientGuiWrapper;
import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.github.tofodroid.mods.mimi.util.InstrumentDataUtils;

public class ItemInstrumentHandheld extends Item implements IInstrumentItem {
    public final String REGISTRY_NAME;
    protected final Byte instrumentId;
    protected final Boolean dyeable;
    protected final Integer defaultColor;

    public ItemInstrumentHandheld(String name, Byte instrumentId, Boolean dyeable, Integer defaultColor) {
        super(new Properties().stacksTo(1));
        this.REGISTRY_NAME = name;
        this.instrumentId = instrumentId;
        this.dyeable = dyeable;
        this.defaultColor = defaultColor;
    }

    @Override
    public Boolean isDyeable() {
        return this.dyeable;

    }

    @Override
    public Integer getDefaultColor() {
        return this.defaultColor;
    }

    @Override
    public Byte getInstrumentId() {
        return this.instrumentId;
    }

    @Override
    @Nonnull
    public InteractionResult useOn(UseOnContext context) {
        if(washItem(context)) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        if(worldIn.isClientSide && !playerIn.isCrouching()) {
            ClientGuiWrapper.openInstrumentGui(worldIn, playerIn, handIn);
		    return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
        }

		return new InteractionResultHolder<>(InteractionResult.PASS, playerIn.getItemInHand(handIn));
    }

    @Override
    public void verifyTagAfterLoad(CompoundTag tag) {
        if(tag == null) return;

        if(tag.contains("Items")) {
            ListTag listtag = tag.getList("Items", 10);

            if(listtag.size() > 0) {
                for(int i = 0; i < listtag.size(); ++i) {
                    CompoundTag stackTag = listtag.getCompound(i);
                    String itemId = stackTag.getString("id");

                    if(itemId.equalsIgnoreCase("mimi:switchboard") && stackTag.contains("tag", 10)) {
                        MIMIMod.LOGGER.info("Converting TileInstrument from Switchboard.");
                        tag.merge(stackTag);
                        tag.remove("Items");
                    }
                }
            }
        }
    }

    public static ItemStack getEntityHeldInstrumentStack(LivingEntity entity, InteractionHand handIn) {
        ItemStack heldStack = entity.getItemInHand(handIn);

        if(heldStack != null && heldStack.getItem() instanceof ItemInstrumentHandheld) {
            return heldStack;
        }

        return null;
    }
    
    public static Boolean isEntityHoldingInstrument(LivingEntity entity) {
        return getEntityHeldInstrumentStack(entity, InteractionHand.MAIN_HAND) != null 
            || getEntityHeldInstrumentStack(entity, InteractionHand.OFF_HAND) != null;
    }

    public static Byte getEntityHeldInstrumentId(LivingEntity entity, InteractionHand handIn) {
        ItemStack instrumentStack = getEntityHeldInstrumentStack(entity, handIn);

        if(instrumentStack != null) {
            return ((ItemInstrumentHandheld)instrumentStack.getItem()).getInstrumentId();
        }

        return null;
    }
    
    public static Boolean shouldHandleMessage(ItemStack stack, UUID sender, Byte channel, Boolean publicTransmit) {
        return stack.getItem() instanceof IInstrumentItem && InstrumentDataUtils.isChannelEnabled(stack, channel) && 
            ( 
                (publicTransmit && InstrumentDataUtils.PUBLIC_SOURCE_ID.equals(InstrumentDataUtils.getMidiSource(stack))) 
                || (sender != null && sender.equals(InstrumentDataUtils.getMidiSource(stack)))
            );
    }
}
