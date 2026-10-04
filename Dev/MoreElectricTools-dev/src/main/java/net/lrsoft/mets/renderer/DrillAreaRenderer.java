package net.lrsoft.mets.renderer;

import java.util.List;

import net.lrsoft.mets.MoreElectricTools;
import net.lrsoft.mets.item.ItemOsmiumDrill;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = MoreElectricTools.MODID, value = Side.CLIENT)
public class DrillAreaRenderer {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        EntityPlayer player = event.getPlayer();
        ItemStack heldItem = player.getHeldItemMainhand();

        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemOsmiumDrill)) return;

        ItemOsmiumDrill drill = (ItemOsmiumDrill) heldItem.getItem();
        ItemOsmiumDrill.DrillMode mode = drill.getMode(heldItem);

        // Подсвечиваем только area-режимы
        if (!mode.isAreaMode()) return;

        RayTraceResult target = event.getTarget();
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return;

        BlockPos center = target.getBlockPos();
        EnumFacing sideHit = target.sideHit;
        List<BlockPos> blocks = drill.getBlocksToBreak(player.world, center, mode, sideHit);

        event.setCanceled(true);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

        for (BlockPos pos : blocks) {
            IBlockState state = player.world.getBlockState(pos);
            if (state.getBlock().isAir(state, player.world, pos)) continue;

            AxisAlignedBB box = state.getBlock().getSelectedBoundingBox(state, player.world, pos)
                    .grow(0.002).offset(-px, -py, -pz);

            RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 0.4F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}