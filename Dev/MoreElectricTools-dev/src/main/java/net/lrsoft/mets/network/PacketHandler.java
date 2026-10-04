package net.lrsoft.mets.network;

import net.lrsoft.mets.MoreElectricTools;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel(MoreElectricTools.MODID);

    public static void register() {
        INSTANCE.registerMessage(
                FlightToggleMessage.Handler.class,
                FlightToggleMessage.class,
                0,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                FlightStateMessage.Handler.class,
                FlightStateMessage.class,
                1,
                Side.CLIENT
        );
        INSTANCE.registerMessage(
                FlightSpeedMessage.Handler.class,
                FlightSpeedMessage.class,
                2,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                FlightEnableMessage.Handler.class,
                FlightEnableMessage.class,
                3,
                Side.SERVER
        );
    }
}