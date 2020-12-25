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

package net.minecraftforge.client.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;

public abstract class RenderPlayerEvent extends PlayerEvent
{
    private final PlayerEntityRenderer renderer;
    private final float partialRenderTick;
    private final MatrixStack stack;
    private final VertexConsumerProvider buffers;
    private final int light;

    public RenderPlayerEvent(PlayerEntity player, PlayerEntityRenderer renderer, float partialRenderTick, MatrixStack stack, VertexConsumerProvider buffers, int light)
    {
        super(player);
        this.renderer = renderer;
        this.partialRenderTick = partialRenderTick;
        this.stack = stack;
        this.buffers = buffers;
        this.light = light;
    }

    public PlayerEntityRenderer getRenderer() { return renderer; }
    public float getPartialRenderTick() { return partialRenderTick; }
    public MatrixStack getMatrixStack() { return stack; }
    public VertexConsumerProvider getBuffers() { return buffers; }
    public int getLight() { return light; }

    @Cancelable
    public static class Pre extends RenderPlayerEvent
    {
        public Pre(PlayerEntity player, PlayerEntityRenderer renderer, float tick, MatrixStack stack, VertexConsumerProvider buffers, int light) {
            super(player, renderer, tick, stack, buffers, light);
        }
    }

    public static class Post extends RenderPlayerEvent
    {
        public Post(PlayerEntity player, PlayerEntityRenderer renderer, float tick, MatrixStack stack, VertexConsumerProvider buffers, int light) {
            super(player, renderer, tick, stack, buffers, light);
        }
    }

}
