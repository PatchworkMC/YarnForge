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

package net.minecraftforge.client;

import com.google.common.collect.Maps;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraftforge.common.world.ForgeWorldType;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class ForgeWorldTypeScreens
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<ForgeWorldType, net.minecraft.client.world.GeneratorType> GENERATORS = Maps.newHashMap();
    private static final Map<ForgeWorldType, net.minecraft.client.world.GeneratorType.ScreenProvider> GENERATOR_SCREEN_FACTORIES = Maps.newHashMap();

    public static synchronized void registerFactory(ForgeWorldType type, net.minecraft.client.world.GeneratorType.ScreenProvider factory)
    {
        if (GENERATOR_SCREEN_FACTORIES.containsKey(type))
            throw new IllegalStateException("Factory has already been registered for: " + type);

        GENERATOR_SCREEN_FACTORIES.put(type, factory);
    }

    static net.minecraft.client.world.GeneratorType getDefaultGenerator()
    {
        ForgeWorldType def = ForgeWorldType.getDefaultWorldType();
        if (def == null)
        {
            return net.minecraft.client.world.GeneratorType.DEFAULT;
        }

        net.minecraft.client.world.GeneratorType gen = GENERATORS.get(def);
        if (gen == null)
        {
            LOGGER.error("The default world type '{}' has not been added to the GUI. Was it registered too late?", def.getRegistryName());
            return net.minecraft.client.world.GeneratorType.DEFAULT;
        }

        return gen;
    }

    static net.minecraft.client.world.GeneratorType.ScreenProvider getGeneratorScreenFactory(Optional<net.minecraft.client.world.GeneratorType> generator, @Nullable net.minecraft.client.world.GeneratorType.ScreenProvider biomegeneratortypescreens$ifactory)
    {
        return generator.filter(gen -> gen instanceof GeneratorType)
                .map(type -> GENERATOR_SCREEN_FACTORIES.get(((GeneratorType)type).getWorldType()))
                .orElse(biomegeneratortypescreens$ifactory);
    }

    static void registerTypes()
    {
        ForgeRegistries.WORLD_TYPES.forEach(wt -> {
            GeneratorType gen = new GeneratorType(wt);
            GENERATORS.put(wt, gen);
            net.minecraft.client.world.GeneratorType.registerGenerator(gen);
        });
    }

    private static class GeneratorType extends net.minecraft.client.world.GeneratorType
    {
        private final ForgeWorldType worldType;

        public GeneratorType(ForgeWorldType wt)
        {
            super(wt.getDisplayName());
            worldType = wt;
        }

        public ForgeWorldType getWorldType()
        {
            return worldType;
        }

        @Nonnull
        @Override
        public GeneratorOptions createDefaultOptions(@Nonnull DynamicRegistryManager.Impl dynamicRegistries, long seed, boolean generateStructures, boolean bonusChest)
        {
            return worldType.createSettings(dynamicRegistries, seed, generateStructures, bonusChest, "");
        }

        @Nonnull
        @Override
        protected ChunkGenerator getChunkGenerator(@Nonnull Registry<Biome> p_241869_1_, @Nonnull Registry<ChunkGeneratorSettings> p_241869_2_, long p_241869_3_)
        {
            return worldType.createChunkGenerator(p_241869_1_, p_241869_2_, p_241869_3_, "");
        }
    }
}
