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

package net.minecraftforge.fml.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ForgeI18n;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.packs.ModFileResourcePack;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

public class ClientHooks {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker CLIENTHOOKS = MarkerManager.getMarker("CLIENTHOOKS");

	private static final Identifier iconSheet = new Identifier(ForgeVersion.MOD_ID, "textures/gui/icons.png");

	@Nullable

	public static void processForgeListPingData(ServerMetadata packet, ServerInfo target) {
		if (packet.getForgeData() != null) {
			final Map<String, String> mods = packet.getForgeData().getRemoteModData();
			final Map<Identifier, Pair<String, Boolean>> remoteChannels = packet.getForgeData().getRemoteChannels();
			final int fmlver = packet.getForgeData().getFMLNetworkVersion();

			boolean fmlNetMatches = fmlver == FMLNetworkConstants.FMLNETVERSION;
			boolean channelsMatch = NetworkRegistry.checkListPingCompatibilityForClient(remoteChannels);
			AtomicBoolean result = new AtomicBoolean(true);
			final List<String> extraClientMods = new ArrayList<>();
			ModList.get().forEachModContainer((modid, mc) ->
				mc.getCustomExtension(ExtensionPoint.DISPLAYTEST).ifPresent(ext -> {
					boolean foundModOnServer = ext.getRight().test(mods.get(modid), true);
					result.compareAndSet(true, foundModOnServer);
					if (!foundModOnServer) {
						extraClientMods.add(modid);
					}
				})
			);
			boolean modsMatch = result.get();

			final Map<String, String> extraServerMods = mods.entrySet().stream().
				filter(e -> !Objects.equals(FMLNetworkConstants.IGNORESERVERONLY, e.getValue())).
				filter(e -> !ModList.get().isLoaded(e.getKey())).
				collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			LOGGER.debug(CLIENTHOOKS, "Received FML ping data from server at {}: FMLNETVER={}, mod list is compatible : {}, channel list is compatible: {}, extra server mods: {}", target.address, fmlver, modsMatch, channelsMatch, extraServerMods);

			String extraReason = null;

			if (!extraServerMods.isEmpty()) {
				extraReason = "fml.menu.multiplayer.extraservermods";
				LOGGER.info(CLIENTHOOKS, ForgeI18n.parseMessage(extraReason) + ": {}", extraServerMods.entrySet().stream()
					.map(e -> e.getKey() + "@" + e.getValue())
					.collect(Collectors.joining(", ")));
			}
			if (!modsMatch) {
				extraReason = "fml.menu.multiplayer.modsincompatible";
				LOGGER.info(CLIENTHOOKS, "Client has mods that are missing on server: {}", extraClientMods);
			}
			if (!channelsMatch) {
				extraReason = "fml.menu.multiplayer.networkincompatible";
			}

			if (fmlver < FMLNetworkConstants.FMLNETVERSION) {
				extraReason = "fml.menu.multiplayer.serveroutdated";
			}
			if (fmlver > FMLNetworkConstants.FMLNETVERSION) {
				extraReason = "fml.menu.multiplayer.clientoutdated";
			}
			target.forgeData = new ExtendedServerListData("FML", extraServerMods.isEmpty() && fmlNetMatches && channelsMatch && modsMatch, mods.size(), extraReason);
		} else {
			target.forgeData = new ExtendedServerListData("VANILLA", NetworkRegistry.canConnectToVanillaServer(), 0, null);
		}

	}

	public static void drawForgePingInfo(MultiplayerScreen gui, ServerInfo target, MatrixStack mStack, int x, int y, int width, int relativeMouseX, int relativeMouseY) {
		int idx;
		String tooltip;
		if (target.forgeData == null) {
			return;
		}
		switch (target.forgeData.type) {
			case "FML":
				if (target.forgeData.isCompatible) {
					idx = 0;
					tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.compatible", target.forgeData.numberOfMods);
				} else {
					idx = 16;
					if (target.forgeData.extraReason != null) {
						String extraReason = ForgeI18n.parseMessage(target.forgeData.extraReason);
						tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.incompatible.extra", extraReason);
					} else {
						tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.incompatible");
					}
				}
				break;
			case "VANILLA":
				if (target.forgeData.isCompatible) {
					idx = 48;
					tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.vanilla");
				} else {
					idx = 80;
					tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.vanilla.incompatible");
				}
				break;
			default:
				idx = 64;
				tooltip = ForgeI18n.parseMessage("fml.menu.multiplayer.unknown", target.forgeData.type);
		}

		MinecraftClient.getInstance().getTextureManager().bindTexture(iconSheet);
		DrawableHelper.drawTexture(mStack, x + width - 18, y + 10, 16, 16, 0, idx, 16, 16, 256, 256);

		if (relativeMouseX > width - 15 && relativeMouseX < width && relativeMouseY > 10 && relativeMouseY < 26)
		//TODO using StringTextComponent here is a hack, we should be using TranslationTextComponents.
		{
			gui.setTooltip(Collections.singletonList(new LiteralText(tooltip)));
		}

	}

	public static String fixDescription(String description) {
		return description.endsWith(":NOFML§r") ? description.substring(0, description.length() - 8) + "§r" : description;
	}

	@SuppressWarnings("resource")
	static File getSavesDir() {
		return new File(MinecraftClient.getInstance().runDirectory, "saves");
	}

	private static ClientConnection getClientToServerNetworkManager() {
		return MinecraftClient.getInstance().getNetworkHandler() != null ? MinecraftClient.getInstance().getNetworkHandler().getConnection() : null;
	}

	public static void handleClientWorldClosing(ClientWorld world) {
		ClientConnection client = getClientToServerNetworkManager();
		// ONLY revert a non-local connection
		if (client != null && !client.isLocal()) {
			GameData.revertToFrozen();
		}
	}

	private static final SetMultimap<String, Identifier> missingTextures = HashMultimap.create();
	private static final Set<String> badTextureDomains = Sets.newHashSet();
	private static final Table<String, String, Set<Identifier>> brokenTextures = HashBasedTable.create();

	public static void trackMissingTexture(Identifier resourceLocation) {
		badTextureDomains.add(resourceLocation.getNamespace());
		missingTextures.put(resourceLocation.getNamespace(), resourceLocation);
	}

	public static void trackBrokenTexture(Identifier resourceLocation, String error) {
		badTextureDomains.add(resourceLocation.getNamespace());
		Set<Identifier> badType = brokenTextures.get(resourceLocation.getNamespace(), error);
		if (badType == null) {
			badType = Sets.newHashSet();
			brokenTextures.put(resourceLocation.getNamespace(), MoreObjects.firstNonNull(error, "Unknown error"), badType);
		}
		badType.add(resourceLocation);
	}

	public static void logMissingTextureErrors() {
		if (missingTextures.isEmpty() && brokenTextures.isEmpty()) {
			return;
		}
		Logger logger = LogManager.getLogger("FML.TEXTURE_ERRORS");
		logger.error(Strings.repeat("+=", 25));
		logger.error("The following texture errors were found.");
		Map<String, NamespaceResourceManager> resManagers = ObfuscationReflectionHelper.getPrivateValue(ReloadableResourceManagerImpl.class, (ReloadableResourceManagerImpl) MinecraftClient.getInstance().getResourceManager(), "field_199014" + "_c");
		for (String resourceDomain : badTextureDomains) {
			Set<Identifier> missing = missingTextures.get(resourceDomain);
			logger.error(Strings.repeat("=", 50));
			logger.error("  DOMAIN {}", resourceDomain);
			logger.error(Strings.repeat("-", 50));
			logger.error("  domain {} is missing {} texture{}", resourceDomain, missing.size(), missing.size() != 1 ? "s" : "");
			NamespaceResourceManager fallbackResourceManager = resManagers.get(resourceDomain);
			if (fallbackResourceManager == null) {
				logger.error("    domain {} is missing a resource manager - it is probably a side-effect of automatic texture processing", resourceDomain);
			} else {
				List<ResourcePack> resPacks = fallbackResourceManager.packList;
				logger.error("    domain {} has {} location{}:", resourceDomain, resPacks.size(), resPacks.size() != 1 ? "s" : "");
				for (ResourcePack resPack : resPacks) {
					if (resPack instanceof ModFileResourcePack) {
						ModFileResourcePack modRP = (ModFileResourcePack) resPack;
						List<IModInfo> mods = modRP.getModFile().getModInfos();
						logger.error("      mod(s) {} resources at {}", mods.stream().map(IModInfo::getDisplayName).collect(Collectors.toList()), modRP.getModFile().getFilePath());
					} else if (resPack instanceof AbstractFileResourcePack) {
						logger.error("      resource pack at path {}", ((AbstractFileResourcePack) resPack).base.getPath());
					} else {
						logger.error("      unknown resourcepack type {} : {}", resPack.getClass().getName(), resPack.getName());
					}
				}
			}
			logger.error(Strings.repeat("-", 25));
			if (missingTextures.containsKey(resourceDomain)) {
				logger.error("    The missing resources for domain {} are:", resourceDomain);
				for (Identifier rl : missing) {
					logger.error("      {}", rl.getPath());
				}
				logger.error(Strings.repeat("-", 25));
			}
			if (!brokenTextures.containsRow(resourceDomain)) {
				logger.error("    No other errors exist for domain {}", resourceDomain);
			} else {
				logger.error("    The following other errors were reported for domain {}:", resourceDomain);
				Map<String, Set<Identifier>> resourceErrs = brokenTextures.row(resourceDomain);
				for (String error : resourceErrs.keySet()) {
					logger.error(Strings.repeat("-", 25));
					logger.error("    Problem: {}", error);
					for (Identifier rl : resourceErrs.get(error)) {
						logger.error("      {}", rl.getPath());
					}
				}
			}
			logger.error(Strings.repeat("=", 50));
		}
		logger.error(Strings.repeat("+=", 25));
	}

	public static void firePlayerLogin(ClientPlayerInteractionManager pc, ClientPlayerEntity player, ClientConnection networkManager) {
		MinecraftForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggedInEvent(pc, player, networkManager));
	}

	public static void firePlayerLogout(ClientPlayerInteractionManager pc, ClientPlayerEntity player) {
		MinecraftForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggedOutEvent(pc, player, player != null ? player.networkHandler != null ? player.networkHandler.getConnection() : null : null));
	}

	public static void firePlayerRespawn(ClientPlayerInteractionManager pc, ClientPlayerEntity oldPlayer, ClientPlayerEntity newPlayer, ClientConnection networkManager) {
		MinecraftForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.RespawnEvent(pc, oldPlayer, newPlayer, networkManager));
	}

}
