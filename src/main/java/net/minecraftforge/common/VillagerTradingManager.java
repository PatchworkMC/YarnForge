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

package net.minecraftforge.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradeOffers.Factory;
import net.minecraft.village.VillagerProfession;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class VillagerTradingManager
{

    private static final Map<VillagerProfession, Int2ObjectMap<Factory[]>> VANILLA_TRADES = new HashMap<>();
    private static final Int2ObjectMap<Factory[]> WANDERER_TRADES = new Int2ObjectOpenHashMap<>();

    static
    {
        TradeOffers.PROFESSION_TO_LEVELED_TRADE.entrySet().forEach(e ->
        {
            Int2ObjectMap<Factory[]> copy = new Int2ObjectOpenHashMap<>();
            e.getValue().int2ObjectEntrySet().forEach(ent -> copy.put(ent.getIntKey(), Arrays.copyOf(ent.getValue(), ent.getValue().length)));
            VANILLA_TRADES.put(e.getKey(), copy);
        });
        TradeOffers.WANDERING_TRADER_TRADES.int2ObjectEntrySet().forEach(e -> WANDERER_TRADES.put(e.getIntKey(), Arrays.copyOf(e.getValue(), e.getValue().length)));
    }

    static void loadTrades(FMLServerAboutToStartEvent e)
    {
        postWandererEvent();
        postVillagerEvents();
    }

    /**
     * Posts the WandererTradesEvent.
     */
    private static void postWandererEvent()
    {
        List<Factory> generic = DefaultedList.of();
        List<Factory> rare = DefaultedList.of();
        Arrays.stream(WANDERER_TRADES.get(1)).forEach(generic::add);
        Arrays.stream(WANDERER_TRADES.get(2)).forEach(rare::add);
        MinecraftForge.EVENT_BUS.post(new WandererTradesEvent(generic, rare));
        TradeOffers.WANDERING_TRADER_TRADES.put(1, generic.toArray(new Factory[0]));
        TradeOffers.WANDERING_TRADER_TRADES.put(2, rare.toArray(new Factory[0]));
    }

    /**
     * Posts a VillagerTradesEvent for each registered profession.
     */
    private static void postVillagerEvents()
    {
        for (VillagerProfession prof : ForgeRegistries.PROFESSIONS)
        {
            Int2ObjectMap<Factory[]> trades = VANILLA_TRADES.getOrDefault(prof, new Int2ObjectOpenHashMap<>());
            Int2ObjectMap<List<Factory>> mutableTrades = new Int2ObjectOpenHashMap<>();
            for (int i = 1; i < 6; i++)
            {
                mutableTrades.put(i, DefaultedList.of());
            }
            trades.int2ObjectEntrySet().forEach(e ->
            {
                Arrays.stream(e.getValue()).forEach(mutableTrades.get(e.getIntKey())::add);
            });
            MinecraftForge.EVENT_BUS.post(new VillagerTradesEvent(mutableTrades, prof));
            Int2ObjectMap<Factory[]> newTrades = new Int2ObjectOpenHashMap<>();
            mutableTrades.int2ObjectEntrySet().forEach(e -> newTrades.put(e.getIntKey(), e.getValue().toArray(new Factory[0])));
            TradeOffers.PROFESSION_TO_LEVELED_TRADE.put(prof, newTrades);
        }
    }

}
