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

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ChatUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForgeWorldType extends ForgeRegistryEntry<ForgeWorldType>
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static ForgeWorldType getDefaultWorldType()
    {
        String defaultWorldType = ForgeConfig.COMMON.defaultWorldType.get();

        if (ChatUtil.isEmpty(defaultWorldType) || "default".equals(defaultWorldType))
            return null; // use vanilla

        ForgeWorldType def = ForgeRegistries.WORLD_TYPES.getValue(new Identifier(defaultWorldType));
        if (def == null)
        {
            LOGGER.error("The defaultWorldType '{}' specified in the forge config has not been registered. The vanilla default generator will be used.", defaultWorldType);
        }

        return def;
    }

    private final IChunkGeneratorFactory factory;

    public ForgeWorldType(IChunkGeneratorFactory factory)
    {
        this.factory = factory;
    }

    public ForgeWorldType(IBasicChunkGeneratorFactory factory)
    {
        this.factory = factory;
    }

    public String getTranslationKey()
    {
        return Util.createTranslationKey("generator", getRegistryName());
    }

    public Text getDisplayName()
    {
        return new TranslatableText(getTranslationKey());
    }

    /**
     * Called from both the dedicated server and the world creation screen in the client.
     * to construct the DimensionGEneratorSettings:
     * @return The constructed chunk generator.
     */
    public ChunkGenerator createChunkGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> dimensionSettingsRegistry, long seed, String generatorSettings)
    {
        return this.factory.createChunkGenerator(biomeRegistry, dimensionSettingsRegistry, seed, generatorSettings);
    }

    public GeneratorOptions createSettings(DynamicRegistryManager dynamicRegistries, long seed, boolean generateStructures, boolean generateLoot, String generatorSettings)
    {
        return this.factory.createSettings(dynamicRegistries, seed, generateStructures, generateLoot, generatorSettings);
    }

    public interface IChunkGeneratorFactory
    {
        ChunkGenerator createChunkGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> dimensionSettingsRegistry, long seed, String generatorSettings);

        default GeneratorOptions createSettings(DynamicRegistryManager dynamicRegistries, long seed, boolean generateStructures, boolean bonusChest, String generatorSettings) {
            Registry<Biome> biomeRegistry = dynamicRegistries.get(Registry.BIOME_KEY);
            Registry<DimensionType> dimensionTypeRegistry = dynamicRegistries.get(Registry.DIMENSION_TYPE_KEY);
            Registry<ChunkGeneratorSettings> dimensionSettingsRegistry = dynamicRegistries.get(Registry.NOISE_SETTINGS_WORLDGEN);
            return new GeneratorOptions(seed, generateStructures, bonusChest,
                    GeneratorOptions.method_28608(dimensionTypeRegistry,
                            DimensionType.createDefaultDimensionOptions(dimensionTypeRegistry, biomeRegistry, dimensionSettingsRegistry, seed),
                            createChunkGenerator(biomeRegistry, dimensionSettingsRegistry, seed, generatorSettings)));
        }
    }

    public interface IBasicChunkGeneratorFactory extends IChunkGeneratorFactory
    {
        ChunkGenerator createChunkGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> dimensionSettingsRegistry, long seed);

        default ChunkGenerator createChunkGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> dimensionSettingsRegistry, long seed, String generatorSettings)
        {
            return createChunkGenerator(biomeRegistry, dimensionSettingsRegistry, seed);
        }
    }
}
