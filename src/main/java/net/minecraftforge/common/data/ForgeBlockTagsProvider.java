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

import net.minecraft.data.DataGenerator;
import net.minecraft.data.server.BlockTagsProvider;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import static net.minecraftforge.common.Tags.Blocks.*;

import java.util.Locale;
import java.util.function.Consumer;

public class ForgeBlockTagsProvider extends BlockTagsProvider
{
    public ForgeBlockTagsProvider(DataGenerator gen, ExistingFileHelper existingFileHelper)
    {
        super(gen, "forge", existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure()
    {
        getOrCreateTagBuilder(CHESTS).addTags(CHESTS_ENDER, CHESTS_TRAPPED, CHESTS_WOODEN);
        getOrCreateTagBuilder(CHESTS_ENDER).add(Blocks.ENDER_CHEST);
        getOrCreateTagBuilder(CHESTS_TRAPPED).add(Blocks.TRAPPED_CHEST);
        getOrCreateTagBuilder(CHESTS_WOODEN).add(Blocks.CHEST, Blocks.TRAPPED_CHEST);
        getOrCreateTagBuilder(COBBLESTONE).add(Blocks.COBBLESTONE, Blocks.INFESTED_COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        getOrCreateTagBuilder(DIRT).add(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.MYCELIUM);
        getOrCreateTagBuilder(END_STONES).add(Blocks.END_STONE);
        getOrCreateTagBuilder(FENCE_GATES).addTags(FENCE_GATES_WOODEN);
        getOrCreateTagBuilder(FENCE_GATES_WOODEN).add(Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.CRIMSON_FENCE_GATE, Blocks.WARPED_FENCE_GATE);
        getOrCreateTagBuilder(FENCES).addTags(FENCES_NETHER_BRICK, FENCES_WOODEN);
        getOrCreateTagBuilder(FENCES_NETHER_BRICK).add(Blocks.NETHER_BRICK_FENCE);
        getOrCreateTagBuilder(FENCES_WOODEN).add(Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, Blocks.BIRCH_FENCE, Blocks.JUNGLE_FENCE, Blocks.ACACIA_FENCE, Blocks.DARK_OAK_FENCE,  Blocks.CRIMSON_FENCE, Blocks.WARPED_FENCE);
        getOrCreateTagBuilder(GLASS).addTags(GLASS_COLORLESS, STAINED_GLASS);
        getOrCreateTagBuilder(GLASS_COLORLESS).add(Blocks.GLASS);
        addColored(getOrCreateTagBuilder(STAINED_GLASS)::add, GLASS, "{color}_stained_glass");
        getOrCreateTagBuilder(GLASS_PANES).addTags(GLASS_PANES_COLORLESS, STAINED_GLASS_PANES);
        getOrCreateTagBuilder(GLASS_PANES_COLORLESS).add(Blocks.GLASS_PANE);
        addColored(getOrCreateTagBuilder(STAINED_GLASS_PANES)::add, GLASS_PANES, "{color}_stained_glass_pane");
        getOrCreateTagBuilder(GRAVEL).add(Blocks.GRAVEL);
        getOrCreateTagBuilder(NETHERRACK).add(Blocks.NETHERRACK);
        getOrCreateTagBuilder(OBSIDIAN).add(Blocks.OBSIDIAN);
        getOrCreateTagBuilder(ORES).addTags(ORES_COAL, ORES_DIAMOND, ORES_EMERALD, ORES_GOLD, ORES_IRON, ORES_LAPIS, ORES_REDSTONE, ORES_QUARTZ, ORES_NETHERITE_SCRAP);
        getOrCreateTagBuilder(ORES_COAL).add(Blocks.COAL_ORE);
        getOrCreateTagBuilder(ORES_DIAMOND).add(Blocks.DIAMOND_ORE);
        getOrCreateTagBuilder(ORES_EMERALD).add(Blocks.EMERALD_ORE);
        getOrCreateTagBuilder(ORES_GOLD).addTag(BlockTags.GOLD_ORES);
        getOrCreateTagBuilder(ORES_IRON).add(Blocks.IRON_ORE);
        getOrCreateTagBuilder(ORES_LAPIS).add(Blocks.LAPIS_ORE);
        getOrCreateTagBuilder(ORES_QUARTZ).add(Blocks.NETHER_QUARTZ_ORE);
        getOrCreateTagBuilder(ORES_REDSTONE).add(Blocks.REDSTONE_ORE);
        getOrCreateTagBuilder(ORES_NETHERITE_SCRAP).add(Blocks.ANCIENT_DEBRIS);
        getOrCreateTagBuilder(SAND).addTags(SAND_COLORLESS, SAND_RED);
        getOrCreateTagBuilder(SAND_COLORLESS).add(Blocks.SAND);
        getOrCreateTagBuilder(SAND_RED).add(Blocks.RED_SAND);
        getOrCreateTagBuilder(SANDSTONE).add(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE);
        getOrCreateTagBuilder(STONE).add(Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.INFESTED_STONE, Blocks.STONE, Blocks.POLISHED_ANDESITE, Blocks.POLISHED_DIORITE, Blocks.POLISHED_GRANITE);
        getOrCreateTagBuilder(STORAGE_BLOCKS).addTags(STORAGE_BLOCKS_COAL, STORAGE_BLOCKS_DIAMOND, STORAGE_BLOCKS_EMERALD, STORAGE_BLOCKS_GOLD, STORAGE_BLOCKS_IRON, STORAGE_BLOCKS_LAPIS, STORAGE_BLOCKS_QUARTZ, STORAGE_BLOCKS_REDSTONE, STORAGE_BLOCKS_NETHERITE);
        getOrCreateTagBuilder(STORAGE_BLOCKS_COAL).add(Blocks.COAL_BLOCK);
        getOrCreateTagBuilder(STORAGE_BLOCKS_DIAMOND).add(Blocks.DIAMOND_BLOCK);
        getOrCreateTagBuilder(STORAGE_BLOCKS_EMERALD).add(Blocks.EMERALD_BLOCK);
        getOrCreateTagBuilder(STORAGE_BLOCKS_GOLD).add(Blocks.GOLD_BLOCK);
        getOrCreateTagBuilder(STORAGE_BLOCKS_IRON).add(Blocks.IRON_BLOCK);
        getOrCreateTagBuilder(STORAGE_BLOCKS_LAPIS).add(Blocks.LAPIS_BLOCK);
        getOrCreateTagBuilder(STORAGE_BLOCKS_QUARTZ).add(Blocks.QUARTZ_BLOCK);
        getOrCreateTagBuilder(STORAGE_BLOCKS_REDSTONE).add(Blocks.REDSTONE_BLOCK);
        getOrCreateTagBuilder(STORAGE_BLOCKS_NETHERITE).add(Blocks.NETHERITE_BLOCK);
    }

    private void addColored(Consumer<Block> consumer, Tag.Identified<Block> group, String pattern)
    {
        String prefix = group.getId().getPath().toUpperCase(Locale.ENGLISH) + '_';
        for (DyeColor color  : DyeColor.values())
        {
            Identifier key = new Identifier("minecraft", pattern.replace("{color}",  color.getName()));
            Tag.Identified<Block> tag = getForgeTag(prefix + color.getName());
            Block block = ForgeRegistries.BLOCKS.getValue(key);
            if (block == null || block  == Blocks.AIR)
                throw new IllegalStateException("Unknown vanilla block: " + key.toString());
            getOrCreateTagBuilder(tag).add(block);
            consumer.accept(block);
        }
    }

    @SuppressWarnings("unchecked")
    private Tag.Identified<Block> getForgeTag(String name)
    {
        try
        {
            name = name.toUpperCase(Locale.ENGLISH);
            return (Tag.Identified<Block>)Tags.Blocks.class.getDeclaredField(name).get(null);
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
        {
            throw new IllegalStateException(Tags.Blocks.class.getName() + " is missing tag name: " + name);
        }
    }

    @Override
    public String getName()
    {
        return "Forge Block Tags";
    }
}
