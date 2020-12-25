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

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nonnull;

/**
 * This event is called when an item is rendered in an item frame.
 *
 * You can set canceled to do no further vanilla processing.
 */
@Cancelable
public class RenderItemInFrameEvent extends Event
{
    private final ItemStack item;
    private final ItemFrameEntity entityItemFrame;
    private final ItemFrameEntityRenderer renderer;
    private final MatrixStack matrix;
    private final VertexConsumerProvider buffers;
    private final int light;

    public RenderItemInFrameEvent(ItemFrameEntity itemFrame, ItemFrameEntityRenderer renderItemFrame, MatrixStack matrix,
                                  VertexConsumerProvider buffers, int light)
    {
        item = itemFrame.getHeldItemStack();
        entityItemFrame = itemFrame;
        renderer = renderItemFrame;
        this.matrix = matrix;
        this.buffers = buffers;
        this.light = light;
    }

    @Nonnull
    public ItemStack getItem()
    {
        return item;
    }

    public ItemFrameEntity getEntityItemFrame()
    {
        return entityItemFrame;
    }

    public ItemFrameEntityRenderer getRenderer()
    {
        return renderer;
    }

    public MatrixStack getMatrix() {
        return matrix;
    }

    public VertexConsumerProvider getBuffers() {
        return buffers;
    }

    public int getLight() {
        return light;
    }
}
