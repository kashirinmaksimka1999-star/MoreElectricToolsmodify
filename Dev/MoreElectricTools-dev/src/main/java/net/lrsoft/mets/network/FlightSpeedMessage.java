package net.lrsoft.mets.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FlightSpeedMessage implements IMessage {

    private boolean boosting;

    public FlightSpeedMessage() {}

    public FlightSpeedMessage(boolean boosting) {
        this.boosting = boosting;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.boosting = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(boosting);
    }

    public static class Handler implements IMessageHandler<FlightSpeedMessage, IMessage> {
        @Override
        public IMessage onMessage(FlightSpeedMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                NBTTagCompound data = player.getEntityData();
                data.setBoolean("METS_FlightBoost", message.boosting);
            });
            return null;
        }
    }
}