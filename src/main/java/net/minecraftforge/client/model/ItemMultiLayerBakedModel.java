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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ItemMultiLayerBakedModel implements IDynamicBakedModel
{
    private final boolean smoothLighting;
    private final boolean shadedInGui;
    private final boolean sideLit;
    private final Sprite particle;
    private final ModelOverrideList overrides;
    private final ImmutableList<Pair<BakedModel, RenderLayer>> layerModels;
    private final ImmutableMap<ModelTransformation.Mode, AffineTransformation> cameraTransforms;

    public ItemMultiLayerBakedModel(boolean smoothLighting, boolean shadedInGui, boolean sideLit,
                                    Sprite particle, ModelOverrideList overrides,
                                    ImmutableMap<ModelTransformation.Mode, AffineTransformation> cameraTransforms,
                                    ImmutableList<Pair<BakedModel, RenderLayer>> layerModels)
    {
        this.smoothLighting = smoothLighting;
        this.shadedInGui = shadedInGui;
        this.sideLit = sideLit;
        this.particle = particle;
        this.overrides = overrides;
        this.layerModels = layerModels;
        this.cameraTransforms = cameraTransforms;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData modelData)
    {
        List<BakedQuad> quads = Lists.newArrayList();
        layerModels.forEach(lm -> quads.addAll(lm.getFirst().getQuads(state, side, rand, modelData)));
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return smoothLighting;
    }

    @Override
    public boolean hasDepth()
    {
        return shadedInGui;
    }

    @Override
    public boolean isSideLit()
    {
        return sideLit;
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
    public BakedModel handlePerspective(ModelTransformation.Mode cameraTransformType, MatrixStack mat)
    {
        return PerspectiveMapWrapper.handlePerspective(this, cameraTransforms, cameraTransformType, mat);
    }

    //@Override
    public boolean isLayered()
    {
        return true;
    }

    //@Override
    public List<Pair<BakedModel, RenderLayer>> getLayerModels(ItemStack itemStack, boolean fabulous)
    {
        return layerModels;
    }

    public static Builder builder(IModelConfiguration owner, Sprite particle, ModelOverrideList overrides,
                                  ImmutableMap<ModelTransformation.Mode, AffineTransformation> cameraTransforms)
    {
        return new Builder(owner, particle, overrides, cameraTransforms);
    }

    public static class Builder
    {
        private final ImmutableList.Builder<Pair<BakedModel, RenderLayer>> builder = ImmutableList.builder();
        private final List<BakedQuad> quads = Lists.newArrayList();
        private final ModelOverrideList overrides;
        private final ImmutableMap<ModelTransformation.Mode, AffineTransformation> cameraTransforms;
        private final IModelConfiguration owner;
        private Sprite particle;
        private RenderLayer lastRt = null;

        private Builder(IModelConfiguration owner, Sprite particle, ModelOverrideList overrides,
                        ImmutableMap<ModelTransformation.Mode, AffineTransformation> cameraTransforms)
        {
            this.owner = owner;
            this.particle = particle;
            this.overrides = overrides;
            this.cameraTransforms = cameraTransforms;
        }

        private void addLayer(ImmutableList.Builder<Pair<BakedModel, RenderLayer>> builder, List<BakedQuad> quads, RenderLayer rt)
        {
            BakedModel model = new BakedItemModel(ImmutableList.copyOf(quads), particle, ImmutableMap.of(), ModelOverrideList.EMPTY, true, owner.isSideLit());
            builder.add(Pair.of(model, rt));
        }

        private void flushQuads(RenderLayer rt)
        {
            if (rt != lastRt)
            {
                if (quads.size() > 0)
                {
                    addLayer(builder, quads, lastRt);
                    quads.clear();
                }
                lastRt = rt;
            }
        }

        public Builder setParticle(Sprite particleSprite)
        {
            this.particle = particleSprite;
            return this;
        }

        public Builder addQuads(RenderLayer rt, BakedQuad... quadsToAdd)
        {
            flushQuads(rt);
            Collections.addAll(quads, quadsToAdd);
            return this;
        }

        public Builder addQuads(RenderLayer rt, Collection<BakedQuad> quadsToAdd)
        {
            flushQuads(rt);
            quads.addAll(quadsToAdd);
            return this;
        }

        public BakedModel build()
        {
            if (quads.size() > 0)
            {
                addLayer(builder, quads, lastRt);
            }
            return new ItemMultiLayerBakedModel(owner.useSmoothLighting(), owner.isShadedInGui(), owner.isSideLit(),
                    particle, overrides, cameraTransforms, builder.build());
        }
    }
}
