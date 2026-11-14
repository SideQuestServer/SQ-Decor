package com.sqdecor.client;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.sqdecor.item.TunerScene;
import com.sqdecor.item.TunerSettings;
import com.sqdecor.network.LedTunerPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

public class SceneScreen extends Screen {
    private static final int PW = 220;
    private static final int ROW = 22;
    private static final int PH = 24 + TunerScene.MAX * ROW + 54;
    private static final String[] PROGRAM_KEYS = {"off", "rainbow", "chase", "pulse", "blink"};

    private final LedTunerScreen parent;
    private EditBox name;
    private String draft = "";
    private int px;
    private int py;
    private boolean backgroundDrawn;

    public SceneScreen(LedTunerScreen parent) {
        super(Component.translatable("gui.sqdecor.scene.title"));
        this.parent = parent;
    }

    private List<TunerScene> scenes() {
        return parent.scenes;
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;
        int x = px + 10;
        int w = PW - 20;

        List<TunerScene> scenes = scenes();
        for (int i = 0; i < scenes.size(); i++) {
            TunerScene scene = scenes.get(i);
            int y = py + 22 + i * ROW;
            addRenderableWidget(Button.builder(Component.literal(scene.name()), b -> load(scene))
                    .bounds(x, y, w - 24, 20)
                    .tooltip(Tooltip.create(summary(scene.settings())))
                    .build());
            addRenderableWidget(Button.builder(Component.literal("x"), b -> {
                scenes().remove(scene);
                save();
                rebuildWidgets();
            }).bounds(x + w - 20, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.sqdecor.scene.delete")))
                    .build());
        }

        int y = py + 24 + TunerScene.MAX * ROW;
        name = new EditBox(font, x, y, w - 80, 20, Component.translatable("gui.sqdecor.scene.name"));
        name.setMaxLength(TunerScene.NAME_MAX);
        name.setHint(Component.translatable("gui.sqdecor.scene.name"));
        name.setValue(draft);
        name.setResponder(v -> draft = v);
        addRenderableWidget(name);
        Button saveButton = addRenderableWidget(Button.builder(Component.translatable("gui.sqdecor.scene.save"), b -> saveCurrent())
                .bounds(x + w - 76, y, 76, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.sqdecor.scene.save_hint")))
                .build());
        saveButton.active = scenes.size() < TunerScene.MAX || !draft.isBlank();

        addRenderableWidget(Button.builder(Component.translatable("gui.sqdecor.scene.back"), b -> onClose())
                .bounds(x, y + 26, w, 20).build());
    }

    private void load(TunerScene scene) {
        PacketDistributor.sendToServer(new LedTunerPayload(scene.settings(), List.copyOf(scenes())));
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("message.sqdecor.scene_loaded", scene.name()), true);
        }
        minecraft.setScreen(null);
    }

    private void save() {
        PacketDistributor.sendToServer(new LedTunerPayload(parent.stored, List.copyOf(scenes())));
    }

    private void saveCurrent() {
        List<TunerScene> scenes = scenes();
        String n = TunerScene.cleanName(draft);
        if (n.isEmpty()) n = I18n.get("gui.sqdecor.scene.default_name", scenes.size() + 1);
        TunerScene scene = new TunerScene(n, parent.current());
        boolean replaced = false;
        for (int i = 0; i < scenes.size(); i++) {
            if (scenes.get(i).name().equalsIgnoreCase(n)) {
                scenes.set(i, scene);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            if (scenes.size() >= TunerScene.MAX) return;
            scenes.add(scene);
        }
        save();
        draft = "";
        rebuildWidgets();
    }

    private static Component summary(TunerSettings s) {
        String base = "item.sqdecor.led_tuner.tooltip.";
        MutableComponent out = Component.translatable(base + "level", s.level());
        out.append("\n").append(s.rgb() < 0
                ? Component.translatable(base + "color_keep")
                : Component.translatable(base + "color", String.format("%06X", s.rgb())));
        out.append("\n").append(Component.translatable(base + "program",
                Component.translatable("gui.sqdecor.tuner.program." + PROGRAM_KEYS[s.program()])));
        return out;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (name != null && name.isFocused()
                && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
            saveCurrent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (backgroundDrawn) return;
        backgroundDrawn = true;
        g.fill(0, 0, width, height, 0x90000000);
        g.fill(px, py, px + PW, py + PH, 0xFF2B2B2B);
        g.renderOutline(px, py, PW, PH, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        backgroundDrawn = false;
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, px + 10, py + 7, 0xFFFFFF, true);

        List<TunerScene> scenes = scenes();
        if (scenes.isEmpty()) {
            g.drawString(font, Component.translatable("gui.sqdecor.scene.empty"), px + 10, py + 28, 0xFFA0A0A0, false);
        }
        for (int i = 0; i < scenes.size(); i++) {
            TunerSettings s = scenes.get(i).settings();
            int bx = px + 10 + (PW - 20 - 24) - 16;
            int by = py + 22 + i * ROW + 5;
            if (s.program() == 1) {
                g.fill(bx, by, bx + 4, by + 10, 0xFFFF4040);
                g.fill(bx + 4, by, bx + 8, by + 10, 0xFF40FF40);
                g.fill(bx + 8, by, bx + 12, by + 10, 0xFF4060FF);
            } else if (s.level() == 0) {
                g.fill(bx, by, bx + 12, by + 10, 0xFF202020);
            } else if (s.rgb() >= 0) {
                g.fill(bx, by, bx + 12, by + 10, 0xFF000000 | s.rgb());
            }
        }
    }
}
