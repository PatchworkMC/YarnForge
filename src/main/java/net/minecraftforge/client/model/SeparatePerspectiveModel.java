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

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import net.minecraftforge.client.model.geometry.IModelGeometry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class SeparatePerspectiveModel implements IModelGeometry<SeparatePerspectiveModel>
{
    private final JsonUnbakedModel baseModel;
    private final ImmutableMap<ModelTransformation.Mode, JsonUnbakedModel> perspectives;

    public SeparatePerspectiveModel(JsonUnbakedModel baseModel, ImmutableMap<ModelTransformation.Mode, JsonUnbakedModel> perspectives)
    {
        this.baseModel = baseModel;
        this.perspectives = perspectives;
    }

    @Override
    public net.minecraft.client.render.model.BakedModel bake(IModelConfiguration owner, ModelLoader bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelTransform, ModelOverrideList overrides, Identifier modelLocation)
    {
        return new BakedModel(
                owner.useSmoothLighting(), owner.isShadedInGui(), owner.isSideLit(),
                spriteGetter.apply(owner.resolveTexture("particle")), overrides,
                baseModel.bake(bakery, baseModel, spriteGetter, modelTransform, modelLocation, owner.isSideLit()),
                ImmutableMap.copyOf(Maps.transformValues(perspectives, value -> {
                    return value.bake(bakery, value, spriteGetter, modelTransform, modelLocation, owner.isSideLit());
                }))
        );
    }

    @Override
    public Collection<SpriteIdentifier> getTextures(IModelConfiguration owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
    {
        Set<SpriteIdentifier> textures = Sets.newHashSet();
        textures.addAll(baseModel.getTextureDependencies(modelGetter, missingTextureErrors));
        for(JsonUnbakedModel model : perspectives.values())
            textures.addAll(model.getTextureDependencies(modelGetter, missingTextureErrors));
        return textures;
    }

    public static class BakedModel implements net.minecraft.client.render.model.BakedModel
    {
        private final boolean isAmbientOcclusion;
        private final boolean isGui3d;
        private final boolean isSideLit;
        private final Sprite particle;
        private final ModelOverrideList overrides;
        private final net.minecraft.client.render.model.BakedModel baseModel;
        private final ImmutableMap<ModelTransformation.Mode, net.minecraft.client.render.model.BakedModel> perspectives;

        public BakedModel(boolean isAmbientOcclusion, boolean isGui3d, boolean isSideLit, Sprite particle, ModelOverrideList overrides, net.minecraft.client.render.model.BakedModel baseModel, ImmutableMap<ModelTransformation.Mode, net.minecraft.client.render.model.BakedModel> perspectives)
        {
            this.isAmbientOcclusion = isAmbientOcclusion;
            this.isGui3d = isGui3d;
            this.isSideLit = isSideLit;
            this.particle = particle;
            this.overrides = overrides;
            this.baseModel = baseModel;
            this.perspectives = perspectives;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand)
        {
            return Collections.emptyList();
        }

        @Override
        public boolean useAmbientOcclusion()
        {
            return isAmbientOcclusion;
        }

        @Override
        public boolean hasDepth()
        {
            return isGui3d;
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
        public ModelOverrideList getOverrides()
        {
            return overrides;
        }

        @Override
        public boolean doesHandlePerspectives()
        {
            return true;
        }

        @Override
        public ModelTransformation getTransformation()
        {
            return ModelTransformation.NONE;
        }

        @Override
        public net.minecraft.client.render.model.BakedModel handlePerspective(ModelTransformation.Mode cameraTransformType, MatrixStack mat)
        {
            if (perspectives.containsKey(cameraTransformType))
            {
                net.minecraft.client.render.model.BakedModel p = perspectives.get(cameraTransformType);
                return p.handlePerspective(cameraTransformType, mat);
            }
            return baseModel.handlePerspective(cameraTransformType, mat);
        }
    }

    public static class Loader implements IModelLoader<SeparatePerspectiveModel>
    {
        public static final Loader INSTANCE = new Loader();

        public static final ImmutableBiMap<String, ModelTransformation.Mode> PERSPECTIVES = ImmutableBiMap.<String, ModelTransformation.Mode>builder()
                .put("none", ModelTransformation.Mode.NONE)
                .put("third_person_left_hand", ModelTransformation.Mode.THIRD_PERSON_LEFT_HAND)
                .put("third_person_right_hand", ModelTransformation.Mode.THIRD_PERSON_RIGHT_HAND)
                .put("first_person_left_hand", ModelTransformation.Mode.FIRST_PERSON_LEFT_HAND)
                .put("first_person_right_hand", ModelTransformation.Mode.FIRST_PERSON_RIGHT_HAND)
                .put("head", ModelTransformation.Mode.HEAD)
                .put("gui", ModelTransformation.Mode.GUI)
                .put("ground", ModelTransformation.Mode.GROUND)
                .put("fixed", ModelTransformation.Mode.FIXED)
                .build();

        @Override
        public void apply(ResourceManager resourceManager)
        {
            // Not used
        }

        @Override
        public SeparatePerspectiveModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents)
        {
            JsonUnbakedModel baseModel = deserializationContext.deserialize(JsonHelper.getObject(modelContents, "base"), JsonUnbakedModel.class);

            JsonObject perspectiveData = JsonHelper.getObject(modelContents, "perspectives");

            ImmutableMap.Builder<ModelTransformation.Mode, JsonUnbakedModel> perspectives = ImmutableMap.builder();
            for(Map.Entry<String, ModelTransformation.Mode> perspective : PERSPECTIVES.entrySet())
            {
                if (perspectiveData.has(perspective.getKey()))
                {
                    JsonUnbakedModel perspectiveModel = deserializationContext.deserialize(JsonHelper.getObject(perspectiveData, perspective.getKey()), JsonUnbakedModel.class);
                    perspectives.put(perspective.getValue(), perspectiveModel);
                }
            }

            return new SeparatePerspectiveModel(baseModel, perspectives.build());
        }
    }
}
