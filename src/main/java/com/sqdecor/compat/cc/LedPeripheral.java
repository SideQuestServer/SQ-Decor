package com.sqdecor.compat.cc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sqdecor.CommonConfig;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.block.led.LedBlock;
import com.sqdecor.led.AutoMode;
import com.sqdecor.led.LedAnimator;
import com.sqdecor.led.LedLogic;
import com.sqdecor.led.RedstoneMode;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LedPeripheral implements IPeripheral {
    private final BlockEntity be;

    public LedPeripheral(BlockEntity be) {
        this.be = be;
    }

    @Override
    public String getType() {
        return "sqdecor_led";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof LedPeripheral p && p.be == be;
    }

    @Override
    public Object getTarget() {
        return be;
    }

    private Level world() throws LuaException {
        Level l = be.getLevel();
        if (l == null) throw new LuaException("not loaded");
        return l;
    }

    private List<BlockPos> leds() throws LuaException {
        return LedLogic.network(world(), be.getBlockPos());
    }

    private BlockPos at(int index) throws LuaException {
        List<BlockPos> list = leds();
        if (index < 1 || index > list.size()) throw new LuaException("LED index out of range (1-" + list.size() + ")");
        return list.get(index - 1);
    }

    private BlockPos own() throws LuaException {
        if (world().getBlockState(be.getBlockPos()).getBlock() instanceof LedBlock) return be.getBlockPos();
        List<BlockPos> list = leds();
        if (list.isEmpty()) throw new LuaException("no LED connected");
        return list.get(0);
    }

    private static int level(int v) throws LuaException {
        if (v < 0 || v > 15) throw new LuaException("level must be 0-15");
        return v;
    }

    private static int channel(int v) throws LuaException {
        if (v < 0 || v > 255) throw new LuaException("color value must be 0-255");
        return v;
    }

    static int color(IArguments args, int i) throws LuaException {
        return color(args.get(i), i);
    }

    static int color(Object o, int i) throws LuaException {
        if (o instanceof Number n) return n.intValue() & 0xFFFFFF;
        if (o instanceof String s) {
            String t = s.trim();
            if (t.startsWith("#")) {
                try {
                    return Integer.parseInt(t.substring(1), 16) & 0xFFFFFF;
                } catch (NumberFormatException e) {
                    throw new LuaException("bad color '" + s + "'");
                }
            }
            String name = t.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            DyeColor dye = DyeColor.byName(name, null);
            if (dye == null) throw new LuaException("unknown color '" + s + "'");
            return dye.getTextureDiffuseColor() & 0xFFFFFF;
        }
        throw new LuaException("bad argument #" + (i + 1) + " (color expected: number, name or #RRGGBB)");
    }

    private void setColorAt(BlockPos p, int rgb) throws LuaException {
        if (world().getBlockEntity(p) instanceof LedBlockEntity led) led.setRgb(rgb);
    }

    private int colorOf(BlockPos p) throws LuaException {
        return world().getBlockEntity(p) instanceof LedBlockEntity led ? led.getRgb() : 0;
    }

    private int levelOf(BlockPos p) throws LuaException {
        var state = world().getBlockState(p);
        return state.getBlock() instanceof LedBlock ? state.getValue(LedLogic.LIGHT_LEVEL) : 0;
    }

    @LuaFunction(mainThread = true)
    public final int getCount() throws LuaException {
        return leds().size();
    }

    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getLeds() throws LuaException {
        List<Map<String, Object>> out = new ArrayList<>();
        List<BlockPos> list = leds();
        for (int i = 0; i < list.size(); i++) {
            BlockPos p = list.get(i);
            Map<String, Object> m = new HashMap<>();
            m.put("index", i + 1);
            m.put("x", p.getX());
            m.put("y", p.getY());
            m.put("z", p.getZ());
            m.put("level", levelOf(p));
            m.put("color", colorOf(p));
            out.add(m);
        }
        return out;
    }

    @LuaFunction(mainThread = true)
    public final int getLevel() throws LuaException {
        return levelOf(own());
    }

    @LuaFunction(mainThread = true)
    public final int getLevelAt(int index) throws LuaException {
        return levelOf(at(index));
    }

    @LuaFunction(mainThread = true)
    public final int getColor() throws LuaException {
        return colorOf(own());
    }

    @LuaFunction(mainThread = true)
    public final int getColorAt(int index) throws LuaException {
        return colorOf(at(index));
    }

    @LuaFunction(mainThread = true)
    public final void setLevel(int value) throws LuaException {
        int n = level(value);
        for (BlockPos p : leds()) LedLogic.applyLevel(world(), p, n, true);
    }

    @LuaFunction(mainThread = true)
    public final void setLevelAt(int index, int value) throws LuaException {
        LedLogic.applyLevel(world(), at(index), level(value), true);
    }

    @LuaFunction(mainThread = true)
    public final void setColor(IArguments args) throws LuaException {
        int rgb = color(args, 0);
        for (BlockPos p : leds()) setColorAt(p, rgb);
    }

    @LuaFunction(mainThread = true)
    public final void setColorAt(IArguments args) throws LuaException {
        BlockPos p = at(args.getInt(0));
        setColorAt(p, color(args, 1));
    }

    @LuaFunction(mainThread = true)
    public final void setRGB(int r, int g, int b) throws LuaException {
        int rgb = (channel(r) << 16) | (channel(g) << 8) | channel(b);
        for (BlockPos p : leds()) setColorAt(p, rgb);
    }

    @LuaFunction(mainThread = true)
    public final void setRGBAt(int index, int r, int g, int b) throws LuaException {
        setColorAt(at(index), (channel(r) << 16) | (channel(g) << 8) | channel(b));
    }

    @LuaFunction(mainThread = true)
    public final void gradient(IArguments args) throws LuaException {
        int c1 = color(args, 0);
        int c2 = color(args, 1);
        List<BlockPos> list = leds();
        int n = list.size();
        for (int i = 0; i < n; i++) {
            double t = n <= 1 ? 0 : i / (double) (n - 1);
            int r = (int) Math.round(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
            int g = (int) Math.round(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
            int b = (int) Math.round((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
            setColorAt(list.get(i), (r << 16) | (g << 8) | b);
        }
    }

    @LuaFunction(mainThread = true)
    public final void setRedstoneMode(String mode) throws LuaException {
        RedstoneMode m = switch (mode.toLowerCase(Locale.ROOT)) {
            case "off", "ignore" -> RedstoneMode.IGNORE;
            case "analog" -> RedstoneMode.ANALOG;
            case "switch", "onoff" -> RedstoneMode.SWITCH;
            default -> throw new LuaException("mode must be 'off', 'analog' or 'switch'");
        };
        for (BlockPos p : leds()) LedLogic.setRedstoneMode(world(), p, m);
    }

    @LuaFunction(mainThread = true)
    public final String getRedstoneMode() throws LuaException {
        if (world().getBlockEntity(own()) instanceof LedBlockEntity led) {
            return switch (led.getRedstoneMode()) {
                case ANALOG -> "analog";
                case SWITCH -> "switch";
                default -> "off";
            };
        }
        return "off";
    }

    @LuaFunction(mainThread = true)
    public final void setAutoMode(String mode) throws LuaException {
        AutoMode m = switch (mode.toLowerCase(Locale.ROOT)) {
            case "off" -> AutoMode.OFF;
            case "night" -> AutoMode.NIGHT;
            case "nearby", "player" -> AutoMode.NEARBY;
            default -> throw new LuaException("mode must be 'off', 'night' or 'nearby'");
        };
        for (BlockPos p : leds()) LedLogic.setAutoMode(world(), p, m);
    }

    @LuaFunction(mainThread = true)
    public final String getAutoMode() throws LuaException {
        if (world().getBlockEntity(own()) instanceof LedBlockEntity led) return led.getAutoMode().name().toLowerCase(Locale.ROOT);
        return "off";
    }

    @LuaFunction(mainThread = true)
    public final void setAutoRange(int blocks) throws LuaException {
        int max = Math.min(AutoMode.MAX_RANGE, CommonConfig.AUTO_MAX_RANGE.get());
        if (blocks < AutoMode.MIN_RANGE || blocks > max) {
            throw new LuaException("range must be " + AutoMode.MIN_RANGE + " to " + max);
        }
        for (BlockPos p : leds()) {
            if (world().getBlockEntity(p) instanceof LedBlockEntity led) led.setAutoRange(blocks);
        }
    }

    @LuaFunction(mainThread = true)
    public final int getAutoRange() throws LuaException {
        if (world().getBlockEntity(own()) instanceof LedBlockEntity led) return led.getAutoRange();
        return AutoMode.DEFAULT_RANGE;
    }

    @LuaFunction(mainThread = true)
    public final void rainbow(double speed) throws LuaException {
        LedAnimator.start(world(), leds(), LedAnimator.Type.RAINBOW, Mth.clamp(speed, 0.01, 5), 0, 0, 0, 0);
    }

    @LuaFunction(mainThread = true)
    public final void chase(IArguments args) throws LuaException {
        int rgb = color(args, 0);
        double speed = Mth.clamp(args.getDouble(1), 0.1, 20);
        int base = level(args.optInt(2, 0));
        LedAnimator.start(world(), leds(), LedAnimator.Type.CHASE, speed, rgb, 0, base, 0);
    }

    @LuaFunction(mainThread = true)
    public final void fade(IArguments args) throws LuaException {
        int c1 = color(args, 0);
        int c2 = color(args, 1);
        double seconds = Mth.clamp(args.getDouble(2), 0.1, 600);
        LedAnimator.start(world(), leds(), LedAnimator.Type.FADE, 0, c1, c2, 0, (int) Math.round(seconds * 20));
    }

    @LuaFunction(mainThread = true)
    public final void pulse(IArguments args) throws LuaException {
        int rgb = color(args, 0);
        double speed = Mth.clamp(args.optDouble(1, 0.5), 0.05, 5);
        int max = level(args.optInt(2, 15));
        LedAnimator.start(world(), leds(), LedAnimator.Type.PULSE, speed, rgb, 0, max, 0);
    }

    @LuaFunction(mainThread = true)
    public final void blink(IArguments args) throws LuaException {
        int rgb = color(args, 0);
        double speed = Mth.clamp(args.optDouble(1, 1), 0.05, 10);
        int lvl = level(args.optInt(2, 15));
        LedAnimator.start(world(), leds(), LedAnimator.Type.BLINK, speed, rgb, 0, lvl, 0);
    }

    @LuaFunction(mainThread = true)
    public final void stopAnimation() throws LuaException {
        LedAnimator.stop(world(), leds());
    }
}
