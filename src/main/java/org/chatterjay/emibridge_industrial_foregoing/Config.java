package org.chatterjay.emibridge_industrial_foregoing;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Emibridge_industrial_foregoing.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}
