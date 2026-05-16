package io.github.nazottix.tasmdtstorage.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.nazottix.tasmdtstorage.tasmdtstorage;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = tasmdtstorage.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class TalismanKeyMappings {
    public static final KeyMapping TOGGLE_TALISMAN_PICKUP = new KeyMapping(
            "key.tasmdtstorage.toggle_talisman_pickup",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.tasmdtstorage"
    );

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_TALISMAN_PICKUP);
    }
}
