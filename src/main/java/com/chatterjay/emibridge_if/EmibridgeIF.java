package com.chatterjay.emibridge_if;

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
import com.buuz135.industrial.recipe.*;
import com.buuz135.industrial.utils.IndustrialTags;
import com.chatterjay.emibridge.adapter.AdapterManager;
import com.chatterjay.emibridge.compat.EmibridgeEMIPlugin;
import com.chatterjay.emibridge.ir.RecipeIR;
import com.chatterjay.emibridge_if.adapter.IndustrialForegoingAdapter;
import com.hrznstudio.titanium.util.RecipeUtil;
import com.hrznstudio.titanium.util.TagUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Mod(EmibridgeIF.MODID)
public class EmibridgeIF {
    public static final String MODID = "emibridge_if";
    private static final Logger LOGGER = LoggerFactory.getLogger("EmiBridge/IF");

    private static volatile boolean extractionDone = false;
    private IndustrialForegoingAdapter adapter;

    public EmibridgeIF(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded("industrialforegoing")) {
            LOGGER.warn("Industrial Foregoing not loaded, skipping adapter registration");
            return;
        }
        adapter = new IndustrialForegoingAdapter();
        var am = AdapterManager.getInstance();
        am.registerAdapter(adapter);

        // 加载内置黑名单（来自 jar 里的 IndustrialForegoingAdapter_blacklist.json）
        loadBuiltinBlacklist(am);

        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Registered IndustrialForegoing adapter with EmiBridge core (am={})", System.identityHashCode(am));
    }

    @SuppressWarnings("unchecked")
    private void loadBuiltinBlacklist(AdapterManager am) {
        try (var is = getClass().getResourceAsStream("/IndustrialForegoingAdapter_blacklist.json")) {
            if (is == null) {
                LOGGER.warn("Builtin blacklist resource not found");
                return;
            }
            var json = new String(is.readAllBytes());
            var gson = new com.google.gson.Gson();
            var raw = gson.fromJson(json, java.util.LinkedHashMap.class);
            var patterns = (java.util.List<String>) raw.get("patterns");
            if (patterns == null || patterns.isEmpty()) {
                LOGGER.info("Builtin blacklist resource has no patterns");
                return;
            }
            var blacklist = java.util.Map.of(
                    "IndustrialForegoingAdapter", java.util.List.copyOf(patterns));
            am.setAdapterBlacklist(blacklist);
            LOGGER.info("Loaded builtin blacklist for IndustrialForegoingAdapter: {} patterns", patterns.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load builtin blacklist", e);
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (extractionDone) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        // Wait until JEI runtime is available — this ensures RecipeManager has loaded all recipes
        if (!ModList.get().isLoaded("jei")) {
            extractionDone = true;
            return;
        }
        mezz.jei.api.runtime.IJeiRuntime jeiRuntime;
        try {
            jeiRuntime = mezz.jei.common.Internal.getJeiRuntime();
        } catch (Exception e) {
            return;
        }
        if (jeiRuntime == null) return;

        extractionDone = true;
        LOGGER.info("Starting direct IF recipe extraction (bypassing JEI)...");

        try {
            var results = new ArrayList<RecipeIR>();

            // 1. Fluid Extractor
            extractStandard(results, level, ModuleCore.FLUID_EXTRACTOR_TYPE.get(), "industrialforegoing:fluid_extractor");

            // 2. Dissolution Chamber
            extractStandard(results, level, ModuleCore.DISSOLUTION_TYPE.get(), "industrialforegoing:dissolution");

            // 3. Laser Drill Ore (filter out empty outputs, matching JEICustomPlugin)
            var laserOreRecipes = RecipeUtil.getRecipes(level, (RecipeType<LaserDrillOreRecipe>) ModuleCore.LASER_DRILL_TYPE.get())
                    .stream().filter(r -> !r.output.ingredient().isEmpty()).toList();
            for (var recipe : laserOreRecipes) {
                safeTranslate(results, recipe, "industrialforegoing:laser_ore");
            }
            LOGGER.info("  laser_ore: {} recipes", laserOreRecipes.size());

            // 4. Laser Drill Fluid
            extractStandard(results, level, ModuleCore.LASER_DRILL_FLUID_TYPE.get(), "industrialforegoing:laser_fluid");

            // 5. Stone Work Generator
            extractStandard(results, level, ModuleCore.STONEWORK_GENERATE_TYPE.get(), "industrialforegoing:stone_work_generator");

            // 6. Bioreactor
            var bioreactorRecipes = RecipeViewerHelper.generateBioreactorRecipes();
            for (var recipe : bioreactorRecipes) {
                safeTranslate(results, recipe, "industrialforegoing:bioreactor");
            }
            LOGGER.info("  bioreactor: {} recipes", bioreactorRecipes.size());

            // 7. Stone Work (action chains)
            var stoneWorkRecipes = RecipeViewerHelper.getStoneWork();
            for (var recipe : stoneWorkRecipes) {
                safeTranslate(results, recipe, "industrialforegoing:stone_work");
            }
            LOGGER.info("  stone_work: {} recipes", stoneWorkRecipes.size());

            // 8. Machine Produce (hardcoded wrappers matching JEICustomPlugin)
            var machineWrappers = List.of(
                    new MachineProduceWrapper(ModuleCore.LATEX_PROCESSING.getBlock(), new ItemStack(ModuleCore.DRY_RUBBER.get())),
                    new MachineProduceWrapper(ModuleResourceProduction.SLUDGE_REFINER.getBlock(), IndustrialTags.Items.SLUDGE_OUTPUT),
                    new MachineProduceWrapper(ModuleAgricultureHusbandry.SEWAGE_COMPOSTER.getBlock(), new ItemStack(ModuleCore.FERTILIZER.get())),
                    new MachineProduceWrapper(ModuleResourceProduction.DYE_MIXER.getBlock(), Tags.Items.DYES),
                    new MachineProduceWrapper(ModuleResourceProduction.SPORES_RECREATOR.getBlock(), Tags.Items.MUSHROOMS),
                    new MachineProduceWrapper(ModuleResourceProduction.SPORES_RECREATOR.getBlock(), new ItemStack(Items.CRIMSON_FUNGUS), new ItemStack(Items.WARPED_FUNGUS)),
                    new MachineProduceWrapper(ModuleAgricultureHusbandry.MOB_CRUSHER.getBlock(), new FluidStack(ModuleCore.ESSENCE.getSourceFluid().get(), 1000)),
                    new MachineProduceWrapper(ModuleAgricultureHusbandry.SLAUGHTER_FACTORY.getBlock(), new FluidStack(ModuleCore.MEAT.getSourceFluid().get(), 1000)),
                    new MachineProduceWrapper(ModuleAgricultureHusbandry.SLAUGHTER_FACTORY.getBlock(), new FluidStack(ModuleCore.PINK_SLIME.getSourceFluid().get(), 1000)),
                    new MachineProduceWrapper(ModuleAgricultureHusbandry.ANIMAL_RANCHER.getBlock(), new FluidStack(NeoForgeMod.MILK.get(), 1000)),
                    new MachineProduceWrapper(ModuleAgricultureHusbandry.SEWER.getBlock(), new FluidStack(ModuleCore.SEWAGE.getSourceFluid().get(), 1000)),
                    new MachineProduceWrapper(ModuleAgricultureHusbandry.PLANT_GATHERER.getBlock(), new FluidStack(ModuleCore.SLUDGE.getSourceFluid().get(), 1000)),
                    new MachineProduceWrapper(ModuleResourceProduction.WATER_CONDENSATOR.getBlock(), new FluidStack(Fluids.WATER, 1000))
            );
            for (var wrapper : machineWrappers) {
                safeTranslate(results, wrapper, "industrialforegoing:machine_produce");
            }
            LOGGER.info("  machine_produce: {} recipes", machineWrappers.size());

            // 9-11. Ore processing: Washer, Fermenter, Sieve (tag-based, matching JEICustomPlugin)
            List<OreFluidEntryRaw> washer = new ArrayList<>();
            List<OreFluidEntryFermenter> fermenters = new ArrayList<>();
            List<OreFluidEntrySieve> sieves = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagNames().map(tk -> tk.location())
                    .filter(loc -> loc.toString().startsWith("c:raw_materials/") && OreTitaniumFluidType.isValid(loc))
                    .forEach(loc -> {
                        TagKey<Item> tag = TagUtil.getItemTag(loc);
                        TagKey<Item> dust = TagUtil.getItemTag(
                                ResourceLocation.parse(loc.toString().replace("c:raw_materials/", "c:dusts/")));
                        washer.add(new OreFluidEntryRaw(tag,
                                new FluidStack(ModuleCore.MEAT.getSourceFluid().get(), 100),
                                OreTitaniumFluidType.getFluidWithTag(ModuleCore.RAW_ORE_MEAT, 100, loc)));
                        fermenters.add(new OreFluidEntryFermenter(
                                OreTitaniumFluidType.getFluidWithTag(ModuleCore.RAW_ORE_MEAT, 100, loc),
                                OreTitaniumFluidType.getFluidWithTag(ModuleCore.FERMENTED_ORE_MEAT, 200, loc)));
                        sieves.add(new OreFluidEntrySieve(
                                OreTitaniumFluidType.getFluidWithTag(ModuleCore.FERMENTED_ORE_MEAT, 100, loc),
                                TagUtil.getItemWithPreference(dust),
                                ItemTags.SAND));
                    });
            for (var r : washer) {
                safeTranslate(results, r, "industrialforegoing:ore_washer");
            }
            for (var r : fermenters) {
                safeTranslate(results, r, "industrialforegoing:fermenter");
            }
            for (var r : sieves) {
                safeTranslate(results, r, "industrialforegoing:ore_sieve");
            }
            LOGGER.info("  ore_washer: {}, fermenter: {}, ore_sieve: {}", washer.size(), fermenters.size(), sieves.size());

            LOGGER.info("Direct IF extraction complete: {} recipes total", results.size());
            EmibridgeEMIPlugin.submitRecipes(results);
        } catch (Exception e) {
            LOGGER.error("Failed to extract IF recipes directly", e);
        }
    }

    private void safeTranslate(List<RecipeIR> results, Object recipe, String categoryId) {
        AdapterManager.getInstance().safeTranslate(adapter, recipe, categoryId).ifPresent(results::add);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void extractStandard(List<RecipeIR> results, net.minecraft.world.level.Level level, Object recipeType, String categoryId) {
        var typed = (RecipeType) recipeType;
        var recipes = RecipeUtil.getRecipes(level, typed);
        for (var recipe : recipes) {
            safeTranslate(results, recipe, categoryId);
        }
        LOGGER.info("  {}: {} recipes", categoryId.substring(categoryId.lastIndexOf(':') + 1), recipes.size());
    }
}
