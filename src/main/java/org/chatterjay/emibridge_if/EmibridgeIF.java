package org.chatterjay.emibridge_if;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.chatterjay.emibridge.adapter.AdapterManager;
import org.chatterjay.emibridge_if.adapter.IndustrialForegoingAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(EmibridgeIF.MODID)
public class EmibridgeIF {
    public static final String MODID = "emibridge_if";
    private static final Logger LOGGER = LoggerFactory.getLogger("EmiBridge/IF");

    public EmibridgeIF(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        if (!net.neoforged.fml.ModList.get().isLoaded("industrialforegoing")) {
            LOGGER.warn("Industrial Foregoing not loaded, skipping adapter registration");
            return;
        }
        AdapterManager.getInstance().registerAdapter(new IndustrialForegoingAdapter());
        LOGGER.info("Registered IndustrialForegoing adapter");
    }
}
