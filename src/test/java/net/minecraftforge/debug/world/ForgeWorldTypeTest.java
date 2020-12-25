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

package net.minecraftforge.debug.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraftforge.client.ForgeWorldTypeScreens;
import net.minecraftforge.common.world.ForgeWorldType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ObjectHolder;

@Mod("forge_world_type_test")
public class ForgeWorldTypeTest
{
    @ObjectHolder("forge_world_type_test:test_world_type")
    public static ForgeWorldType testWorldType;

    public ForgeWorldTypeTest()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(ForgeWorldType.class, this::registerWorldTypes);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerWorldTypeScreenFactories);
    }

    private void registerWorldTypes(RegistryEvent.Register<ForgeWorldType> event)
    {
        event.getRegistry().registerAll(
                new ForgeWorldType(GeneratorOptions::createOverworldGenerator).setRegistryName("test_world_type")
        );
        event.getRegistry().registerAll(
                new ForgeWorldType(this::createChunkGenerator).setRegistryName("test_world_type2")
        );
    }

    private ChunkGenerator createChunkGenerator(Registry<Biome> biomes, Registry<ChunkGeneratorSettings> dimensionSettings, long seed, String settings)
    {
        return GeneratorOptions.createOverworldGenerator(biomes, dimensionSettings, seed);
    }

    private void registerWorldTypeScreenFactories(FMLClientSetupEvent event)
    {
        ForgeWorldTypeScreens.registerFactory(testWorldType, (returnTo, dimensionGeneratorSettings) -> new Screen(testWorldType.getDisplayName())
        {
            @Override
            protected void init()
            {
                super.init();

                addButton(new ButtonWidget(0, 0, 120, 20, new LiteralText("close"), btn -> {
                    MinecraftClient.getInstance().openScreen(returnTo);
                }));
            }
        });
    }
}
