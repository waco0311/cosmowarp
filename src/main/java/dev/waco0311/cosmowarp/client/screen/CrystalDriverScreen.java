package dev.waco0311.cosmowarp.client.screen;

import dev.waco0311.cosmowarp.data.WarpPoint;
import dev.waco0311.cosmowarp.menu.CrystalDriverMenu;
import dev.waco0311.cosmowarp.network.CrystalDriverActionPayload;
import dev.waco0311.cosmowarp.network.CrystalDriverSelectPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public class CrystalDriverScreen extends AbstractContainerScreen<CrystalDriverMenu> {

    private static final int PANEL_W = 200;
    private static final int PANEL_H = CrystalDriverMenu.PANEL_H;

    private static final int LIST_X = 10;
    private static final int LIST_Y = 32;
    private static final int LIST_W = 180;
    private static final int ROW_H = 14;
    private static final int LIST_ROWS = 5;
    private static final int LIST_H = ROW_H * LIST_ROWS; // 70, ends at 102

    // Scrollbar sits just outside the list's existing 2px background margin, still well within
    // the panel (LIST_X + LIST_W + 5 = 195, panel is 200 wide).
    private static final int SCROLLBAR_W = 3;
    private static final int SCROLLBAR_X = LIST_X + LIST_W + 2;

    private static final int COPY_BTN_Y = LIST_Y + LIST_H + 8; // 110
    private static final int BTN_W = 87;

    private static final int COLOR_PANEL_BG = 0xFF2B2B2B;
    private static final int COLOR_LIST_BG = 0xFF1A1A1A;
    private static final int COLOR_ROW = 0xFF000000;
    private static final int COLOR_ROW_SELECTED = 0xFF4A6FA5;
    private static final int COLOR_TEXT = 0xFFE4E4E4;
    private static final int COLOR_TEXT_MUTED = 0xFF9A9A9A;
    private static final int COLOR_SLOT_BG = 0xFF3A3A3A;
    private static final int COLOR_SCROLLBAR_TRACK = 0xFF141414;
    private static final int COLOR_SCROLLBAR_THUMB = 0xFF6A6A6A;

    private List<WarpPoint> cachedPoints = List.of();
    private UUID selected = null;
    private Button copyButton;
    private Button deleteButton;

    // How many rows the list is scrolled down by. See the matching field in WarpDriveScreen for
    // why this is re-clamped every time cachedPoints changes.
    private int scrollOffset = 0;

    public CrystalDriverScreen(CrystalDriverMenu menu, net.minecraft.world.entity.player.Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = CrystalDriverMenu.HOTBAR_Y + 18 + 6;
        this.inventoryLabelY = PANEL_H + 4;
    }

    @Override
    protected void init() {
        super.init();

        copyButton = addRenderableWidget(Button.builder(Component.translatable("gui.cosmowarp.copy_to_target"),
                        b -> sendAction(CrystalDriverActionPayload.Action.COPY))
                .bounds(leftPos + LIST_X, topPos + COPY_BTN_Y, BTN_W, 18)
                .build());

        deleteButton = addRenderableWidget(Button.builder(Component.translatable("gui.cosmowarp.delete"),
                        b -> sendAction(CrystalDriverActionPayload.Action.DELETE))
                .bounds(leftPos + LIST_X + BTN_W + 6, topPos + COPY_BTN_Y, BTN_W, 18)
                .build());

        refreshFromCrystal();
    }

    private void sendAction(CrystalDriverActionPayload.Action action) {
        PacketDistributor.sendToServer(new CrystalDriverActionPayload(action));
    }

    private void selectPoint(UUID id) {
        this.selected = id;
        PacketDistributor.sendToServer(new CrystalDriverSelectPayload(id));
    }

    private int maxScroll() {
        return Math.max(0, cachedPoints.size() - LIST_ROWS);
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
    }

    private void refreshFromCrystal() {
        cachedPoints = menu.blockEntity.getSourcePoints();
        clampScroll();
        selected = menu.blockEntity.getSelectedId().orElse(null);

        boolean hasTarget = menu.blockEntity.hasTarget();
        boolean hasSelection = selected != null;
        // copy needs source + target + a selection; delete needs source + selection, and only
        // while target is empty (mirrors the block's documented behaviour).
        copyButton.active = hasTarget && hasSelection;
        deleteButton.active = !hasTarget && hasSelection;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshFromCrystal();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + PANEL_H, COLOR_PANEL_BG);

        // slot backdrops
        int srcX = leftPos + CrystalDriverMenu.SOURCE_SLOT_X - 1;
        int srcY = topPos + CrystalDriverMenu.SOURCE_SLOT_Y - 1;
        graphics.fill(srcX, srcY, srcX + 18, srcY + 18, COLOR_SLOT_BG);
        graphics.drawString(font, "1", leftPos + CrystalDriverMenu.SOURCE_SLOT_X + 21, topPos + CrystalDriverMenu.SOURCE_SLOT_Y + 5, COLOR_TEXT_MUTED, false);

        int tgtX = leftPos + CrystalDriverMenu.TARGET_SLOT_X - 1;
        int tgtY = topPos + CrystalDriverMenu.TARGET_SLOT_Y - 1;
        graphics.fill(tgtX, tgtY, tgtX + 18, tgtY + 18, COLOR_SLOT_BG);
        graphics.drawString(font, "2", leftPos + CrystalDriverMenu.TARGET_SLOT_X + 21, topPos + CrystalDriverMenu.TARGET_SLOT_Y + 5, COLOR_TEXT_MUTED, false);

        // source list
        graphics.fill(leftPos + LIST_X - 2, topPos + LIST_Y - 2,
                leftPos + LIST_X + LIST_W + 2, topPos + LIST_Y + LIST_H + 2, COLOR_LIST_BG);

        int visibleCount = Math.min(LIST_ROWS, cachedPoints.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            WarpPoint p = cachedPoints.get(scrollOffset + i);
            int rowY = topPos + LIST_Y + i * ROW_H;
            int color = p.id().equals(selected) ? COLOR_ROW_SELECTED : COLOR_ROW;
            graphics.fill(leftPos + LIST_X, rowY, leftPos + LIST_X + LIST_W, rowY + ROW_H - 1, color);
            graphics.drawString(font, trimToWidth(p.name(), LIST_W - 6), leftPos + LIST_X + 3, rowY + 3, COLOR_TEXT, false);
        }

        // --- scrollbar (only when there's actually something to scroll to) ---
        if (cachedPoints.size() > LIST_ROWS) {
            int trackX = leftPos + SCROLLBAR_X;
            int trackY = topPos + LIST_Y;
            graphics.fill(trackX, trackY, trackX + SCROLLBAR_W, trackY + LIST_H, COLOR_SCROLLBAR_TRACK);

            int thumbH = Math.max(8, LIST_H * LIST_ROWS / cachedPoints.size());
            int maxThumbY = LIST_H - thumbH;
            int thumbY = maxScroll() == 0 ? 0 : maxThumbY * scrollOffset / maxScroll();
            graphics.fill(trackX, trackY + thumbY, trackX + SCROLLBAR_W, trackY + thumbY + thumbH, COLOR_SCROLLBAR_THUMB);
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (!text.isEmpty() && font.width(text + "..") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "..";
    }

    private boolean isOverList(double mouseX, double mouseY) {
        return mouseX >= leftPos + LIST_X && mouseX <= leftPos + LIST_X + LIST_W
                && mouseY >= topPos + LIST_Y && mouseY <= topPos + LIST_Y + LIST_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int visibleCount = Math.min(LIST_ROWS, cachedPoints.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            int rowY = topPos + LIST_Y + i * ROW_H;
            if (mouseX >= leftPos + LIST_X && mouseX <= leftPos + LIST_X + LIST_W
                    && mouseY >= rowY && mouseY <= rowY + ROW_H - 1) {
                selectPoint(cachedPoints.get(scrollOffset + i).id());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverList(mouseX, mouseY) && cachedPoints.size() > LIST_ROWS) {
            scrollOffset -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COLOR_TEXT_MUTED, false);
    }
}
