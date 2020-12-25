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

package net.minecraftforge.fml;

import java.util.List;
import net.minecraft.text.LiteralText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.TranslatableText;

public class TextComponentMessageFormatHandler {
	public static int handle(final TranslatableText parent, final List<StringVisitable> children, final Object[] formatArgs, final String format) {
		try {
			LiteralText component = new LiteralText(ForgeI18n.parseFormat(format, formatArgs));
			component.getStyle().withParent(parent.getStyle());
			children.add(component);
			return format.length();
		} catch (IllegalArgumentException ex) {
			return 0;
		}
	}
}
