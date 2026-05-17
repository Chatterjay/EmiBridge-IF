package com.chatterjay.emibridge_if.adapter;

import com.buuz135.industrial.api.recipe.ore.OreFluidEntryFermenter;
import com.buuz135.industrial.api.recipe.ore.OreFluidEntryRaw;
import com.buuz135.industrial.api.recipe.ore.OreFluidEntrySieve;
import com.buuz135.industrial.config.machine.core.DissolutionChamberConfig;
import com.buuz135.industrial.plugin.jei.StoneWorkWrapper;
import com.buuz135.industrial.plugin.jei.category.BioReactorRecipeCategory;
import com.buuz135.industrial.plugin.jei.machineproduce.MachineProduceWrapper;
import com.buuz135.industrial.recipe.*;
import com.buuz135.industrial.recipe.data.EntityData;
import com.chatterjay.emibridge.adapter.EmiBridgeAdapter;
import com.chatterjay.emibridge.adapter.IRecipeAdapter;
import com.chatterjay.emibridge.ir.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@EmiBridgeAdapter(modId = "industrialforegoing", priority = 100)
public class IndustrialForegoingAdapter implements IRecipeAdapter<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger("EmiBridge/IF/Adapter");

    private static final String MOD_ID = "industrialforegoing";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public List<String> getCategoryIds() {
        return List.of(
                "industrialforegoing:laser_ore",
                "industrialforegoing:laser_fluid",
                "industrialforegoing:fluid_extractor",
                "industrialforegoing:dissolution",
                "industrialforegoing:fermenter",
                "industrialforegoing:ore_washer",
                "industrialforegoing:ore_sieve",
                "industrialforegoing:bioreactor",
                "industrialforegoing:stone_work",
                "industrialforegoing:stone_work_generator",
                "industrialforegoing:machine_produce"
        );
    }

    @Override
    public Class<Object> getRecipeClass() {
        return Object.class;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public RecipeIR translate(Object jeiRecipe, String categoryId) {
        LOGGER.trace("translate: category={}, recipeType={}", categoryId, jeiRecipe.getClass().getSimpleName());
        RecipeIR result;
        try {
            result = switch (categoryId) {
                case "industrialforegoing:laser_ore" ->
                        translateLaserOre((LaserDrillOreRecipe) jeiRecipe);
                case "industrialforegoing:laser_fluid" ->
                        translateLaserFluid((LaserDrillFluidRecipe) jeiRecipe);
                case "industrialforegoing:fluid_extractor" ->
                        translateFluidExtractor((FluidExtractorRecipe) jeiRecipe);
                case "industrialforegoing:dissolution" ->
                        translateDissolution((DissolutionChamberRecipe) jeiRecipe);
                case "industrialforegoing:fermenter" ->
                        translateFermenter((OreFluidEntryFermenter) jeiRecipe);
                case "industrialforegoing:ore_washer" ->
                        translateOreWasher((OreFluidEntryRaw) jeiRecipe);
                case "industrialforegoing:ore_sieve" ->
                        translateFluidSieve((OreFluidEntrySieve) jeiRecipe);
                case "industrialforegoing:bioreactor" ->
                        translateBioReactor((BioReactorRecipeCategory.ReactorRecipeWrapper) jeiRecipe);
                case "industrialforegoing:stone_work" ->
                        translateStoneWork((StoneWorkWrapper) jeiRecipe);
                case "industrialforegoing:stone_work_generator" ->
                        translateStoneWorkGenerator((StoneWorkGenerateRecipe) jeiRecipe);
                case "industrialforegoing:machine_produce" ->
                        translateMachineProduce((MachineProduceWrapper) jeiRecipe);
                default -> null;
            };
        } catch (Exception e) {
            LOGGER.error("translate: exception in category {}: {}", categoryId, e.getMessage());
            return null;
        }

        if (result != null) {
            LOGGER.debug("translate: category={} -> id={}, {} inputs, {} outputs, {} catalysts, {} fluidIn, {} fluidOut",
                    categoryId, result.getId(),
                    result.getInputs().size(), result.getOutputs().size(),
                    result.getCatalysts().size(),
                    result.getFluidInputs().size(), result.getFluidOutputs().size());
        }
        return result;
    }

    // ─── Laser Drill (Ore) ────────────────────────────────────────────────────

    private RecipeIR translateLaserOre(LaserDrillOreRecipe recipe) {
        var builder = baseBuilder("laser_ore", recipe);
        addCatalyst(builder, "ore_laser_base");
        addCatalyst(builder, "laser_drill");
        addIngredientInput(builder, recipe.catalyst);
        addSizedIngredientOutput(builder, recipe.output);
        var items = recipe.output.ingredient().getItems();
        if (items.length > 0) {
            builder.displayNameTranslationKey("recipe.emibridge_if.laser_ore",
                    List.of(items[0].getDescriptionId()));
        } else {
            builder.displayName("Laser Drill");
        }
        addRarityWidgets(builder, recipe.rarity);
        addEntityWidget(builder, recipe.entityData);
        return builder.build();
    }

    // ─── Laser Drill (Fluid) ──────────────────────────────────────────────────

    private RecipeIR translateLaserFluid(LaserDrillFluidRecipe recipe) {
        var builder = baseBuilder("laser_fluid", recipe);
        addCatalyst(builder, "fluid_laser_base");
        addCatalyst(builder, "laser_drill");
        addIngredientInput(builder, recipe.catalyst);
        addSizedFluidOutput(builder, recipe.output);

        if (recipe.output != null && !recipe.output.ingredient().isEmpty()) {
            var stacks = recipe.output.getFluids();
            if (stacks != null && stacks.length > 0 && !stacks[0].isEmpty()) {
                builder.displayNameTranslationKey("recipe.emibridge_if.laser_fluid",
                        List.of(stacks[0].getFluid().getFluidType().getDescriptionId()));
            } else {
                builder.displayName("Laser Drill");
            }
        } else {
            builder.displayName("Laser Drill");
        }

        addRarityWidgets(builder, recipe.rarity);
        addEntityWidget(builder, recipe.entityData);
        return builder.build();
    }

    // ─── Fluid Extractor ──────────────────────────────────────────────────────

    private RecipeIR translateFluidExtractor(FluidExtractorRecipe recipe) {
        var builder = baseBuilder("fluid_extractor", recipe);
        addCatalyst(builder, "fluid_extractor");
        addIngredientInput(builder, recipe.input);

        if (!recipe.output.isEmpty()) {
            var logItems = recipe.input.getItems();
            if (logItems.length > 0) {
                builder.displayNameTranslationKey("recipe.emibridge_if.fluid_extractor",
                        List.of(
                                recipe.output.getFluid().getFluidType().getDescriptionId(),
                                logItems[0].getDescriptionId()));
            } else {
                builder.displayNameTranslationKey("recipe.emibridge_if.fluid_extractor",
                        List.of(recipe.output.getFluid().getFluidType().getDescriptionId()));
            }
        }

        var resultItem = new ItemStack(recipe.result.getBlock());
        if (!resultItem.isEmpty()) {
            builder.addOutput(toSlot(resultItem));
        }

        if (!recipe.output.isEmpty()) {
            addFluidOutput(builder, recipe.output);
        }

        builder.addWidget(new WidgetDescriptor(
                WidgetType.TEXT, 0, 60, 100, 10,
                Map.of("text", recipe.output.getAmount() + " mB/tick", "color", 0xAAAAAA)));

        if (recipe.outputsLatex()) {
            builder.addWidget(new WidgetDescriptor(
                    WidgetType.TEXT, 0, 72, 110, 10,
                    Map.of("text", "Tripled when powered", "color", 0x55CCCC)));
        }

        return builder.build();
    }

    // ─── Dissolution Chamber ──────────────────────────────────────────────────

    private RecipeIR translateDissolution(DissolutionChamberRecipe recipe) {
        var builder = baseBuilder("dissolution", recipe);
        addCatalyst(builder, "dissolution_chamber");

        for (var ing : recipe.input) {
            addIngredientInput(builder, ing);
        }

        addSizedFluidInput(builder, recipe.inputFluid);

        if (recipe.output.isPresent()) {
            builder.displayNameTranslationKey("recipe.emibridge_if.dissolution",
                    List.of(recipe.output.get().getDescriptionId()));
        } else if (recipe.outputFluid.isPresent()) {
            builder.displayNameTranslationKey("recipe.emibridge_if.dissolution",
                    List.of(recipe.outputFluid.get().getFluid().getFluidType().getDescriptionId()));
        } else {
            builder.displayName("Dissolution");
        }

        recipe.output.ifPresent(stack -> builder.addOutput(toSlot(stack)));
        recipe.outputFluid.ifPresent(fluid -> addFluidOutput(builder, fluid));

        long energy = (long) recipe.processingTime * DissolutionChamberConfig.powerPerTick;
        builder.energyCost(energy);
        builder.duration(recipe.processingTime);

        builder.addWidget(new WidgetDescriptor(
                WidgetType.ENERGY_BAR, 0, 12, 14, 48,
                Map.of("energy", energy)));
        builder.addWidget(new WidgetDescriptor(
                WidgetType.PROGRESS_ARROW, 92, 33, 24, 17,
                Map.of("time", recipe.processingTime)));

        return builder.build();
    }

    // ─── Fermentation Station ─────────────────────────────────────────────────

    private RecipeIR translateFermenter(OreFluidEntryFermenter recipe) {
        var hasher = System.identityHashCode(recipe);
        var builder = RecipeIR.builder()
                .id("industrialforegoing:/fermentation_station/" + hasher)
                .sourceMod(MOD_ID)
                .categoryKey("industrialforegoing:fermentation_station");
        if (!recipe.getInput().isEmpty() && !recipe.getOutput().isEmpty()) {
            builder.displayNameTranslationKey("recipe.emibridge_if.fermenter",
                    List.of(
                            recipe.getOutput().getFluid().getFluidType().getDescriptionId(),
                            recipe.getInput().getFluid().getFluidType().getDescriptionId()));
        } else {
            builder.displayName("Fermentation");
        }
        addCatalyst(builder, "fermentation_station");
        addFluidInput(builder, recipe.getInput());
        addFluidOutput(builder, recipe.getOutput());
        return builder.build();
    }

    // ─── Ore Washer ───────────────────────────────────────────────────────────

    private RecipeIR translateOreWasher(OreFluidEntryRaw recipe) {
        var hasher = System.identityHashCode(recipe);
        var builder = RecipeIR.builder()
                .id("industrialforegoing:/washing_factory/" + hasher)
                .sourceMod(MOD_ID)
                .categoryKey("industrialforegoing:washing_factory");
        var optItem = BuiltInRegistries.ITEM.getTag(recipe.getOre())
                .flatMap(tag -> tag.stream().findFirst())
                .map(holder -> holder.value().getDescriptionId());
        if (optItem.isPresent()) {
            builder.displayNameTranslationKey("recipe.emibridge_if.ore_washer", List.of(optItem.get()));
        } else {
            builder.displayName("Wash: " + recipe.getOre().location().getPath().replace("raw_materials/", ""));
        }
        addCatalyst(builder, "washing_factory");
        addTagInput(builder, recipe.getOre().location());
        addFluidInput(builder, recipe.getInput());
        addFluidOutput(builder, recipe.getOutput());
        return builder.build();
    }

    // ─── Fluid Sieve ──────────────────────────────────────────────────────────

    private RecipeIR translateFluidSieve(OreFluidEntrySieve recipe) {
        var hasher = System.identityHashCode(recipe);
        var builder = RecipeIR.builder()
                .id("industrialforegoing:/fluid_sieving_machine/" + hasher)
                .sourceMod(MOD_ID)
                .categoryKey("industrialforegoing:fluid_sieving_machine");
        builder.displayNameTranslationKey("recipe.emibridge_if.ore_sieve",
                List.of(recipe.getOutput().getDescriptionId()));
        addCatalyst(builder, "fluid_sieving_machine");
        addFluidInput(builder, recipe.getInput());
        addTagInput(builder, recipe.getSieveItem().location());
        builder.addOutput(toSlot(recipe.getOutput()));
        return builder.build();
    }

    // ─── BioReactor ───────────────────────────────────────────────────────────

    private RecipeIR translateBioReactor(BioReactorRecipeCategory.ReactorRecipeWrapper recipe) {
        var builder = baseBuilder("bioreactor", recipe);
        if (!recipe.getFluid().isEmpty()) {
            builder.displayNameTranslationKey("recipe.emibridge_if.bioreactor",
                    List.of(recipe.getFluid().getFluid().getFluidType().getDescriptionId()));
        } else {
            builder.displayName("Bio Reactor");
        }
        addCatalyst(builder, "bioreactor");
        addTagInput(builder, recipe.getStack().location());
        addFluidOutput(builder, recipe.getFluid());
        return builder.build();
    }

    // ─── Stone Work Factory ───────────────────────────────────────────────────

    private RecipeIR translateStoneWork(StoneWorkWrapper recipe) {
        var builder = baseBuilder("stone_work", recipe);
        builder.displayNameTranslationKey("recipe.emibridge_if.stone_work",
                List.of(recipe.output().getDescriptionId()));
        addCatalyst(builder, "material_stonework_factory");
        builder.addInput(toSlot(recipe.input()));

        for (var mode : recipe.modes()) {
            var wrapper = toWrapper(mode.getIcon());
            if (wrapper != null) {
                builder.addCatalyst(new IngredientSlot(List.of(wrapper)));
            }
        }

        builder.addOutput(toSlot(recipe.output()));
        return builder.build();
    }

    // ─── Stone Work Generator ─────────────────────────────────────────────────

    private RecipeIR translateStoneWorkGenerator(StoneWorkGenerateRecipe recipe) {
        var builder = baseBuilder("stone_work_generator", recipe);
        builder.displayNameTranslationKey("recipe.emibridge_if.stone_work_generator",
                List.of(recipe.output.getDescriptionId()));
        addCatalyst(builder, "material_stonework_factory");
        builder.addOutput(toSlot(recipe.output));

        builder.addWidget(new WidgetDescriptor(
                WidgetType.TEXT, 0, 60, 130, 10,
                Map.of("text", "Water: " + recipe.waterNeed + " mB (-" + recipe.waterConsume + "/op)", "color", 0x4444FF)));
        builder.addWidget(new WidgetDescriptor(
                WidgetType.TEXT, 0, 72, 130, 10,
                Map.of("text", "Lava: " + recipe.lavaNeed + " mB (-" + recipe.lavaConsume + "/op)", "color", 0xFF4444)));

        return builder.build();
    }

    // ─── Machine Produce ──────────────────────────────────────────────────────

    private RecipeIR translateMachineProduce(MachineProduceWrapper recipe) {
        var blockId = BuiltInRegistries.BLOCK.getKey(recipe.getBlock());
        var hash = System.identityHashCode(recipe);
        var categoryKey = blockId != null ? blockId.getNamespace() + ":" + blockId.getPath() : "industrialforegoing:machine_produce";
        var builder = RecipeIR.builder()
                .id("industrialforegoing:/" + (blockId != null ? blockId.getPath() : "machine_produce") + "/" + hash)
                .sourceMod(MOD_ID)
                .categoryKey(categoryKey);

        if (blockId != null) {
            builder.addCatalyst(new IngredientSlot(List.of(
                    new EmiIngredientWrapper(blockId.getNamespace(), blockId.getPath(), 1))));
            var blockItem = recipe.getBlock().asItem();
            String machineDescId = blockItem != net.minecraft.world.item.Items.AIR
                    ? blockItem.getDescriptionId()
                    : recipe.getBlock().getDescriptionId();
            String outputDescId = null;
            if (recipe.getOutputItem() != null && !recipe.getOutputItem().isEmpty()) {
                var stacks = recipe.getOutputItem().getItems();
                if (stacks.length > 0) {
                    outputDescId = stacks[0].getDescriptionId();
                }
            } else if (!recipe.getOutputFluid().isEmpty()) {
                outputDescId = recipe.getOutputFluid().getFluid().getFluidType().getDescriptionId();
            }
            if (outputDescId != null) {
                builder.displayNameTranslationKey("recipe.emibridge_if.machine_produce",
                        List.of(machineDescId, outputDescId));
            } else {
                builder.displayNameTranslationKey("recipe.emibridge_if.machine_produce.single",
                        List.of(machineDescId));
            }
        }

        if (recipe.getOutputItem() != null && !recipe.getOutputItem().isEmpty()) {
            var stacks = recipe.getOutputItem().getItems();
            var wrappers = new ArrayList<EmiIngredientWrapper>();
            for (var stack : stacks) {
                var wrapper = toWrapper(stack);
                if (wrapper != null) {
                    wrappers.add(wrapper);
                }
            }
            if (!wrappers.isEmpty()) {
                builder.addOutput(new IngredientSlot(wrappers));
            }
        }

        if (!recipe.getOutputFluid().isEmpty()) {
            addFluidOutput(builder, recipe.getOutputFluid());
        }

        return builder.build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static RecipeIR.Builder baseBuilder(String category, Object recipe) {
        var hasher = System.identityHashCode(recipe);
        return RecipeIR.builder()
                .id("industrialforegoing:/" + category + "/" + hasher)
                .sourceMod(MOD_ID)
                .categoryKey("industrialforegoing:" + category);
    }

    private static void addCatalyst(RecipeIR.Builder builder, String machinePath) {
        builder.addCatalyst(new IngredientSlot(List.of(
                new EmiIngredientWrapper(MOD_ID, machinePath, 1))));
    }

    private static void addTagInput(RecipeIR.Builder builder, ResourceLocation loc) {
        builder.addInput(new IngredientSlot(List.of(
                new EmiIngredientWrapper(loc.getNamespace(), loc.getPath(), 1))));
    }

    private void addIngredientInput(RecipeIR.Builder builder, Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return;
        var wrappers = flattenIngredient(ingredient);
        if (!wrappers.isEmpty()) {
            builder.addInput(new IngredientSlot(wrappers));
        }
    }

    private void addSizedIngredientOutput(RecipeIR.Builder builder, SizedIngredient sized) {
        if (sized == null || sized.ingredient().isEmpty()) return;
        var wrappers = new ArrayList<EmiIngredientWrapper>();
        for (var stack : sized.ingredient().getItems()) {
            var id = stack.getItemHolder().unwrapKey()
                    .map(key -> key.location()).orElse(null);
            if (id != null) {
                wrappers.add(new EmiIngredientWrapper(
                        id.getNamespace(), id.getPath(), sized.count()));
            }
        }
        if (!wrappers.isEmpty()) {
            builder.addOutput(new IngredientSlot(wrappers));
        }
    }

    private static void addSizedFluidInput(RecipeIR.Builder builder, net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient sized) {
        if (sized == null || sized.ingredient().isEmpty()) return;
        var stacks = sized.getFluids();
        if (stacks == null || stacks.length == 0 || stacks[0].isEmpty()) return;
        var loc = BuiltInRegistries.FLUID.getKey(stacks[0].getFluid());
        if (loc != null) {
            builder.addFluidInput(new FluidSlot(
                    new FluidStackWrapper(loc.getNamespace(), loc.getPath(),
                            stacks[0].getAmount()), 1.0f));
        }
    }

    private static void addSizedFluidOutput(RecipeIR.Builder builder, net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient sized) {
        if (sized == null || sized.ingredient().isEmpty()) return;
        var stacks = sized.getFluids();
        if (stacks == null || stacks.length == 0 || stacks[0].isEmpty()) return;
        var loc = BuiltInRegistries.FLUID.getKey(stacks[0].getFluid());
        if (loc != null) {
            builder.addFluidOutput(new FluidSlot(
                    new FluidStackWrapper(loc.getNamespace(), loc.getPath(),
                            stacks[0].getAmount()), 1.0f));
        }
    }

    private static void addFluidInput(RecipeIR.Builder builder, FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return;
        var loc = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        if (loc != null) {
            builder.addFluidInput(new FluidSlot(
                    new FluidStackWrapper(loc.getNamespace(), loc.getPath(),
                            fluid.getAmount()), 1.0f));
        }
    }

    private static void addFluidOutput(RecipeIR.Builder builder, FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return;
        var loc = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        if (loc != null) {
            builder.addFluidOutput(new FluidSlot(
                    new FluidStackWrapper(loc.getNamespace(), loc.getPath(),
                            fluid.getAmount()), 1.0f));
        }
    }

    private static void addRarityWidgets(RecipeIR.Builder builder, List<LaserDrillRarity> rarities) {
        if (rarities == null || rarities.isEmpty()) return;
        int y = 60;
        for (int i = 0; i < Math.min(rarities.size(), 4); i++) {
            var r = rarities.get(i);
            var info = "Y:" + r.depth_min() + "-" + r.depth_max()
                    + " W:" + r.weight()
                    + dimTagStr(r.dimensionRarity())
                    + biomeTagStr(r.biomeRarity());
            builder.addWidget(new WidgetDescriptor(
                    WidgetType.TEXT, 0, y, 155, 10,
                    Map.of("text", info, "color", 0xAAAAAA)));
            y += 10;
        }
    }

    private static void addEntityWidget(RecipeIR.Builder builder, Optional<EntityData> entityData) {
        if (entityData == null || entityData.isEmpty()) return;
        var entity = entityData.get().getEntity();
        var name = entity.isTag()
                ? "#" + entity.tag().location()
                : entity.getType().getDescription().getString();
        builder.addWidget(new WidgetDescriptor(
                WidgetType.TEXT, 0, 52, 120, 10,
                Map.of("text", "Over: " + name, "color", 0xFFAA00)));
    }

    private static String dimTagStr(LaserDrillRarity.DimensionRarity dim) {
        if (!dim.whitelist().isEmpty())
            return " dim:" + dim.whitelist().getFirst().location().getPath();
        if (!dim.blacklist().isEmpty())
            return " !" + dim.blacklist().getFirst().location().getPath();
        return "";
    }

    private static String biomeTagStr(LaserDrillRarity.BiomeRarity biome) {
        if (!biome.whitelist().isEmpty()) {
            var tag = biome.whitelist().getFirst().location();
            return " biome:" + tag.getPath();
        }
        if (!biome.blacklist().isEmpty()) {
            var tag = biome.blacklist().getFirst().location();
            return " !" + tag.getPath();
        }
        return "";
    }

    private List<EmiIngredientWrapper> flattenIngredient(Ingredient ingredient) {
        var result = new ArrayList<EmiIngredientWrapper>();
        for (var stack : ingredient.getItems()) {
            var wrapper = toWrapper(stack);
            if (wrapper != null) {
                result.add(wrapper);
            }
        }
        return result;
    }

    private static IngredientSlot toSlot(ItemStack stack) {
        var wrapper = toWrapper(stack);
        if (wrapper == null) return null;
        return new IngredientSlot(List.of(wrapper));
    }

    private static EmiIngredientWrapper toWrapper(ItemStack stack) {
        var id = stack.getItemHolder().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        if (id == null) return null;
        return new EmiIngredientWrapper(
                id.getNamespace(), id.getPath(), stack.getCount());
    }
}
