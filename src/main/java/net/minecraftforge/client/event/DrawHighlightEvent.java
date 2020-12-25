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

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * An event called whenever the selection highlight around blocks is about to be rendered.
 * Canceling this event stops the selection highlight from being rendered.
 * 
 * TODO: Rename to DrawSelectionEvent
 */
@Cancelable
public class DrawHighlightEvent extends Event
{
    private final WorldRenderer context;
    private final Camera info;
    private final HitResult target;
    private final float partialTicks;
    private final MatrixStack matrix;
    private final VertexConsumerProvider buffers;

    public DrawHighlightEvent(WorldRenderer context, Camera info, HitResult target, float partialTicks, MatrixStack matrix, VertexConsumerProvider buffers)
    {
        this.context = context;
        this.info = info;
        this.target = target;
        this.partialTicks= partialTicks;
        this.matrix = matrix;
        this.buffers = buffers;
    }

    public WorldRenderer getContext() { return context; }
    public Camera getInfo() { return info; }
    public HitResult getTarget() { return target; }
    public float getPartialTicks() { return partialTicks; }
    public MatrixStack getMatrix() { return matrix; }
    public VertexConsumerProvider getBuffers() { return buffers; }

    /**
     * A variant of the DrawHighlightEvent only called when a block is highlighted.
     */
    @Cancelable
    public static class HighlightBlock extends DrawHighlightEvent
    {
        public HighlightBlock(WorldRenderer context, Camera info, HitResult target, float partialTicks, MatrixStack matrix, VertexConsumerProvider buffers)
        {
            super(context, info, target, partialTicks, matrix, buffers);
        }

        @Override
        public BlockHitResult getTarget()
        {
            return (BlockHitResult) super.target;
        }
    }

    /**
     * A variant of the DrawHighlightEvent only called when an entity is highlighted.
     * Canceling this event has no effect.
     */
    @Cancelable
    public static class HighlightEntity extends DrawHighlightEvent
    {
        public HighlightEntity(WorldRenderer context, Camera info, HitResult target, float partialTicks, MatrixStack matrix, VertexConsumerProvider buffers)
        {
            super(context, info, target, partialTicks, matrix, buffers);
        }

        @Override
        public EntityHitResult getTarget()
        {
            return (EntityHitResult) super.target;
        }
    }
}
