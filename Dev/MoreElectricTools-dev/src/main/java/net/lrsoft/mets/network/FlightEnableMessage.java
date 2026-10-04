package net.lrsoft.mets.network;

import io.netty.buffer.ByteBuf;
import net.lrsoft.mets.armor.AdvancedQuantumSuit;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FlightEnableMessage implements IMessage {

    public FlightEnableMessage() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<FlightEnableMessage, IMessage> {
        @Override
        public IMessage onMessage(FlightEnableMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
                if (chest.isEmpty() || !(chest.getItem() instanceof AdvancedQuantumSuit)) return;

                NBTTagCompound nbt = chest.getTagCompound();
                if (nbt == null || !nbt.getBoolean("FlightEnabled")) return;

                // Включаем полёт
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.fallDistance = 0;
                player.sendPlayerAbilities();

                // ВАЖНО: сбрасываем накопленную максимальную высоту падения,
                // иначе при следующем отключении полёта игрок умрёт от "старого" падения
                NBTTagCompound data = player.getEntityData();
                data.setDouble("METS_MaxY", 0);
            });
            return null;
        }
    }
}