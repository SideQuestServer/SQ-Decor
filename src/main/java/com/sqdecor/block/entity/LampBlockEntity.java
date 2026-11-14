package com.sqdecor.block.entity;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.sqdecor.block.led.LedBlock;
import com.sqdecor.lamp.LampModelData;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.led.LedLook;
import com.sqdecor.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class LampBlockEntity extends LedBlockEntity {
    public static final int NO_COLOR = -1;

    private int caseRgb = NO_COLOR;
    private int case2Rgb = NO_COLOR;
    @Nullable
    private BlockState caseTexture;
    @Nullable
    private BlockState case2Texture;
    private int next;

    private int anim;
    private int animSpeed = 1;

    public int anim() {
        return anim;
    }

    public int animSpeed() {
        return animSpeed;
    }

    public void setAnim(int anim) {
        this.anim = anim;
        changed();
    }

    public void setAnimSpeed(int speed) {
        this.animSpeed = speed;
        changed();
    }

    public int chainLength = -1;
    public long chainCheckedAt;

    @Nullable
    public BlockPos spot;

    public static final int SHELF_SLOTS = 15;
    @Nullable
    private ItemStack[] shelfItems;

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAMP.get(), pos, state);
    }

    public ItemStack shelfItem(int slot) {
        if (shelfItems == null || slot < 0 || slot >= SHELF_SLOTS) return ItemStack.EMPTY;
        return shelfItems[slot];
    }

    public boolean hasShelfItems() {
        return shelfItems != null;
    }

    public void setShelfItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SHELF_SLOTS) return;
        if (shelfItems == null) {
            if (stack.isEmpty()) return;
            shelfItems = new ItemStack[SHELF_SLOTS];
            java.util.Arrays.fill(shelfItems, ItemStack.EMPTY);
        }
        shelfItems[slot] = stack;
        boolean any = false;
        for (ItemStack item : shelfItems) any |= !item.isEmpty();
        if (!any) shelfItems = null;
        changed();
    }

    public void dropShelfItems(Level level, BlockPos pos) {
        if (shelfItems == null) return;
        for (ItemStack item : shelfItems) {
            if (!item.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item);
        }
        shelfItems = null;
    }

    public int color(LampPart part) {
        return switch (part) {
            case BULB -> 0xFF000000 | getRgb();
            case CASE -> caseRgb == NO_COLOR ? NO_COLOR : 0xFF000000 | caseRgb;
            case CASE2 -> case2Rgb == NO_COLOR ? NO_COLOR : 0xFF000000 | case2Rgb;
            case FIXED -> NO_COLOR;
        };
    }

    public int caseColor(LampPart part) {
        return switch (part) {
            case CASE -> caseRgb;
            case CASE2 -> case2Rgb;
            default -> NO_COLOR;
        };
    }

    @Nullable
    public BlockState texture(LampPart part) {
        return switch (part) {
            case CASE -> caseTexture;
            case CASE2 -> case2Texture;
            default -> null;
        };
    }

    public LampPart paintNext(List<LampPart> parts, int rgb) {
        int i = next % parts.size();
        LampPart part = parts.get(i);
        next = (i + 1) % parts.size();
        paint(part, rgb);
        return part;
    }

    public void paint(LampPart part, int rgb) {
        if (part == LampPart.BULB) {
            setRgb(rgb);
        } else {
            setCase(part, rgb & 0xFFFFFF, null);
        }
    }

    public void setCase(LampPart part, int rgb, @Nullable BlockState texture) {
        switch (part) {
            case CASE -> {
                caseRgb = rgb;
                caseTexture = texture;
            }
            case CASE2 -> {
                case2Rgb = rgb;
                case2Texture = texture;
            }
            default -> {
                return;
            }
        }
        changed();
    }

    @Nullable
    public LampPart textureNext(List<LampPart> parts, Block block) {
        for (int step = 0; step < parts.size(); step++) {
            int i = (next + step) % parts.size();
            LampPart part = parts.get(i);
            if (part != LampPart.CASE && part != LampPart.CASE2) continue;
            next = (i + 1) % parts.size();
            setCase(part, NO_COLOR, block.defaultBlockState());
            return part;
        }
        return null;
    }

    public static boolean canUseAsTexture(Block block) {
        BlockState state = block.defaultBlockState();
        return !(block instanceof LedBlock) && !state.isAir() && state.getRenderShape() == RenderShape.MODEL;
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public LedLook look() {
        return new LedLook(getRgb(), caseRgb, case2Rgb,
                Optional.ofNullable(caseTexture).map(BlockState::getBlock),
                Optional.ofNullable(case2Texture).map(BlockState::getBlock));
    }

    @Override
    protected void applyLook(LedLook look) {
        super.applyLook(look);
        caseRgb = look.caseRgb() < 0 ? NO_COLOR : look.caseRgb() & 0xFFFFFF;
        case2Rgb = look.case2Rgb() < 0 ? NO_COLOR : look.case2Rgb() & 0xFFFFFF;
        caseTexture = look.caseBlock().filter(LampBlockEntity::canUseAsTexture).map(Block::defaultBlockState).orElse(null);
        case2Texture = look.case2Block().filter(LampBlockEntity::canUseAsTexture).map(Block::defaultBlockState).orElse(null);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("case_rgb");
        tag.remove("case2_rgb");
        tag.remove("case_block");
        tag.remove("case2_block");
        tag.remove("shelf");
    }

    @Override
    public ModelData getModelData() {
        ModelData.Builder data = ModelData.builder();
        if (caseTexture != null) data.with(LampModelData.CASE, caseTexture);
        if (case2Texture != null) data.with(LampModelData.CASE2, case2Texture);
        return data.build();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("case_rgb", caseRgb);
        tag.putInt("case2_rgb", case2Rgb);
        if (caseTexture != null) tag.putString("case_block", blockId(caseTexture));
        if (case2Texture != null) tag.putString("case2_block", blockId(case2Texture));
        tag.putInt("paint_next", next);
        tag.putInt("anim", anim);
        tag.putInt("anim_speed", animSpeed);
        if (shelfItems != null) {
            ListTag list = new ListTag();
            for (int i = 0; i < SHELF_SLOTS; i++) {
                if (shelfItems[i].isEmpty()) continue;
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", shelfItems[i].save(registries));
                list.add(entry);
            }
            if (!list.isEmpty()) tag.put("shelf", list);
        }
        if (spot != null) tag.putLong("spot", spot.asLong());
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readData(tag, registries);
        caseRgb = tag.contains("case_rgb") ? tag.getInt("case_rgb") : NO_COLOR;
        case2Rgb = tag.contains("case2_rgb") ? tag.getInt("case2_rgb") : NO_COLOR;
        caseTexture = readBlock(tag, "case_block");
        case2Texture = readBlock(tag, "case2_block");
        next = Math.max(0, tag.getInt("paint_next"));
        anim = tag.getInt("anim");
        animSpeed = tag.contains("anim_speed") ? tag.getInt("anim_speed") : 1;
        shelfItems = null;
        if (tag.contains("shelf", Tag.TAG_LIST)) {
            ListTag list = tag.getList("shelf", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getByte("Slot");
                if (slot < 0 || slot >= SHELF_SLOTS) continue;
                ItemStack item = ItemStack.parse(registries, entry.getCompound("Item")).orElse(ItemStack.EMPTY);
                if (item.isEmpty()) continue;
                if (shelfItems == null) {
                    shelfItems = new ItemStack[SHELF_SLOTS];
                    java.util.Arrays.fill(shelfItems, ItemStack.EMPTY);
                }
                shelfItems[slot] = item;
            }
        }
        spot = tag.contains("spot") ? BlockPos.of(tag.getLong("spot")) : null;
    }

    @Override
    protected int modelLook() {
        return java.util.Objects.hash(caseRgb, case2Rgb, caseTexture, case2Texture);
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    @Nullable
    private static BlockState readBlock(CompoundTag tag, String key) {
        if (!tag.contains(key)) return null;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(key));
        if (id == null) return null;
        return BuiltInRegistries.BLOCK.getOptional(id).filter(LampBlockEntity::canUseAsTexture)
                .map(Block::defaultBlockState).orElse(null);
    }
}
