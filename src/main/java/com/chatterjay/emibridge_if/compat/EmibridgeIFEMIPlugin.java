package com.chatterjay.emibridge_if.compat;

import com.buuz135.industrial.module.ModuleAgricultureHusbandry;
import com.buuz135.industrial.module.ModuleCore;
import com.buuz135.industrial.module.ModuleGenerator;
import com.buuz135.industrial.module.ModuleResourceProduction;
import com.chatterjay.emibridge.generator.EMIRecipeGenerator;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@dev.emi.emi.api.EmiEntrypoint
public class EmibridgeIFEMIPlugin implements EmiPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("EmiBridge/IF");

    @Override
    public void register(EmiRegistry registry) {
        LOGGER.info("EmibridgeIF EMIPlugin registering...");

        registerCategory(registry, "bioreactor", ModuleGenerator.BIOREACTOR.getBlock());
        registerCategory(registry, "stone_work", ModuleResourceProduction.MATERIAL_STONEWORK_FACTORY.getBlock());
        registerCategory(registry, "stone_work_generator", ModuleResourceProduction.MATERIAL_STONEWORK_FACTORY.getBlock());
        registerCategory(registry, "laser_ore", ModuleResourceProduction.ORE_LASER_BASE.getBlock());
        registerCategory(registry, "laser_fluid", ModuleResourceProduction.FLUID_LASER_BASE.getBlock());
        registerCategory(registry, "fluid_extractor", ModuleCore.FLUID_EXTRACTOR.getBlock());
        registerCategory(registry, "dissolution", ModuleCore.DISSOLUTION_CHAMBER.getBlock());

        var machines = new com.hrznstudio.titanium.module.BlockWithTile[]{
                ModuleCore.LATEX_PROCESSING,
                ModuleResourceProduction.SLUDGE_REFINER,
                ModuleAgricultureHusbandry.SEWAGE_COMPOSTER,
                ModuleResourceProduction.DYE_MIXER,
                ModuleResourceProduction.SPORES_RECREATOR,
                ModuleAgricultureHusbandry.MOB_CRUSHER,
                ModuleAgricultureHusbandry.SLAUGHTER_FACTORY,
                ModuleAgricultureHusbandry.ANIMAL_RANCHER,
                ModuleAgricultureHusbandry.SEWER,
                ModuleAgricultureHusbandry.PLANT_GATHERER,
                ModuleResourceProduction.WATER_CONDENSATOR,
                ModuleResourceProduction.FERMENTATION_STATION,
                ModuleResourceProduction.WASHING_FACTORY,
                ModuleResourceProduction.FLUID_SIEVING_MACHINE,
        };
        for (var machine : machines) {
            var blockId = BuiltInRegistries.BLOCK.getKey(machine.getBlock());
            if (blockId == null) continue;
            var catKey = blockId.getNamespace() + ":" + blockId.getPath();
            var cat = new EmiRecipeCategory(blockId, EmiStack.of(machine.getBlock()));
            registry.addCategory(cat);
            EMIRecipeGenerator.registerCategory(catKey, cat);
            registry.addWorkstation(cat, EmiStack.of(machine.getBlock()));
        }

        // Suppress EMI's built-in block tag recipes
        var tagCatId = ResourceLocation.parse("emi:tag");
        registry.removeRecipes(recipe -> {
            if (tagCatId.equals(recipe.getCategory().getId())) {
                var id = recipe.getId();
                return id != null && id.getNamespace().equals("emi")
                        && id.getPath().contains("minecraft:block");
            }
            return false;
        });

        LOGGER.info("EmibridgeIF EMIPlugin registration complete");
    }

    private void registerCategory(EmiRegistry registry, String path, net.minecraft.world.level.block.Block block) {
        var id = ResourceLocation.fromNamespaceAndPath("industrialforegoing", path);
        var categoryKey = "industrialforegoing:" + path;
        var cat = new EmiRecipeCategory(id, EmiStack.of(block));
        registry.addCategory(cat);
        EMIRecipeGenerator.registerCategory(categoryKey, cat);
        registry.addWorkstation(cat, EmiStack.of(block));
    }
}
