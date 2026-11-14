package com.sqdecor.client;

import java.util.List;

import com.sqdecor.item.LedRemoteItem;
import com.sqdecor.item.RemoteGroups;
import com.sqdecor.item.RemoteGroups.Group;
import com.sqdecor.network.RemotePayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class RemoteScreen extends Screen {
    private static final int PW = 270;
    private static final int ROW = 22;

    private final InteractionHand hand;
    private RemoteGroups groups;
    private EditBox name;
    private String draft = "";
    private int draftFor = -1;
    private List<FormattedCharSequence> hint = List.of();
    private int px;
    private int py;
    private int ph;
    private boolean backgroundDrawn;
    private boolean asked;
    private List<Boolean> on;
    private List<Integer> leds;

    public RemoteScreen(InteractionHand hand) {
        super(Component.translatable("gui.sqdecor.remote.title"));
        this.hand = hand;
        this.groups = current();
    }

    private RemoteGroups current() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? RemoteGroups.EMPTY : read(mc.player.getItemInHand(hand));
    }

    private static RemoteGroups read(ItemStack stack) {
        return stack.getItem() instanceof LedRemoteItem ? RemoteGroups.of(stack) : RemoteGroups.EMPTY;
    }

    private void send(int action, int index, int value, String text) {
        PacketDistributor.sendToServer(new RemotePayload(hand == InteractionHand.MAIN_HAND, action, index, value, text));
    }

    void status(List<Boolean> on, List<Integer> leds) {
        this.on = on;
        this.leds = leds;
        rebuildWidgets();
    }

    private boolean isOn(int index) {
        return on != null && index < on.size() && on.get(index);
    }

    @Override
    protected void init() {
        if (!asked) {
            asked = true;
            send(RemotePayload.STATUS, 0, 0, "");
        }
        List<Group> list = groups.groups();
        hint = font.split(Component.translatable("gui.sqdecor.remote.hint"), PW - 20);
        ph = 26 + Math.max(1, list.size()) * ROW + 30 + hint.size() * 10 + 30;
        px = (width - PW) / 2;
        py = (height - ph) / 2;
        int x = px + 10;
        int y = py + 22;

        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            Group group = list.get(i);
            boolean selected = i == groups.selected();
            Component label = selected ? Component.literal("▶ ").append(group.label(i)) : group.label(i);
            addRenderableWidget(Button.builder(label, b -> {
                commitName();
                send(RemotePayload.SELECT, index, 0, "");
            }).bounds(x, y, 100, 20).build());
            boolean lit = isOn(i);
            Component state = Component.translatable(lit ? "gui.sqdecor.remote.on" : "gui.sqdecor.remote.off")
                    .withStyle(lit ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            addRenderableWidget(Button.builder(state, b -> send(isOn(index) ? RemotePayload.OFF : RemotePayload.ON, index, 0, ""))
                    .bounds(x + 104, y, 48, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.sqdecor.remote.switch_hint")))
                    .build());
            addRenderableWidget(Button.builder(programText(group.program()), b -> {
                int next = (list.get(index).program() + 1) % (RemoteGroups.PROGRAM_MAX + 1);
                send(RemotePayload.PROGRAM, index, next, "");
            }).bounds(x + 188, y, 62, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.sqdecor.remote.program_hint")))
                    .build());
            y += ROW;
        }
        if (list.isEmpty()) y += ROW;

        y += 6;
        Group selected = groups.selectedGroup();
        name = new EditBox(font, x, y, 128, 20, Component.translatable("gui.sqdecor.remote.name"));
        name.setMaxLength(RemoteGroups.NAME_MAX);
        name.setHint(Component.translatable("gui.sqdecor.remote.name"));
        if (selected != null) {
            if (draftFor != groups.selected()) {
                draft = selected.name();
                draftFor = groups.selected();
            }
            name.setValue(draft);
        }
        name.setEditable(selected != null);
        name.setResponder(v -> draft = v);
        addRenderableWidget(name);
        Button add = addRenderableWidget(Button.builder(Component.translatable("gui.sqdecor.remote.add"), b -> {
            commitName();
            send(RemotePayload.ADD, 0, 0, "");
        }).bounds(x + 132, y, 54, 20).build());
        add.active = list.size() < RemoteGroups.MAX;
        Button delete = addRenderableWidget(Button.builder(Component.translatable("gui.sqdecor.remote.delete"), b -> {
            draftFor = -1;
            send(RemotePayload.DELETE, groups.selected(), 0, "");
        }).bounds(x + 190, y, 50, 20).build());
        delete.active = selected != null;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x + 70, py + ph - 26, 100, 20).build());
    }

    private static Component programText(int program) {
        return Component.translatable("gui.sqdecor.tuner.program." + RemotePayload.PROGRAM_KEYS[program]);
    }

    private void commitName() {
        Group selected = groups.selectedGroup();
        if (selected != null && draftFor == groups.selected() && !draft.strip().equals(selected.name())) {
            send(RemotePayload.RENAME, groups.selected(), 0, draft);
        }
    }

    @Override
    public void tick() {
        RemoteGroups now = current();
        if (!now.equals(groups)) {
            groups = now;
            rebuildWidgets();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (name != null && name.isFocused() && (keyCode == 257 || keyCode == 335)) {
            commitName();
            name.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        commitName();
        super.onClose();
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
        g.fill(px, py, px + PW, py + ph, 0xFF2B2B2B);
        g.renderOutline(px, py, PW, ph, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        backgroundDrawn = false;
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, px + 10, py + 7, 0xFFFFFF, true);
        if (groups.groups().isEmpty()) {
            g.drawString(font, Component.translatable("gui.sqdecor.remote.empty"), px + 10, py + 28, 0xFFA0A0A0, false);
        }
        for (int i = 0; i < groups.groups().size(); i++) {
            Component count = leds == null || i >= leds.size()
                    ? Component.literal("...")
                    : Component.translatable("gui.sqdecor.remote.leds", leds.get(i));
            int color = leds != null && i < leds.size() && leds.get(i) == 0 ? 0xFFE08060 : 0xFFA0A0A0;
            g.drawString(font, count, px + 10 + 156, py + 22 + i * ROW + 6, color, false);
        }
        int y = py + 22 + Math.max(1, groups.groups().size()) * ROW + 32;
        for (FormattedCharSequence line : hint) {
            g.drawString(font, line, px + 10, y, 0xFFA0A0A0, false);
            y += 10;
        }
    }
}
