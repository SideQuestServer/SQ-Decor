package com.sqdecor.client;

import java.util.ArrayList;
import java.util.List;

import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.item.TunerScene;
import com.sqdecor.item.TunerSettings;
import com.sqdecor.led.AutoMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

public final class LedTunerClient {
    private LedTunerClient() {}

    public static void open(ItemStack stack) {
        List<TunerScene> scenes = TunerScene.has(stack) ? new ArrayList<>(TunerScene.of(stack)) : exampleScenes();
        Minecraft.getInstance().setScreen(new LedTunerScreen(TunerSettings.of(stack), scenes));
    }

    private static List<TunerScene> exampleScenes() {
        int keep = TunerSettings.KEEP;
        List<TunerScene> out = new ArrayList<>();
        out.add(scene("evening", new TunerSettings(6, 0xFFB46B, keep, 0, keep, keep, "", "", keep, AutoMode.DEFAULT_RANGE)));
        out.add(scene("party", new TunerSettings(15, keep, keep, 1, keep, keep, "", "", keep, AutoMode.DEFAULT_RANGE)));
        out.add(scene("bright", new TunerSettings(15, LedBlockEntity.DEFAULT_RGB, keep, 0, keep, keep, "", "", keep, AutoMode.DEFAULT_RANGE)));
        out.add(scene("off", new TunerSettings(0, keep, keep, 0, keep, keep, "", "", keep, AutoMode.DEFAULT_RANGE)));
        return out;
    }

    private static TunerScene scene(String key, TunerSettings settings) {
        return new TunerScene(I18n.get("gui.sqdecor.scene.example." + key), settings);
    }
}
