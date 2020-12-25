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

package net.minecraftforge.fml.server;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.CrashReportExtender;
import net.minecraftforge.fml.LoadingFailedException;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.ModWorkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraftforge.fml.loading.LogMarkers.LOADING;

public class ServerModLoader {
	private static final Logger LOGGER = LogManager.getLogger();
	private static boolean hasErrors = false;

	public static void load() {
		LogicalSidedProvider.setServer(() -> {
			throw new IllegalStateException("Unable to access server yet");
		});
		LanguageHook.loadForgeAndMCLangs();
		try {
			ModLoader.get().gatherAndInitializeMods(ModWorkManager.syncExecutor(), ModWorkManager.parallelExecutor(), () -> {});
			ModLoader.get().loadMods(ModWorkManager.syncExecutor(), ModWorkManager.parallelExecutor(), e -> CompletableFuture.runAsync(() -> {}, e), e -> CompletableFuture.runAsync(() -> {}, e), () -> {});
			ModLoader.get().finishMods(ModWorkManager.syncExecutor(), ModWorkManager.parallelExecutor(), () -> {});
		} catch (LoadingFailedException error) {
			ServerModLoader.hasErrors = true;
			// In case its not loaded properly
			LanguageHook.loadForgeAndMCLangs();
			CrashReportExtender.dumpModLoadingCrashReport(LOGGER, error, new File("."));
			throw error;
		}
		List<ModLoadingWarning> warnings = ModLoader.get().getWarnings();
		if (!warnings.isEmpty()) {
			LOGGER.warn(LOADING, "Mods loaded with {} warnings", warnings.size());
			warnings.forEach(warning -> LOGGER.warn(LOADING, warning.formatToString()));
		}
		MinecraftForge.EVENT_BUS.start();
	}

	public static boolean hasErrors() {
		return ServerModLoader.hasErrors;
	}
}
