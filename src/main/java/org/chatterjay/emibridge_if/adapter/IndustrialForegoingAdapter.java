package org.chatterjay.emibridge_if.adapter;

import com.buuz135.industrial.api.recipe.ore.OreFluidEntryFermenter;
import com.buuz135.industrial.api.recipe.ore.OreFluidEntryRaw;
import com.buuz135.industrial.api.recipe.ore.OreFluidEntrySieve;
import com.buuz135.industrial.config.machine.core.DissolutionChamberConfig;
import com.buuz135.industrial.plugin.jei.StoneWorkWrapper;
import com.buuz135.industrial.plugin.jei.category.BioReactorRecipeCategory;
import com.buuz135.industrial.plugin.jei.machineproduce.MachineProduceWrapper;
import com.buuz135.industrial.recipe.*;
import com.buuz135.industrial.recipe.data.EntityData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import org.chatterjay.emibridge.api.IRecipeAdapter;
import org.chatterjay.emibridge.api.RecipeIR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
                "industrialforegoing:fermentation_station",
                "industrialforegoing:washing_factory",
                "industrialforegoing:fluid_sieving_machine",
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
                case "industrialforegoing:fermentation_station" ->
                        translateFermenter((OreFluidEntryFermenter) jeiRecipe);
                case "industrialforegoing:washing_factory" ->
                        translateOreWasher((OreFluidEntryRaw) jeiRecipe);
                case "industrialforegoing:fluid_sieving_machine" ->
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
            LOGGER.error("translate: exception in category {} for recipe {}: {}", categoryId, jeiRecipe, e.getMessage());
            return null;
        }
        if (result != null) {
            LOGGER.debug("translate: category={} -> id={}, {} inputs, {} outputs, {} catalysts, {} fluidIn, {} fluidOut",
                    categoryId, result.getId(),
                    result.getInputs().size(), result.getOutputs().size(),
                    result.getCatalysts().size(),
                    result.getFluidInputs().size(), result.getFluidOutputs().size());
        } else {
            LOGGER.debug("translate: category={} -> null (recipe not translated)", categoryId);
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

        addRarityWidgets(builder, recipe.rarity);
        addEntityWidget(builder, recipe.entityData);
        return builder.build();
    }

    // ─── Fluid Extractor ──────────────────────────────────────────────────────

    private RecipeIR translateFluidExtractor(FluidExtractorRecipe recipe) {
        var builder = baseBuilder("fluid_extractor", recipe);
        addCatalyst(builder, "fluid_extractor");
        addIngredientInput(builder, recipe.input);

        var resultItem = new ItemStack(recipe.result.getBlock());
        if (!resultItem.isEmpty()) {
            builder.addOutput(toSlot(resultItem));
        }

        if (!recipe.output.isEmpty()) {
            addFluidOutput(builder, recipe.output);
        }

        builder.addWidget(new RecipeIR.WidgetDescriptor(
                RecipeIR.WidgetType.TEXT, 0, 60, 100, 10,
                Map.of("text", recipe.output.getAmount() + " mB/tick", "color", 0xAAAAAA)));

        if (recipe.outputsLatex()) {
            builder.addWidget(new RecipeIR.WidgetDescriptor(
                    RecipeIR.WidgetType.TEXT, 0, 72, 110, 10,
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

        recipe.output.ifPresent(stack -> builder.addOutput(toSlot(stack)));
        recipe.outputFluid.ifPresent(fluid -> addFluidOutput(builder, fluid));

        long energy = (long) recipe.processingTime * DissolutionChamberConfig.powerPerTick;
        builder.energyCost(energy);
        builder.duration(recipe.processingTime);

        builder.addWidget(new RecipeIR.WidgetDescriptor(
                RecipeIR.WidgetType.ENERGY_BAR, 0, 12, 14, 48,
                Map.of("energy", energy)));
        builder.addWidget(new RecipeIR.WidgetDescriptor(
                RecipeIR.WidgetType.PROGRESS_ARROW, 92, 33, 24, 17,
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
        addCatalyst(builder, "fluid_sieving_machine");
        addFluidInput(builder, recipe.getInput());
        addTagInput(builder, recipe.getSieveItem().location());
        builder.addOutput(toSlot(recipe.getOutput()));
        return builder.build();
    }

    // ─── BioReactor ───────────────────────────────────────────────────────────

    private RecipeIR translateBioReactor(BioReactorRecipeCategory.ReactorRecipeWrapper recipe) {
        var builder = baseBuilder("bioreactor", recipe);
        addCatalyst(builder, "bioreactor");
        addTagInput(builder, recipe.getStack().location());
        addFluidOutput(builder, recipe.getFluid());
        return builder.build();
    }

    // ─── Stone Work Factory ───────────────────────────────────────────────────

    private RecipeIR translateStoneWork(StoneWorkWrapper recipe) {
        var builder = baseBuilder("stone_work", recipe);
        addCatalyst(builder, "material_stonework_factory");
        builder.addInput(toSlot(recipe.input()));

        for (var mode : recipe.modes()) {
            var wrapper = toWrapper(mode.getIcon());
            if (wrapper != null) {
                builder.addCatalyst(new RecipeIR.IngredientSlot(List.of(wrapper)));
            }
        }

        builder.addOutput(toSlot(recipe.output()));
        return builder.build();
    }

    // ─── Stone Work Generator ─────────────────────────────────────────────────

    private RecipeIR translateStoneWorkGenerator(StoneWorkGenerateRecipe recipe) {
        var builder = baseBuilder("stone_work_generator", recipe);
        addCatalyst(builder, "material_stonework_factory");
        builder.addOutput(toSlot(recipe.output));

        builder.addWidget(new RecipeIR.WidgetDescriptor(
                RecipeIR.WidgetType.TEXT, 0, 60, 130, 10,
                Map.of("text", "Water: " + recipe.waterNeed + " mB (-" + recipe.waterConsume + "/op)", "color", 0x4444FF)));
        builder.addWidget(new RecipeIR.WidgetDescriptor(
                RecipeIR.WidgetType.TEXT, 0, 72, 130, 10,
                Map.of("text", "Lava: " + recipe.lavaNeed + " mB (-" + recipe.lavaConsume + "/op)", "color", 0xFF4444)));

        return builder.build();
    }

    // ─── Machine Produce ──────────────────────────────────────────────────────

    private RecipeIR translateMachineProduce(MachineProduceWrapper recipe) {
        var blockId = BuiltInRegistries.BLOCK.getKey(recipe.getBlock());
        var hash = System.identityHashCode(recipe);
        // 分类 key 改为方块路径，例如 industrialforegoing:latex_processing
        var categoryKey = blockId != null ? blockId.getNamespace() + ":" + blockId.getPath() : "industrialforegoing:machine_produce";
        var builder = RecipeIR.builder()
                .id("industrialforegoing:/" + (blockId != null ? blockId.getPath() : "machine_produce") + "/" + hash)
                .sourceMod(MOD_ID)
                .categoryKey(categoryKey);

        if (blockId != null) {
            builder.addCatalyst(new RecipeIR.IngredientSlot(List.of(
                    new RecipeIR.EmiIngredientWrapper(blockId.getNamespace(), blockId.getPath(), 1))));
            // 获取机器显示名：优先 item 名，fallback 到 block 名
            String machineName;
            var blockItem = recipe.getBlock().asItem();
            if (blockItem != net.minecraft.world.item.Items.AIR) {
                machineName = blockItem.getName(new ItemStack(blockItem)).getString();
            } else {
                machineName = recipe.getBlock().getName().getString();
            }
            // 追加产出物描述区分同名机器（如屠宰厂→肉 vs 屠宰厂→粉色史莱姆）
            String outputDesc = null;
            if (recipe.getOutputItem() != null && !recipe.getOutputItem().isEmpty()) {
                var stacks = recipe.getOutputItem().getItems();
                if (stacks.length == 1) {
                    outputDesc = stacks[0].getDisplayName().getString();
                } else if (stacks.length > 1) {
                    outputDesc = stacks[0].getDisplayName().getString() + " +" + (stacks.length - 1);
                }
            } else if (!recipe.getOutputFluid().isEmpty()) {
                outputDesc = recipe.getOutputFluid().getHoverName().getString();
            }
            if (outputDesc != null) {
                builder.displayName(machineName + " → " + outputDesc);
                LOGGER.debug("translateMachineProduce: machine={}, displayName='{} → {}'",
                        blockId.getPath(), machineName, outputDesc);
            } else {
                builder.displayName(machineName);
                LOGGER.debug("translateMachineProduce: machine={}, displayName='{}' (no output desc)", blockId.getPath(), machineName);
            }
        }

        if (recipe.getOutputItem() != null && !recipe.getOutputItem().isEmpty()) {
            var stacks = recipe.getOutputItem().getItems();
            LOGGER.debug("translateMachineProduce: {} item output stacks from tag expansion", stacks.length);
            var wrappers = new ArrayList<RecipeIR.EmiIngredientWrapper>();
            for (var stack : stacks) {
                var wrapper = toWrapper(stack);
                if (wrapper != null) {
                    wrappers.add(wrapper);
                } else {
                    LOGGER.trace("translateMachineProduce: failed to wrap item stack {}", stack.getDisplayName().getString());
                }
            }
            if (!wrappers.isEmpty()) {
                builder.addOutput(new RecipeIR.IngredientSlot(wrappers));
                LOGGER.debug("translateMachineProduce: added {} item outputs (no tag cycling)", wrappers.size());
            } else {
                LOGGER.warn("translateMachineProduce: output item tag expanded to 0 wrappers");
            }
        }

        if (!recipe.getOutputFluid().isEmpty()) {
            var fluidId = BuiltInRegistries.FLUID.getKey(recipe.getOutputFluid().getFluid());
            LOGGER.debug("translateMachineProduce: fluid output {} x {} mB", fluidId, recipe.getOutputFluid().getAmount());
            addFluidOutput(builder, recipe.getOutputFluid());
        } else {
            LOGGER.trace("translateMachineProduce: no fluid output");
        }

        return builder.build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static RecipeIR.Builder baseBuilder(String category, Object recipe) {
        var hasher = System.identityHashCode(recipe);
        LOGGER.trace("baseBuilder: category={}, identityHash={}", category, hasher);
        return RecipeIR.builder()
                .id("industrialforegoing:/" + category + "/" + hasher)
                .sourceMod(MOD_ID)
                .categoryKey("industrialforegoing:" + category);
    }

    private static void addCatalyst(RecipeIR.Builder builder, String machinePath) {
        LOGGER.trace("addCatalyst: machine={}", machinePath);
        builder.addCatalyst(new RecipeIR.IngredientSlot(List.of(
                new RecipeIR.EmiIngredientWrapper(MOD_ID, machinePath, 1))));
    }

    private static void addTagInput(RecipeIR.Builder builder, net.minecraft.resources.ResourceLocation loc) {
        LOGGER.trace("addTagInput: tag={}", loc);
        builder.addInput(new RecipeIR.IngredientSlot(List.of(
                new RecipeIR.EmiIngredientWrapper(loc.getNamespace(), loc.getPath(), 1))));
    }

    private void addIngredientInput(RecipeIR.Builder builder, Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            LOGGER.trace("addIngredientInput: null or empty ingredient");
            return;
        }
        var wrappers = flattenIngredient(ingredient);
        if (!wrappers.isEmpty()) {
            builder.addInput(new RecipeIR.IngredientSlot(wrappers));
            LOGGER.trace("addIngredientInput: added {} alternatives", wrappers.size());
        } else {
            LOGGER.debug("addIngredientInput: ingredient expanded to 0 wrappers");
        }
    }

    private void addSizedIngredientOutput(RecipeIR.Builder builder, SizedIngredient sized) {
        if (sized == null || sized.ingredient().isEmpty()) {
            LOGGER.trace("addSizedIngredientOutput: null or empty sized ingredient");
            return;
        }
        var wrappers = new ArrayList<RecipeIR.EmiIngredientWrapper>();
        for (var stack : sized.ingredient().getItems()) {
            var id = stack.getItemHolder().unwrapKey()
                    .map(key -> key.location()).orElse(null);
            if (id != null) {
                wrappers.add(new RecipeIR.EmiIngredientWrapper(
                        id.getNamespace(), id.getPath(), sized.count()));
            } else {
                LOGGER.trace("addSizedIngredientOutput: unwrapKey failed for item in sized ingredient");
            }
        }
        if (!wrappers.isEmpty()) {
            builder.addOutput(new RecipeIR.IngredientSlot(wrappers));
            LOGGER.trace("addSizedIngredientOutput: added {} outputs", wrappers.size());
        } else {
            LOGGER.debug("addSizedIngredientOutput: sized ingredient expanded to 0 wrappers");
        }
    }

    private static void addSizedFluidInput(RecipeIR.Builder builder, net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient sized) {
        if (sized == null || sized.ingredient().isEmpty()) {
            LOGGER.trace("addSizedFluidInput: null or empty sized fluid");
            return;
        }
        var stacks = sized.getFluids();
        if (stacks == null || stacks.length == 0 || stacks[0].isEmpty()) {
            LOGGER.trace("addSizedFluidInput: no fluid stacks resolved");
            return;
        }
        var loc = BuiltInRegistries.FLUID.getKey(stacks[0].getFluid());
        if (loc != null) {
            builder.addFluidInput(new RecipeIR.FluidSlot(
                    new RecipeIR.FluidStackWrapper(loc.getNamespace(), loc.getPath(),
                            stacks[0].getAmount()), 1.0f));
            LOGGER.trace("addSizedFluidInput: {} x {} mB", loc, stacks[0].getAmount());
        } else {
            LOGGER.debug("addSizedFluidInput: fluid not found in registry");
        }
    }

    private static void addSizedFluidOutput(RecipeIR.Builder builder, net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient sized) {
        if (sized == null || sized.ingredient().isEmpty()) {
            LOGGER.trace("addSizedFluidOutput: null or empty sized fluid");
            return;
        }
        var stacks = sized.getFluids();
        if (stacks == null || stacks.length == 0 || stacks[0].isEmpty()) {
            LOGGER.trace("addSizedFluidOutput: no fluid stacks resolved");
            return;
        }
        var loc = BuiltInRegistries.FLUID.getKey(stacks[0].getFluid());
        if (loc != null) {
            builder.addFluidOutput(new RecipeIR.FluidSlot(
                    new RecipeIR.FluidStackWrapper(loc.getNamespace(), loc.getPath(),
                            stacks[0].getAmount()), 1.0f));
            LOGGER.trace("addSizedFluidOutput: {} x {} mB", loc, stacks[0].getAmount());
        } else {
            LOGGER.debug("addSizedFluidOutput: fluid not found in registry");
        }
    }

    private static void addFluidInput(RecipeIR.Builder builder, FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            LOGGER.trace("addFluidInput: null or empty fluid");
            return;
        }
        var loc = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        if (loc != null) {
            builder.addFluidInput(new RecipeIR.FluidSlot(
                    new RecipeIR.FluidStackWrapper(loc.getNamespace(), loc.getPath(),
                            fluid.getAmount()), 1.0f));
            LOGGER.trace("addFluidInput: {} x {} mB", loc, fluid.getAmount());
        } else {
            LOGGER.debug("addFluidInput: fluid not found in registry: {}", fluid.getFluid());
        }
    }

    private static void addFluidOutput(RecipeIR.Builder builder, FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            LOGGER.trace("addFluidOutput: null or empty fluid");
            return;
        }
        var loc = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        if (loc != null) {
            builder.addFluidOutput(new RecipeIR.FluidSlot(
                    new RecipeIR.FluidStackWrapper(loc.getNamespace(), loc.getPath(),
                            fluid.getAmount()), 1.0f));
            LOGGER.trace("addFluidOutput: {} x {} mB", loc, fluid.getAmount());
        } else {
            LOGGER.debug("addFluidOutput: fluid not found in registry: {}", fluid.getFluid());
        }
    }

    private static void addRarityWidgets(RecipeIR.Builder builder, List<LaserDrillRarity> rarities) {
        if (rarities == null || rarities.isEmpty()) {
            LOGGER.trace("addRarityWidgets: no rarities");
            return;
        }
        LOGGER.trace("addRarityWidgets: {} rarities", rarities.size());
        int y = 60;
        for (int i = 0; i < Math.min(rarities.size(), 4); i++) {
            var r = rarities.get(i);
            var info = "Y:" + r.depth_min() + "-" + r.depth_max()
                    + " W:" + r.weight()
                    + dimTagStr(r.dimensionRarity())
                    + biomeTagStr(r.biomeRarity());
            builder.addWidget(new RecipeIR.WidgetDescriptor(
                    RecipeIR.WidgetType.TEXT, 0, y, 155, 10,
                    Map.of("text", info, "color", 0xAAAAAA)));
            LOGGER.trace("addRarityWidgets[{}]: {}", i, info);
            y += 10;
        }
    }

    private static void addEntityWidget(RecipeIR.Builder builder, Optional<EntityData> entityData) {
        if (entityData == null || entityData.isEmpty()) {
            LOGGER.trace("addEntityWidget: no entity data");
            return;
        }
        var entity = entityData.get().getEntity();
        var name = entity.isTag()
                ? "#" + entity.tag().location()
                : entity.getType().getDescription().getString();
        builder.addWidget(new RecipeIR.WidgetDescriptor(
                RecipeIR.WidgetType.TEXT, 0, 52, 120, 10,
                Map.of("text", "Over: " + name, "color", 0xFFAA00)));
        LOGGER.trace("addEntityWidget: {}", name);
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

    private List<RecipeIR.EmiIngredientWrapper> flattenIngredient(Ingredient ingredient) {
        var result = new ArrayList<RecipeIR.EmiIngredientWrapper>();
        for (var stack : ingredient.getItems()) {
            var wrapper = toWrapper(stack);
            if (wrapper != null) {
                result.add(wrapper);
            } else {
                LOGGER.trace("flattenIngredient: failed to wrap {}", stack.getDisplayName().getString());
            }
        }
        if (result.isEmpty()) {
            LOGGER.debug("flattenIngredient: ingredient expanded to 0 wrappers");
        }
        return result;
    }

    private static RecipeIR.IngredientSlot toSlot(ItemStack stack) {
        var wrapper = toWrapper(stack);
        if (wrapper == null) {
            LOGGER.trace("toSlot: null wrapper for {}", stack.getDisplayName().getString());
            return null;
        }
        return new RecipeIR.IngredientSlot(List.of(wrapper));
    }

    private static RecipeIR.EmiIngredientWrapper toWrapper(ItemStack stack) {
        var id = stack.getItemHolder().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        if (id == null) {
            LOGGER.trace("toWrapper: unwrapKey returned null for stack {}", stack.getDisplayName().getString());
            return null;
        }
        return new RecipeIR.EmiIngredientWrapper(
                id.getNamespace(), id.getPath(), stack.getCount());
    }
}
