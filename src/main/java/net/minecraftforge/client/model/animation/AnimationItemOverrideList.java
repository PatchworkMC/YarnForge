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

package net.minecraftforge.client.model.animation;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelOverride;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelTransformComposition;
import net.minecraftforge.common.model.animation.CapabilityAnimation;

public final class AnimationItemOverrideList extends ModelOverrideList
{
    private final net.minecraft.client.render.model.ModelLoader bakery;
    private final UnbakedModel model;
    private final Identifier modelLoc;
    private final ModelBakeSettings state;


    private final Function<SpriteIdentifier, Sprite> bakedTextureGetter;

    public AnimationItemOverrideList(net.minecraft.client.render.model.ModelLoader bakery, UnbakedModel model, Identifier modelLoc, ModelBakeSettings state, Function<SpriteIdentifier, Sprite> bakedTextureGetter, ModelOverrideList overrides)
    {
        this(bakery, model, modelLoc, state, bakedTextureGetter, overrides.getOverrides().reverse());
    }

    public AnimationItemOverrideList(net.minecraft.client.render.model.ModelLoader bakery, UnbakedModel model, Identifier modelLoc, ModelBakeSettings state, Function<SpriteIdentifier, Sprite> bakedTextureGetter, List<ModelOverride> overrides)
    {
        super(bakery, model, ModelLoader.defaultModelGetter(), bakedTextureGetter, overrides);
        this.bakery = bakery;
        this.model = model;
        this.modelLoc = modelLoc;
        this.state = state;
        this.bakedTextureGetter = bakedTextureGetter;
    }

    @SuppressWarnings("resource")
    @Override
    public BakedModel apply(BakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity)
    {
        return stack.getCapability(CapabilityAnimation.ANIMATION_CAPABILITY, null)
            .map(asm ->
            {
                World w = world;
                // TODO caching?
                if(w == null && entity != null)
                {
                    w = entity.world;
                }
                if(world == null)
                {
                    w = MinecraftClient.getInstance().world;
                }
                return asm.apply(Animation.getWorldTime(world, Animation.getPartialTickTime())).getLeft();
            })
            // TODO where should uvlock data come from?
            .map(state -> model.bake(bakery, bakedTextureGetter, new ModelTransformComposition(state, this.state), modelLoc))
            .orElseGet(() -> super.apply(originalModel, stack, world, entity));
    }
}
