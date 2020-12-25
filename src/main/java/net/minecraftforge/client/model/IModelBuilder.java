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

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BasicBakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public interface IModelBuilder<T extends IModelBuilder<T>>
{
    static IModelBuilder<?> of(IModelConfiguration owner, ModelOverrideList overrides, Sprite particle)
    {
        return new Simple(new BasicBakedModel.Builder(owner, overrides).setParticle(particle));
    }

    T addFaceQuad(Direction facing, BakedQuad quad);
    T addGeneralQuad(BakedQuad quad);

    BakedModel build();

    class Simple implements IModelBuilder<Simple> {
        final BasicBakedModel.Builder builder;

        Simple(BasicBakedModel.Builder builder)
        {
            this.builder = builder;
        }

        @Override
        public Simple addFaceQuad(Direction facing, BakedQuad quad)
        {
            builder.addQuad(facing, quad);
            return this;
        }

        @Override
        public Simple addGeneralQuad(BakedQuad quad)
        {
            builder.addQuad(quad);
            return this;
        }

        @Override
        public BakedModel build()
        {
            return builder.build();
        }
    }
}

