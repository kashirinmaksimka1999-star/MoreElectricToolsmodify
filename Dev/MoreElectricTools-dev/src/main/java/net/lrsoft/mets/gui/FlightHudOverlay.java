package net.lrsoft.mets.gui;

import ic2.api.item.ElectricItem;
import net.lrsoft.mets.MoreElectricTools;
import net.lrsoft.mets.armor.AdvancedQuantumSuit;
import net.lrsoft.mets.event.FlightToggleHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = MoreElectricTools.MODID, value = Side.CLIENT)
public class FlightHudOverlay {

    // Размеры полоски энергии
    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 4;
    private static final int BAR_BORDER = 1;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;
        if (mc.gameSettings.showDebugInfo) return;

        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (chest.isEmpty() || !(chest.getItem() instanceof AdvancedQuantumSuit)) return;

        boolean enabled = FlightToggleHandler.isFlightEnabled();

        FontRenderer fr = mc.fontRenderer;
        int screenWidth = event.getResolution().getScaledWidth();

        float scale = 1.5f;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);

        int y = 8;

        // ===== СТРОКА 1: СТАТУС ПОЛЁТА =====
        // Три части: "Engines " (белый) + "On"/"Off" (цветной) + " [F]" (белый)
        String enginesLabel = "Engines ";
        String statusText = enabled ? "On" : "Off";
        String keyHint = " [F]";
        int statusColor = enabled ? 0x55FF55 : 0xFF5555;

        int enginesWidth = fr.getStringWidth(enginesLabel);
        int statusWidth = fr.getStringWidth(statusText);
        int keyHintWidth = fr.getStringWidth(keyHint);
        int totalFlightWidth = enginesWidth + statusWidth + keyHintWidth;
        int flightX = (int) ((screenWidth / scale) - totalFlightWidth - 8);

        fr.drawStringWithShadow(enginesLabel, flightX, y, 0xFFFFFF);
        fr.drawStringWithShadow(statusText, flightX + enginesWidth, y, statusColor);
        fr.drawStringWithShadow(keyHint, flightX + enginesWidth + statusWidth, y, 0xFFFFFF);

        // ===== СТРОКА 2: ЗАРЯД В ПРОЦЕНТАХ =====
        double charge = ElectricItem.manager.getCharge(chest);
        double maxCharge = ((AdvancedQuantumSuit) chest.getItem()).getMaxCharge(chest);
        int percent = (int) Math.round((charge / maxCharge) * 100.0);
        if (percent > 100) percent = 100;
        if (percent < 0) percent = 0;

        int chargeColor;
        if (percent <= 10) {
            chargeColor = 0xFF0000; // красный
        } else if (percent <= 30) {
            chargeColor = 0xFF8800; // оранжевый
        } else if (percent <= 50) {
            chargeColor = 0xFFFF00; // жёлтый
        } else {
            chargeColor = 0x55FF55; // зелёный
        }

        String label = "Заряд ";
        String percentText = percent + "%";

        int labelWidth = fr.getStringWidth(label);
        int percentWidth = fr.getStringWidth(percentText);
        int totalWidth = labelWidth + percentWidth;

        int chargeY = y + fr.FONT_HEIGHT + 8;
        int chargeX = (int) ((screenWidth / scale) - totalWidth - 8);

        // Слово "Заряд" — белое
        fr.drawStringWithShadow(label, chargeX, chargeY, 0xFFFFFF);
        // Процент — цветной
        fr.drawStringWithShadow(percentText, chargeX + labelWidth, chargeY, chargeColor);

        // ===== ПОЛОСКА ЭНЕРГИИ =====
        int barY = chargeY + fr.FONT_HEIGHT + 4;
        int barX = (int) ((screenWidth / scale) - BAR_WIDTH - 8);

        // Тёмный фон (рамка)
        Gui.drawRect(barX - BAR_BORDER, barY - BAR_BORDER,
                barX + BAR_WIDTH + BAR_BORDER, barY + BAR_HEIGHT + BAR_BORDER, 0xFF000000);

        // Серый внутренний фон (пустая часть)
        Gui.drawRect(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF333333);

        // Заполненная часть (цветная по проценту)
        int fillWidth = (int) ((BAR_WIDTH) * (percent / 100.0));
        if (fillWidth > 0) {
            Gui.drawRect(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, 0xFF000000 | chargeColor);
        }

        // Тонкая светлая полоска сверху для «объёма»
        if (fillWidth > 0) {
            Gui.drawRect(barX, barY, barX + fillWidth, barY + 1, 0x55FFFFFF);
        }

        GlStateManager.popMatrix();
    }
}