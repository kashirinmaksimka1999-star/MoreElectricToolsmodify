package net.lrsoft.mets.network;

import io.netty.buffer.ByteBuf;
import ic2.core.IC2;
import net.lrsoft.mets.armor.AdvancedQuantumSuit;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FlightToggleMessage implements IMessage {

    public FlightToggleMessage() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<FlightToggleMessage, IMessage> {
        @Override
        public IMessage onMessage(FlightToggleMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
                if (chest.isEmpty() || !(chest.getItem() instanceof AdvancedQuantumSuit)) return;

                NBTTagCompound nbt = chest.getTagCompound();
                if (nbt == null) nbt = new NBTTagCompound();

                boolean newValue = !nbt.getBoolean("FlightEnabled");
                nbt.setBoolean("FlightEnabled", newValue);
                chest.setTagCompound(nbt);

                if (!player.capabilities.isCreativeMode) {
                    player.capabilities.allowFlying = newValue;
                    if (newValue) {
                        // Если включили полёт в воздухе или в воде — сразу зависаем
                        if (!player.onGround || player.isInWater()) {
                            player.capabilities.isFlying = true;
                        }
                    } else {
                        player.capabilities.isFlying = false;
                    }
                    player.sendPlayerAbilities();
                }

                // Отправляем ответный пакет клиенту с новым состоянием
                PacketHandler.INSTANCE.sendTo(
                        new FlightStateMessage(newValue),
                        player
                );

            });
            return null;
        }
    }
}