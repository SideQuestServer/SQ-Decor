package com.sqdecor.item;

import com.sqdecor.led.AutoMode;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record TunerSettings(int level, int rgb, int mode, int program, int caseRgb, int case2Rgb,
                            String caseBlock, String case2Block, int auto, int autoRange,
                            int lightMode, int lightTint) {

    public static final int KEEP = -1;
    public static final int ORIGINAL = -2;
    public static final int PROGRAM_MAX = 4;
    public static final int AUTO_MAX = 2;
    public static final int LIGHT_MODE_MAX = 2;

    public static final TunerSettings DEFAULT =
            new TunerSettings(8, KEEP, KEEP, 0, KEEP, KEEP, "", "", KEEP, AutoMode.DEFAULT_RANGE, KEEP, KEEP);

    public static final StreamCodec<FriendlyByteBuf, TunerSettings> STREAM_CODEC =
            StreamCodec.of((buf, s) -> s.write(buf), TunerSettings::read);

    public TunerSettings {
        level = Mth.clamp(level, 0, 15);
        rgb = rgb < 0 ? KEEP : rgb & 0xFFFFFF;
        mode = Mth.clamp(mode, KEEP, 2);
        program = Mth.clamp(program, 0, PROGRAM_MAX);
        caseRgb = caseColor(caseRgb);
        case2Rgb = caseColor(case2Rgb);
        caseBlock = caseBlock == null ? "" : caseBlock;
        case2Block = case2Block == null ? "" : case2Block;
        auto = Mth.clamp(auto, KEEP, AUTO_MAX);
        autoRange = Mth.clamp(autoRange, AutoMode.MIN_RANGE, AutoMode.MAX_RANGE);
        lightMode = Mth.clamp(lightMode, KEEP, LIGHT_MODE_MAX);
        lightTint = Mth.clamp(lightTint, KEEP, 1);
    }

    public TunerSettings(int level, int rgb, int mode, int program, int caseRgb, int case2Rgb,
                         String caseBlock, String case2Block, int auto, int autoRange) {
        this(level, rgb, mode, program, caseRgb, case2Rgb, caseBlock, case2Block, auto, autoRange, KEEP, KEEP);
    }

    private static int caseColor(int value) {
        if (value == ORIGINAL) return ORIGINAL;
        return value < 0 ? KEEP : value & 0xFFFFFF;
    }

    public static TunerSettings of(ItemStack stack) {
        return fromTag(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    public void store(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, this::writeTo);
    }

    public static TunerSettings fromTag(CompoundTag t) {
        return new TunerSettings(
                t.contains("level") ? t.getInt("level") : DEFAULT.level,
                t.contains("rgb") ? t.getInt("rgb") : KEEP,
                t.contains("mode") ? t.getInt("mode") : KEEP,
                t.getInt("program"),
                t.contains("case_rgb") ? t.getInt("case_rgb") : KEEP,
                t.contains("case2_rgb") ? t.getInt("case2_rgb") : KEEP,
                t.getString("case_block"),
                t.getString("case2_block"),
                t.contains("auto") ? t.getInt("auto") : KEEP,
                t.contains("auto_range") ? t.getInt("auto_range") : AutoMode.DEFAULT_RANGE,
                t.contains("light_mode") ? t.getInt("light_mode") : KEEP,
                t.contains("light_tint") ? t.getInt("light_tint") : KEEP);
    }

    public void writeTo(CompoundTag t) {
        t.putInt("level", level);
        t.putInt("rgb", rgb);
        t.putInt("mode", mode);
        t.putInt("program", program);
        t.putInt("case_rgb", caseRgb);
        t.putInt("case2_rgb", case2Rgb);
        t.putString("case_block", caseBlock);
        t.putString("case2_block", case2Block);
        t.putInt("auto", auto);
        t.putInt("auto_range", autoRange);
        t.putInt("light_mode", lightMode);
        t.putInt("light_tint", lightTint);
    }

    public TunerSettings withLevel(int value) {
        return new TunerSettings(value, rgb, mode, program, caseRgb, case2Rgb, caseBlock, case2Block, auto, autoRange,
                lightMode, lightTint);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeInt(rgb);
        buf.writeVarInt(mode);
        buf.writeVarInt(program);
        buf.writeInt(caseRgb);
        buf.writeInt(case2Rgb);
        buf.writeUtf(caseBlock, 256);
        buf.writeUtf(case2Block, 256);
        buf.writeVarInt(auto);
        buf.writeVarInt(autoRange);
        buf.writeVarInt(lightMode);
        buf.writeVarInt(lightTint);
    }

    private static TunerSettings read(FriendlyByteBuf buf) {
        return new TunerSettings(buf.readVarInt(), buf.readInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readInt(), buf.readInt(), buf.readUtf(256), buf.readUtf(256), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt());
    }
}
