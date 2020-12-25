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

package net.minecraftforge.debug.fluid;

import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraftforge.common.util.Lazy;
import org.apache.commons.lang3.Validate;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(NewFluidTest.MODID)
public class NewFluidTest
{
    public static final String MODID = "new_fluid_test";

    public static final Identifier FLUID_STILL = new Identifier("minecraft:block/brown_mushroom_block");
    public static final Identifier FLUID_FLOWING = new Identifier("minecraft:block/mushroom_stem");
    public static final Identifier FLUID_OVERLAY = new Identifier("minecraft:block/obsidian");

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MODID);

    private static ForgeFlowingFluid.Properties makeProperties()
    {
        return new ForgeFlowingFluid.Properties(test_fluid, test_fluid_flowing,
                FluidAttributes.builder(FLUID_STILL, FLUID_FLOWING).overlay(FLUID_OVERLAY).color(0x3F1080FF))
                .bucket(test_fluid_bucket).block(test_fluid_block);
    }

    public static RegistryObject<FlowableFluid> test_fluid = FLUIDS.register("test_fluid", () ->
            new ForgeFlowingFluid.Source(makeProperties())
    );
    public static RegistryObject<FlowableFluid> test_fluid_flowing = FLUIDS.register("test_fluid_flowing", () ->
            new ForgeFlowingFluid.Flowing(makeProperties())
    );

    public static RegistryObject<FluidBlock> test_fluid_block = BLOCKS.register("test_fluid_block", () ->
            new FluidBlock(test_fluid, Block.Properties.of(net.minecraft.block.Material.WATER).noCollision().strength(100.0F).dropsNothing())
    );
    public static RegistryObject<Item> test_fluid_bucket = ITEMS.register("test_fluid_bucket", () ->
            new BucketItem(test_fluid, new Item.Settings().recipeRemainder(Items.BUCKET).maxCount(1).group(ItemGroup.MISC))
    );

    // WARNING: this doesn't allow "any fluid", only the fluid from this test mod!
    public static RegistryObject<Block> fluidloggable_block = BLOCKS.register("fluidloggable_block", () ->
            new FluidloggableBlock(Block.Properties.of(Material.WOOD).noCollision().strength(100.0F).dropsNothing())
    );
    public static RegistryObject<Item> fluidloggable_blockitem = ITEMS.register("fluidloggable_block", () ->
            new BlockItem(fluidloggable_block.get(), new Item.Settings().group(ItemGroup.MISC))
    );

    public NewFluidTest()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::loadComplete);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        FLUIDS.register(modEventBus);
    }

    public void loadComplete(FMLLoadCompleteEvent event)
    {
        // some sanity checks
        BlockState state = Fluids.WATER.getDefaultState().getBlockState();
        BlockState state2 = Fluids.WATER.getAttributes().getBlock(null,null,Fluids.WATER.getDefaultState());
        Validate.isTrue(state.getBlock() == Blocks.WATER && state2 == state);
        ItemStack stack = Fluids.WATER.getAttributes().getBucket(new FluidStack(Fluids.WATER, 1));
        Validate.isTrue(stack.getItem() == Fluids.WATER.getBucketItem());
    }

    // WARNING: this doesn't allow "any fluid", only the fluid from this test mod!
    private static class FluidloggableBlock extends Block implements Waterloggable
    {
        public static final BooleanProperty FLUIDLOGGED = BooleanProperty.of("fluidlogged");

        public FluidloggableBlock(Settings properties)
        {
            super(properties);
            setDefaultState(getStateManager().getDefaultState().with(FLUIDLOGGED, false));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
        {
            builder.add(FLUIDLOGGED);
        }

        @Override
        public boolean canFillWithFluid(BlockView worldIn, BlockPos pos, BlockState state, Fluid fluidIn) {
            return !state.get(FLUIDLOGGED) && fluidIn == test_fluid.get();
        }

        @Override
        public boolean tryFillWithFluid(WorldAccess worldIn, BlockPos pos, BlockState state, FluidState fluidStateIn) {
            if (canFillWithFluid(worldIn, pos, state, fluidStateIn.getFluid())) {
                if (!worldIn.isClient()) {
                    worldIn.setBlockState(pos, state.with(FLUIDLOGGED, true), 3);
                    worldIn.getFluidTickScheduler().schedule(pos, fluidStateIn.getFluid(), fluidStateIn.getFluid().getTickRate(worldIn));
                }

                return true;
            } else {
                return false;
            }
        }

        @Override
        public Fluid tryDrainFluid(WorldAccess worldIn, BlockPos pos, BlockState state) {
            if (state.get(FLUIDLOGGED)) {
                worldIn.setBlockState(pos, state.with(FLUIDLOGGED, false), 3);
                return test_fluid.get();
            } else {
                return Fluids.EMPTY;
            }
        }

        @Override
        public FluidState getFluidState(BlockState state)
        {
            return state.get(FLUIDLOGGED) ? test_fluid.get().getDefaultState() : Fluids.EMPTY.getDefaultState();
        }
    }
}
