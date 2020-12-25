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

package net.minecraftforge.debug.client.model;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

@Mod(CompositeModelTest.MODID)
public class CompositeModelTest
{
    public static final String MODID = "composite_model_test";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static RegistryObject<Block> composite_block = BLOCKS.register("composite_block", () ->
            new Block(Block.Properties.of(Material.WOOD).strength(10)) {
                @Override
                protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
                {
                    builder.add(Properties.HORIZONTAL_FACING);
                }

                @Nullable
                @Override
                public BlockState getPlacementState(ItemPlacementContext context)
                {
                    return getDefaultState().with(
                            Properties.HORIZONTAL_FACING, context.getPlayerFacing()
                    );
                }

                @Override
                public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
                    return VoxelShapes.union(
                            createCuboidShape(5.6, 5.6, 5.6, 10.4, 10.4, 10.4),
                            createCuboidShape(0, 0, 0, 4.8, 4.8, 4.8),
                            createCuboidShape(11.2, 0, 0, 16, 4.8, 4.8),
                            createCuboidShape(0, 0, 11.2, 4.8, 4.8, 16),
                            createCuboidShape(11.2, 0, 11.2, 16, 4.8, 16),
                            createCuboidShape(0, 11.2, 0, 4.8, 16, 4.8),
                            createCuboidShape(11.2, 11.2, 0, 16, 16, 4.8),
                            createCuboidShape(0, 11.2, 11.2, 4.8, 16, 16),
                            createCuboidShape(11.2, 11.2, 11.2, 16, 16, 16)
                    );
                }
            }
    );

    public static RegistryObject<Item> composite_item = ITEMS.register("composite_block", () ->
            new BlockItem(composite_block.get(), new Item.Settings().group(ItemGroup.MISC)) {
                @Override
                public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity)
                {
                    return armorType == EquipmentSlot.HEAD;
                }
            }
    );

    public CompositeModelTest()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
