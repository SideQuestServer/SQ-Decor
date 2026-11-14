package com.sqdecor.block.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sqdecor.block.cable.CableRackBlock;
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

public class CableRackBlockEntity extends BlockEntity {
    public record Link(int slot, BlockPos other, int otherSlot) {}

    private final List<Link> links = new ArrayList<>();
    private final DyeColor[] colors;

    public CableRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_RACK.get(), pos, state);
        int slots = state.getBlock() instanceof CableRackBlock rack ? rack.slots() : 10;
        colors = new DyeColor[slots];
        Arrays.fill(colors, DyeColor.BLACK);
    }

    public int slots() {
        return colors.length;
    }

    public List<Link> links() {
        return links;
    }

    public DyeColor color(int slot) {
        return slot >= 0 && slot < colors.length ? colors[slot] : DyeColor.BLACK;
    }

    public void setColor(int slot, DyeColor color) {
        if (slot < 0 || slot >= colors.length) return;
        colors[slot] = color;
        changed();
    }

    public boolean hasLink(int slot, BlockPos other, int otherSlot) {
        return links.contains(new Link(slot, other, otherSlot));
    }

    public boolean addLink(int slot, BlockPos other, int otherSlot) {
        Link link = new Link(slot, other.immutable(), otherSlot);
        if (links.contains(link)) return false;
        links.add(link);
        changed();
        return true;
    }

    public boolean removeLink(int slot, BlockPos other, int otherSlot) {
        boolean removed = links.remove(new Link(slot, other, otherSlot));
        if (removed) changed();
        return removed;
    }

    public int removeLinksTo(BlockPos other) {
        int before = links.size();
        links.removeIf(l -> l.otherSlot() < 0 && l.other().equals(other));
        int removed = before - links.size();
        if (removed > 0) changed();
        return removed;
    }

    public void colorLinksTo(BlockPos other, DyeColor color) {
        boolean any = false;
        for (Link l : links) {
            if (l.otherSlot() < 0 && l.other().equals(other)) {
                colors[l.slot()] = color;
                any = true;
            }
        }
        if (any) changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        long[] others = new long[links.size()];
        int[] slots = new int[links.size()];
        int[] otherSlots = new int[links.size()];
        for (int i = 0; i < others.length; i++) {
            Link l = links.get(i);
            others[i] = l.other().asLong();
            slots[i] = l.slot();
            otherSlots[i] = l.otherSlot();
        }
        tag.putLongArray("links", others);
        tag.putIntArray("slots", slots);
        tag.putIntArray("other_slots", otherSlots);
        int[] packed = new int[colors.length];
        for (int i = 0; i < packed.length; i++) packed[i] = colors[i].getId();
        tag.putIntArray("colors", packed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        links.clear();
        long[] others = tag.getLongArray("links");
        int[] slots = tag.getIntArray("slots");
        int[] otherSlots = tag.getIntArray("other_slots");
        int n = Math.min(others.length, Math.min(slots.length, otherSlots.length));
        for (int i = 0; i < n; i++) {
            if (slots[i] >= 0 && slots[i] < colors.length) links.add(new Link(slots[i], BlockPos.of(others[i]), otherSlots[i]));
        }
        int[] packed = tag.getIntArray("colors");
        for (int i = 0; i < colors.length; i++) colors[i] = i < packed.length ? DyeColor.byId(packed[i]) : DyeColor.BLACK;
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
