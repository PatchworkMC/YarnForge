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

package net.minecraftforge.common.data;

import com.mojang.datafixers.util.Pair;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.server.LootTablesProvider;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.*;
import net.minecraft.loot.condition.AlternativeLootCondition;
import net.minecraft.loot.condition.InvertedLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.loot.entry.CombinedEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Currently used only for replacing shears item to shears tag
 */
public class ForgeLootTableProvider extends LootTablesProvider {

    public ForgeLootTableProvider(DataGenerator gen) {
        super(gen);
    }

    @Override
    protected void validate(Map<Identifier, LootTable> map, LootTableReporter validationtracker) {
        // do not validate against all registered loot tables
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<Identifier, LootTable.Builder>>>, LootContextType>> getTables() {
        return super.getTables().stream().map(pair -> {
            // provides new consumer with filtering only changed loot tables and replacing condition item to condition tag
            return new Pair<Supplier<Consumer<BiConsumer<Identifier, LootTable.Builder>>>, LootContextType>(() -> replaceAndFilterChangesOnly(pair.getFirst().get()), pair.getSecond());
        }).collect(Collectors.toList());
    }

    private Consumer<BiConsumer<Identifier, LootTable.Builder>> replaceAndFilterChangesOnly(Consumer<BiConsumer<Identifier, LootTable.Builder>> consumer) {
        return (newConsumer) -> consumer.accept((resourceLocation, builder) -> {
            if (findAndReplaceInLootTableBuilder(builder, Items.SHEARS, Tags.Items.SHEARS)) {
                newConsumer.accept(resourceLocation, builder);
            }
        });
    }

    private boolean findAndReplaceInLootTableBuilder(LootTable.Builder builder, Item from, Tag.Identified<Item> to) {
        List<LootPool> lootPools = ObfuscationReflectionHelper.getPrivateValue(LootTable.Builder.class, builder, "field_216041_a");
        boolean found = false;

        if (lootPools == null) {
            throw new IllegalStateException(LootTable.Builder.class.getName() + " is missing field field_216041_a");
        }

        for (LootPool lootPool : lootPools) {
            if (findAndReplaceInLootPool(lootPool, from, to)) {
                found = true;
            }
        }

        return found;
    }

    private boolean findAndReplaceInLootPool(LootPool lootPool, Item from, Tag.Identified<Item> to) {
        List<LootPoolEntry> lootEntries = ObfuscationReflectionHelper.getPrivateValue(LootPool.class, lootPool, "field_186453_a");
        List<LootCondition> lootConditions = ObfuscationReflectionHelper.getPrivateValue(LootPool.class, lootPool, "field_186454_b");
        boolean found = false;

        if (lootEntries == null) {
            throw new IllegalStateException(LootPool.class.getName() + " is missing field field_186453_a");
        }

        for (LootPoolEntry lootEntry : lootEntries) {
            if (lootEntry instanceof CombinedEntry) {
                if (findAndReplaceInParentedLootEntry((CombinedEntry) lootEntry, from, to)) {
                    found = true;
                }
            }
        }

        if (lootConditions == null) {
            throw new IllegalStateException(LootPool.class.getName() + " is missing field field_186454_b");
        }

        for (int i = 0; i < lootConditions.size(); i++) {
            LootCondition lootCondition = lootConditions.get(i);
            if (lootCondition instanceof MatchToolLootCondition && checkMatchTool((MatchToolLootCondition) lootCondition, from)) {
                lootConditions.set(i, MatchToolLootCondition.builder(ItemPredicate.Builder.create().tag(to)).build());
                found = true;
            } else if (lootCondition instanceof InvertedLootCondition) {
                LootCondition invLootCondition = ObfuscationReflectionHelper.getPrivateValue(InvertedLootCondition.class, (InvertedLootCondition) lootCondition, "field_215981_a");

                if (invLootCondition instanceof MatchToolLootCondition && checkMatchTool((MatchToolLootCondition) invLootCondition, from)) {
                    lootConditions.set(i, InvertedLootCondition.builder(MatchToolLootCondition.builder(ItemPredicate.Builder.create().tag(to))).build());
                    found = true;
                } else if (invLootCondition instanceof AlternativeLootCondition && findAndReplaceInAlternative((AlternativeLootCondition) invLootCondition, from, to)) {
                    found = true;
                }
            }
        }

        return found;
    }

    private boolean findAndReplaceInParentedLootEntry(CombinedEntry entry, Item from, Tag.Identified<Item> to) {
        LootPoolEntry[] lootEntries = ObfuscationReflectionHelper.getPrivateValue(CombinedEntry.class, entry, "field_216147_c");
        boolean found = false;

        if (lootEntries == null) {
            throw new IllegalStateException(CombinedEntry.class.getName() + " is missing field field_216147_c");
        }

        for (LootPoolEntry lootEntry : lootEntries) {
            if (findAndReplaceInLootEntry(lootEntry, from, to)) {
                found = true;
            }
        }

        return found;
    }

    private boolean findAndReplaceInLootEntry(LootPoolEntry entry, Item from, Tag.Identified<Item> to) {
        LootCondition[] lootConditions = ObfuscationReflectionHelper.getPrivateValue(LootPoolEntry.class, entry, "field_216144_d");
        boolean found = false;

        if (lootConditions == null) {
            throw new IllegalStateException(LootPoolEntry.class.getName() + " is missing field field_216144_d");
        }

        for (int i = 0; i < lootConditions.length; i++) {
            if (lootConditions[i] instanceof AlternativeLootCondition && findAndReplaceInAlternative((AlternativeLootCondition) lootConditions[i], from, to)) {
                found = true;
            } else if (lootConditions[i] instanceof MatchToolLootCondition && checkMatchTool((MatchToolLootCondition) lootConditions[i], from)) {
                lootConditions[i] = MatchToolLootCondition.builder(ItemPredicate.Builder.create().tag(to)).build();
                found = true;
            }
        }

        return found;
    }

    private boolean findAndReplaceInAlternative(AlternativeLootCondition alternative, Item from, Tag.Identified<Item> to) {
        LootCondition[] lootConditions = ObfuscationReflectionHelper.getPrivateValue(AlternativeLootCondition.class, alternative, "field_215962_a");
        boolean found = false;

        if (lootConditions == null) {
            throw new IllegalStateException(AlternativeLootCondition.class.getName() + " is missing field field_215962_a");
        }

        for (int i = 0; i < lootConditions.length; i++) {
            if (lootConditions[i] instanceof MatchToolLootCondition && checkMatchTool((MatchToolLootCondition) lootConditions[i], from)) {
                lootConditions[i] = MatchToolLootCondition.builder(ItemPredicate.Builder.create().tag(to)).build();
                found = true;
            }
        }

        return found;
    }

    private boolean checkMatchTool(MatchToolLootCondition lootCondition, Item expected) {
        ItemPredicate predicate = ObfuscationReflectionHelper.getPrivateValue(MatchToolLootCondition.class, lootCondition, "field_216014_a");
        Item item = ObfuscationReflectionHelper.getPrivateValue(ItemPredicate.class, predicate, "field_192496_b");
        return item != null && item.equals(expected);
    }
}
