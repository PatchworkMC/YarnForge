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

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.client.model.geometry.IModelGeometryPart;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class BlockModelConfiguration implements IModelConfiguration
{
    public final JsonUnbakedModel owner;
    public final VisibilityData visibilityData = new VisibilityData();
    @Nullable
    private IModelGeometry<?> customGeometry;
    @Nullable
    private ModelBakeSettings customModelState;

    public BlockModelConfiguration(JsonUnbakedModel owner)
    {
        this.owner = owner;
    }

    @Nullable
    @Override
    public UnbakedModel getOwnerModel()
    {
        return owner;
    }

    @Override
    public String getModelName()
    {
        return owner.id;
    }

    public boolean hasCustomGeometry()
    {
        return getCustomGeometry() != null;
    }

    @Nullable
    public IModelGeometry<?> getCustomGeometry()
    {
        return owner.parent != null && customGeometry == null ? owner.parent.customData.getCustomGeometry() : customGeometry;
    }

    public void setCustomGeometry(IModelGeometry<?> geometry)
    {
        this.customGeometry = geometry;
    }

    @Nullable
    public ModelBakeSettings getCustomModelState()
    {
        return owner.parent != null && customModelState == null ? owner.parent.customData.getCustomModelState() : customModelState;
    }

    public void setCustomModelState(ModelBakeSettings modelState)
    {
        this.customModelState = modelState;
    }

    @Override
    public boolean getPartVisibility(IModelGeometryPart part, boolean fallback)
    {
        return owner.parent != null && !visibilityData.hasCustomVisibility(part) ?
                owner.parent.customData.getPartVisibility(part, fallback):
                visibilityData.isVisible(part, fallback);
    }

    @Override
    public boolean isTexturePresent(String name)
    {
        return owner.textureExists(name);
    }

    @Override
    public SpriteIdentifier resolveTexture(String name)
    {
        return owner.resolveSprite(name);
    }

    @Override
    public boolean isShadedInGui() {
        return true;
    }

    @Override
    public boolean isSideLit()
    {
        return owner.getGuiLight().isSide();
    }

    @Override
    public boolean useSmoothLighting()
    {
        return owner.useAmbientOcclusion();
    }

    @Override
    public ModelTransformation getCameraTransforms()
    {
        return owner.getTransformations();
    }

    @Override
    public ModelBakeSettings getCombinedTransform()
    {
        ModelBakeSettings state = getCustomModelState();

        return state != null
                ? new SimpleModelTransform(PerspectiveMapWrapper.getTransformsWithFallback(state, getCameraTransforms()), state.getRotation())
                : new SimpleModelTransform(PerspectiveMapWrapper.getTransforms(getCameraTransforms()));
    }

    public void copyFrom(BlockModelConfiguration other)
    {
        this.customGeometry = other.customGeometry;
        this.customModelState = other.customModelState;
        this.visibilityData.copyFrom(other.visibilityData);
    }

    public Collection<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
    {
        IModelGeometry<?> geometry = getCustomGeometry();
        return geometry == null ? Collections.emptySet() :
                geometry.getTextures(this, modelGetter, missingTextureErrors);
    }

    public BakedModel bake(ModelLoader bakery, Function<SpriteIdentifier, Sprite> bakedTextureGetter, ModelBakeSettings modelTransform, ModelOverrideList overrides, Identifier modelLocation)
    {
        IModelGeometry<?> geometry = getCustomGeometry();
        if (geometry == null)
            throw new IllegalStateException("Can not use custom baking without custom geometry");
        return geometry.bake(this, bakery, bakedTextureGetter, modelTransform, overrides, modelLocation);
    }

    public static class VisibilityData
    {
        private final Map<String, Boolean> data = new HashMap<>();

        public boolean hasCustomVisibility(IModelGeometryPart part)
        {
            return data.containsKey(part.name());
        }

        public boolean isVisible(IModelGeometryPart part, boolean fallback)
        {
            return data.getOrDefault(part.name(), fallback);
        }

        public void setVisibilityState(String partName, boolean type)
        {
            data.put(partName, type);
        }

        public void copyFrom(VisibilityData visibilityData)
        {
            data.clear();
            data.putAll(visibilityData.data);
        }
    }
}
