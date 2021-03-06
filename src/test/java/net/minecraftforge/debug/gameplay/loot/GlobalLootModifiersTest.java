/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.debug.gameplay.loot;

import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Mod(GlobalLootModifiersTest.MODID)
public class GlobalLootModifiersTest {
    public static final String MODID = "global_loot_test";
    public static final boolean ENABLE = true;

    public GlobalLootModifiersTest()
    {
        if(ENABLE)
        {
            GLM.register(FMLJavaModLoadingContext.get().getModEventBus());
            ENCHANTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    private static final DeferredRegister<GlobalLootModifierSerializer<?>> GLM = DeferredRegister.create(ForgeRegistries.LOOT_MODIFIER_SERIALIZERS, MODID);
    private static final DeferredRegister<Enchantment> ENCHANTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MODID);

    private static final RegistryObject<SmeltingEnchantmentModifier.Serializer> SMELTING = GLM.register("smelting", SmeltingEnchantmentModifier.Serializer::new);
    private static final RegistryObject<WheatSeedsConverterModifier.Serializer> WHEATSEEDS = GLM.register("wheat_harvest", WheatSeedsConverterModifier.Serializer::new);
    private static final RegistryObject<SilkTouchTestModifier.Serializer> SILKTOUCH = GLM.register("silk_touch_bamboo", SilkTouchTestModifier.Serializer::new);
    private static final RegistryObject<Enchantment> SMELT = ENCHANTS.register("smelt", () -> new SmelterEnchantment(Rarity.UNCOMMON, EnchantmentTarget.DIGGER, EquipmentSlot.MAINHAND));

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class EventHandlers {
        @SubscribeEvent
        public static void runData(GatherDataEvent event)
        {
            if(ENABLE)
                event.getGenerator().install(new DataProvider(event.getGenerator(), MODID));
        }
    }

    private static class DataProvider extends GlobalLootModifierProvider
    {
        public DataProvider(DataGenerator gen, String modid)
        {
            super(gen, modid);
        }

        @Override
        protected void start()
        {
            add("smelting", SMELTING.get(), new SmeltingEnchantmentModifier(
                    new LootCondition[]{
                            MatchToolLootCondition.builder(
                                    ItemPredicate.Builder.create().enchantment(
                                            new EnchantmentPredicate(SMELT.get(), NumberRange.IntRange.atLeast(1))))
                                    .build()
                    })
            );

            add("wheat_harvest", WHEATSEEDS.get(), new WheatSeedsConverterModifier(
                    new LootCondition[] {
                            MatchToolLootCondition.builder(ItemPredicate.Builder.create().item(Items.SHEARS)).build(),
                            BlockStatePropertyLootCondition.builder(Blocks.WHEAT).build()
                    },
                    3, Items.WHEAT_SEEDS, Items.WHEAT)
            );
        }
    }

    private static class SmelterEnchantment extends Enchantment {
        protected SmelterEnchantment(Rarity rarityIn, EnchantmentTarget typeIn, EquipmentSlot... slots) {
            super(rarityIn, typeIn, slots);
        }
    }

    /**
     * The smelting enchantment causes this modifier to be invoked, via the smelting loot_modifier json
     *
     */
    private static class SmeltingEnchantmentModifier extends LootModifier {
        public SmeltingEnchantmentModifier(LootCondition[] conditionsIn) {
            super(conditionsIn);
        }

        @Nonnull
        @Override
        public List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
            ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
            generatedLoot.forEach((stack) -> ret.add(smelt(stack, context)));
            return ret;
        }

        private static ItemStack smelt(ItemStack stack, LootContext context) {
            return context.getWorld().getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SimpleInventory(stack), context.getWorld())
                    .map(SmeltingRecipe::getOutput)
                    .filter(itemStack -> !itemStack.isEmpty())
                    .map(itemStack -> ItemHandlerHelper.copyStackWithSize(itemStack, stack.getCount() * itemStack.getCount()))
                    .orElse(stack);
        }

        private static class Serializer extends GlobalLootModifierSerializer<SmeltingEnchantmentModifier> {
            @Override
            public SmeltingEnchantmentModifier read(Identifier name, JsonObject json, LootCondition[] conditionsIn) {
                return new SmeltingEnchantmentModifier(conditionsIn);
            }

            @Override
            public JsonObject write(SmeltingEnchantmentModifier instance) {
                return makeConditions(instance.conditions);
            }
        }
    }

    /**
     * When harvesting blocks with bamboo, this modifier is invoked, via the silk_touch_bamboo loot_modifier json
     *
     */
    private static class SilkTouchTestModifier extends LootModifier {
        public SilkTouchTestModifier(LootCondition[] conditionsIn) {
            super(conditionsIn);
        }

        @Nonnull
        @Override
        public List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
            ItemStack ctxTool = context.get(LootContextParameters.TOOL);
            //return early if silk-touch is already applied (otherwise we'll get stuck in an infinite loop).
            if(EnchantmentHelper.get(ctxTool).containsKey(Enchantments.SILK_TOUCH)) return generatedLoot;
            ItemStack fakeTool = ctxTool.copy();
            fakeTool.addEnchantment(Enchantments.SILK_TOUCH, 1);
            LootContext.Builder builder = new LootContext.Builder(context);
            builder.parameter(LootContextParameters.TOOL, fakeTool);
            LootContext ctx = builder.build(LootContextTypes.BLOCK);
            LootTable loottable = context.getWorld().getServer().getLootManager().getTable(context.get(LootContextParameters.BLOCK_STATE).getBlock().getLootTableId());
            return loottable.generateLoot(ctx);
        }

        private static class Serializer extends GlobalLootModifierSerializer<SilkTouchTestModifier> {
            @Override
            public SilkTouchTestModifier read(Identifier name, JsonObject json, LootCondition[] conditionsIn) {
                return new SilkTouchTestModifier(conditionsIn);
            }

            @Override
            public JsonObject write(SilkTouchTestModifier instance) {
                return makeConditions(instance.conditions);
            }
        }
    }

    /**
     * When harvesting wheat with shears, this modifier is invoked via the wheat_harvest loot_modifier json<br/>
     * This modifier checks how many seeds were harvested and turns X seeds into Y wheat (3:1)
     *
     */
    private static class WheatSeedsConverterModifier extends LootModifier {
        private final int numSeedsToConvert;
        private final Item itemToCheck;
        private final Item itemReward;
        public WheatSeedsConverterModifier(LootCondition[] conditionsIn, int numSeeds, Item itemCheck, Item reward) {
            super(conditionsIn);
            numSeedsToConvert = numSeeds;
            itemToCheck = itemCheck;
            itemReward = reward;
        }

        @Nonnull
        @Override
        public List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
            //
            // Additional conditions can be checked, though as much as possible should be parameterized via JSON data.
            // It is better to write a new ILootCondition implementation than to do things here.
            //
            int numSeeds = 0;
            for(ItemStack stack : generatedLoot) {
                if(stack.getItem() == itemToCheck)
                    numSeeds+=stack.getCount();
            }
            if(numSeeds >= numSeedsToConvert) {
                generatedLoot.removeIf(x -> x.getItem() == itemToCheck);
                generatedLoot.add(new ItemStack(itemReward, (numSeeds/numSeedsToConvert)));
                numSeeds = numSeeds%numSeedsToConvert;
                if(numSeeds > 0)
                    generatedLoot.add(new ItemStack(itemToCheck, numSeeds));
            }
            return generatedLoot;
        }

        private static class Serializer extends GlobalLootModifierSerializer<WheatSeedsConverterModifier> {

            @Override
            public WheatSeedsConverterModifier read(Identifier name, JsonObject object, LootCondition[] conditionsIn) {
                int numSeeds = JsonHelper.getInt(object, "numSeeds");
                Item seed = ForgeRegistries.ITEMS.getValue(new Identifier((JsonHelper.getString(object, "seedItem"))));
                Item wheat = ForgeRegistries.ITEMS.getValue(new Identifier(JsonHelper.getString(object, "replacement")));
                return new WheatSeedsConverterModifier(conditionsIn, numSeeds, seed, wheat);
            }

            @Override
            public JsonObject write(WheatSeedsConverterModifier instance) {
                JsonObject json = makeConditions(instance.conditions);
                json.addProperty("numSeeds", instance.numSeedsToConvert);
                json.addProperty("seedItem", ForgeRegistries.ITEMS.getKey(instance.itemToCheck).toString());
                json.addProperty("replacement", ForgeRegistries.ITEMS.getKey(instance.itemReward).toString());
                return json;
            }
        }
    }
}
