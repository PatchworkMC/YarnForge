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

package net.minecraftforge.debug.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.Material;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ObjectHolder;

@Mod(CustomPlantTypeTest.MODID)
@Mod.EventBusSubscriber(bus = Bus.MOD)
public class CustomPlantTypeTest
{
    static final String MODID = "custom_plant_type_test";
    private static final String CUSTOM_SOIL_BLOCK = "test_custom_block";
    private static final String CUSTOM_PLANT_BLOCK = "test_custom_plant";

    @ObjectHolder(CUSTOM_SOIL_BLOCK)
    public static Block CUSTOM_SOIL;
    @ObjectHolder(CUSTOM_PLANT_BLOCK)
    public static Block CUSTOM_PLANT;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().registerAll(new CustomBlock(), new CustomPlantBlock());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(new BlockItem(CUSTOM_SOIL, (new Item.Settings())).setRegistryName(MODID, CUSTOM_SOIL_BLOCK),
                new BlockItem(CUSTOM_PLANT, (new Item.Settings())).setRegistryName(MODID, CUSTOM_PLANT_BLOCK));
    }

    public static class CustomBlock extends Block
    {
        public CustomBlock()
        {
            super(Block.Properties.of(Material.STONE));
            this.setRegistryName(MODID, CUSTOM_SOIL_BLOCK);
        }

        @Override
        public boolean canSustainPlant(BlockState state, BlockView world, BlockPos pos, Direction facing, IPlantable plantable)
        {
            PlantType type = plantable.getPlantType(world, pos.offset(facing));
            if (type != null && type == CustomPlantBlock.pt)
            {
                return true;
            }
            return super.canSustainPlant(state, world, pos, facing, plantable);
        }
    }

    public static class CustomPlantBlock extends FlowerBlock implements IPlantable
    {
        public static PlantType pt = PlantType.get("custom_plant_type");

        public CustomPlantBlock()
        {
            super(StatusEffects.WEAKNESS, 9, Block.Properties.of(Material.PLANT).noCollision().sounds(BlockSoundGroup.GRASS));
            this.setRegistryName(MODID, CUSTOM_PLANT_BLOCK);
        }

        @Override
        public PlantType getPlantType(BlockView world, BlockPos pos)
        {
            return pt;
        }

        @Override
        public BlockState getPlant(BlockView world, BlockPos pos)
        {
            return getDefaultState();
        }

        @Override
        public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos)
        {
            BlockState soil = world.getBlockState(pos.down());
            return soil.canSustainPlant(world, pos, Direction.UP, this);
        }

        @Override
        public boolean canPlantOnTop(BlockState state, BlockView worldIn, BlockPos pos)
        {
            Block block = state.getBlock();
            return block == CUSTOM_SOIL;
        }
    }
}
