package com.sqdecor.compat.cc;

import java.util.List;

import com.sqdecor.block.entity.MarqueeBlockEntity;
import com.sqdecor.block.marquee.MarqueeBlock;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MarqueePeripheral implements IPeripheral {
    private final BlockEntity be;

    public MarqueePeripheral(BlockEntity be) {
        this.be = be;
    }

    @Override
    public String getType() {
        return "sqdecor_marquee";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof MarqueePeripheral p && p.be == be;
    }

    @Override
    public Object getTarget() {
        return be;
    }

    private Level world() throws LuaException {
        Level l = be.getLevel();
        if (l == null || be.isRemoved()) throw new LuaException("not loaded");
        return l;
    }

    private MarqueeBlockEntity self() throws LuaException {
        if (world().getBlockEntity(be.getBlockPos()) instanceof MarqueeBlockEntity m) return m;
        throw new LuaException("not loaded");
    }

    private void apply(String text, int rgb, int speed, boolean on) throws LuaException {
        Level level = world();
        for (BlockPos p : MarqueeBlock.row(level, be.getBlockPos())) {
            if (level.getBlockEntity(p) instanceof MarqueeBlockEntity panel) panel.set(text, rgb, speed, on);
        }
    }

    @LuaFunction(mainThread = true)
    public final void setText(String text) throws LuaException {
        MarqueeBlockEntity m = self();
        apply(text, m.getRgb(), m.getSpeed(), m.isOn());
    }

    @LuaFunction(mainThread = true)
    public final String getText() throws LuaException {
        return self().getText();
    }

    @LuaFunction(mainThread = true)
    public final void setColor(IArguments args) throws LuaException {
        MarqueeBlockEntity m = self();
        apply(m.getText(), LedPeripheral.color(args, 0), m.getSpeed(), m.isOn());
    }

    @LuaFunction(mainThread = true)
    public final int getColor() throws LuaException {
        return self().getRgb();
    }

    @LuaFunction(mainThread = true)
    public final void setSpeed(int speed) throws LuaException {
        if (speed < 0 || speed > MarqueeBlockEntity.MAX_SPEED) {
            throw new LuaException("speed must be 0-" + MarqueeBlockEntity.MAX_SPEED);
        }
        MarqueeBlockEntity m = self();
        apply(m.getText(), m.getRgb(), speed, m.isOn());
    }

    @LuaFunction(mainThread = true)
    public final int getSpeed() throws LuaException {
        return self().getSpeed();
    }

    @LuaFunction(mainThread = true)
    public final void setOn(boolean on) throws LuaException {
        MarqueeBlockEntity m = self();
        apply(m.getText(), m.getRgb(), m.getSpeed(), on);
    }

    @LuaFunction(mainThread = true)
    public final boolean isOn() throws LuaException {
        return self().isOn();
    }

    @LuaFunction(mainThread = true)
    public final int getWidth() throws LuaException {
        List<BlockPos> row = MarqueeBlock.row(world(), be.getBlockPos());
        return row.size();
    }
}
