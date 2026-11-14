package com.sqdecor.client.compendium;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sqdecor.SQDecor;
import com.sqdecor.client.compendium.Compendium.Chapter;
import com.sqdecor.client.compendium.Compendium.Entry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class CompendiumScreen extends Screen {
    private static final int FRAME = 0xFF2B2420;
    private static final int SIDEBAR = 0xFF3A312B;
    private static final int SIDEBAR_TEXT = 0xFFE8DCC8;
    private static final int SIDEBAR_DIM = 0xFFA89880;
    private static final int SELECTED = 0xFF5C4B3E;
    private static final int HOVER = 0xFF4A3E35;
    private static final int PAGE = 0xFFF2E8D5;
    private static final int PAGE_EDGE = 0xFFD9CBB0;
    private static final int TEXT = 0xFF3B2F25;
    private static final int TEXT_DIM = 0xFF8A7A66;
    private static final int ACCENT = 0xFFB5651D;
    private static final int SLOT_DARK = 0xFF8C7B66;
    private static final int SLOT_LIGHT = 0xFFFFFAF0;
    private static final int SLOT = 0xFFE3D6BE;

    private static final int SIDE_W = 116;
    private static final int CHAPTER_ROW = 18;
    private static final int ENTRY_ROW = 14;
    private static final int HEADER_H = 30;
    private static final int FOOTER_H = 20;
    private static final int PICTURE = 32;
    private static final int RECIPE_H = 72;
    private static final int CUTTER_H = 34;

    private static Chapter openChapter;
    private static Entry openEntry;

    private int left, top, w, h;
    private EditBox search;
    private Button prev, next;
    private double sideScroll;
    private int page;
    private List<List<Block>> pages = List.of();
    private final List<Row> rows = new ArrayList<>();
    private ItemStack hovered = ItemStack.EMPTY;

    public CompendiumScreen() {
        super(Component.translatable("compendium.sqdecor.title"));
    }

    @Override
    protected void init() {
        w = Math.min(360, width - 16);
        h = Math.min(224, height - 16);
        left = (width - w) / 2;
        top = (height - h) / 2;

        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, left + 6, top + 6, SIDE_W - 12, 14, Component.translatable("compendium.sqdecor.search"));
        search.setHint(Component.translatable("compendium.sqdecor.search").withStyle(ChatFormatting.GRAY));
        search.setValue(query);
        search.setResponder(text -> {
            sideScroll = 0;
            buildRows();
        });
        addRenderableWidget(search);

        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1))
                .bounds(left + SIDE_W + 8, top + h - FOOTER_H, 20, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1))
                .bounds(left + w - 28, top + h - FOOTER_H, 20, 16).build());

        if (openEntry == null) {
            openChapter = Compendium.chapters().get(0);
            openEntry = openChapter.entries().get(0);
        }
        buildRows();
        layout();
    }

    private record Row(Chapter chapter, Entry entry) {}

    private void buildRows() {
        rows.clear();
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        for (Chapter chapter : Compendium.chapters()) {
            if (query.isEmpty()) {
                rows.add(new Row(chapter, null));
                if (chapter == openChapter) chapter.entries().forEach(e -> rows.add(new Row(chapter, e)));
            } else {
                for (Entry entry : chapter.entries()) {
                    if (matches(entry, query)) rows.add(new Row(chapter, entry));
                }
            }
        }
    }

    private static boolean matches(Entry entry, String query) {
        if (entry.title().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        if (entry.text().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        return entry.pictures().stream().anyMatch(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query));
    }

    private static int rowHeight(Row row) {
        return row.entry == null ? CHAPTER_ROW : ENTRY_ROW;
    }

    private int listTop() {
        return top + 26;
    }

    private int listBottom() {
        return top + h - 6;
    }

    private int listHeight() {
        return rows.stream().mapToInt(CompendiumScreen::rowHeight).sum();
    }

    private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
        g.enableScissor(left + 2, listTop(), left + SIDE_W, listBottom());
        int y = listTop() - (int) sideScroll;
        boolean searching = !search.getValue().isBlank();
        for (Row row : rows) {
            int rh = rowHeight(row);
            boolean over = mouseX >= left + 2 && mouseX < left + SIDE_W && mouseY >= y && mouseY < y + rh
                    && mouseY >= listTop() && mouseY < listBottom();
            boolean selected = row.entry != null ? row.entry == openEntry : row.chapter == openChapter;
            if (selected && row.entry != null) g.fill(left + 2, y, left + SIDE_W, y + rh, SELECTED);
            else if (over) g.fill(left + 2, y, left + SIDE_W, y + rh, HOVER);

            if (row.entry == null) {
                g.renderItem(row.chapter.icon(), left + 6, y + 1);
                String title = font.plainSubstrByWidth(row.chapter.title().getString(), SIDE_W - 30);
                g.drawString(font, Component.literal(title).withStyle(ChatFormatting.BOLD), left + 25, y + 5,
                        selected ? 0xFFFFD08A : SIDEBAR_TEXT, false);
            } else {
                int indent = searching ? 6 : 14;
                ItemStack icon = row.entry.pictures().isEmpty() ? row.chapter.icon() : row.entry.pictures().get(0);
                g.pose().pushPose();
                g.pose().translate(left + indent, y + 1, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.renderItem(icon, 0, 0);
                g.pose().popPose();
                String title = font.plainSubstrByWidth(row.entry.title().getString(), SIDE_W - indent - 18);
                g.drawString(font, title, left + indent + 15, y + 3, selected ? 0xFFFFFFFF : SIDEBAR_DIM, false);
            }
            y += rh;
        }
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable("compendium.sqdecor.no_results"), left + 8, listTop() + 4, SIDEBAR_DIM, false);
        }
        g.disableScissor();
    }

    private boolean clickSidebar(double mouseX, double mouseY) {
        if (mouseX < left + 2 || mouseX >= left + SIDE_W || mouseY < listTop() || mouseY >= listBottom()) return false;
        int y = listTop() - (int) sideScroll;
        for (Row row : rows) {
            int rh = rowHeight(row);
            if (mouseY >= y && mouseY < y + rh) {
                if (row.entry != null) open(row.chapter, row.entry);
                else open(row.chapter, row.chapter.entries().get(0));
                return true;
            }
            y += rh;
        }
        return false;
    }

    private void open(Chapter chapter, Entry entry) {
        boolean newChapter = chapter != openChapter;
        openChapter = chapter;
        openEntry = entry;
        page = 0;
        buildRows();
        if (newChapter && search.getValue().isBlank()) sideScroll = 0;
        layout();
    }

    private void turn(int delta) {
        page = Math.max(0, Math.min(pages.size() - 1, page + delta));
        updateButtons();
    }

    private void updateButtons() {
        prev.visible = pages.size() > 1;
        next.visible = pages.size() > 1;
        prev.active = page > 0;
        next.active = page < pages.size() - 1;
    }

    private int pageLeft() {
        return left + SIDE_W + 10;
    }

    private int pageWidth() {
        return w - SIDE_W - 20;
    }

    private interface Block {
        int height();

        void render(GuiGraphics g, int x, int y, int mouseX, int mouseY);

        default ItemStack itemAt(int x, int y, double mouseX, double mouseY) {
            return ItemStack.EMPTY;
        }
    }

    private void layout() {
        List<Block> blocks = new ArrayList<>();
        int width = pageWidth();

        List<ItemStack> pictures = openEntry.pictures();
        int perRow = Math.max(1, (width + 4) / (PICTURE + 4));
        for (int i = 0; i < pictures.size(); i += perRow) {
            blocks.add(new Pictures(pictures.subList(i, Math.min(pictures.size(), i + perRow))));
        }
        if (!pictures.isEmpty()) blocks.add(new Gap(4));

        for (String paragraph : openEntry.text().getString().split("\n", -1)) {
            if (paragraph.isBlank()) {
                blocks.add(new Gap(5));
                continue;
            }
            Component line;
            int color = TEXT;
            if (paragraph.startsWith("# ")) {
                line = Component.literal(paragraph.substring(2)).withStyle(Style.EMPTY.withBold(true));
                color = ACCENT;
            } else if (paragraph.startsWith("- ")) {
                line = Component.literal("• " + paragraph.substring(2));
            } else {
                line = Component.literal(paragraph);
            }
            List<FormattedCharSequence> wrapped = font.split(line, width);
            for (int i = 0; i < wrapped.size(); i++) {
                boolean indent = i > 0 && paragraph.startsWith("- ");
                blocks.add(new Line(wrapped.get(i), color, indent ? 7 : 0));
            }
        }

        List<RecipeHolder<?>> recipes = recipesFor(openEntry.recipes());
        if (!recipes.isEmpty()) {
            blocks.add(new Gap(6));
            blocks.add(new Line(Component.translatable("compendium.sqdecor.recipes")
                    .withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(), ACCENT, 0));
            for (RecipeHolder<?> recipe : recipes) blocks.add(new RecipeBlock(recipe.value()));
        }

        int pageHeight = h - HEADER_H - FOOTER_H - 6;
        List<List<Block>> result = new ArrayList<>();
        List<Block> current = new ArrayList<>();
        int used = 0;
        for (Block block : blocks) {
            if (used + block.height() > pageHeight && !current.isEmpty()) {
                result.add(current);
                current = new ArrayList<>();
                used = 0;
                if (block instanceof Gap) continue;
            }
            current.add(block);
            used += block.height();
        }
        if (!current.isEmpty()) result.add(current);
        if (result.isEmpty()) result.add(List.of());
        pages = result;
        page = Math.min(page, pages.size() - 1);
        updateButtons();
    }

    private static List<RecipeHolder<?>> recipesFor(List<ItemStack> outputs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || outputs.isEmpty()) return List.of();
        List<RecipeHolder<?>> found = new ArrayList<>();
        for (ItemStack output : outputs) {
            for (RecipeHolder<?> holder : mc.level.getRecipeManager().getRecipes()) {
                Recipe<?> recipe = holder.value();
                if (recipe.isSpecial() || recipe.getIngredients().isEmpty()) continue;
                if (!recipe.getResultItem(mc.level.registryAccess()).is(output.getItem())) continue;
                if (found.contains(holder)) continue;
                found.add(holder);
            }
        }
        found.sort((a, b) -> order(a.value()) - order(b.value()));
        return found;
    }

    private static int order(Recipe<?> recipe) {
        if (recipe.getType() == RecipeType.CRAFTING) return 0;
        if (recipe.getType() == RecipeType.STONECUTTING) return 1;
        return 2;
    }

    private record Gap(int height) implements Block {
        @Override
        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {}
    }

    private class Line implements Block {
        private final FormattedCharSequence text;
        private final int color;
        private final int indent;

        Line(FormattedCharSequence text, int color, int indent) {
            this.text = text;
            this.color = color;
            this.indent = indent;
        }

        @Override
        public int height() {
            return 10;
        }

        @Override
        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            g.drawString(font, text, x + indent, y, color, false);
        }
    }

    private class Pictures implements Block {
        private final List<ItemStack> items;

        Pictures(List<ItemStack> items) {
            this.items = items;
        }

        @Override
        public int height() {
            return PICTURE + 4;
        }

        @Override
        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            for (int i = 0; i < items.size(); i++) {
                int px = x + i * (PICTURE + 4);
                boolean over = mouseX >= px && mouseX < px + PICTURE && mouseY >= y && mouseY < y + PICTURE;
                g.fill(px, y, px + PICTURE, y + PICTURE, over ? PAGE_EDGE : 0xFFE9DDC6);
                g.pose().pushPose();
                g.pose().translate(px, y, 0);
                g.pose().scale(2, 2, 1);
                g.renderItem(items.get(i), 0, 0);
                g.pose().popPose();
            }
        }

        @Override
        public ItemStack itemAt(int x, int y, double mouseX, double mouseY) {
            if (mouseY < y || mouseY >= y + PICTURE || mouseX < x) return ItemStack.EMPTY;
            int i = (int) (mouseX - x) / (PICTURE + 4);
            if (i >= items.size() || mouseX - x - i * (PICTURE + 4) >= PICTURE) return ItemStack.EMPTY;
            return items.get(i);
        }
    }

    private class RecipeBlock implements Block {
        private final Recipe<?> recipe;
        private final boolean cutter;

        RecipeBlock(Recipe<?> recipe) {
            this.recipe = recipe;
            this.cutter = recipe.getIngredients().size() == 1 && recipe.getType() != RecipeType.CRAFTING;
        }

        @Override
        public int height() {
            return cutter ? CUTTER_H : RECIPE_H;
        }

        private String kind() {
            if (recipe.getType() == RecipeType.CRAFTING) return "crafting";
            if (recipe.getType() == RecipeType.STONECUTTING) return "stonecutting";
            if (recipe.getType() == RecipeType.SMELTING) return "smelting";
            return "other";
        }

        private Ingredient slot(int col, int row) {
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            if (cutter) return col == 0 && row == 0 ? ingredients.get(0) : null;
            int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            int height = recipe instanceof ShapedRecipe shaped ? shaped.getHeight() : 3;
            if (col >= width || row >= height) return null;
            int index = row * width + col;
            return index < ingredients.size() ? ingredients.get(index) : null;
        }

        private int gridRows() {
            return cutter ? 1 : 3;
        }

        private int gridCols() {
            return cutter ? 1 : 3;
        }

        private int resultX(int x) {
            return x + gridCols() * 18 + 26;
        }

        private int gridY(int y) {
            return y + 12;
        }

        @Override
        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            g.drawString(font, Component.translatable("compendium.sqdecor.recipe." + kind()), x, y + 2, TEXT_DIM, false);
            int gy = gridY(y);
            for (int row = 0; row < gridRows(); row++) {
                for (int col = 0; col < gridCols(); col++) {
                    int sx = x + col * 18;
                    int sy = gy + row * 18;
                    slotBackground(g, sx, sy);
                    ItemStack stack = shown(slot(col, row));
                    if (!stack.isEmpty()) g.renderItem(stack, sx + 1, sy + 1);
                }
            }
            int arrowY = gy + (gridRows() * 18) / 2 - 4;
            g.drawString(font, "→", x + gridCols() * 18 + 9, arrowY, TEXT, false);
            int rx = resultX(x);
            int ry = gy + (gridRows() * 18) / 2 - 9;
            slotBackground(g, rx, ry);
            ItemStack result = result();
            g.renderItem(result, rx + 1, ry + 1);
            g.renderItemDecorations(font, result, rx + 1, ry + 1);
        }

        private ItemStack result() {
            Minecraft mc = Minecraft.getInstance();
            return mc.level == null ? ItemStack.EMPTY : recipe.getResultItem(mc.level.registryAccess());
        }

        @Override
        public ItemStack itemAt(int x, int y, double mouseX, double mouseY) {
            int gy = gridY(y);
            for (int row = 0; row < gridRows(); row++) {
                for (int col = 0; col < gridCols(); col++) {
                    int sx = x + col * 18;
                    int sy = gy + row * 18;
                    if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) return shown(slot(col, row));
                }
            }
            int rx = resultX(x);
            int ry = gy + (gridRows() * 18) / 2 - 9;
            if (mouseX >= rx && mouseX < rx + 18 && mouseY >= ry && mouseY < ry + 18) return result();
            return ItemStack.EMPTY;
        }
    }

    private static void slotBackground(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT_DARK);
        g.fill(x + 1, y + 1, x + 18, y + 18, SLOT_LIGHT);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
    }

    private static ItemStack shown(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return ItemStack.EMPTY;
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        return items[(int) (System.currentTimeMillis() / 1000 % items.length)];
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + w, top + h, FRAME);
        g.fill(left + 2, top + 2, left + SIDE_W, top + h - 2, SIDEBAR);
        g.fill(left + SIDE_W + 2, top + 2, left + w - 2, top + h - 2, PAGE);
        g.fill(left + SIDE_W + 2, top + 2, left + SIDE_W + 4, top + h - 2, PAGE_EDGE);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderSidebar(g, mouseX, mouseY);

        int px = pageLeft();
        int pw = pageWidth();
        g.drawString(font, openChapter.title(), px, top + 6, TEXT_DIM, false);
        g.drawString(font, openEntry.title().copy().withStyle(ChatFormatting.BOLD), px, top + 17, TEXT, false);
        g.fill(px, top + HEADER_H - 2, px + pw, top + HEADER_H - 1, PAGE_EDGE);

        hovered = ItemStack.EMPTY;
        int y = top + HEADER_H + 3;
        for (Block block : pages.get(page)) {
            block.render(g, px, y, mouseX, mouseY);
            ItemStack at = block.itemAt(px, y, mouseX, mouseY);
            if (!at.isEmpty()) hovered = at;
            y += block.height();
        }

        if (pages.size() > 1) {
            Component number = Component.translatable("compendium.sqdecor.page", page + 1, pages.size());
            g.drawString(font, number, px + pw / 2 - font.width(number) / 2, top + h - FOOTER_H + 4, TEXT_DIM, false);
        }
        if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && clickSidebar(mouseX, mouseY)) return true;
        if (button == 0 && !hovered.isEmpty()) {
            Item item = hovered.getItem();
            for (Chapter chapter : Compendium.chapters()) {
                for (Entry entry : chapter.entries()) {
                    if (entry == openEntry) continue;
                    boolean ours = BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(SQDecor.MODID);
                    if (ours && entry.recipes().stream().anyMatch(s -> s.is(item))) {
                        open(chapter, entry);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < left + SIDE_W) {
            int max = Math.max(0, listHeight() - (listBottom() - listTop()));
            sideScroll = Math.max(0, Math.min(max, sideScroll - scrollY * 14));
        } else {
            turn(scrollY > 0 ? -1 : 1);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!search.isFocused()) {
            if (keyCode == 262 || keyCode == 267) {
                turn(1);
                return true;
            }
            if (keyCode == 263 || keyCode == 266) {
                turn(-1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
