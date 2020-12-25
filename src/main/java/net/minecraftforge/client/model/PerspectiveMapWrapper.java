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

import java.util.EnumMap;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.ModelOverride;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.model.TransformationHelper;

import javax.annotation.Nullable;
import java.util.List;

public class PerspectiveMapWrapper implements IDynamicBakedModel
{
    private final BakedModel parent;
    private final ImmutableMap<ModelTransformation.Mode, AffineTransformation> transforms;
    private final OverrideListWrapper overrides = new OverrideListWrapper();

    public PerspectiveMapWrapper(BakedModel parent, ImmutableMap<ModelTransformation.Mode, AffineTransformation> transforms)
    {
        this.parent = parent;
        this.transforms = transforms;
    }

    public PerspectiveMapWrapper(BakedModel parent, ModelBakeSettings state)
    {
        this(parent, getTransforms(state));
    }

    public static ImmutableMap<ModelTransformation.Mode, AffineTransformation> getTransforms(ModelBakeSettings state)
    {
        EnumMap<ModelTransformation.Mode, AffineTransformation> map = new EnumMap<>(ModelTransformation.Mode.class);
        for(ModelTransformation.Mode type : ModelTransformation.Mode.values())
        {
            AffineTransformation tr = state.getPartTransformation(type);
            if(!tr.isIdentity())
            {
                map.put(type, tr);
            }
        }
        return ImmutableMap.copyOf(map);
    }

    @SuppressWarnings("deprecation")
    public static ImmutableMap<ModelTransformation.Mode, AffineTransformation> getTransforms(ModelTransformation transforms)
    {
        EnumMap<ModelTransformation.Mode, AffineTransformation> map = new EnumMap<>(ModelTransformation.Mode.class);
        for(ModelTransformation.Mode type : ModelTransformation.Mode.values())
        {
            if (transforms.isTransformationDefined(type))
            {
                map.put(type, TransformationHelper.toTransformation(transforms.getTransformation(type)));
            }
        }
        return ImmutableMap.copyOf(map);
    }

    @SuppressWarnings("deprecation")
    public static ImmutableMap<ModelTransformation.Mode, AffineTransformation> getTransformsWithFallback(ModelBakeSettings state, ModelTransformation transforms)
    {
        EnumMap<ModelTransformation.Mode, AffineTransformation> map = new EnumMap<>(ModelTransformation.Mode.class);
        for(ModelTransformation.Mode type : ModelTransformation.Mode.values())
        {
            AffineTransformation tr = state.getPartTransformation(type);
            if(!tr.isIdentity())
            {
                map.put(type, tr);
            }
            else if (transforms.isTransformationDefined(type))
            {
                map.put(type, TransformationHelper.toTransformation(transforms.getTransformation(type)));
            }
        }
        return ImmutableMap.copyOf(map);
    }

    public static BakedModel handlePerspective(BakedModel model, ImmutableMap<ModelTransformation.Mode, AffineTransformation> transforms, ModelTransformation.Mode cameraTransformType, MatrixStack mat)
    {
        AffineTransformation tr = transforms.getOrDefault(cameraTransformType, AffineTransformation.identity());
        if (!tr.isIdentity())
        {
            tr.push(mat);
        }
        return model;
    }

    public static BakedModel handlePerspective(BakedModel model, ModelBakeSettings state, ModelTransformation.Mode cameraTransformType, MatrixStack mat)
    {
        AffineTransformation tr = state.getPartTransformation(cameraTransformType);
        if (!tr.isIdentity())
        {
            tr.push(mat);
        }
        return model;
    }

    @Override public boolean useAmbientOcclusion() { return parent.useAmbientOcclusion(); }
    @Override public boolean isAmbientOcclusion(BlockState state) { return parent.isAmbientOcclusion(state); }
    @Override public boolean hasDepth() { return parent.hasDepth(); }
    @Override public boolean isSideLit() { return parent.isSideLit(); }
    @Override public boolean isBuiltin() { return parent.isBuiltin(); }
    @Override public Sprite getSprite() { return parent.getSprite(); }
    @SuppressWarnings("deprecation")
    @Override public ModelTransformation getTransformation() { return parent.getTransformation(); }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData extraData)
    {
        return parent.getQuads(state, side, rand, extraData);
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
        return handlePerspective(this, transforms, cameraTransformType, mat);
    }

    private class OverrideListWrapper extends ModelOverrideList
    {
        public OverrideListWrapper()
        {
            super();
        }

        @Nullable
        @Override
        public BakedModel apply(BakedModel model, ItemStack stack, @Nullable ClientWorld worldIn, @Nullable LivingEntity entityIn)
        {
            model = parent.getOverrides().apply(parent, stack, worldIn, entityIn);
            return new PerspectiveMapWrapper(model, transforms);
        }

        @Override
        public ImmutableList<ModelOverride> getOverrides()
        {
            return parent.getOverrides().getOverrides();
        }
    }
}
