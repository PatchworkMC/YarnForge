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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.model.data.IModelData;

public abstract class BakedModelWrapper<T extends BakedModel> implements BakedModel
{
    protected final T originalModel;

    public BakedModelWrapper(T originalModel)
    {
        this.originalModel = originalModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand)
    {
        return originalModel.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return originalModel.useAmbientOcclusion();
    }

    @Override
    public boolean isAmbientOcclusion(BlockState state)
    {
        return originalModel.isAmbientOcclusion(state);
    }

    @Override
    public boolean hasDepth()
    {
        return originalModel.hasDepth();
    }

    @Override
    public boolean isSideLit()
    {
        return originalModel.isSideLit();
    }

    @Override
    public boolean isBuiltin()
    {
        return originalModel.isBuiltin();
    }

    @Override
    public Sprite getSprite()
    {
        return originalModel.getSprite();
    }

    @Override
    public ModelTransformation getTransformation()
    {
        return originalModel.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides()
    {
        return originalModel.getOverrides();
    }

    @Override
    public boolean doesHandlePerspectives()
    {
        return originalModel.doesHandlePerspectives();
    }

    @Override
    public BakedModel handlePerspective(ModelTransformation.Mode cameraTransformType, MatrixStack mat)
    {
        return originalModel.handlePerspective(cameraTransformType, mat);
    }

    @Override
    public Sprite getParticleTexture(@Nonnull IModelData data)
    {
        return originalModel.getParticleTexture(data);
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData)
    {
        return originalModel.getQuads(state, side, rand, extraData);
    }

    @Nonnull
    @Override
    public IModelData getModelData(@Nonnull BlockRenderView world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData tileData)
    {
        return originalModel.getModelData(world, pos, state, tileData);
    }
}
