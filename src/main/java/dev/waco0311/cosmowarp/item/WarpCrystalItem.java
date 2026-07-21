package dev.waco0311.cosmowarp.item;

import dev.waco0311.cosmowarp.data.WarpPoint;
import dev.waco0311.cosmowarp.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WarpCrystalItem extends Item {

    private static final int MAX_NAMES_SHOWN = 5;

    public WarpCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        List<WarpPoint> points = stack.getOrDefault(ModDataComponents.WARP_POINTS.get(), List.of());

        if (points.isEmpty()) {
            tooltip.add(Component.translatable("item.cosmowarp.warp_crystal.tooltip.empty")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("item.cosmowarp.warp_crystal.tooltip.count", points.size())
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
