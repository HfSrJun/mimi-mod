package io.github.tofodroid.mods.mimi.common.tile;

import javax.annotation.Nullable;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.github.tofodroid.mods.mimi.common.block.BlockInstrument;
import io.github.tofodroid.mods.mimi.common.item.IDyeableItem;
import io.github.tofodroid.mods.mimi.common.item.IInstrumentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class TileInstrument extends AInventoryTile {
    public static final String COLOR_TAG = "color";
    protected Integer color;

    public TileInstrument(BlockPos pos, BlockState state) {
        super(ModTiles.INSTRUMENT, pos, state, 1);
        items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    }

    public void setInstrumentStack(ItemStack stack) {
        if(stack.getItem() instanceof IInstrumentItem) {
            this.setItem(0, stack);
            
            if(this.blockInstrument().isDyeable() && ((IDyeableItem)stack.getItem()).hasColor(stack)) {
                this.color = ((IDyeableItem)stack.getItem()).getColor(stack);
            }
        }
    }

    public ItemStack getInstrumentStack() {
        if(items.isEmpty() || items.get(0) == null) {
            return ItemStack.EMPTY;
        } else {
            return items.get(0);
        }
    }

    public Byte getInstrumentId() {
        return this.blockInstrument().getInstrumentId();
    }

    public Boolean hasColor() {
        return color != null && this.blockInstrument().isDyeable();
    }

    public Integer getColor() { 
        if(!this.blockInstrument().isDyeable()) {
            return -1;
        }

        return hasColor() ? color : this.blockInstrument().getDefaultColor();
    }

    private BlockInstrument blockInstrument() {
        return (BlockInstrument)getBlockState().getBlock();
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);

        if(this.color != null) {
            compound.putInt(COLOR_TAG, color);
        }
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        
        // Fallback for missing stack data
        if(this.items.get(0) == null || this.items.get(0).isEmpty()) {
            MIMIMod.LOGGER.warn("TileInstrument had no saved instrument stack! Re-initializing.");
            this.initializeInstrumentStack();
        }

        if(compound.contains(COLOR_TAG)) {
            this.color = compound.getInt(COLOR_TAG);
        }
    }

    @Override
    public void loadItems(CompoundTag compound) {
        // START TEMPORARY LEGACY COMPATIBILITY CODE
        // Filter out switchboard items so that we can convert them
        ListTag listtag = compound.getList("Items", 10);

        if(listtag.size() > 0) {
            CompoundTag stackTag = listtag.getCompound(0);
            String itemId = stackTag.getString("id");

            if(itemId.equalsIgnoreCase("mimi:switchboard") && stackTag.contains("tag", 10)) {
                MIMIMod.LOGGER.info("Converting TileInstrument from Switchboard.");
                compound.put(
                    "Items",
                    ContainerHelper.saveAllItems(
                        new CompoundTag(),
                        NonNullList.of(initializeInstrumentStack(stackTag))
                    ).get("Items")
                );
            }
        }
        ContainerHelper.loadAllItems(compound, items);        
        // END TEMPORARY LEGACY COMPATIBILITY CODE
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public ItemStack removeItem(int i, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    }

    private ItemStack initializeInstrumentStack() {
        return this.initializeInstrumentStack(null);
    }

    private ItemStack initializeInstrumentStack(CompoundTag stackTag) {
        ItemStack instrumentStack = new ItemStack(this.blockInstrument().asItem(), 1);

        if(stackTag != null && stackTag.contains("tag")) {
            instrumentStack.setTag(stackTag.getCompound("tag").copy());
        }

        return instrumentStack;
    }
}