package io.github.tofodroid.mods.mimi.common.item;

import java.util.List;

import javax.annotation.Nullable;

import io.github.tofodroid.mods.mimi.common.block.BlockInstrument;
import io.github.tofodroid.mods.mimi.util.InstrumentDataUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemInstrumentBlock extends BlockItem implements IInstrumentItem {
    public final String REGISTRY_NAME;

    public ItemInstrumentBlock(BlockInstrument blockIn, Properties builder, String name) {
        super(blockIn, builder);
        this.REGISTRY_NAME = name;
    }

    public Byte getInstrumentId() {
        return ((BlockInstrument)getBlock()).getInstrumentId();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);

        // Client-side only
        if(worldIn != null && worldIn.isClientSide) {
            InstrumentDataUtils.appendMidiSettingsTooltip(stack, tooltip);
        }
    }

    @Override
    public Boolean isDyeable() {
        return ((BlockInstrument)getBlock()).isDyeable();
    }

    @Override
    public Integer getDefaultColor() {
        return ((BlockInstrument)getBlock()).getDefaultColor();
    }

    @Override
    public String getDefaultChannels() {
        return ((BlockInstrument)getBlock()).getDefaultChannels();
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(washItem(context)) {
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
