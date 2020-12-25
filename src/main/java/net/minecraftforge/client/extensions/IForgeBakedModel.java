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

package net.minecraftforge.client.extensions;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.model.data.IModelData;

public interface IForgeBakedModel
{
    default BakedModel getBakedModel()
    {
        return (BakedModel) this;
    }

    @Nonnull
    default List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData)
    {
        return getBakedModel().getQuads(state, side, rand);
    }

    default boolean isAmbientOcclusion(BlockState state) { return getBakedModel().useAmbientOcclusion(); }

    /**
     * Override to tell the new model loader that it shouldn't wrap this model
     */
    default boolean doesHandlePerspectives() { return false; }

    /*
     * Returns the pair of the model for the given perspective, and the matrix that
     * should be applied to the GL state before rendering it (matrix may be null).
     */
    default BakedModel handlePerspective(ModelTransformation.Mode cameraTransformType, MatrixStack mat)
    {
        return net.minecraftforge.client.ForgeHooksClient.handlePerspective(getBakedModel(), cameraTransformType, mat);
    }

    default @Nonnull IModelData getModelData(@Nonnull BlockRenderView world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData tileData)
    {
        return tileData;
    }

    default Sprite getParticleTexture(@Nonnull IModelData data)
    {
        return getBakedModel().getSprite();
    }

    /**
     * Override to true, to tell forge to call the getLayerModels method below.
     */
    default boolean isLayered()
    {
        return false;
    }

    /**
     * If {@see isLayered()} returns true, this is called to get the list of layers to draw.
     */
    default List<Pair<BakedModel, RenderLayer>> getLayerModels(ItemStack itemStack, boolean fabulous)
    {
        return Collections.singletonList(Pair.of(getBakedModel(), RenderLayers.getItemLayer(itemStack, fabulous)));
    }
}
