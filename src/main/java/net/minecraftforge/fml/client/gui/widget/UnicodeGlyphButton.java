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

package net.minecraftforge.fml.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraftforge.fml.client.gui.GuiUtils;

/**
 * This class provides a button that shows a string glyph at the beginning. The glyph can be scaled using the glyphScale parameter.
 *
 * @author bspkrs
 */
public class UnicodeGlyphButton extends ExtendedButton {
	public String glyph;
	public float glyphScale;

	public UnicodeGlyphButton(int xPos, int yPos, int width, int height, Text displayString, String glyph, float glyphScale, PressAction handler) {
		super(xPos, yPos, width, height, displayString, handler);
		this.glyph = glyph;
		this.glyphScale = glyphScale;
	}

	@Override
	public void render(MatrixStack mStack, int mouseX, int mouseY, float partial) {
		if (this.visible) {
			MinecraftClient mc = MinecraftClient.getInstance();
			this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			int k = this.getYImage(this.hovered);
			GuiUtils.drawContinuousTexturedBox(mStack, ButtonWidget.WIDGETS_LOCATION, this.x, this.y, 0, 46 + k * 20, this.width, this.height, 200, 20, 2, 3, 2, 2, this.getZOffset());
			this.renderBg(mStack, mc, mouseX, mouseY);

			Text buttonText = this.getNarrationMessage();
			int glyphWidth = (int) (mc.textRenderer.getWidth(glyph) * glyphScale);
			int strWidth = mc.textRenderer.getWidth(buttonText);
			int ellipsisWidth = mc.textRenderer.getWidth("...");
			int totalWidth = strWidth + glyphWidth;

			if (totalWidth > width - 6 && totalWidth > ellipsisWidth) {
				buttonText = new LiteralText(mc.textRenderer.trimToWidth(buttonText, width - 6 - ellipsisWidth).getString().trim() + "...");
			}

			strWidth = mc.textRenderer.getWidth(buttonText);
			totalWidth = glyphWidth + strWidth;

			mStack.push();
			mStack.scale(glyphScale, glyphScale, 1.0F);
			drawCenteredText(mStack, mc.textRenderer, new LiteralText(glyph),
				(int) (((this.x + (this.width / 2) - (strWidth / 2)) / glyphScale) - (glyphWidth / (2 * glyphScale)) + 2),
				(int) (((this.y + ((this.height - 8) / glyphScale) / 2) - 1) / glyphScale), getFGColor());
			mStack.pop();

			drawCenteredText(mStack, mc.textRenderer, buttonText, (int) (this.x + (this.width / 2) + (glyphWidth / glyphScale)),
				this.y + (this.height - 8) / 2, getFGColor());

		}
	}
}
