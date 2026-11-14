package com.sqdecor.block.entity;

import java.util.ArrayList;
import java.util.List;

import com.sqdecor.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TapeAnchorBlockEntity extends BlockEntity {
    private final List<BlockPos> links = new ArrayList<>();
    private DyeColor color = DyeColor.YELLOW;
    public static java.util.function.Consumer<TapeAnchorBlockEntity> clientChanged = be -> {};

    public TapeAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TAPE_ANCHOR.get(), pos, state);
    }

    public List<BlockPos> links() {
        return links;
    }

    public DyeColor getColor() {
        return color;
    }

    public boolean addLink(BlockPos other) {
        if (links.contains(other)) return false;
        links.add(other.immutable());
        changed();
        return true;
    }

    public void removeLink(BlockPos other) {
        if (links.remove(other)) changed();
    }

    public void setColor(DyeColor color) {
        this.color = color;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        long[] packed = new long[links.size()];
        for (int i = 0; i < packed.length; i++) packed[i] = links.get(i).asLong();
        tag.putLongArray("links", packed);
        tag.putInt("color", color.getId());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        links.clear();
        for (long l : tag.getLongArray("links")) links.add(BlockPos.of(l));
        color = DyeColor.byId(tag.contains("color") ? tag.getInt("color") : DyeColor.YELLOW.getId());
        if (level != null && level.isClientSide) clientChanged.accept(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) clientChanged.accept(this);
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
