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

import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.surfacebuilder.ConfiguredSurfaceBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BiomeGenerationSettingsBuilder extends GenerationSettings.Builder
{
    public BiomeGenerationSettingsBuilder(GenerationSettings orig)
    {
        surfaceBuilder = Optional.of(orig.getSurfaceBuilder());
        orig.getCarvingStages().forEach(k -> carvers.put(k, new ArrayList<>(orig.getCarversForStep(k))));
        orig.getFeatures().forEach(l -> features.add(new ArrayList<>(l)));
        structureFeatures.addAll(orig.getStructureFeatures());
    }

    public List<Supplier<ConfiguredFeature<?, ?>>> getFeatures(GenerationStep.Feature stage) {
        addFeatureStep(stage.ordinal());
        return features.get(stage.ordinal());
    }

    public Optional<Supplier<ConfiguredSurfaceBuilder<?>>> getSurfaceBuilder() {
        return surfaceBuilder;
    }

    public List<Supplier<ConfiguredCarver<?>>> getCarvers(GenerationStep.Carver stage) {
        return carvers.computeIfAbsent(stage, key -> new ArrayList<>());
    }

    public List<Supplier<ConfiguredStructureFeature<?, ?>>> getStructures() {
        return structureFeatures;
    }
}