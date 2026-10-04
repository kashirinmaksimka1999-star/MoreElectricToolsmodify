package net.lrsoft.mets.renderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.lrsoft.mets.MoreElectricTools;
import net.lrsoft.mets.item.ItemOsmiumDrill;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = MoreElectricTools.MODID, value = Side.CLIENT)
public class DrillBreakAnimHandler {

    private static Map<Integer, BlockPos> lastAffectedBlocks = new HashMap<>();

    private static int progressTick = 0;
    private static final int MAX_TICKS = 10;

    // Запоминаем текущий центральный блок, чтобы сбрасывать прогресс при смене
    private static BlockPos lastCenter = null;

    // Базовый id для генерации уникальных breakerId
    private static final int BASE_ID = 1_000_000;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (mc.world == null || player == null) {
            clearOld(mc);
            return;
        }

        ItemStack held = player.getHeldItemMainhand();
        boolean isDrill = !held.isEmpty() && held.getItem() instanceof ItemOsmiumDrill;

        Map<Integer, BlockPos> newBlocks = new HashMap<>();

        if (isDrill) {
            ItemOsmiumDrill drill = (ItemOsmiumDrill) held.getItem();
            ItemOsmiumDrill.DrillMode mode = drill.getMode(held);

            boolean holdingAttack = mc.gameSettings.keyBindAttack.isKeyDown();
            RayTraceResult target = mc.objectMouseOver;

            if (mode.isAreaMode() && holdingAttack && target != null
                    && target.typeOfHit == RayTraceResult.Type.BLOCK) {

                BlockPos center = target.getBlockPos();
                EnumFacing sideHit = target.sideHit;

                // === КЛЮЧЕВОЙ МОМЕНТ: если центр сменился — сбрасываем прогресс ===
                if (lastCenter == null || !lastCenter.equals(center)) {
                    progressTick = 0;
                    lastCenter = center;
                }

                progressTick++;
                if (progressTick > MAX_TICKS) progressTick = MAX_TICKS;

                int breakProgress = (int)((progressTick / (float) MAX_TICKS) * 10);
                if (breakProgress > 9) breakProgress = 9;

                List<BlockPos> blocks = drill.getBlocksToBreak(mc.world, center, mode, sideHit);

                int index = 0;
                for (BlockPos pos : blocks) {
                    int uniqueId = BASE_ID + index;
                    mc.renderGlobal.sendBlockBreakProgress(uniqueId, pos, breakProgress);
                    newBlocks.put(uniqueId, pos);
                    index++;
                }
            } else {
                // Не копаем — сбрасываем
                progressTick = 0;
                lastCenter = null;
            }
        } else {
            progressTick = 0;
            lastCenter = null;
        }

        // Очищаем блоки, которых нет в новом списке
        for (Map.Entry<Integer, BlockPos> entry : lastAffectedBlocks.entrySet()) {
            if (!newBlocks.containsKey(entry.getKey())) {
                mc.renderGlobal.sendBlockBreakProgress(entry.getKey(), entry.getValue(), -1);
            }
        }

        lastAffectedBlocks = newBlocks;
    }

    private static void clearOld(Minecraft mc) {
        if (mc.world != null) {
            for (Map.Entry<Integer, BlockPos> entry : lastAffectedBlocks.entrySet()) {
                mc.renderGlobal.sendBlockBreakProgress(entry.getKey(), entry.getValue(), -1);
            }
        }
        lastAffectedBlocks = new HashMap<>();
        progressTick = 0;
        lastCenter = null;
    }
}