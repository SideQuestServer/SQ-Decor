package com.sqdecor.item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record TunerScene(String name, TunerSettings settings) {
    public static final int MAX = 8;
    public static final int NAME_MAX = 32;
    private static final String KEY = "scenes";

    public static final StreamCodec<FriendlyByteBuf, TunerScene> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(NAME_MAX * 4), TunerScene::name,
            TunerSettings.STREAM_CODEC, TunerScene::settings,
            TunerScene::new);
    public static final StreamCodec<FriendlyByteBuf, List<TunerScene>> LIST_STREAM_CODEC =
            STREAM_CODEC.apply(ByteBufCodecs.list(MAX));

    public TunerScene {
        name = cleanName(name);
    }

    public static String cleanName(@Nullable String value) {
        String s = StringUtil.filterText(value == null ? "" : value).trim();
        return s.length() > NAME_MAX ? s.substring(0, NAME_MAX) : s;
    }

    public static boolean has(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(KEY);
    }

    public static List<TunerScene> of(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<TunerScene> out = new ArrayList<>();
        ListTag list = tag.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && out.size() < MAX; i++) {
            CompoundTag t = list.getCompound(i);
            out.add(new TunerScene(t.getString("name"), TunerSettings.fromTag(t)));
        }
        return out;
    }

    public static void store(ItemStack stack, List<TunerScene> scenes) {
        ListTag list = new ListTag();
        for (int i = 0; i < scenes.size() && i < MAX; i++) {
            TunerScene scene = scenes.get(i);
            if (scene.name().isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putString("name", scene.name());
            scene.settings().writeTo(t);
            list.add(t);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.put(KEY, list));
    }

    @Nullable
    public static TunerScene matching(ItemStack stack) {
        TunerSettings current = TunerSettings.of(stack);
        for (TunerScene scene : of(stack)) {
            if (scene.settings().equals(current)) return scene;
        }
        return null;
    }
}
