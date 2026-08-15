package dev.waco0311.cosmowarp.client.screen;

import dev.waco0311.cosmowarp.data.WarpPoint;
import dev.waco0311.cosmowarp.menu.WarpDriveMenu;
import dev.waco0311.cosmowarp.network.WarpDriveRenamePayload;
import dev.waco0311.cosmowarp.network.WarpDriveSelectPayload;
import dev.waco0311.cosmowarp.network.WarpDriveSimpleActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public class WarpDriveScreen extends AbstractContainerScreen<WarpDriveMenu> {

    // --- layout constants (relative to leftPos/topPos) ---
    // PANEL_H and the crystal slot position come from WarpDriveMenu (single source of truth,
    // shared with the server-side slot placement) so screen and menu can never drift apart.
    private static final int PANEL_W = 236;
    private static final int PANEL_H = WarpDriveMenu.PANEL_H;

    // Top row: crystal slot (left) mirrors FE header (right). Kept clear of the list below
    // so slot clicks and list-row clicks never overlap.
    private static final int SLOT_LABEL_X = WarpDriveMenu.CRYSTAL_SLOT_X + 22;
    private static final int SLOT_ROW_Y = WarpDriveMenu.CRYSTAL_SLOT_Y + 5;

    private static final int LIST_X = 10;
    private static final int LIST_Y = 30;
    private static final int LIST_W = 100;
    private static final int ROW_H = 14;
    private static final int LIST_ROWS = 6;
    private static final int LIST_H = ROW_H * LIST_ROWS; // 84, list ends at 114

    // Scrollbar sits in the gap between the list and the right-hand column, so it never eats
    // into the row text width.
    private static final int SCROLLBAR_W = 3;
    private static final int SCROLLBAR_X = LIST_X + LIST_W + 3;

    private static final int REGISTER_Y = LIST_Y + LIST_H + 8; // 122

    private static final int RIGHT_X = LIST_X + LIST_W + 12; // 122
    private static final int RIGHT_W = PANEL_W - RIGHT_X - 10;

    private static final int FE_HEADER_Y = SLOT_ROW_Y;
    private static final int FE_BAR_Y = 20;
    private static final int FE_BAR_H = 8;
    private static final int FE_AMOUNT_Y = 32;

    private static final int INFO_HEADER_Y = 54;
    private static final int NAME_BOX_Y = 66;
    private static final int DIM_TEXT_Y = 86;
    private static final int POS_TEXT_Y = 98;

    private static final int WARP_BTN_Y = 118; // ends at 138, same as register button

    // --- palette (matches the earlier mockup) ---
    private static final int COLOR_PANEL_BG = 0xFF2B2B2B;
    private static final int COLOR_LIST_BG = 0xFF1A1A1A;
    private static final int COLOR_ROW = 0xFF000000;
    private static final int COLOR_ROW_SELECTED = 0xFF4A6FA5;
    private static final int COLOR_BAR_TRACK = 0xFF404040;
    private static final int COLOR_BAR_FILL = 0xFFE0A020;
    private static final int COLOR_TEXT = 0xFFE4E4E4;
    private static final int COLOR_TEXT_MUTED = 0xFF9A9A9A;
    private static final int COLOR_SLOT_BG = 0xFF3A3A3A;
    private static final int COLOR_SCROLLBAR_TRACK = 0xFF141414;
    private static final int COLOR_SCROLLBAR_THUMB = 0xFF6A6A6A;

    private List<WarpPoint> cachedPoints = List.of();
    private UUID selected = null;
    private EditBox nameBox;
    private Button warpButton;

    // How many rows the list is scrolled down by. 0 = showing the first LIST_ROWS points.
    // Kept clamped to [0, maxScroll()] any time cachedPoints changes (see clampScroll()), since
    // the underlying list can shrink out from under an existing scroll position (e.g. a point
    // gets deleted elsewhere, or the crystal is swapped for one with fewer saved points).
    private int scrollOffset = 0;

    public WarpDriveScreen(WarpDriveMenu menu, net.minecraft.world.entity.player.Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = WarpDriveMenu.HOTBAR_Y + 18 + 6;
        this.inventoryLabelY = PANEL_H + 4;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();

        nameBox = new EditBox(font, leftPos + RIGHT_X, topPos + NAME_BOX_Y, RIGHT_W, 14, Component.literal("name"));
        nameBox.setMaxLength(48);
        nameBox.setBordered(true);
        nameBox.setResponder(this::onNameEdited);
        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.cosmowarp.register_here"),
                        b -> sendSimple(WarpDriveSimpleActionPayload.Action.REGISTER_HERE))
                .bounds(leftPos + LIST_X, topPos + REGISTER_Y, LIST_W, 16)
                .build());

        warpButton = addRenderableWidget(Button.builder(Component.translatable("gui.cosmowarp.warp"),
                        b -> sendSimple(WarpDriveSimpleActionPayload.Action.WARP))
                .bounds(leftPos + RIGHT_X, topPos + WARP_BTN_Y, RIGHT_W, 20)
                .build());

        refreshFromCrystal();
    }

    private void sendSimple(WarpDriveSimpleActionPayload.Action action) {
        PacketDistributor.sendToServer(new WarpDriveSimpleActionPayload(action));
    }

    private void onNameEdited(String text) {
        if (selected == null) return;
        PacketDistributor.sendToServer(new WarpDriveRenamePayload(selected, text));
    }

    private void selectPoint(UUID id) {
        this.selected = id;
        PacketDistributor.sendToServer(new WarpDriveSelectPayload(id));
        WarpPoint point = cachedPoints.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
        if (point != null) nameBox.setValue(point.name());
    }

    private int maxScroll() {
        return Math.max(0, cachedPoints.size() - LIST_ROWS);
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
    }

    private void refreshFromCrystal() {
        cachedPoints = menu.blockEntity.getWarpPoints();
        clampScroll();
        selected = menu.blockEntity.getSelectedId().orElse(null);
        if (selected != null && !nameBox.isFocused()) {
            cachedPoints.stream().filter(p -> p.id().equals(selected)).findFirst()
                    .ifPresent(p -> nameBox.setValue(p.name()));
        }
        if (warpButton != null) {
            warpButton.active = !menu.blockEntity.isCharging();
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // The crystal ItemStack (with its data components) is synced as normal slot contents,
        // so re-reading it each tick keeps the list/selection in sync without extra packets.
        refreshFromCrystal();
    }

    private WarpPoint selectedPoint() {
        if (selected == null) return null;
        return cachedPoints.stream().filter(p -> p.id().equals(selected)).findFirst().orElse(null);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + PANEL_H, COLOR_PANEL_BG);

        // --- crystal slot backdrop (18x18, matches vanilla slot size) so it doesn't look like
        // an empty void when no crystal is inserted ---
        int slotX = leftPos + WarpDriveMenu.CRYSTAL_SLOT_X - 1;
        int slotY = topPos + WarpDriveMenu.CRYSTAL_SLOT_Y - 1;
        graphics.fill(slotX, slotY, slotX + 18, slotY + 18, COLOR_SLOT_BG);
        graphics.drawString(font, "Crystal", leftPos + SLOT_LABEL_X, topPos + SLOT_ROW_Y, COLOR_TEXT_MUTED, false);

        // --- warp point list ---
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

        // --- FE bar ---
        graphics.drawString(font, "FE", leftPos + RIGHT_X, topPos + FE_HEADER_Y, COLOR_TEXT_MUTED, false);
        long stored = menu.getEnergyStored();
        long cap = menu.getEnergyCapacity();
        graphics.fill(leftPos + RIGHT_X, topPos + FE_BAR_Y, leftPos + RIGHT_X + RIGHT_W, topPos + FE_BAR_Y + FE_BAR_H, COLOR_BAR_TRACK);
        int filled = (int) (RIGHT_W * Math.min(1.0, (double) stored / (double) cap));
        if (filled > 0) {
            graphics.fill(leftPos + RIGHT_X, topPos + FE_BAR_Y, leftPos + RIGHT_X + filled, topPos + FE_BAR_Y + FE_BAR_H, COLOR_BAR_FILL);
        }
        graphics.drawString(font, formatFe(stored) + " / " + formatFe(cap),
                leftPos + RIGHT_X, topPos + FE_AMOUNT_Y, COLOR_TEXT_MUTED, false);

        // --- selected point info ---
        graphics.drawString(font, "Selected Point", leftPos + RIGHT_X, topPos + INFO_HEADER_Y, COLOR_TEXT_MUTED, false);
        WarpPoint sel = selectedPoint();
        if (sel != null) {
            drawScaledString(graphics, "dim: " + shortDimensionName(sel), leftPos + RIGHT_X, topPos + DIM_TEXT_Y, COLOR_TEXT_MUTED, 0.8f);
            drawScaledString(graphics, "x:" + sel.pos().getX() + " y:" + sel.pos().getY() + " z:" + sel.pos().getZ(),
                    leftPos + RIGHT_X, topPos + POS_TEXT_Y, COLOR_TEXT_MUTED, 0.8f);
        } else {
            graphics.drawString(font, "none selected", leftPos + RIGHT_X, topPos + DIM_TEXT_Y, COLOR_TEXT_MUTED, false);
        }

        // --- charging countdown (shown above the warp button while active) ---
        if (menu.blockEntity.isCharging()) {
            float seconds = menu.blockEntity.getChargeTicksRemaining() / 20f;
            graphics.drawString(font, String.format("Charging... %.1fs", seconds),
                    leftPos + RIGHT_X, topPos + WARP_BTN_Y - 12, COLOR_BAR_FILL, false);
        }
    }

    private static String shortDimensionName(WarpPoint point) {
        net.minecraft.resources.ResourceLocation loc = point.dimension().location();
        return loc.getNamespace().equals("minecraft") ? loc.getPath() : loc.toString();
    }

    /** Draws text at a smaller scale so it stays inside a narrow panel instead of overflowing it. */
    private void drawScaledString(GuiGraphics graphics, String text, int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static String formatFe(long value) {
        return String.format("%,d", value);
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
                nameBox.setFocused(false);
                selectPoint(cachedPoints.get(scrollOffset + i).id());
                return true;
            }
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        // Clicking anywhere that isn't the name box itself drops its focus, so a later 'E'
        // press closes the screen normally instead of being swallowed by the text field.
        if (!nameBox.isMouseOver(mouseX, mouseY)) {
            nameBox.setFocused(false);
        }

        return handled;
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC must always be able to close the screen, even while the rename box is focused.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (nameBox.isFocused()) {
            // Enter drops focus instead of inserting a newline, so a later 'E' press closes
            // the screen normally instead of being swallowed by the text field.
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                nameBox.setFocused(false);
                return true;
            }
            // Let the box handle its own navigation/editing keys (backspace, arrows, etc.);
            // for anything else, swallow the key instead of letting it fall through to the
            // inventory-close shortcut (E by default).
            nameBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title intentionally omitted: the top-left corner is used for the crystal slot instead.
        // Draw only the player inventory label (mirrors AbstractContainerScreen#renderLabels
        // minus the title.draw call).
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COLOR_TEXT_MUTED, false);
    }
}
