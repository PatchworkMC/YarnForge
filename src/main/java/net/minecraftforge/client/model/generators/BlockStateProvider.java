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

package net.minecraftforge.client.model.generators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.StairShape;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.block.enums.WallShape;
import net.minecraft.data.DataCache;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Data provider for blockstate files. Extends {@link BlockModelProvider} so that
 * blockstates and their referenced models can be provided in tandem.
 */
public abstract class BlockStateProvider implements DataProvider {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

    @VisibleForTesting
    protected final Map<Block, IGeneratedBlockstate> registeredBlocks = new LinkedHashMap<>();

    private final DataGenerator generator;
    private final String modid;
    private final BlockModelProvider blockModels;
    private final ItemModelProvider itemModels;

    public BlockStateProvider(DataGenerator gen, String modid, ExistingFileHelper exFileHelper) {
        this.generator = gen;
        this.modid = modid;
        this.blockModels = new BlockModelProvider(gen, modid, exFileHelper) {
            @Override protected void registerModels() {}
        };
        this.itemModels = new ItemModelProvider(gen, modid, this.blockModels.existingFileHelper) {
            @Override protected void registerModels() {}
        };
    }

    @Override
    public void run(DataCache cache) throws IOException {
        models().clear();
        itemModels().clear();
        registeredBlocks.clear();
        registerStatesAndModels();
        models().generateAll(cache);
        itemModels().generateAll(cache);
        for (Map.Entry<Block, IGeneratedBlockstate> entry : registeredBlocks.entrySet()) {
            saveBlockState(cache, entry.getValue().toJson(), entry.getKey());
        }
    }

    protected abstract void registerStatesAndModels();

    public VariantBlockStateBuilder getVariantBuilder(Block b) {
        if (registeredBlocks.containsKey(b)) {
            IGeneratedBlockstate old = registeredBlocks.get(b);
            Preconditions.checkState(old instanceof VariantBlockStateBuilder);
            return (VariantBlockStateBuilder) old;
        } else {
            VariantBlockStateBuilder ret = new VariantBlockStateBuilder(b);
            registeredBlocks.put(b, ret);
            return ret;
        }
    }

    public MultiPartBlockStateBuilder getMultipartBuilder(Block b) {
        if (registeredBlocks.containsKey(b)) {
            IGeneratedBlockstate old = registeredBlocks.get(b);
            Preconditions.checkState(old instanceof MultiPartBlockStateBuilder);
            return (MultiPartBlockStateBuilder) old;
        } else {
            MultiPartBlockStateBuilder ret = new MultiPartBlockStateBuilder(b);
            registeredBlocks.put(b, ret);
            return ret;
        }
    }

    public BlockModelProvider models() {
        return blockModels;
    }

    public ItemModelProvider itemModels() {
        return itemModels;
    }

    public Identifier modLoc(String name) {
        return new Identifier(modid, name);
    }

    public Identifier mcLoc(String name) {
        return new Identifier(name);
    }

    private String name(Block block) {
        return block.getRegistryName().getPath();
    }

    public Identifier blockTexture(Block block) {
        Identifier name = block.getRegistryName();
        return new Identifier(name.getNamespace(), ModelProvider.BLOCK_FOLDER + "/" + name.getPath());
    }

    private Identifier extend(Identifier rl, String suffix) {
        return new Identifier(rl.getNamespace(), rl.getPath() + suffix);
    }

    public ModelFile cubeAll(Block block) {
        return models().cubeAll(name(block), blockTexture(block));
    }

    public void simpleBlock(Block block) {
        simpleBlock(block, cubeAll(block));
    }

    public void simpleBlock(Block block, Function<ModelFile, ConfiguredModel[]> expander) {
        simpleBlock(block, expander.apply(cubeAll(block)));
    }

    public void simpleBlock(Block block, ModelFile model) {
        simpleBlock(block, new ConfiguredModel(model));
    }

    public void simpleBlockItem(Block block, ModelFile model) {
        itemModels().getBuilder(block.getRegistryName().getPath()).parent(model);
    }

    public void simpleBlock(Block block, ConfiguredModel... models) {
        getVariantBuilder(block)
            .partialState().setModels(models);
    }

    public void axisBlock(PillarBlock block) {
        axisBlock(block, blockTexture(block));
    }

    public void logBlock(PillarBlock block) {
        axisBlock(block, blockTexture(block), extend(blockTexture(block), "_top"));
    }

    public void axisBlock(PillarBlock block, Identifier baseName) {
        axisBlock(block, extend(baseName, "_side"), extend(baseName, "_end"));
    }

    public void axisBlock(PillarBlock block, Identifier side, Identifier end) {
        axisBlock(block, models().cubeColumn(name(block), side, end), models().cubeColumnHorizontal(name(block) + "_horizontal", side, end));
    }

    public void axisBlock(PillarBlock block, ModelFile vertical, ModelFile horizontal) {
        getVariantBuilder(block)
            .partialState().with(PillarBlock.AXIS, Axis.Y)
                .modelForState().modelFile(vertical).addModel()
            .partialState().with(PillarBlock.AXIS, Axis.Z)
                .modelForState().modelFile(horizontal).rotationX(90).addModel()
            .partialState().with(PillarBlock.AXIS, Axis.X)
                .modelForState().modelFile(horizontal).rotationX(90).rotationY(90).addModel();
    }

    private static final int DEFAULT_ANGLE_OFFSET = 180;

    public void horizontalBlock(Block block, Identifier side, Identifier front, Identifier top) {
        horizontalBlock(block, models().orientable(name(block), side, front, top));
    }

    public void horizontalBlock(Block block, ModelFile model) {
        horizontalBlock(block, model, DEFAULT_ANGLE_OFFSET);
    }

    public void horizontalBlock(Block block, ModelFile model, int angleOffset) {
        horizontalBlock(block, $ -> model, angleOffset);
    }

    public void horizontalBlock(Block block, Function<BlockState, ModelFile> modelFunc) {
        horizontalBlock(block, modelFunc, DEFAULT_ANGLE_OFFSET);
    }

    public void horizontalBlock(Block block, Function<BlockState, ModelFile> modelFunc, int angleOffset) {
        getVariantBuilder(block)
            .forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(modelFunc.apply(state))
                    .rotationY(((int) state.get(Properties.HORIZONTAL_FACING).asRotation() + angleOffset) % 360)
                    .build()
            );
    }

    public void horizontalFaceBlock(Block block, ModelFile model) {
        horizontalFaceBlock(block, model, DEFAULT_ANGLE_OFFSET);
    }

    public void horizontalFaceBlock(Block block, ModelFile model, int angleOffset) {
        horizontalFaceBlock(block, $ -> model, angleOffset);
    }

    public void horizontalFaceBlock(Block block, Function<BlockState, ModelFile> modelFunc) {
        horizontalFaceBlock(block, modelFunc, DEFAULT_ANGLE_OFFSET);
    }

    public void horizontalFaceBlock(Block block, Function<BlockState, ModelFile> modelFunc, int angleOffset) {
        getVariantBuilder(block)
            .forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(modelFunc.apply(state))
                    .rotationX(state.get(Properties.WALL_MOUNT_LOCATION).ordinal() * 90)
                    .rotationY((((int) state.get(Properties.HORIZONTAL_FACING).asRotation() + angleOffset) + (state.get(Properties.WALL_MOUNT_LOCATION) == WallMountLocation.CEILING ? 180 : 0)) % 360)
                    .build()
            );
    }

    public void directionalBlock(Block block, ModelFile model) {
        directionalBlock(block, model, DEFAULT_ANGLE_OFFSET);
    }

    public void directionalBlock(Block block, ModelFile model, int angleOffset) {
        directionalBlock(block, $ -> model, angleOffset);
    }

    public void directionalBlock(Block block, Function<BlockState, ModelFile> modelFunc) {
        directionalBlock(block, modelFunc, DEFAULT_ANGLE_OFFSET);
    }

    public void directionalBlock(Block block, Function<BlockState, ModelFile> modelFunc, int angleOffset) {
        getVariantBuilder(block)
            .forAllStates(state -> {
                Direction dir = state.get(Properties.FACING);
                return ConfiguredModel.builder()
                    .modelFile(modelFunc.apply(state))
                    .rotationX(dir == Direction.DOWN ? 180 : dir.getAxis().isHorizontal() ? 90 : 0)
                    .rotationY(dir.getAxis().isVertical() ? 0 : (((int) dir.asRotation()) + angleOffset) % 360)
                    .build();
            });
    }

    public void stairsBlock(StairsBlock block, Identifier texture) {
        stairsBlock(block, texture, texture, texture);
    }

    public void stairsBlock(StairsBlock block, String name, Identifier texture) {
        stairsBlock(block, name, texture, texture, texture);
    }

    public void stairsBlock(StairsBlock block, Identifier side, Identifier bottom, Identifier top) {
        stairsBlockInternal(block, block.getRegistryName().toString(), side, bottom, top);
    }

    public void stairsBlock(StairsBlock block, String name, Identifier side, Identifier bottom, Identifier top) {
        stairsBlockInternal(block, name + "_stairs", side, bottom, top);
    }

    private void stairsBlockInternal(StairsBlock block, String baseName, Identifier side, Identifier bottom, Identifier top) {
        ModelFile stairs = models().stairs(baseName, side, bottom, top);
        ModelFile stairsInner = models().stairsInner(baseName + "_inner", side, bottom, top);
        ModelFile stairsOuter = models().stairsOuter(baseName + "_outer", side, bottom, top);
        stairsBlock(block, stairs, stairsInner, stairsOuter);
    }

    public void stairsBlock(StairsBlock block, ModelFile stairs, ModelFile stairsInner, ModelFile stairsOuter) {
        getVariantBuilder(block)
            .forAllStatesExcept(state -> {
               Direction facing = state.get(StairsBlock.FACING);
               BlockHalf half = state.get(StairsBlock.HALF);
               StairShape shape = state.get(StairsBlock.SHAPE);
               int yRot = (int) facing.rotateYClockwise().asRotation(); // Stairs model is rotated 90 degrees clockwise for some reason
               if (shape == StairShape.INNER_LEFT || shape == StairShape.OUTER_LEFT) {
                   yRot += 270; // Left facing stairs are rotated 90 degrees clockwise
               }
               if (shape != StairShape.STRAIGHT && half == BlockHalf.TOP) {
                   yRot += 90; // Top stairs are rotated 90 degrees clockwise
               }
               yRot %= 360;
               boolean uvlock = yRot != 0 || half == BlockHalf.TOP; // Don't set uvlock for states that have no rotation
               return ConfiguredModel.builder()
                       .modelFile(shape == StairShape.STRAIGHT ? stairs : shape == StairShape.INNER_LEFT || shape == StairShape.INNER_RIGHT ? stairsInner : stairsOuter)
                       .rotationX(half == BlockHalf.BOTTOM ? 0 : 180)
                       .rotationY(yRot)
                       .uvLock(uvlock)
                       .build();
            }, StairsBlock.WATERLOGGED);
    }

    public void slabBlock(SlabBlock block, Identifier doubleslab, Identifier texture) {
        slabBlock(block, doubleslab, texture, texture, texture);
    }

    public void slabBlock(SlabBlock block, Identifier doubleslab, Identifier side, Identifier bottom, Identifier top) {
        slabBlock(block, models().slab(name(block), side, bottom, top), models().slabTop(name(block) + "_top", side, bottom, top), models().getExistingFile(doubleslab));
    }

    public void slabBlock(SlabBlock block, ModelFile bottom, ModelFile top, ModelFile doubleslab) {
        getVariantBuilder(block)
            .partialState().with(SlabBlock.TYPE, SlabType.BOTTOM).addModels(new ConfiguredModel(bottom))
            .partialState().with(SlabBlock.TYPE, SlabType.TOP).addModels(new ConfiguredModel(top))
            .partialState().with(SlabBlock.TYPE, SlabType.DOUBLE).addModels(new ConfiguredModel(doubleslab));
    }

    public void fourWayBlock(HorizontalConnectingBlock block, ModelFile post, ModelFile side) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block)
                .part().modelFile(post).addModel().end();
        fourWayMultipart(builder, side);
    }

    public void fourWayMultipart(MultiPartBlockStateBuilder builder, ModelFile side) {
        ConnectingBlock.FACING_PROPERTIES.entrySet().forEach(e -> {
            Direction dir = e.getKey();
            if (dir.getAxis().isHorizontal()) {
                builder.part().modelFile(side).rotationY((((int) dir.asRotation()) + 180) % 360).uvLock(true).addModel()
                    .condition(e.getValue(), true);
            }
        });
    }

    public void fenceBlock(FenceBlock block, Identifier texture) {
        String baseName = block.getRegistryName().toString();
        fourWayBlock(block, models().fencePost(baseName + "_post", texture), models().fenceSide(baseName + "_side", texture));
    }

    public void fenceBlock(FenceBlock block, String name, Identifier texture) {
        fourWayBlock(block, models().fencePost(name + "_fence_post", texture), models().fenceSide(name + "_fence_side", texture));
    }

    public void fenceGateBlock(FenceGateBlock block, Identifier texture) {
        fenceGateBlockInternal(block, block.getRegistryName().toString(), texture);
    }

    public void fenceGateBlock(FenceGateBlock block, String name, Identifier texture) {
        fenceGateBlockInternal(block, name + "_fence_gate", texture);
    }

    private void fenceGateBlockInternal(FenceGateBlock block, String baseName, Identifier texture) {
        ModelFile gate = models().fenceGate(baseName, texture);
        ModelFile gateOpen = models().fenceGateOpen(baseName + "_open", texture);
        ModelFile gateWall = models().fenceGateWall(baseName + "_wall", texture);
        ModelFile gateWallOpen = models().fenceGateWallOpen(baseName + "_wall_open", texture);
        fenceGateBlock(block, gate, gateOpen, gateWall, gateWallOpen);
    }

    public void fenceGateBlock(FenceGateBlock block, ModelFile gate, ModelFile gateOpen, ModelFile gateWall, ModelFile gateWallOpen) {
        getVariantBuilder(block).forAllStatesExcept(state -> {
            ModelFile model = gate;
            if (state.get(FenceGateBlock.IN_WALL)) {
                model = gateWall;
            }
            if (state.get(FenceGateBlock.OPEN)) {
                model = model == gateWall ? gateWallOpen : gateOpen;
            }
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY((int) state.get(FenceGateBlock.FACING).asRotation())
                    .uvLock(true)
                    .build();
        }, FenceGateBlock.POWERED);
    }

    public void wallBlock(WallBlock block, Identifier texture) {
        wallBlockInternal(block, block.getRegistryName().toString(), texture);
    }

    public void wallBlock(WallBlock block, String name, Identifier texture) {
        wallBlockInternal(block, name + "_wall", texture);
    }

    private void wallBlockInternal(WallBlock block, String baseName, Identifier texture) {
        wallBlock(block, models().wallPost(baseName + "_post", texture), models().wallSide(baseName + "_side", texture), models().wallSideTall(baseName + "_side_tall", texture));
    }
    
    public static final ImmutableMap<Direction, Property<WallShape>> WALL_PROPS = ImmutableMap.<Direction, Property<WallShape>>builder()
    		.put(Direction.EAST,  Properties.EAST_WALL_SHAPE)
    		.put(Direction.NORTH, Properties.NORTH_WALL_SHAPE)
    		.put(Direction.SOUTH, Properties.SOUTH_WALL_SHAPE)
    		.put(Direction.WEST,  Properties.WEST_WALL_SHAPE)
    		.build();

    public void wallBlock(WallBlock block, ModelFile post, ModelFile side, ModelFile sideTall) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block)
                .part().modelFile(post).addModel()
                    .condition(WallBlock.UP, true).end();
        WALL_PROPS.entrySet().stream()
        	.filter(e -> e.getKey().getAxis().isHorizontal())
        	.forEach(e -> {
        		wallSidePart(builder, side, e, WallShape.LOW);
        		wallSidePart(builder, sideTall, e, WallShape.TALL);
        	});
    }
    
    private void wallSidePart(MultiPartBlockStateBuilder builder, ModelFile model, Map.Entry<Direction, Property<WallShape>> entry, WallShape height) {
        builder.part()
        	.modelFile(model)
        		.rotationY((((int) entry.getKey().asRotation()) + 180) % 360)
        		.uvLock(true)
        		.addModel()
    		.condition(entry.getValue(), height);
    }

    public void paneBlock(PaneBlock block, Identifier pane, Identifier edge) {
        paneBlockInternal(block, block.getRegistryName().toString(), pane, edge);
    }

    public void paneBlock(PaneBlock block, String name, Identifier pane, Identifier edge) {
        paneBlockInternal(block, name + "_pane", pane, edge);
    }

    private void paneBlockInternal(PaneBlock block, String baseName, Identifier pane, Identifier edge) {
        ModelFile post = models().panePost(baseName + "_post", pane, edge);
        ModelFile side = models().paneSide(baseName + "_side", pane, edge);
        ModelFile sideAlt = models().paneSideAlt(baseName + "_side_alt", pane, edge);
        ModelFile noSide = models().paneNoSide(baseName + "_noside", pane);
        ModelFile noSideAlt = models().paneNoSideAlt(baseName + "_noside_alt", pane);
        paneBlock(block, post, side, sideAlt, noSide, noSideAlt);
    }

    public void paneBlock(PaneBlock block, ModelFile post, ModelFile side, ModelFile sideAlt, ModelFile noSide, ModelFile noSideAlt) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block)
                .part().modelFile(post).addModel().end();
        ConnectingBlock.FACING_PROPERTIES.entrySet().forEach(e -> {
            Direction dir = e.getKey();
            if (dir.getAxis().isHorizontal()) {
                boolean alt = dir == Direction.SOUTH;
                builder.part().modelFile(alt || dir == Direction.WEST ? sideAlt : side).rotationY(dir.getAxis() == Axis.X ? 90 : 0).addModel()
                    .condition(e.getValue(), true).end()
                .part().modelFile(alt || dir == Direction.EAST ? noSideAlt : noSide).rotationY(dir == Direction.WEST ? 270 : dir == Direction.SOUTH ? 90 : 0).addModel()
                    .condition(e.getValue(), false);
            }
        });
    }

    public void doorBlock(DoorBlock block, Identifier bottom, Identifier top) {
        doorBlockInternal(block, block.getRegistryName().toString(), bottom, top);
    }

    public void doorBlock(DoorBlock block, String name, Identifier bottom, Identifier top) {
        doorBlockInternal(block, name + "_door", bottom, top);
    }

    private void doorBlockInternal(DoorBlock block, String baseName, Identifier bottom, Identifier top) {
        ModelFile bottomLeft = models().doorBottomLeft(baseName + "_bottom", bottom, top);
        ModelFile bottomRight = models().doorBottomRight(baseName + "_bottom_hinge", bottom, top);
        ModelFile topLeft = models().doorTopLeft(baseName + "_top", bottom, top);
        ModelFile topRight = models().doorTopRight(baseName + "_top_hinge", bottom, top);
        doorBlock(block, bottomLeft, bottomRight, topLeft, topRight);
    }

    public void doorBlock(DoorBlock block, ModelFile bottomLeft, ModelFile bottomRight, ModelFile topLeft, ModelFile topRight) {
        getVariantBuilder(block).forAllStatesExcept(state -> {
            int yRot = ((int) state.get(DoorBlock.FACING).asRotation()) + 90;
            boolean rh = state.get(DoorBlock.HINGE) == DoorHinge.RIGHT;
            boolean open = state.get(DoorBlock.OPEN);
            boolean right = rh ^ open;
            if (open) {
                yRot += 90;
            }
            if (rh && open) {
                yRot += 180;
            }
            yRot %= 360;
            return ConfiguredModel.builder().modelFile(state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? (right ? bottomRight : bottomLeft) : (right ? topRight : topLeft))
                    .rotationY(yRot)
                    .build();
        }, DoorBlock.POWERED);
    }

    public void trapdoorBlock(TrapdoorBlock block, Identifier texture, boolean orientable) {
        trapdoorBlockInternal(block, block.getRegistryName().toString(), texture, orientable);
    }

    public void trapdoorBlock(TrapdoorBlock block, String name, Identifier texture, boolean orientable) {
        trapdoorBlockInternal(block, name + "_trapdoor", texture, orientable);
    }

    private void trapdoorBlockInternal(TrapdoorBlock block, String baseName, Identifier texture, boolean orientable) {
        ModelFile bottom = orientable ? models().trapdoorOrientableBottom(baseName + "_bottom", texture) : models().trapdoorBottom(baseName + "_bottom", texture);
        ModelFile top = orientable ? models().trapdoorOrientableTop(baseName + "_top", texture) : models().trapdoorTop(baseName + "_top", texture);
        ModelFile open = orientable ? models().trapdoorOrientableOpen(baseName + "_open", texture) : models().trapdoorOpen(baseName + "_open", texture);
        trapdoorBlock(block, bottom, top, open, orientable);
    }

    public void trapdoorBlock(TrapdoorBlock block, ModelFile bottom, ModelFile top, ModelFile open, boolean orientable) {
        getVariantBuilder(block).forAllStatesExcept(state -> {
            int xRot = 0;
            int yRot = ((int) state.get(TrapdoorBlock.FACING).asRotation()) + 180;
            boolean isOpen = state.get(TrapdoorBlock.OPEN);
            if (orientable && isOpen && state.get(TrapdoorBlock.HALF) == BlockHalf.TOP) {
                xRot += 180;
                yRot += 180;
            }
            if (!orientable && !isOpen) {
                yRot = 0;
            }
            yRot %= 360;
            return ConfiguredModel.builder().modelFile(isOpen ? open : state.get(TrapdoorBlock.HALF) == BlockHalf.TOP ? top : bottom)
                    .rotationX(xRot)
                    .rotationY(yRot)
                    .build();
        }, TrapdoorBlock.POWERED, TrapdoorBlock.WATERLOGGED);
    }

    private void saveBlockState(DataCache cache, JsonObject stateJson, Block owner) {
        Identifier blockName = Preconditions.checkNotNull(owner.getRegistryName());
        Path mainOutput = generator.getOutput();
        String pathSuffix = "assets/" + blockName.getNamespace() + "/blockstates/" + blockName.getPath() + ".json";
        Path outputPath = mainOutput.resolve(pathSuffix);
        try {
            DataProvider.writeToPath(GSON, cache, stateJson, outputPath);
        } catch (IOException e) {
            LOGGER.error("Couldn't save blockstate to {}", outputPath, e);
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "Block States: " + modid;
    }

    public static class ConfiguredModelList {
        private final List<ConfiguredModel> models;

        private ConfiguredModelList(List<ConfiguredModel> models) {
            Preconditions.checkArgument(!models.isEmpty());
            this.models = models;
        }

        public ConfiguredModelList(ConfiguredModel model) {
            this(ImmutableList.of(model));
        }

        public ConfiguredModelList(ConfiguredModel... models) {
            this(Arrays.asList(models));
        }

        public JsonElement toJSON() {
            if (models.size()==1) {
                return models.get(0).toJSON(false);
            } else {
                JsonArray ret = new JsonArray();
                for (ConfiguredModel m:models) {
                    ret.add(m.toJSON(true));
                }
                return ret;
            }
        }

        public ConfiguredModelList append(ConfiguredModel... models) {
            return new ConfiguredModelList(ImmutableList.<ConfiguredModel>builder().addAll(this.models).add(models).build());
        }
    }
}
