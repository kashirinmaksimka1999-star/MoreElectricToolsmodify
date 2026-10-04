package net.lrsoft.mets.util;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class KeyBindings {
    public static KeyBinding FLIGHT_TOGGLE;

    public static void register() {
        // Клавиша F для переключения полёта
        FLIGHT_TOGGLE = new KeyBinding("key.mets.flight_toggle", Keyboard.KEY_F, "key.categories.mets");
        ClientRegistry.registerKeyBinding(FLIGHT_TOGGLE);
    }
}