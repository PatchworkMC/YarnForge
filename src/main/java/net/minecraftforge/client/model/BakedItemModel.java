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

import javax.annotation.Nullable;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation.Mode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

public class BakedItemModel implements BakedModel
{
    protected final ImmutableList<BakedQuad> quads;
    protected final Sprite particle;
    protected final ImmutableMap<Mode, AffineTransformation> transforms;
    protected final ModelOverrideList overrides;
    protected final BakedModel guiModel;
    protected final boolean isSideLit;

    public BakedItemModel(ImmutableList<BakedQuad> quads, Sprite particle, ImmutableMap<Mode, AffineTransformation> transforms, ModelOverrideList overrides, boolean untransformed, boolean isSideLit)
    {
        this.quads = quads;
        this.particle = particle;
        this.transforms = transforms;
        this.overrides = overrides;
        this.isSideLit = isSideLit;
        this.guiModel = untransformed && hasGuiIdentity(transforms) ? new BakedGuiItemModel<>(this) : null;
    }

    private static boolean hasGuiIdentity(ImmutableMap<Mode, AffineTransformation> transforms)
    {
        AffineTransformation guiTransform = transforms.get(Mode.GUI);
        return guiTransform == null || guiTransform.isIdentity();
    }

    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean hasDepth() { return false; }
    @Override public boolean isSideLit() { return isSideLit; }
    @Override public boolean isBuiltin() { return false; }
    @Override public Sprite getSprite() { return particle; }
    @Override public ModelOverrideList getOverrides() { return overrides; }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand)
    {
        if (side == null)
        {
            return quads;
        }
        return ImmutableList.of();
    }

    @Override
    public BakedModel handlePerspective(Mode type, MatrixStack mat)
    {
        if (type == Mode.GUI && this.guiModel != null)
        {
            return this.guiModel.handlePerspective(type, mat);
        }
        return PerspectiveMapWrapper.handlePerspective(this, transforms, type, mat);
    }

    public static class BakedGuiItemModel<T extends BakedItemModel> extends BakedModelWrapper<T>
    {
        private final ImmutableList<BakedQuad> quads;

        public BakedGuiItemModel(T originalModel)
        {
            super(originalModel);
            ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
            for (BakedQuad quad : originalModel.quads)
            {
                if (quad.getFace() == Direction.SOUTH)
                {
                    builder.add(quad);
                }
            }
            this.quads = builder.build();
        }

        @Override
        public List<BakedQuad> getQuads (@Nullable BlockState state, @Nullable Direction side, Random rand)
        {
            if(side == null)
            {
                return quads;
            }
            return ImmutableList.of();
        }

        @Override
        public boolean doesHandlePerspectives()
        {
            return true;
        }

        @Override
        public BakedModel handlePerspective(Mode type, MatrixStack mat)
        {
            if (type == Mode.GUI)
            {
                return PerspectiveMapWrapper.handlePerspective(this, originalModel.transforms, type, mat);
            }
            return this.originalModel.handlePerspective(type, mat);
        }
    }
}
