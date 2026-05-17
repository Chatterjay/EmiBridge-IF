package org.chatterjay.emibridge_if.compat;

import com.buuz135.industrial.api.recipe.ore.OreFluidEntryFermenter;
import com.buuz135.industrial.api.recipe.ore.OreFluidEntryRaw;
import com.buuz135.industrial.api.recipe.ore.OreFluidEntrySieve;
import com.buuz135.industrial.fluid.OreTitaniumFluidType;
import com.buuz135.industrial.module.ModuleAgricultureHusbandry;
import com.buuz135.industrial.module.ModuleCore;
import com.buuz135.industrial.module.ModuleGenerator;
import com.buuz135.industrial.module.ModuleResourceProduction;
import com.buuz135.industrial.plugin.RecipeViewerHelper;
import com.buuz135.industrial.plugin.jei.machineproduce.MachineProduceWrapper;
import com.buuz135.industrial.utils.IndustrialTags;
import com.hrznstudio.titanium.util.TagUtil;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import org.chatterjay.emibridge.api.RecipeIR;
import org.chatterjay.emibridge.compat.EmibridgeEMIPlugin;
import org.chatterjay.emibridge.generator.EMIRecipeGenerator;
import org.chatterjay.emibridge_if.adapter.IndustrialForegoingAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * EMI plugin that bypasses JEI's broken @JeiPlugin discovery in NeoForge dev env.
 * Gets IF recipe data directly from IF's APIs and submits to EmiBridge.
 */
@dev.emi.emi.api.EmiEntrypoint
public class EmibridgeIFEMIPlugin implements EmiPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("EmiBridge/IF");
    /** 使用 EmiBridge 核心提供的共享生成器，避免每个适配器各自创建 */
    private final EMIRecipeGenerator generator = EmibridgeEMIPlugin.getGenerator();
    private final IndustrialForegoingAdapter adapter = new IndustrialForegoingAdapter();

    @Override
    public void register(EmiRegistry registry) {
        LOGGER.info("EmibridgeIF EMIPlugin registering...");

        // Register custom JEI-only categories so EMI accepts our recipes.
        // Must also register with EMIRecipeGenerator so the generator reuses
        // these exact category objects (EMI matches categories by identity).
        var bioCat = new EmiRecipeCategory(
                ResourceLocation.fromNamespaceAndPath("industrialforegoing", "bioreactor"),
                EmiStack.of(ModuleGenerator.BIOREACTOR.getBlock()));
        registry.addCategory(bioCat);
        EMIRecipeGenerator.registerCategory("industrialforegoing:bioreactor", bioCat);
        registry.addWorkstation(bioCat, EmiStack.of(ModuleGenerator.BIOREACTOR.getBlock()));

        var stoneCat = new EmiRecipeCategory(
                ResourceLocation.fromNamespaceAndPath("industrialforegoing", "stone_work"),
                EmiStack.of(ModuleResourceProduction.MATERIAL_STONEWORK_FACTORY.getBlock()));
        registry.addCategory(stoneCat);
        EMIRecipeGenerator.registerCategory("industrialforegoing:stone_work", stoneCat);
        registry.addWorkstation(stoneCat, EmiStack.of(ModuleResourceProduction.MATERIAL_STONEWORK_FACTORY.getBlock()));

        // ─── 为每个机器注册独立分类 ──────────────────────────────────────
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
            LOGGER.info("Registered category: {} icon={}", catKey, blockId);
        }

        // ─── Deferred callback: generate & accept recipes DIRECTLY ─────────────
        // NOTE: We do NOT route through EmibridgeEMIPlugin.submitRecipes() because
        // that path depends on EmiBridge plugin's deferred callback having run first
        // to set recipeAcceptor. If the EmiBridge plugin doesn't register (e.g.
        // @EmiEntrypoint not discovered for a library JAR), recipes silently queue
        // in pendingRecipes and are never processed.
        registry.addDeferredRecipes(acceptor -> {
            LOGGER.info("Processing deferred IF recipes (direct pipeline)...");
            var results = extractIFRecipes();
            LOGGER.info("Extracted {} IF recipes, generating EMI recipes...", results.size());
            int success = 0, failed = 0;
            for (var ir : results) {
                try {
                    var emiRecipe = generator.generate(ir);
                    // Diagnostic: log each output before accepting
                    var outputStrs = emiRecipe.getOutputs().stream()
                            .map(s -> s.getId() + "@" + s.getAmount() + " isEmpty=" + s.isEmpty())
                            .collect(java.util.stream.Collectors.joining(", "));
                    LOGGER.info("[DIAG] Recipe {} -> outputs: [{}]", ir.getId(), outputStrs);
                    acceptor.accept(emiRecipe);
                    success++;
                } catch (Exception e) {
                    LOGGER.error("Failed to generate/accept recipe {}: {}", ir.getId(), e.getMessage());
                    failed++;
                }
            }
            LOGGER.info("Direct pipeline complete: {}/{} accepted, {} failed", success, results.size(), failed);

            // Also attempt the old path for any third-party integration
            LOGGER.info("Also submitting {} recipes via EmiBridge pipeline (if acceptor ready)...", results.size());
            EmibridgeEMIPlugin.submitRecipes(results);
        });
    }

    private List<RecipeIR> extractIFRecipes() {
        List<RecipeIR> results = new ArrayList<>();
        extractBioreactor(results);
        extractStoneWork(results);
        extractMachineProduce(results);
        extractOreWasherRecipes(results);
        extractFermenterRecipes(results);
        extractFluidSieveRecipes(results);
        return results;
    }

    private void extractBioreactor(List<RecipeIR> results) {
        try {
            var recipes = RecipeViewerHelper.generateBioreactorRecipes();
            LOGGER.info("Got {} bioreactor recipes", recipes.size());
            int count = 0;
            for (var recipe : recipes) {
                var ir = adapter.translate(recipe, "industrialforegoing:bioreactor");
                if (ir != null) {
                    results.add(ir);
                    count++;
                    LOGGER.debug("  Bioreactor[{}]: fluid={}, tag={} -> {}",
                            count, recipe.getFluid().getFluid().getFluidType().getDescription().getString(),
                            recipe.getStack().location(), ir.getId());
                }
            }
            LOGGER.info("Translated {}/{} bioreactor recipes", count, recipes.size());
        } catch (Exception e) {
            LOGGER.error("Failed to extract bioreactor recipes: {}", e.getMessage());
        }
    }

    private void extractStoneWork(List<RecipeIR> results) {
        try {
            var recipes = RecipeViewerHelper.getStoneWork();
            LOGGER.info("Got {} stone work recipes", recipes.size());
            int count = 0;
            for (var recipe : recipes) {
                var ir = adapter.translate(recipe, "industrialforegoing:stone_work");
                if (ir != null) {
                    results.add(ir);
                    count++;
                    LOGGER.debug("  StoneWork[{}]: input={}, output={} -> {}",
                            count, recipe.input().getDisplayName().getString(),
                            recipe.output().getDisplayName().getString(), ir.getId());
                }
            }
            LOGGER.info("Translated {}/{} stone work recipes", count, recipes.size());
        } catch (Exception e) {
            LOGGER.error("Failed to extract stone work recipes: {}", e.getMessage());
        }
    }

    private void extractMachineProduce(List<RecipeIR> results) {
        try {
            List<MachineProduceWrapper> wrappers = new ArrayList<>();

            // 0: Latex Processing -> Dry Rubber
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleCore.LATEX_PROCESSING),
                    new ItemStack[]{new ItemStack(ModuleCore.DRY_RUBBER.get())}
            ));

            // 1: Sludge Refiner -> SLUDGE_OUTPUT tag
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleResourceProduction.SLUDGE_REFINER),
                    IndustrialTags.Items.SLUDGE_OUTPUT
            ));

            // 2: Sewage Composter -> Fertilizer
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleAgricultureHusbandry.SEWAGE_COMPOSTER),
                    new ItemStack[]{new ItemStack(ModuleCore.FERTILIZER.get())}
            ));

            // 3: Dye Mixer -> DYES tag
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleResourceProduction.DYE_MIXER),
                    Tags.Items.DYES
            ));

            // 4: Spores Recreator -> MUSHROOMS tag
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleResourceProduction.SPORES_RECREATOR),
                    Tags.Items.MUSHROOMS
            ));

            // 5: Spores Recreator -> Crimson + Warped Fungus
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleResourceProduction.SPORES_RECREATOR),
                    new ItemStack[]{new ItemStack(Items.CRIMSON_FUNGUS), new ItemStack(Items.WARPED_FUNGUS)}
            ));

            // 6: Mob Crusher -> Essence 1000mB
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleAgricultureHusbandry.MOB_CRUSHER),
                    new FluidStack(ModuleCore.ESSENCE.getSourceFluid().get(), 1000)
            ));

            // 7: Slaughter Factory -> Meat 1000mB
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleAgricultureHusbandry.SLAUGHTER_FACTORY),
                    new FluidStack(ModuleCore.MEAT.getSourceFluid().get(), 1000)
            ));

            // 8: Slaughter Factory -> Pink Slime 1000mB
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleAgricultureHusbandry.SLAUGHTER_FACTORY),
                    new FluidStack(ModuleCore.PINK_SLIME.getSourceFluid().get(), 1000)
            ));

            // 9: Animal Rancher -> Milk 1000mB
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleAgricultureHusbandry.ANIMAL_RANCHER),
                    new FluidStack(NeoForgeMod.MILK.get(), 1000)
            ));

            // 10: Sewer -> Sewage 1000mB
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleAgricultureHusbandry.SEWER),
                    new FluidStack(ModuleCore.SEWAGE.getSourceFluid().get(), 1000)
            ));

            // 11: Plant Gatherer -> Sludge 1000mB
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleAgricultureHusbandry.PLANT_GATHERER),
                    new FluidStack(ModuleCore.SLUDGE.getSourceFluid().get(), 1000)
            ));

            // 12: Water Condensator -> Water 1000mB
            wrappers.add(new MachineProduceWrapper(
                    block(ModuleResourceProduction.WATER_CONDENSATOR),
                    new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000)
            ));

            LOGGER.info("Created {} machine produce wrappers", wrappers.size());
            int mpCount = 0;
            for (int i = 0; i < wrappers.size(); i++) {
                var wrapper = wrappers.get(i);
                var blockId = BuiltInRegistries.BLOCK.getKey(wrapper.getBlock());
                String outputDesc;
                if (wrapper.getOutputItem() != null && !wrapper.getOutputItem().isEmpty()) {
                    var stacks = wrapper.getOutputItem().getItems();
                    outputDesc = stacks.length + " items (first: " +
                            (stacks.length > 0 ? stacks[0].getDisplayName().getString() : "none") + ")";
                } else if (!wrapper.getOutputFluid().isEmpty()) {
                    outputDesc = wrapper.getOutputFluid().getAmount() + " mB of " +
                            BuiltInRegistries.FLUID.getKey(wrapper.getOutputFluid().getFluid());
                } else {
                    outputDesc = "empty";
                }
                var ir = adapter.translate(wrapper, "industrialforegoing:machine_produce");
                if (ir != null) {
                    results.add(ir);
                    mpCount++;
                    LOGGER.debug("  MachineProduce[{}]: machine={}, output={} -> {}",
                            i, blockId, outputDesc, ir.getId());
                } else {
                    LOGGER.warn("  MachineProduce[{}]: machine={}, output={} -> translation FAILED",
                            i, blockId, outputDesc);
                }
            }
            LOGGER.info("Translated {}/{} machine produce recipes", mpCount, wrappers.size());
        } catch (Exception e) {
            LOGGER.error("Failed to extract machine produce recipes: {}", e.getMessage());
        }
    }

    // ─── Ore Washer (Washing Factory) ────────────────────────────────────────

    private void extractOreWasherRecipes(List<RecipeIR> results) {
        try {
            List<OreFluidEntryRaw> washer = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagNames().map(TagKey::location)
                    .filter(loc -> loc.toString().startsWith("c:raw_materials/") && OreTitaniumFluidType.isValid(loc))
                    .forEach(loc -> {
                        TagKey<Item> tag = TagUtil.getItemTag(loc);
                        washer.add(new OreFluidEntryRaw(tag,
                                new FluidStack(ModuleCore.MEAT.getSourceFluid().get(), 100),
                                OreTitaniumFluidType.getFluidWithTag(ModuleCore.RAW_ORE_MEAT, 100, loc)));
                    });
            LOGGER.info("Got {} ore washer recipes", washer.size());
            int count = 0;
            for (var recipe : washer) {
                var ir = adapter.translate(recipe, "industrialforegoing:washing_factory");
                if (ir != null) {
                    results.add(ir);
                    count++;
                }
            }
            LOGGER.info("Translated {}/{} ore washer recipes", count, washer.size());
        } catch (Exception e) {
            LOGGER.error("Failed to extract ore washer recipes: {}", e.getMessage());
        }
    }

    // ─── Fermenter (Fermentation Station) ────────────────────────────────────

    private void extractFermenterRecipes(List<RecipeIR> results) {
        try {
            List<OreFluidEntryFermenter> fermenters = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagNames().map(TagKey::location)
                    .filter(loc -> loc.toString().startsWith("c:raw_materials/") && OreTitaniumFluidType.isValid(loc))
                    .forEach(loc -> {
                        fermenters.add(new OreFluidEntryFermenter(
                                OreTitaniumFluidType.getFluidWithTag(ModuleCore.RAW_ORE_MEAT, 100, loc),
                                OreTitaniumFluidType.getFluidWithTag(ModuleCore.FERMENTED_ORE_MEAT, 200, loc)));
                    });
            LOGGER.info("Got {} fermenter recipes", fermenters.size());
            int count = 0;
            for (var recipe : fermenters) {
                var ir = adapter.translate(recipe, "industrialforegoing:fermentation_station");
                if (ir != null) {
                    results.add(ir);
                    count++;
                }
            }
            LOGGER.info("Translated {}/{} fermenter recipes", count, fermenters.size());
        } catch (Exception e) {
            LOGGER.error("Failed to extract fermenter recipes: {}", e.getMessage());
        }
    }

    // ─── Fluid Sieve (Fluid Sieving Machine) ────────────────────────────────

    private void extractFluidSieveRecipes(List<RecipeIR> results) {
        try {
            List<OreFluidEntrySieve> sieves = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagNames().map(TagKey::location)
                    .filter(loc -> loc.toString().startsWith("c:raw_materials/") && OreTitaniumFluidType.isValid(loc))
                    .forEach(loc -> {
                        TagKey<Item> dust = TagUtil.getItemTag(ResourceLocation.parse(
                                loc.toString().replace("c:raw_materials/", "c:dusts/")));
                        sieves.add(new OreFluidEntrySieve(
                                OreTitaniumFluidType.getFluidWithTag(ModuleCore.FERMENTED_ORE_MEAT, 100, loc),
                                TagUtil.getItemWithPreference(dust),
                                ItemTags.SAND));
                    });
            LOGGER.info("Got {} fluid sieve recipes", sieves.size());
            int count = 0;
            for (var recipe : sieves) {
                var ir = adapter.translate(recipe, "industrialforegoing:fluid_sieving_machine");
                if (ir != null) {
                    results.add(ir);
                    count++;
                }
            }
            LOGGER.info("Translated {}/{} fluid sieve recipes", count, sieves.size());
        } catch (Exception e) {
            LOGGER.error("Failed to extract fluid sieve recipes: {}", e.getMessage());
        }
    }

    private static Block block(com.hrznstudio.titanium.module.BlockWithTile bwt) {
        return bwt.getBlock();
    }
}
