package dev.waco0311.cosmowarp.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.waco0311.cosmowarp.Config;
import dev.waco0311.cosmowarp.block.WarpDriveBlock;
import dev.waco0311.cosmowarp.block.entity.WarpDriveBlockEntity;
import dev.waco0311.cosmowarp.data.WarpPoint;
import dev.waco0311.cosmowarp.util.FeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Draws text on the Warp Drive's console screen (the "display" element in the Blockbench model:
 * from [3,12,0] to [13,13,5], tilted -22.5deg around X at origin [8,11.75,0]).
 *
 * The exact position/tilt/scale below are a first-pass approximation from the raw Blockbench
 * numbers -- they will very likely need small numeric tweaks once seen rendered in-game (nudge
 * PANEL_* constants and re-test rather than re-deriving from scratch).
 *
 * DIAGNOSTIC BUILD: SURFACE_OFFSET is temporarily blown way up (0.6, more than half a block) so
 * it's obvious in-game which direction the text moves in. Once we know the correct sign/direction,
 * dial this back down to something small like 0.02 again.
 */
public class WarpDriveBlockEntityRenderer implements BlockEntityRenderer<WarpDriveBlockEntity> {

    // Blockbench "display" element center, converted from 0-16 units to 0-1 block-space:
    // center = ((3+13)/2, (12+13)/2, (0+5)/2) / 16 = (8, 12.5, 2.5) / 16

    // Blockbench "display" element: from [3,12,0] to [13,13,5], rotation.origin [8,11.75,0].
    // The rotation ORIGIN (pivot) is not the box's own center -- using the wrong point here was
    // the previous bug (text swung far off to the side once rotated).
    private static final double ROTATION_ORIGIN_X = 8.0 / 16.0;
    private static final double ROTATION_ORIGIN_Y = 11.75 / 16.0;
    private static final double ROTATION_ORIGIN_Z = 0.0 / 16.0;

    // Box center relative to the rotation origin (still in the box's own unrotated local space);
    // this offset gets carried along correctly once it's translated AFTER the pivot rotation.
    private static final double BOX_OFFSET_X = 0.0;
    private static final double BOX_OFFSET_Y = 0.75 / 16.0;
    private static final double BOX_OFFSET_Z = 2.5 / 16.0;

    // Panel tilt in the Blockbench model (rotation.angle for the "display" element).
    private static final float PANEL_TILT_DEGREES = -22.5f;

    // Nudges text off the panel surface so it doesn't z-fight with the console glass.
    // TEMPORARILY BLOWN UP for direction testing -- normally ~0.02.
    private static final double SURFACE_OFFSET = -0.035;

    private static final float TEXT_SCALE = 0.0045f;

    public WarpDriveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WarpDriveBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Direction facing = be.getBlockState().getValue(WarpDriveBlock.FACING);
        String text = computeDisplayText(be);
        if (text.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        poseStack.pushPose();

        // Move to the block's center, then rotate to match FACING (blockstate rotates the whole
        // model around Y the same way the blockstates/warp_drive.json variants do).
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facingYRotation(facing)));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Rotate around the panel's ACTUAL pivot (not the box's own center), matching exactly
        // how the block model itself is rotated, then move out to the box's center and reorient
        // the text plane to lie against the tilted "up" face (the only face this element has a
        // texture on -- there's no "south"/front face, so this is a dashboard-style screen you
        // look down onto, not a vertical sign facing outward).
        poseStack.translate(ROTATION_ORIGIN_X, ROTATION_ORIGIN_Y, ROTATION_ORIGIN_Z);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(PANEL_TILT_DEGREES));
        poseStack.translate(BOX_OFFSET_X, BOX_OFFSET_Y, BOX_OFFSET_Z);

        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f));
        poseStack.translate(0, 0, SURFACE_OFFSET);

        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        int width = font.width(text);
        poseStack.translate(-width / 2.0, -font.lineHeight / 2.0, 0);

        font.drawInBatch(text, 0, 0, 0xFFE0FFFF, false, poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    /** Matches the y-rotation used in blockstates/warp_drive.json for each FACING value. */
    private static float facingYRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f; // NORTH
        };
    }

    private static String computeDisplayText(WarpDriveBlockEntity be) {
        Config.DisplayMode mode = Config.WARP_DRIVE_DISPLAY_MODE.get();
        return switch (mode) {
            case FE -> FeFormat.shortForm(be.getEnergyStorage().getEnergyStored());
            case COUNTDOWN -> be.isCharging()
                    ? String.format("%.1fs", be.getChargeTicksRemaining() / 20f)
                    : "";
            case COORDINATES -> selectedPoint(be)
                    .map(p -> "X:" + p.pos().getX() + " Y:" + p.pos().getY() + " Z:" + p.pos().getZ())
                    .orElse("");
            case POINT_NAME -> selectedPoint(be).map(WarpPoint::name).orElse("");
        };
    }

    private static Optional<WarpPoint> selectedPoint(WarpDriveBlockEntity be) {
        Optional<UUID> selected = be.getSelectedId();
        if (selected.isEmpty()) return Optional.empty();
        List<WarpPoint> points = be.getWarpPoints();
        return points.stream().filter(p -> p.id().equals(selected.get())).findFirst();
    }
}