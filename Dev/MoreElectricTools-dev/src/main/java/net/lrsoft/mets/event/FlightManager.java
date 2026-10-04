package net.lrsoft.mets.event;

import ic2.api.item.ElectricItem;
import net.lrsoft.mets.MoreElectricTools;
import net.lrsoft.mets.armor.AdvancedQuantumSuit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = MoreElectricTools.MODID)
public class FlightManager {

    private static final float FLY_SPEED_NORMAL = 0.05f;
    private static final float FLY_SPEED_BOOST  = 0.05f * 4.0f * 1.5f;

    private static final double FLY_DRAIN_NORMAL  = 512.0;
    private static final double FLY_DRAIN_BOOST   = 1536.0;
    private static final double FLY_DRAIN_AIR     = 512.0;
    private static final double FLY_DRAIN_PASSIVE = 150.0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.side != Side.SERVER) return;

        EntityPlayer player = event.player;
        if (player.capabilities.isCreativeMode) return;

        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        boolean hasSuit = !chest.isEmpty() && chest.getItem() instanceof AdvancedQuantumSuit;

        boolean flightEnabled = false;
        if (hasSuit) {
            NBTTagCompound nbt = chest.getTagCompound();
            if (nbt != null) {
                flightEnabled = nbt.getBoolean("FlightEnabled");
            }
        }

        NBTTagCompound data = player.getEntityData();
        boolean hadSuitLastTick = data.getBoolean("METS_HadSuit");
        boolean boosting = data.getBoolean("METS_FlightBoost");

        if (flightEnabled) {

            if (player.ticksExisted < 5 && !player.onGround && !player.isInWater()
                    && !player.capabilities.isFlying) {
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
            }
            else if (hasSuit && !hadSuitLastTick
                    && !player.onGround && !player.isInWater()
                    && !player.capabilities.isFlying) {
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
            }

            // ===== Падение > 3 блоков → allowFlying = false, MC обрабатывает урон =====
            if (!player.onGround && !player.isInWater() && !player.capabilities.isFlying) {
                double maxY = data.getDouble("METS_MaxY");
                if (maxY == 0 || player.posY > maxY) {
                    data.setDouble("METS_MaxY", player.posY);
                    maxY = player.posY;
                }
                double distance = maxY - player.posY;

                if (distance > 3.0) {
                    if (player.capabilities.allowFlying) {
                        player.capabilities.allowFlying = false;
                        player.sendPlayerAbilities();
                    }
                    // ВАЖНО: НЕ трогаем player.fallDistance — MC считает его сам
                } else {
                    if (!player.capabilities.allowFlying) {
                        player.capabilities.allowFlying = true;
                        player.sendPlayerAbilities();
                    }
                }
            } else {
                if (!player.capabilities.allowFlying) {
                    player.capabilities.allowFlying = true;
                    player.sendPlayerAbilities();
                }
                if (player.onGround) {
                    data.setDouble("METS_MaxY", 0);
                }
            }

            // ===== Скорость полёта =====
            float targetSpeed = (boosting && player.capabilities.isFlying)
                    ? FLY_SPEED_BOOST : FLY_SPEED_NORMAL;

            if (Math.abs(player.capabilities.getFlySpeed() - targetSpeed) > 0.001f) {
                player.capabilities.setFlySpeed(targetSpeed);
                player.sendPlayerAbilities();
            }

            // ===== Расход энергии =====
            double drain;
            if (player.onGround) {
                drain = FLY_DRAIN_PASSIVE;
            } else if (player.capabilities.isFlying) {
                drain = boosting ? FLY_DRAIN_BOOST : FLY_DRAIN_NORMAL;
            } else {
                drain = FLY_DRAIN_AIR;
            }

            if (drain > 0.0) {
                boolean hasEnergy = ElectricItem.manager.use(chest, drain, player);
                if (!hasEnergy) {
                    player.capabilities.allowFlying = false;
                    player.capabilities.isFlying = false;
                    player.capabilities.setFlySpeed(FLY_SPEED_NORMAL);
                    player.sendPlayerAbilities();

                    NBTTagCompound nbt = chest.getTagCompound();
                    if (nbt != null) {
                        nbt.setBoolean("FlightEnabled", false);
                    }
                }
            }
        } else {
            if (player.capabilities.allowFlying || player.capabilities.isFlying) {
                player.capabilities.allowFlying = false;
                player.capabilities.isFlying = false;
                player.capabilities.setFlySpeed(FLY_SPEED_NORMAL);
                player.sendPlayerAbilities();
            }
            data.setDouble("METS_MaxY", 0);
        }

        data.setBoolean("METS_HadSuit", hasSuit);
    }
}