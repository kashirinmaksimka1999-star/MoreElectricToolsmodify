package net.lrsoft.mets.network;

import io.netty.buffer.ByteBuf;
import net.lrsoft.mets.event.FlightToggleHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FlightStateMessage implements IMessage {

    private boolean enabled;

    public FlightStateMessage() {}

    public FlightStateMessage(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.enabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static class Handler implements IMessageHandler<FlightStateMessage, IMessage> {
        @Override
        public IMessage onMessage(FlightStateMessage message, MessageContext ctx) {
            // Этот код выполняется на клиенте
            FlightToggleHandler.setFlightEnabled(message.isEnabled());
            return null;
        }
    }
}