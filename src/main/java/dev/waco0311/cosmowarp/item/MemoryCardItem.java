package dev.waco0311.cosmowarp.item;

import dev.waco0311.cosmowarp.Config;
import dev.waco0311.cosmowarp.data.WarpPoint;
import dev.waco0311.cosmowarp.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Cheap, non-Moon-derived recording medium. Can be inserted into a Warp Drive to register
 * locations (same as a Warp Crystal) and holds a capacity-limited list of WarpPoints, but cannot
 * be used to actually trigger a warp -- see WarpDriveBlockEntity#beginWarp(). Use a Crystal
 * Driver to copy its saved points onto a real Warp Crystal (or delete them).
 */
public class MemoryCardItem extends Item {

    private static final int MAX_NAMES_SHOWN = 5;

    public MemoryCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.translatable("item.cosmowarp.memory_card.tooltip.cannot_warp")
                .withStyle(ChatFormatting.RED));

        List<WarpPoint> points = stack.getOrDefault(ModDataComponents.WARP_POINTS.get(), List.of());
        int capacity = Config.MEMORY_CARD_CAPACITY.get();

        if (points.isEmpty()) {
            tooltip.add(Component.translatable("item.cosmowarp.memory_card.tooltip.empty", capacity)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("item.cosmowarp.memory_card.tooltip.count", points.size(), capacity)
                .withStyle(ChatFormatting.GRAY));

        int shown = Math.min(points.size(), MAX_NAMES_SHOWN);
        for (int i = 0; i < shown; i++) {
            tooltip.add(Component.literal(" \u2022 " + points.get(i).name())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (points.size() > MAX_NAMES_SHOWN) {
            tooltip.add(Component.translatable("item.cosmowarp.warp_crystal.tooltip.more", points.size() - MAX_NAMES_SHOWN)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
