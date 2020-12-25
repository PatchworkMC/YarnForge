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

package net.minecraftforge.common.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Class to help manage entity spawns inside of structures
 */
public class StructureSpawnManager
{
    private static Map<StructureFeature<?>, StructureSpawnInfo> structuresWithSpawns = Collections.emptyMap();

    /**
     * Gathers potential entity spawns for all the different registered structures.
     * @apiNote Internal
     */
    public static void gatherEntitySpawns()
    {
        //We use a linked hash map to ensure that we check the structures in an order that if there are multiple structures a position satisfies
        // then we have the same behavior as vanilla as vanilla checks, swamp huts, pillager outposts, ocean monuments, and nether fortresses in
        // that order.
        Map<StructureFeature<?>, StructureSpawnInfo> structuresWithSpawns = new LinkedHashMap<>();
        gatherEntitySpawns(structuresWithSpawns, StructureFeature.SWAMP_HUT);
        gatherEntitySpawns(structuresWithSpawns, StructureFeature.PILLAGER_OUTPOST);
        gatherEntitySpawns(structuresWithSpawns, StructureFeature.MONUMENT);
        gatherEntitySpawns(structuresWithSpawns, StructureFeature.FORTRESS);
        for (StructureFeature<?> structure : ForgeRegistries.STRUCTURE_FEATURES.getValues())
        {
            if (structure != StructureFeature.SWAMP_HUT && structure != StructureFeature.PILLAGER_OUTPOST && structure != StructureFeature.MONUMENT &&
                structure != StructureFeature.FORTRESS)
            {
                //If we didn't already gather the spawns already to ensure we do vanilla ones already
                // gather the spawns for this structure
                gatherEntitySpawns(structuresWithSpawns, structure);
            }
        }
        StructureSpawnManager.structuresWithSpawns = structuresWithSpawns;
    }

    private static void gatherEntitySpawns(Map<StructureFeature<?>, StructureSpawnInfo> structuresWithSpawns, StructureFeature<?> structure)
    {
        StructureSpawnListGatherEvent event = new StructureSpawnListGatherEvent(structure);
        MinecraftForge.EVENT_BUS.post(event);
        ImmutableMap.Builder<net.minecraft.entity.SpawnGroup, List<SpawnSettings.SpawnEntry>> builder = ImmutableMap.builder();
        event.getEntitySpawns().forEach((classification, spawns) -> {
            if (!spawns.isEmpty())
                builder.put(classification, ImmutableList.copyOf(spawns));
        });
        Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> entitySpawns = builder.build();
        if (!entitySpawns.isEmpty())
            structuresWithSpawns.put(structure, new StructureSpawnInfo(entitySpawns, event.isInsideOnly()));
    }

    /**
     * Looks up if a given position is within a structure and returns any entity spawns that structure has for the given classification, or null if
     * none are found.
     * @param structureManager Structure Manager, used to check if a position is within a structure.
     * @param classification   Entity classification
     * @param pos              Position to get entity spawns of
     */
    @Nullable
    public static List<SpawnSettings.SpawnEntry> getStructureSpawns(StructureAccessor structureManager, SpawnGroup classification, BlockPos pos)
    {
        for (Entry<StructureFeature<?>, StructureSpawnInfo> entry : structuresWithSpawns.entrySet())
        {
            StructureFeature<?> structure = entry.getKey();
            StructureSpawnInfo spawnInfo = entry.getValue();
            //Note: We check if the structure has spawns for a type first before looking at the world as it should be a cheaper check
            if (spawnInfo.spawns.containsKey(classification) && structureManager.getStructureAt(pos, spawnInfo.insideOnly, structure).hasChildren())
                return spawnInfo.spawns.get(classification);
        }
        return null;
    }

    /**
     * Gets the entity spawn lists for entities of a given classification for a given structure.
     * @param structure      The Structure
     * @param classification The classification to lookup
     */
    public static List<SpawnSettings.SpawnEntry> getSpawnList(StructureFeature<?> structure, SpawnGroup classification)
    {
        if (structuresWithSpawns.containsKey(structure))
            return structuresWithSpawns.get(structure).spawns.getOrDefault(classification, Collections.emptyList());
        return Collections.emptyList();
    }

    /**
     * Helper class to keep track of spawns and if the spawns should be restricted to inside the structure pieces.
     */
    private static class StructureSpawnInfo
    {
        private final Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> spawns;
        private final boolean insideOnly;

        private StructureSpawnInfo(Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> spawns, boolean insideOnly)
        {
            this.spawns = spawns;
            this.insideOnly = insideOnly;
        }
    }
}
