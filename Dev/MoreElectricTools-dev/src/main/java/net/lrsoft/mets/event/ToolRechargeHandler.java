package net.lrsoft.mets.event;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.lrsoft.mets.MoreElectricTools;
import net.lrsoft.mets.armor.AdvancedQuantumSuit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MoreElectricTools.MODID)
public class ToolRechargeHandler {

    /**
     * Срабатывает ДО проверки «можно ли ломать блок».
     * Заряжает инструмент ТОЛЬКО если заряд полностью 0 — чтобы дать возможность
     * сломать первый блок. Всё остальное делает onArmorTick после использования.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntityPlayer().world.isRemote) return;

        EntityPlayer player = event.getEntityPlayer();
        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (chest.isEmpty() || !(chest.getItem() instanceof AdvancedQuantumSuit)) return;

        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || !(held.getItem() instanceof IElectricItem)) return;

        IElectricItem electricItem = (IElectricItem) held.getItem();
        double maxCharge = electricItem.getMaxCharge(held);
        double currentCharge = ElectricItem.manager.getCharge(held);

        // Заряжаем только при полном нуле — чтобы запустить процесс
        if (currentCharge > 0) return;

        double available = ElectricItem.manager.getCharge(chest);
        if (available <= 0) return;

        // Даём полный заряд — чтобы сломать блок с нормальной скоростью
        double transfer = Math.min(maxCharge - 1.0, available);

        ElectricItem.manager.discharge(chest, transfer, Integer.MAX_VALUE, true, false, false);
        ElectricItem.manager.charge(held, transfer, Integer.MAX_VALUE, true, false);

        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
        }
    }
}