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

import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.animation.IEventHandler;
import net.minecraftforge.common.model.animation.CapabilityAnimation;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.common.property.Properties;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Generic {@link TileGameRenderer} that works with the Forge model system and animations.
 */
public class TileEntityRendererAnimation<T extends BlockEntity> extends BlockEntityRenderer<T> implements IEventHandler<T>
{
    public TileEntityRendererAnimation(BlockEntityRenderDispatcher rendererDispatcherIn)
    {
        super(rendererDispatcherIn);
    }

    protected static BlockRenderManager blockRenderer;

    @Override
    public void render(T te, float partialTick, MatrixStack mat, VertexConsumerProvider renderer, int light, int otherlight)
    {
        LazyOptional<IAnimationStateMachine> cap = te.getCapability(CapabilityAnimation.ANIMATION_CAPABILITY);
        if(!cap.isPresent())
        {
            return;
        }
        if(blockRenderer == null) blockRenderer = MinecraftClient.getInstance().getBlockRenderManager();
        BlockPos pos = te.getPos();
        BlockRenderView world = MinecraftForgeClient.getRegionRenderCacheOptional(te.getWorld(), pos)
            .map(BlockRenderView.class::cast).orElseGet(() -> te.getWorld());
        BlockState state = world.getBlockState(pos);
        BakedModel model = blockRenderer.getModels().getModel(state);
        IModelData data = model.getModelData(world, pos, state, ModelDataManager.getModelData(te.getWorld(), pos));
        if (data.hasProperty(Properties.AnimationProperty))
        {
            @SuppressWarnings("resource")
            float time = Animation.getWorldTime(MinecraftClient.getInstance().world, partialTick);
            cap
                .map(asm -> asm.apply(time))
                .ifPresent(pair -> {
                    handleEvents(te, time, pair.getRight());

                    // TODO: caching?
                    data.setData(Properties.AnimationProperty, pair.getLeft());
                    blockRenderer.getModelRenderer().renderModel(world, model, state, pos, mat, renderer.getBuffer(TexturedRenderLayers.getEntitySolid()), false, new Random(), 42, light, data);
                });
        }
    }

    @Override
    public void handleEvents(T te, float time, Iterable<Event> pastEvents) {}
}
