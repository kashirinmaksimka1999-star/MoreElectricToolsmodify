package net.lrsoft.mets.event;

import net.lrsoft.mets.MoreElectricTools;
import net.lrsoft.mets.armor.AdvancedQuantumSuit;
import net.lrsoft.mets.network.FlightEnableMessage;
import net.lrsoft.mets.network.FlightSpeedMessage;
import net.lrsoft.mets.network.FlightToggleMessage;
import net.lrsoft.mets.network.PacketHandler;
import net.lrsoft.mets.util.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(modid = MoreElectricTools.MODID, value = Side.CLIENT)
public class FlightToggleHandler {

    private static boolean wasPressed = false;
    private static boolean flightEnabled = false;
    private static boolean wasBoosting = false;
    private static boolean wasSpaceDown = false;
    private static long lastSpaceTime = 0L;

    public static boolean isFlightEnabled() { return flightEnabled; }
    public static void setFlightEnabled(boolean value) { flightEnabled = value; }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        boolean hasSuit = !chest.isEmpty() && chest.getItem() instanceof AdvancedQuantumSuit;

        if (hasSuit && chest.hasTagCompound()) {
            flightEnabled = chest.getTagCompound().getBoolean("FlightEnabled");
        }

        // === F — переключение режима ===
        if (KeyBindings.FLIGHT_TOGGLE != null) {
            boolean isPressed = KeyBindings.FLIGHT_TOGGLE.isKeyDown();
            if (isPressed && !wasPressed && hasSuit) {
                PacketHandler.INSTANCE.sendToServer(new FlightToggleMessage());
            }
            wasPressed = isPressed;
        }

        // === Ctrl — ускорение ===
        boolean isBoosting = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && player.capabilities.isFlying;
        if (isBoosting != wasBoosting) {
            PacketHandler.INSTANCE.sendToServer(new FlightSpeedMessage(isBoosting));
            wasBoosting = isBoosting;
        }

        // === Двойной пробел — возобновление полёта при падении ===
        boolean spaceDown = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
        if (spaceDown && !wasSpaceDown) {
            long now = System.currentTimeMillis();
            if (now - lastSpaceTime < 300L) {
                // Проверяем: игрок падает, режим включён, allowFlying снят
                if (hasSuit && flightEnabled
                        && !player.onGround
                        && !player.capabilities.isFlying
                        && !player.capabilities.allowFlying) {
                    PacketHandler.INSTANCE.sendToServer(new FlightEnableMessage());
                }
            }
            lastSpaceTime = now;
        }
        wasSpaceDown = spaceDown;
    }
}