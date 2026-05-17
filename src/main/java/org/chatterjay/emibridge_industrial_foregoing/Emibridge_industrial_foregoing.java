package org.chatterjay.emibridge_industrial_foregoing;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Emibridge_industrial_foregoing.MODID)
public class Emibridge_industrial_foregoing {

    public static final String MODID = "emibridge_industrial_foregoing";

    public Emibridge_industrial_foregoing(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
