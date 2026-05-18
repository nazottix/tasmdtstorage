package io.github.nazottix.tasmdtstorage;

import com.mojang.logging.LogUtils;
import io.github.nazottix.tasmdtstorage.item.StorageBallItem;
import io.github.nazottix.tasmdtstorage.item.StorageBallItemHandler;
import io.github.nazottix.tasmdtstorage.network.SetInventoryAutoInsertPayload;
import io.github.nazottix.tasmdtstorage.network.StorageBlockTransferPayload;
import io.github.nazottix.tasmdtstorage.recipe.StorageBallRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(tasmdtstorage.MODID)
public class tasmdtstorage {
    public static final String MODID = "tasmdtstorage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    public static final DeferredItem<Item> STORAGE_BALL_ITEM = ITEMS.register("storage_ball", () -> new StorageBallItem(new Item.Properties()));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageBallRecipe>> STORAGE_BALL_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("storage_ball", () -> new SimpleCraftingRecipeSerializer<>(StorageBallRecipe::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tasmdtstorage"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> STORAGE_BALL_ITEM.toStack())
            .displayItems((parameters, output) -> {
                output.accept(STORAGE_BALL_ITEM.get());
                output.accept(Items.NETHER_STAR);
            })
            .build());

    public tasmdtstorage(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerPayloadHandlers);
        modEventBus.addListener(this::registerCapabilities);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(STORAGE_BALL_ITEM);
        }
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                StorageBlockTransferPayload.TYPE,
                StorageBlockTransferPayload.STREAM_CODEC,
                StorageBlockTransferPayload::handle
        );
        registrar.playToServer(
                SetInventoryAutoInsertPayload.TYPE,
                SetInventoryAutoInsertPayload.STREAM_CODEC,
                SetInventoryAutoInsertPayload::handle
        );
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.ItemHandler.ITEM,
                (stack, context) -> new StorageBallItemHandler(stack),
                STORAGE_BALL_ITEM.get()
        );
    }
}



