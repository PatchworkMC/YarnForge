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

package net.minecraftforge.fml.client.gui.screen;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Strings;
import net.minecraftforge.fml.ForgeI18n;
import net.minecraftforge.fml.LoadingFailedException;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.client.ClientHooks;
import net.minecraftforge.fml.client.gui.widget.ExtendedButton;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.FatalErrorScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public class LoadingErrorScreen extends FatalErrorScreen {
	private static final Logger LOGGER = LogManager.getLogger();
	private final Path modsDir;
	private final Path logFile;
	private final List<ModLoadingException> modLoadErrors;
	private final List<ModLoadingWarning> modLoadWarnings;
	private final Path dumpedLocation;
	private LoadingEntryList entryList;
	private Text errorHeader;
	private Text warningHeader;

	public LoadingErrorScreen(LoadingFailedException loadingException, List<ModLoadingWarning> warnings, final File dumpedLocation) {
		super(new LiteralText("Loading Error"), null);
		this.modLoadWarnings = warnings;
		this.modLoadErrors = loadingException == null ? Collections.emptyList() : loadingException.getErrors();
		this.modsDir = FMLPaths.MODSDIR.get();
		this.logFile = FMLPaths.GAMEDIR.get().resolve(Paths.get("logs", "latest.log"));
		this.dumpedLocation = dumpedLocation != null ? dumpedLocation.toPath() : null;
	}

	@Override
	public void init() {
		super.init();
		this.buttons.clear();
		this.children.clear();

		this.errorHeader = new LiteralText(Formatting.RED + ForgeI18n.parseMessage("fml.loadingerrorscreen.errorheader", this.modLoadErrors.size()) + Formatting.RESET);
		this.warningHeader = new LiteralText(Formatting.YELLOW + ForgeI18n.parseMessage("fml.loadingerrorscreen.warningheader", this.modLoadErrors.size()) + Formatting.RESET);

		int yOffset = 46;
		this.addButton(new ExtendedButton(50, this.height - yOffset, this.width / 2 - 55, 20, new LiteralText(ForgeI18n.parseMessage("fml.button.open.mods.folder")), b -> Util.getOperatingSystem().open(modsDir.toFile())));
		this.addButton(new ExtendedButton(this.width / 2 + 5, this.height - yOffset, this.width / 2 - 55, 20, new LiteralText(ForgeI18n.parseMessage("fml.button.open.file", logFile.getFileName())), b -> Util.getOperatingSystem().open(logFile.toFile())));
		if (this.modLoadErrors.isEmpty()) {
			this.addButton(new ExtendedButton(this.width / 4, this.height - 24, this.width / 2, 20, new LiteralText(ForgeI18n.parseMessage("fml.button.continue.launch")), b -> {
				ClientHooks.logMissingTextureErrors();
				this.client.openScreen(null);
			}));
		} else {
			this.addButton(new ExtendedButton(this.width / 4, this.height - 24, this.width / 2, 20, new LiteralText(ForgeI18n.parseMessage("fml.button.open.file", dumpedLocation.getFileName())), b -> Util.getOperatingSystem().open(dumpedLocation.toFile())));
		}

		this.entryList = new LoadingEntryList(this, this.modLoadErrors, this.modLoadWarnings);
		this.children.add(this.entryList);
		this.setFocused(this.entryList);
	}

	@Override
	public void render(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(mStack);
		this.entryList.render(mStack, mouseX, mouseY, partialTicks);
		drawMultiLineCenteredString(mStack, textRenderer, this.modLoadErrors.isEmpty() ? warningHeader : errorHeader, this.width / 2, 10);
		this.buttons.forEach(button -> button.render(mStack, mouseX, mouseY, partialTicks));
	}

	private void drawMultiLineCenteredString(MatrixStack mStack, TextRenderer fr, Text str, int x, int y) {
		for (OrderedText s : fr.wrapLines(str, this.width)) {
			fr.drawWithShadow(mStack, s, (float) (x - fr.getWidth(s) / 2.0), y, 0xFFFFFF);
			y += fr.fontHeight;
		}
	}

	public static class LoadingEntryList extends AlwaysSelectedEntryListWidget<LoadingEntryList.LoadingMessageEntry> {
		LoadingEntryList(final LoadingErrorScreen parent, final List<ModLoadingException> errors, final List<ModLoadingWarning> warnings) {
			super(parent.client, parent.width, parent.height, 35, parent.height - 50, 2 * parent.client.textRenderer.fontHeight + 8);
			boolean both = !errors.isEmpty() && !warnings.isEmpty();
			if (both) {
				addEntry(new LoadingMessageEntry(parent.errorHeader, true));
			}
			errors.forEach(e -> addEntry(new LoadingMessageEntry(new LiteralText(e.formatToString()))));
			if (both) {
				int maxChars = (this.width - 10) / parent.client.textRenderer.getWidth("-");
				addEntry(new LoadingMessageEntry(new LiteralText("\n" + Strings.repeat("-", maxChars) + "\n")));
				addEntry(new LoadingMessageEntry(parent.warningHeader, true));
			}
			warnings.forEach(w -> addEntry(new LoadingMessageEntry(new LiteralText(w.formatToString()))));
		}

		@Override
		protected int getScrollbarPositionX() {
			return this.getRight() - 6;
		}

		@Override
		public int getRowWidth() {
			return this.width;
		}

		public class LoadingMessageEntry extends AlwaysSelectedEntryListWidget.Entry<LoadingMessageEntry> {
			private final Text message;
			private final boolean center;

			LoadingMessageEntry(final Text message) {
				this(message, false);
			}

			LoadingMessageEntry(final Text message, final boolean center) {
				this.message = Objects.requireNonNull(message);
				this.center = center;
			}

			@Override
			public void render(MatrixStack mStack, int entryIdx, int top, int left, final int entryWidth, final int entryHeight, final int mouseX, final int mouseY, final boolean p_194999_5_, final float partialTicks) {
				TextRenderer font = MinecraftClient.getInstance().textRenderer;
				final List<OrderedText> strings = font.wrapLines(message, LoadingEntryList.this.width);
				int y = top + 2;
				for (int i = 0; i < Math.min(strings.size(), 2); i++) {
					if (center) {
						font.draw(mStack, strings.get(i), left + (width) - font.getWidth(strings.get(i)) / 2F, y, 0xFFFFFF);
					} else {
						font.draw(mStack, strings.get(i), left + 5, y, 0xFFFFFF);
					}
					y += font.fontHeight;
				}
			}
		}

	}
}
