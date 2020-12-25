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

import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.vector.*;

/*
 * Replacement interface for ModelRotation to allow custom transformations of vanilla models.
 * You should probably use TRSRTransformation directly.
 */
public interface IForgeTransformationMatrix
{
    default AffineTransformation getTransformaion()
    {
        return (AffineTransformation)this;
    }

    default boolean isIdentity()
    {
        return getTransformaion().equals(AffineTransformation.identity());
    }

    default void push(MatrixStack stack)
    {
        stack.push();

        Vector3f trans = getTransformaion().getTranslation();
        stack.translate(trans.getX(), trans.getY(), trans.getZ());

        stack.multiply(getTransformaion().getRotation2());

        Vector3f scale = getTransformaion().getScale();
        stack.scale(scale.getX(), scale.getY(), scale.getZ());

        stack.multiply(getTransformaion().getRightRot());

    }

    default AffineTransformation compose(AffineTransformation other)
    {
        if (getTransformaion().isIdentity()) return other;
        if (other.isIdentity()) return getTransformaion();
        Matrix4f m = getTransformaion().getMatrix();
        m.multiply(other.getMatrix());
        return new AffineTransformation(m);
    }

    default AffineTransformation inverse()
    {
        if (isIdentity()) return getTransformaion();
        Matrix4f m = getTransformaion().getMatrix().copy();
        m.invert();
        return new AffineTransformation(m);
    }

    default void transformPosition(Vector4f position)
    {
        position.transform(getTransformaion().getMatrix());
    }

    default void transformNormal(Vector3f normal)
    {
        normal.transform(getTransformaion().getNormalMatrix());
        normal.normalize();
    }

    default Direction rotateTransform(Direction facing)
    {
        return Direction.transform(getTransformaion().getMatrix(), facing);
    }

    /**
     * convert transformation from assuming center-block system to opposing-corner-block system
     */
    default AffineTransformation blockCenterToCorner()
    {
        return applyOrigin(new Vector3f(.5f, .5f, .5f));
    }

    /**
     * convert transformation from assuming opposing-corner-block system to center-block system
     */
    default AffineTransformation blockCornerToCenter()
    {
        return applyOrigin(new Vector3f(-.5f, -.5f, -.5f));
    }

    /**
     * Apply this transformation to a different origin.
     * Can be used for switching between coordinate systems.
     * Parameter is relative to the current origin.
     */
    default AffineTransformation applyOrigin(Vector3f origin) {
        AffineTransformation transform = getTransformaion();
        if (transform.isIdentity()) return AffineTransformation.identity();

        Matrix4f ret = transform.getMatrix();
        Matrix4f tmp = Matrix4f.translate(origin.getX(), origin.getY(), origin.getZ());
        ret.multiplyBackward(tmp);
        tmp.loadIdentity();
        tmp.setTranslation(-origin.getX(), -origin.getY(), -origin.getZ());
        ret.multiply(tmp);
        return new AffineTransformation(ret);
    }
}
