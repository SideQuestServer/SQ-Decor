package com.sqdecor.block.entity;

import com.sqdecor.block.industrial.BlockPaint;
import com.sqdecor.block.industrial.Industrial;
import com.sqdecor.registry.ModBlockEntities;
import com.sqdecor.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public class PaintBlockEntity extends BlockEntity {
    private int base = -1;
    private int accent = -1;
    @Nullable
    private BlockState template;

    public static final ModelProperty<BlockState> TEMPLATE = new ModelProperty<>();

    public PaintBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PAINT.get(), pos, state);
    }

    public int base() {
        return base;
    }

    public int accent() {
        return accent;
    }

    public int baseColor() {
        return base >= 0 ? base : getBlockState().getValue(Industrial.COLOR).getTextureDiffuseColor() & 0xFFFFFF;
    }

    public int accentColor() {
        return accent >= 0 ? accent : baseColor();
    }

    @Nullable
    public BlockState template() {
        return template;
    }

    public void setTemplate(@Nullable BlockState template) {
        this.template = template;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public static boolean canUseAsTemplate(Block block) {
        return !(block instanceof com.sqdecor.block.industrial.PaintedBlock)
                && com.sqdecor.block.entity.LampBlockEntity.canUseAsTexture(block);
    }

    @Override
    public ModelData getModelData() {
        return template == null ? ModelData.EMPTY : ModelData.builder().with(TEMPLATE, template).build();
    }

    public BlockPaint paint() {
        return new BlockPaint(base, accent);
    }

    public void set(int base, int accent) {
        this.base = base < 0 ? -1 : base & 0xFFFFFF;
        this.accent = accent < 0 ? -1 : accent & 0xFFFFFF;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (base >= 0) tag.putInt("base", base);
        if (accent >= 0) tag.putInt("accent", accent);
        if (template != null) tag.putString("template", BuiltInRegistries.BLOCK.getKey(template.getBlock()).toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        base = tag.contains("base") ? tag.getInt("base") : -1;
        accent = tag.contains("accent") ? tag.getInt("accent") : -1;
        template = readTemplate(tag.getString("template"));
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (base >= 0 || accent >= 0) components.set(ModDataComponents.BLOCK_PAINT.get(), paint());
        if (template != null) components.set(ModDataComponents.BLOCK_TEMPLATE.get(), template.getBlock());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        BlockPaint paint = input.get(ModDataComponents.BLOCK_PAINT.get());
        if (paint != null) {
            base = paint.base();
            accent = paint.accent();
        }
        Block block = input.get(ModDataComponents.BLOCK_TEMPLATE.get());
        if (block != null && canUseAsTemplate(block)) template = block.defaultBlockState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("base");
        tag.remove("accent");
        tag.remove("template");
    }

    @Nullable
    private static BlockState readTemplate(String id) {
        if (id.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return BuiltInRegistries.BLOCK.getOptional(rl).filter(PaintBlockEntity::canUseAsTemplate)
                .map(Block::defaultBlockState).orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
