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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.*;
import static net.minecraftforge.common.BiomeDictionary.Type.*;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.common.collect.ImmutableList;

public class BiomeDictionary
{
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = LogManager.getLogger();

    public static final class Type
    {
        private static final Map<String, Type> byName = new TreeMap<>();
        private static Collection<Type> allTypes = Collections.unmodifiableCollection(byName.values());

        /*Temperature-based tags. Specifying neither implies a biome is temperate*/
        public static final Type HOT = new Type("HOT");
        public static final Type COLD = new Type("COLD");

        //Tags specifying the amount of vegetation a biome has. Specifying neither implies a biome to have moderate amounts*/
        public static final Type SPARSE = new Type("SPARSE");
        public static final Type DENSE = new Type("DENSE");

        //Tags specifying how moist a biome is. Specifying neither implies the biome as having moderate humidity*/
        public static final Type WET = new Type("WET");
        public static final Type DRY = new Type("DRY");

        /*Tree-based tags, SAVANNA refers to dry, desert-like trees (Such as Acacia), CONIFEROUS refers to snowy trees (Such as Spruce) and JUNGLE refers to jungle trees.
         * Specifying no tag implies a biome has temperate trees (Such as Oak)*/
        public static final Type SAVANNA = new Type("SAVANNA");
        public static final Type CONIFEROUS = new Type("CONIFEROUS");
        public static final Type JUNGLE = new Type("JUNGLE");

        /*Tags specifying the nature of a biome*/
        public static final Type SPOOKY = new Type("SPOOKY");
        public static final Type DEAD = new Type("DEAD");
        public static final Type LUSH = new Type("LUSH");
        public static final Type MUSHROOM = new Type("MUSHROOM");
        public static final Type MAGICAL = new Type("MAGICAL");
        public static final Type RARE = new Type("RARE");
        public static final Type PLATEAU = new Type("PLATEAU");
        public static final Type MODIFIED = new Type("MODIFIED");

        public static final Type OCEAN = new Type("OCEAN");
        public static final Type RIVER = new Type("RIVER");
        /**
         * A general tag for all water-based biomes. Shown as present if OCEAN or RIVER are.
         **/
        public static final Type WATER = new Type("WATER", OCEAN, RIVER);

        /*Generic types which a biome can be*/
        public static final Type MESA = new Type("MESA");
        public static final Type FOREST = new Type("FOREST");
        public static final Type PLAINS = new Type("PLAINS");
        public static final Type MOUNTAIN = new Type("MOUNTAIN");
        public static final Type HILLS = new Type("HILLS");
        public static final Type SWAMP = new Type("SWAMP");
        public static final Type SANDY = new Type("SANDY");
        public static final Type SNOWY = new Type("SNOWY");
        public static final Type WASTELAND = new Type("WASTELAND");
        public static final Type BEACH = new Type("BEACH");
        public static final Type VOID = new Type("VOID");

        /*Tags specifying the dimension a biome generates in. Specifying none implies a biome that generates in a modded dimension*/
        public static final Type OVERWORLD = new Type("OVERWORLD");
        public static final Type NETHER = new Type("NETHER");
        public static final Type END = new Type("END");

        private final String name;
        private final List<Type> subTypes;
        private final Set<RegistryKey<Biome>> biomes = new HashSet<>();
        private final Set<RegistryKey<Biome>> biomesUn = Collections.unmodifiableSet(biomes);

        private Type(String name, Type... subTypes)
        {
            this.name = name;
            this.subTypes = ImmutableList.copyOf(subTypes);

            byName.put(name, this);
        }

        /**
         * Gets the name for this type.
         */
        public String getName()
        {
            return name;
        }

        public String toString()
        {
            return name;
        }

        /**
         * Retrieves a Type instance by name,
         * if one does not exist already it creates one.
         * This can be used as intermediate measure for modders to
         * add their own Biome types.
         * <p>
         * There are <i>no</i> naming conventions besides:
         * <ul><li><b>Must</b> be all upper case (enforced by name.toUpper())</li>
         * <li><b>No</b> Special characters. {Unenforced, just don't be a pain, if it becomes a issue I WILL
         * make this RTE with no worry about backwards compatibility}</li></ul>
         * <p>
         * Note: For performance sake, the return value of this function SHOULD be cached.
         * Two calls with the same name SHOULD return the same value.
         *
         * @param name The name of this Type
         * @return An instance of Type for this name.
         */
        public static Type getType(String name, Type... subTypes)
        {
            name = name.toUpperCase();
            Type t = byName.get(name);
            if (t == null)
            {
                t = new Type(name, subTypes);
            }
            return t;
        }

        /**
         * @return An unmodifiable collection of all current biome types.
         */
        public static Collection<Type> getAll()
        {
            return allTypes;
        }

        @Nullable
        public static Type fromVanilla(Biome.Category category)
        {
            if (category == Biome.Category.NONE)
                return null;
            if (category == Biome.Category.THEEND)
                return VOID;
            return getType(category.name());
        }
    }

    private static final Map<RegistryKey<Biome>, BiomeInfo> biomeInfoMap = new HashMap<>();

    private static class BiomeInfo
    {
        private final Set<Type> types = new HashSet<Type>();
        private final Set<Type> typesUn = Collections.unmodifiableSet(this.types);
    }

    public static void init() {}
    static
    {
        registerVanillaBiomes();
    }

    /**
     * Adds the given types to the biome.
     *
     */
    public static void addTypes(RegistryKey<Biome> biome, Type... types)
    {
        Collection<Type> supertypes = listSupertypes(types);
        Collections.addAll(supertypes, types);

        for (Type type : supertypes)
        {
            type.biomes.add(biome);
        }

        BiomeInfo biomeInfo = getBiomeInfo(biome);
        Collections.addAll(biomeInfo.types, types);
        biomeInfo.types.addAll(supertypes);
    }

    /**
     * Gets the set of biomes that have the given type.
     *
     */
    @Nonnull
    public static Set<RegistryKey<Biome>> getBiomes(Type type)
    {
        return type.biomesUn;
    }

    /**
     * Gets the set of types that have been added to the given biome.
     *
     */
    @Nonnull
    public static Set<Type> getTypes(RegistryKey<Biome> biome)
    {
        return getBiomeInfo(biome).typesUn;
    }

    /**
     * Checks if the two given biomes have types in common.
     *
     * @return returns true if a common type is found, false otherwise
     */
    public static boolean areSimilar(RegistryKey<Biome> biomeA, RegistryKey<Biome> biomeB)
    {
        Set<Type> typesA = getTypes(biomeA);
        Set<Type> typesB = getTypes(biomeB);
        return typesA.stream().anyMatch(typesB::contains);
    }

    /**
     * Checks if the given type has been added to the given biome.
     *
     */
    public static boolean hasType(RegistryKey<Biome> biome, Type type)
    {
        return getTypes(biome).contains(type);
    }

    /**
     * Checks if any type has been added to the given biome.
     *
     */
    public static boolean hasAnyType(RegistryKey<Biome> biome)
    {
        return !getBiomeInfo(biome).types.isEmpty();
    }

    //Internal implementation
    private static BiomeInfo getBiomeInfo(RegistryKey<Biome> biome)
    {
        return biomeInfoMap.computeIfAbsent(biome, k -> new BiomeInfo());
    }

    private static Collection<Type> listSupertypes(Type... types)
    {
        Set<Type> supertypes = new HashSet<Type>();
        Deque<Type> next = new ArrayDeque<Type>();
        Collections.addAll(next, types);

        while (!next.isEmpty())
        {
            Type type = next.remove();

            for (Type sType : Type.byName.values())
            {
                if (sType.subTypes.contains(type) && supertypes.add(sType))
                    next.add(sType);
            }
        }

        return supertypes;
    }

    private static void registerVanillaBiomes()
    {
        addTypes(BiomeKeys.OCEAN, OCEAN, OVERWORLD);
        addTypes(BiomeKeys.PLAINS, PLAINS, OVERWORLD);
        addTypes(BiomeKeys.DESERT, HOT, DRY, SANDY, OVERWORLD);
        addTypes(BiomeKeys.MOUNTAINS, MOUNTAIN, HILLS, OVERWORLD);
        addTypes(BiomeKeys.FOREST, FOREST, OVERWORLD);
        addTypes(BiomeKeys.TAIGA, COLD, CONIFEROUS, FOREST, OVERWORLD);
        addTypes(BiomeKeys.SWAMP, WET, SWAMP, OVERWORLD);
        addTypes(BiomeKeys.RIVER, RIVER, OVERWORLD);
        addTypes(BiomeKeys.NETHER_WASTES, HOT, DRY, NETHER);
        addTypes(BiomeKeys.THE_END, COLD, DRY, END);
        addTypes(BiomeKeys.FROZEN_OCEAN, COLD, OCEAN, SNOWY, OVERWORLD);
        addTypes(BiomeKeys.FROZEN_RIVER, COLD, RIVER, SNOWY, OVERWORLD);
        addTypes(BiomeKeys.SNOWY_TUNDRA, COLD, SNOWY, WASTELAND, OVERWORLD);
        addTypes(BiomeKeys.SNOWY_MOUNTAINS, COLD, SNOWY, MOUNTAIN, OVERWORLD);
        addTypes(BiomeKeys.MUSHROOM_FIELDS, MUSHROOM, RARE, OVERWORLD);
        addTypes(BiomeKeys.MUSHROOM_FIELD_SHORE, MUSHROOM, BEACH, RARE, OVERWORLD);
        addTypes(BiomeKeys.BEACH, BEACH, OVERWORLD);
        addTypes(BiomeKeys.DESERT_HILLS, HOT, DRY, SANDY, HILLS, OVERWORLD);
        addTypes(BiomeKeys.WOODED_HILLS, FOREST, HILLS, OVERWORLD);
        addTypes(BiomeKeys.TAIGA_HILLS, COLD, CONIFEROUS, FOREST, HILLS, OVERWORLD);
        addTypes(BiomeKeys.MOUNTAIN_EDGE, MOUNTAIN, OVERWORLD);
        addTypes(BiomeKeys.JUNGLE, HOT, WET, DENSE, JUNGLE, OVERWORLD);
        addTypes(BiomeKeys.JUNGLE_HILLS, HOT, WET, DENSE, JUNGLE, HILLS, OVERWORLD);
        addTypes(BiomeKeys.JUNGLE_EDGE, HOT, WET, JUNGLE, FOREST, RARE, OVERWORLD);
        addTypes(BiomeKeys.DEEP_OCEAN, OCEAN, OVERWORLD);
        addTypes(BiomeKeys.STONE_SHORE, BEACH, OVERWORLD);
        addTypes(BiomeKeys.SNOWY_BEACH, COLD, BEACH, SNOWY, OVERWORLD);
        addTypes(BiomeKeys.BIRCH_FOREST, FOREST, OVERWORLD);
        addTypes(BiomeKeys.BIRCH_FOREST_HILLS, FOREST, HILLS, OVERWORLD);
        addTypes(BiomeKeys.DARK_FOREST, SPOOKY, DENSE, FOREST, OVERWORLD);
        addTypes(BiomeKeys.SNOWY_TAIGA, COLD, CONIFEROUS, FOREST, SNOWY, OVERWORLD);
        addTypes(BiomeKeys.SNOWY_TAIGA_HILLS, COLD, CONIFEROUS, FOREST, SNOWY, HILLS, OVERWORLD);
        addTypes(BiomeKeys.GIANT_TREE_TAIGA, COLD, CONIFEROUS, FOREST, OVERWORLD);
        addTypes(BiomeKeys.GIANT_TREE_TAIGA_HILLS, COLD, CONIFEROUS, FOREST, HILLS, OVERWORLD);
        addTypes(BiomeKeys.WOODED_MOUNTAINS, MOUNTAIN, FOREST, SPARSE, OVERWORLD);
        addTypes(BiomeKeys.SAVANNA, HOT, SAVANNA, PLAINS, SPARSE, OVERWORLD);
        addTypes(BiomeKeys.SAVANNA_PLATEAU, HOT, SAVANNA, PLAINS, SPARSE, RARE, OVERWORLD, PLATEAU);
        addTypes(BiomeKeys.BADLANDS, MESA, SANDY, DRY, OVERWORLD);
        addTypes(BiomeKeys.WOODED_BADLANDS_PLATEAU, MESA, SANDY, DRY, SPARSE, OVERWORLD, PLATEAU);
        addTypes(BiomeKeys.BADLANDS_PLATEAU, MESA, SANDY, DRY, OVERWORLD, PLATEAU);
        addTypes(BiomeKeys.SMALL_END_ISLANDS, END);
        addTypes(BiomeKeys.END_MIDLANDS, END);
        addTypes(BiomeKeys.END_HIGHLANDS, END);
        addTypes(BiomeKeys.END_BARRENS, END);
        addTypes(BiomeKeys.WARM_OCEAN, OCEAN, HOT, OVERWORLD);
        addTypes(BiomeKeys.LUKEWARM_OCEAN, OCEAN, OVERWORLD);
        addTypes(BiomeKeys.COLD_OCEAN, OCEAN, COLD, OVERWORLD);
        addTypes(BiomeKeys.DEEP_WARM_OCEAN, OCEAN, HOT, OVERWORLD);
        addTypes(BiomeKeys.DEEP_LUKEWARM_OCEAN, OCEAN, OVERWORLD);
        addTypes(BiomeKeys.DEEP_COLD_OCEAN, OCEAN, COLD, OVERWORLD);
        addTypes(BiomeKeys.DEEP_FROZEN_OCEAN, OCEAN, COLD, OVERWORLD);
        addTypes(BiomeKeys.THE_VOID, VOID);
        addTypes(BiomeKeys.SUNFLOWER_PLAINS, PLAINS, RARE, OVERWORLD);
        addTypes(BiomeKeys.DESERT_LAKES, HOT, DRY, SANDY, RARE, OVERWORLD);
        addTypes(BiomeKeys.GRAVELLY_MOUNTAINS, MOUNTAIN, SPARSE, RARE, OVERWORLD);
        addTypes(BiomeKeys.FLOWER_FOREST, FOREST, HILLS, RARE, OVERWORLD);
        addTypes(BiomeKeys.TAIGA_MOUNTAINS, COLD, CONIFEROUS, FOREST, MOUNTAIN, RARE, OVERWORLD);
        addTypes(BiomeKeys.SWAMP_HILLS, WET, SWAMP, HILLS, RARE, OVERWORLD);
        addTypes(BiomeKeys.ICE_SPIKES, COLD, SNOWY, HILLS, RARE, OVERWORLD);
        addTypes(BiomeKeys.MODIFIED_JUNGLE, HOT, WET, DENSE, JUNGLE, MOUNTAIN, RARE, OVERWORLD, MODIFIED);
        addTypes(BiomeKeys.MODIFIED_JUNGLE_EDGE, HOT, SPARSE, JUNGLE, HILLS, RARE, OVERWORLD, MODIFIED);
        addTypes(BiomeKeys.TALL_BIRCH_FOREST, FOREST, DENSE, HILLS, RARE, OVERWORLD);
        addTypes(BiomeKeys.TALL_BIRCH_HILLS, FOREST, DENSE, MOUNTAIN, RARE, OVERWORLD);
        addTypes(BiomeKeys.DARK_FOREST_HILLS, SPOOKY, DENSE, FOREST, MOUNTAIN, RARE, OVERWORLD);
        addTypes(BiomeKeys.SNOWY_TAIGA_MOUNTAINS, COLD, CONIFEROUS, FOREST, SNOWY, MOUNTAIN, RARE, OVERWORLD);
        addTypes(BiomeKeys.GIANT_SPRUCE_TAIGA, DENSE, FOREST, RARE, OVERWORLD);
        addTypes(BiomeKeys.GIANT_SPRUCE_TAIGA_HILLS, DENSE, FOREST, HILLS, RARE, OVERWORLD);
        addTypes(BiomeKeys.MODIFIED_GRAVELLY_MOUNTAINS, MOUNTAIN, SPARSE, RARE, OVERWORLD, MODIFIED);
        addTypes(BiomeKeys.SHATTERED_SAVANNA, HOT, DRY, SPARSE, SAVANNA, MOUNTAIN, RARE, OVERWORLD);
        addTypes(BiomeKeys.SHATTERED_SAVANNA_PLATEAU, HOT, DRY, SPARSE, SAVANNA, HILLS, RARE, OVERWORLD, PLATEAU);
        addTypes(BiomeKeys.ERODED_BADLANDS, HOT, DRY, SPARSE, MOUNTAIN, RARE, OVERWORLD);
        addTypes(BiomeKeys.MODIFIED_WOODED_BADLANDS_PLATEAU, HOT, DRY, SPARSE, HILLS, RARE, OVERWORLD, PLATEAU, MODIFIED);
        addTypes(BiomeKeys.MODIFIED_BADLANDS_PLATEAU, HOT, DRY, SPARSE, MOUNTAIN, RARE, OVERWORLD, PLATEAU, MODIFIED);
        addTypes(BiomeKeys.BAMBOO_JUNGLE, HOT, WET, RARE, JUNGLE, OVERWORLD);
        addTypes(BiomeKeys.BAMBOO_JUNGLE_HILLS, HOT, WET, RARE, JUNGLE, HILLS, OVERWORLD);
        addTypes(BiomeKeys.SOUL_SAND_VALLEY, HOT, DRY, NETHER);
        addTypes(BiomeKeys.CRIMSON_FOREST, HOT, DRY, NETHER, FOREST);
        addTypes(BiomeKeys.WARPED_FOREST, HOT, DRY, NETHER, FOREST);
        addTypes(BiomeKeys.BASALT_DELTAS, HOT, DRY, NETHER);

        if (DEBUG)
        {
            StringBuilder buf = new StringBuilder();
            buf.append("BiomeDictionary:\n");
            Type.byName.forEach((name, type) ->
                buf.append("    ").append(type.name).append(": ")
                .append(type.biomes.stream()
                    .map(RegistryKey::getValue)
                    .sorted((a,b) -> a.compareNamespaced(b))
                    .map(Object::toString)
                    .collect(Collectors.joining(", "))
                )
                .append('\n')
            );

            boolean missing = false;
            List<RegistryKey<Biome>> all = StreamSupport.stream(ForgeRegistries.BIOMES.spliterator(), false)
                .map(b -> RegistryKey.of(Registry.BIOME_KEY, b.getRegistryName()))
                .sorted().collect(Collectors.toList());

            for (RegistryKey<Biome> key : all) {
                if (!biomeInfoMap.containsKey(key)) {
                    if (!missing) {
                        buf.append("Missing:\n");
                        missing = true;
                    }
                    buf.append("    ").append(key.getValue()).append('\n');
                }
            }
            LOGGER.debug(buf.toString());
        }
    }
}
