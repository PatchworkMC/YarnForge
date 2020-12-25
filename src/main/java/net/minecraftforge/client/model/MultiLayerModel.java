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

package net.minecraftforge.client.model;

import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.*;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation.Mode;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.MinecraftForgeClient;

import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A model that can be rendered in multiple {@link RenderType}.
 */
public final class MultiLayerModel implements IModelGeometry<MultiLayerModel>
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final ImmutableList<Pair<RenderLayer, UnbakedModel>> models;
    private final boolean convertRenderTypes;

    public MultiLayerModel(Map<RenderLayer, UnbakedModel> models)
    {
        this(models.entrySet().stream().map(kv -> Pair.of(kv.getKey(), kv.getValue())).collect(ImmutableList.toImmutableList()), true);
    }

    public MultiLayerModel(ImmutableList<Pair<RenderLayer, UnbakedModel>> models, boolean convertRenderTypes)
    {
        this.models = models;
        this.convertRenderTypes = convertRenderTypes;
    }

    @Override
    public Collection<SpriteIdentifier> getTextures(IModelConfiguration owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
    {
        Set<SpriteIdentifier> materials = Sets.newHashSet();
        materials.add(owner.resolveTexture("particle"));
        for (Pair<RenderLayer, UnbakedModel> m : models)
            materials.addAll(m.getSecond().getTextureDependencies(modelGetter, missingTextureErrors));
        return materials;
    }

    private static ImmutableList<Pair<RenderLayer, BakedModel>> buildModels(List<Pair<RenderLayer, UnbakedModel>> models, ModelBakeSettings modelTransform,
                                                                            ModelLoader bakery, Function<SpriteIdentifier, Sprite> spriteGetter, Identifier modelLocation)
    {
        ImmutableList.Builder<Pair<RenderLayer, BakedModel>> builder = ImmutableList.builder();
        for(Pair<RenderLayer, UnbakedModel> entry : models)
        {
            builder.add(Pair.of(entry.getFirst(), entry.getSecond().bake(bakery, spriteGetter, modelTransform, modelLocation)));
        }
        return builder.build();
    }

    @Override
    public BakedModel bake(IModelConfiguration owner, ModelLoader bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelTransform, ModelOverrideList overrides, Identifier modelLocation)
    {
        UnbakedModel missing = net.minecraftforge.client.model.ModelLoader.instance().getMissingModel();

        return new MultiLayerBakedModel(
                owner.useSmoothLighting(), owner.isShadedInGui(), owner.isSideLit(),
                spriteGetter.apply(owner.resolveTexture("particle")), overrides, convertRenderTypes,
                missing.bake(bakery, spriteGetter, modelTransform, modelLocation),
                buildModels(models, modelTransform, bakery, spriteGetter, modelLocation),
                PerspectiveMapWrapper.getTransforms(new ModelTransformComposition(owner.getCombinedTransform(), modelTransform))
        );
    }

    private static final class MultiLayerBakedModel implements IDynamicBakedModel
    {
        private final ImmutableMap<RenderLayer, BakedModel> models;
        private final ImmutableMap<Mode, AffineTransformation> cameraTransforms;
        protected final boolean ambientOcclusion;
        protected final boolean gui3d;
        protected final boolean isSideLit;
        protected final Sprite particle;
        protected final ModelOverrideList overrides;
        private final BakedModel missing;
        private final boolean convertRenderTypes;
        private final List<Pair<BakedModel, RenderLayer>> itemLayers;

        public MultiLayerBakedModel(
                boolean ambientOcclusion, boolean isGui3d, boolean isSideLit, Sprite particle, ModelOverrideList overrides,
                boolean convertRenderTypes, BakedModel missing, List<Pair<RenderLayer, BakedModel>> models,
                ImmutableMap<Mode, AffineTransformation> cameraTransforms)
        {
            this.isSideLit = isSideLit;
            this.cameraTransforms = cameraTransforms;
            this.missing = missing;
            this.ambientOcclusion = ambientOcclusion;
            this.gui3d = isGui3d;
            this.particle = particle;
            this.overrides = overrides;
            this.convertRenderTypes = convertRenderTypes;
            this.models = ImmutableMap.copyOf(models.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
            this.itemLayers = models.stream().map(kv -> {
                RenderLayer rt = kv.getFirst();
                if (convertRenderTypes) rt = ITEM_RENDER_TYPE_MAPPING.getOrDefault(rt, rt);
                return Pair.of(kv.getSecond(), rt);
            }).collect(Collectors.toList());
        }

        @Nonnull
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData)
        {
            RenderLayer layer = MinecraftForgeClient.getRenderLayer();
            if (layer == null)
            {
                ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
                for (BakedModel model : models.values())
                {
                    builder.addAll(model.getQuads(state, side, rand, extraData));
                }
                return builder.build();
            }
            // support for item layer rendering
            if (state == null && convertRenderTypes)
                layer = ITEM_RENDER_TYPE_MAPPING.inverse().getOrDefault(layer, layer);
            // assumes that child model will handle this state properly. FIXME?
            return models.getOrDefault(layer, missing).getQuads(state, side, rand, extraData);
        }

        @Override
        public boolean useAmbientOcclusion()
        {
            return ambientOcclusion;
        }

        @Override
        public boolean isAmbientOcclusion(BlockState state)
        {
            return ambientOcclusion;
        }

        @Override
        public boolean hasDepth()
        {
            return gui3d;
        }

        @Override
        public boolean isSideLit()
        {
            return isSideLit;
        }

        @Override
        public boolean isBuiltin()
        {
            return false;
        }

        @Override
        public Sprite getSprite()
        {
            return particle;
        }

        @Override
        public boolean doesHandlePerspectives()
        {
            return true;
        }

        @Override
        public BakedModel handlePerspective(Mode cameraTransformType, MatrixStack mat)
        {
            return PerspectiveMapWrapper.handlePerspective(this, cameraTransforms, cameraTransformType, mat);
        }

        @Override
        public ModelOverrideList getOverrides()
        {
            return ModelOverrideList.EMPTY;
        }

        @Override
        public boolean isLayered()
        {
            return true;
        }

        @Override
        public List<Pair<BakedModel, RenderLayer>> getLayerModels(ItemStack itemStack, boolean fabulous)
        {
            return itemLayers;
        }

        public static BiMap<RenderLayer, RenderLayer> ITEM_RENDER_TYPE_MAPPING = HashBiMap.create();
        static {
            ITEM_RENDER_TYPE_MAPPING.put(RenderLayer.getSolid(), ForgeRenderTypes.ITEM_LAYERED_SOLID.get());
            ITEM_RENDER_TYPE_MAPPING.put(RenderLayer.getCutout(), ForgeRenderTypes.ITEM_LAYERED_CUTOUT.get());
            ITEM_RENDER_TYPE_MAPPING.put(RenderLayer.getCutoutMipped(), ForgeRenderTypes.ITEM_LAYERED_CUTOUT_MIPPED.get());
            ITEM_RENDER_TYPE_MAPPING.put(RenderLayer.getTranslucent(), ForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get());
        }
    }

    public static final class Loader implements IModelLoader<MultiLayerModel>
    {
        public static final ImmutableBiMap<String, RenderLayer> BLOCK_LAYERS = ImmutableBiMap.<String, RenderLayer>builder()
                .put("solid", RenderLayer.getSolid())
                .put("cutout", RenderLayer.getCutout())
                .put("cutout_mipped", RenderLayer.getCutoutMipped())
                .put("translucent", RenderLayer.getTranslucent())
                .put("tripwire", RenderLayer.getTripwire())
                .build();

        public static final Loader INSTANCE = new Loader();

        private Loader() {}

        @Override
        public void apply(ResourceManager resourceManager)
        {

        }

        @Override
        public MultiLayerModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents)
        {
            ImmutableList.Builder<Pair<RenderLayer, UnbakedModel>> builder = ImmutableList.builder();
            JsonObject layersObject = JsonHelper.getObject(modelContents, "layers");
            for(Map.Entry<String, RenderLayer> layer : BLOCK_LAYERS.entrySet()) // block layers
            {
                String layerName = layer.getKey(); // mc overrides toString to return the ID for the layer
                if(layersObject.has(layerName))
                {
                    builder.add(Pair.of(layer.getValue(), deserializationContext.deserialize(JsonHelper.getObject(layersObject, layerName), JsonUnbakedModel.class)));
                }
            }
            boolean convertRenderTypes = JsonHelper.getBoolean(modelContents, "convert_render_types", true);
            return new MultiLayerModel(builder.build(), convertRenderTypes);
        }
    }
}
