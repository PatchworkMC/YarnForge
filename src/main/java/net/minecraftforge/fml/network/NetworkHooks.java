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

package net.minecraftforge.fml.network;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.config.ConfigTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.TagGroup;
import net.minecraft.tag.TagManager;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class NetworkHooks {
	private static final Logger LOGGER = LogManager.getLogger();

	public static String getFMLVersion(final String ip) {
		return ip.contains("\0") ? Objects.equals(ip.split("\0")[1], FMLNetworkConstants.NETVERSION) ? FMLNetworkConstants.NETVERSION : ip.split("\0")[1] : FMLNetworkConstants.NOVERSION;
	}

	public static ConnectionType getConnectionType(final Supplier<ClientConnection> connection) {
		return getConnectionType(connection.get().channel());
	}

	public static ConnectionType getConnectionType(ChannelHandlerContext context) {
		return getConnectionType(context.channel());
	}

	private static ConnectionType getConnectionType(Channel channel) {
		return ConnectionType.forVersionFlag(channel.attr(FMLNetworkConstants.FML_NETVERSION).get());
	}

	public static Packet<?> getEntitySpawningPacket(Entity entity) {
		return FMLNetworkConstants.playChannel.toVanillaPacket(new FMLPlayMessages.SpawnEntity(entity), NetworkDirection.PLAY_TO_CLIENT);
	}

	public static boolean onCustomPayload(final ICustomPacket<?> packet, final ClientConnection manager) {
		return NetworkRegistry.findTarget(packet.getName()).
			filter(ni -> validateSideForProcessing(packet, ni, manager)).
			map(ni -> ni.dispatch(packet.getDirection(), packet, manager)).orElse(Boolean.FALSE);
	}

	private static boolean validateSideForProcessing(final ICustomPacket<?> packet, final NetworkInstance ni, final ClientConnection manager) {
		if (packet.getDirection().getReceptionSide() != EffectiveSide.get()) {
			manager.disconnect(new LiteralText("Illegal packet received, terminating connection"));
			return false;
		}
		return true;
	}

	public static void validatePacketDirection(final NetworkDirection packetDirection, final Optional<NetworkDirection> expectedDirection, final ClientConnection connection) {
		if (packetDirection != expectedDirection.orElse(packetDirection)) {
			connection.disconnect(new LiteralText("Illegal packet received, terminating connection"));
			throw new IllegalStateException("Invalid packet received, aborting connection");
		}
	}

	public static void registerServerLoginChannel(ClientConnection manager, HandshakeC2SPacket packet) {
		manager.channel().attr(FMLNetworkConstants.FML_NETVERSION).set(packet.getFMLVersion());
		FMLHandshakeHandler.registerHandshake(manager, NetworkDirection.LOGIN_TO_CLIENT);
	}

	public synchronized static void registerClientLoginChannel(ClientConnection manager) {
		manager.channel().attr(FMLNetworkConstants.FML_NETVERSION).set(FMLNetworkConstants.NOVERSION);
		FMLHandshakeHandler.registerHandshake(manager, NetworkDirection.LOGIN_TO_SERVER);
	}

	public synchronized static void sendMCRegistryPackets(ClientConnection manager, String direction) {
		final Set<Identifier> resourceLocations = NetworkRegistry.buildChannelVersions().keySet().stream().
			filter(rl -> !Objects.equals(rl.getNamespace(), "minecraft")).
			collect(Collectors.toSet());
		FMLMCRegisterPacketHandler.INSTANCE.addChannels(resourceLocations, manager);
		FMLMCRegisterPacketHandler.INSTANCE.sendRegistry(manager, NetworkDirection.valueOf(direction));
	}

	//TODO Dimensions..
/*    public synchronized static void sendDimensionDataPacket(NetworkManager manager, ServerPlayerEntity player) {
        // don't send vanilla dims
        if (player.dimension.isVanilla()) return;
        // don't sent to local - we already have a valid dim registry locally
        if (manager.isLocalChannel()) return;
        FMLNetworkConstants.playChannel.sendTo(new FMLPlayMessages.DimensionInfoMessage(player.dimension), manager, NetworkDirection.PLAY_TO_CLIENT);
    }*/

	public static boolean isVanillaConnection(ClientConnection manager) {
		if (manager == null || manager.channel() == null) {
			throw new NullPointerException("ARGH! Network Manager is null (" + manager != null ? "CHANNEL" : "MANAGER" + ")");
		}
		return getConnectionType(() -> manager) == ConnectionType.VANILLA;
	}

	public static void handleClientLoginSuccess(ClientConnection manager) {
		if (isVanillaConnection(manager)) {
			LOGGER.info("Connected to a vanilla server. Catching up missing behaviour.");
			ConfigTracker.INSTANCE.loadDefaultServerConfigs();
		} else {
			LOGGER.info("Connected to a modded server.");
		}
	}

	public static boolean tickNegotiation(ServerLoginNetworkHandler netHandlerLoginServer, ClientConnection networkManager, ServerPlayerEntity player) {
		return FMLHandshakeHandler.tickLogin(networkManager);
	}

	/**
	 * Request to open a GUI on the client, from the server
	 *
	 * Refer to {@link net.minecraftforge.fml.ExtensionPoint#GUIFACTORY} for how to provide a function to consume
	 * these GUI requests on the client.
	 *
	 * The {@link IInteractionObject#getGuiID()} is treated as a {@link ResourceLocation}.
	 * It should refer to a valid modId namespace, to trigger opening on the client.
	 * The namespace is directly used to lookup the modId in the client side.
	 *
	 * @param player The player to open the GUI for
	 * @param containerSupplier A supplier of container properties including the registry name of the container
	 */
	public static void openGui(ServerPlayerEntity player, NamedScreenHandlerFactory containerSupplier) {
		openGui(player, containerSupplier, buf -> {});
	}

	/**
	 * Request to open a GUI on the client, from the server
	 *
	 * Refer to {@link net.minecraftforge.fml.ExtensionPoint#GUIFACTORY} for how to provide a function to consume
	 * these GUI requests on the client.
	 *
	 * The {@link IInteractionObject#getGuiID()} is treated as a {@link ResourceLocation}.
	 * It should refer to a valid modId namespace, to trigger opening on the client.
	 * The namespace is directly used to lookup the modId in the client side.
	 *
	 * @param player The player to open the GUI for
	 * @param containerSupplier A supplier of container properties including the registry name of the container
	 * @param pos A block pos, which will be encoded into the auxillary data for this request
	 */
	public static void openGui(ServerPlayerEntity player, NamedScreenHandlerFactory containerSupplier, BlockPos pos) {
		openGui(player, containerSupplier, buf -> buf.writeBlockPos(pos));
	}

	/**
	 * Request to open a GUI on the client, from the server
	 *
	 * Refer to {@link net.minecraftforge.fml.ExtensionPoint#GUIFACTORY} for how to provide a function to consume
	 * these GUI requests on the client.
	 *
	 * The {@link IInteractionObject#getGuiID()} is treated as a {@link ResourceLocation}.
	 * It should refer to a valid modId namespace, to trigger opening on the client.
	 * The namespace is directly used to lookup the modId in the client side.
	 * The maximum size for #extraDataWriter is 32600 bytes.
	 *
	 * @param player The player to open the GUI for
	 * @param containerSupplier A supplier of container properties including the registry name of the container
	 * @param extraDataWriter Consumer to write any additional data the GUI needs
	 */
	public static void openGui(ServerPlayerEntity player, NamedScreenHandlerFactory containerSupplier, Consumer<PacketByteBuf> extraDataWriter) {
		if (player.world.isClient) {
			return;
		}
		player.closeScreenHandler();
		player.incrementScreenHandlerSyncId();
		int openContainerId = player.screenHandlerSyncId;
		PacketByteBuf extraData = new PacketByteBuf(Unpooled.buffer());
		extraDataWriter.accept(extraData);
		extraData.readerIndex(0); // reset to beginning in case modders read for whatever reason

		PacketByteBuf output = new PacketByteBuf(Unpooled.buffer());
		output.writeVarInt(extraData.readableBytes());
		output.writeBytes(extraData);

		if (output.readableBytes() > 32600 || output.readableBytes() < 1) {
			throw new IllegalArgumentException("Invalid PacketBuffer for openGui, found " + output.readableBytes() + " bytes");
		}
		ScreenHandler c = containerSupplier.createMenu(openContainerId, player.inventory, player);
		ScreenHandlerType<?> type = c.getType();
		FMLPlayMessages.OpenContainer msg = new FMLPlayMessages.OpenContainer(type, openContainerId, containerSupplier.getDisplayName(), output);
		FMLNetworkConstants.playChannel.sendTo(msg, player.networkHandler.getConnection(), NetworkDirection.PLAY_TO_CLIENT);

		player.currentScreenHandler = c;
		player.currentScreenHandler.addListener(player);
		MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, c));
	}

	/**
	 * Syncs the custom tag types attached to a {@link ITagCollectionSupplier} to all connected players.
	 * @param tagCollectionSupplier The tag collection supplier containing the custom tags
	 */
	public static void syncCustomTagTypes(TagManager tagCollectionSupplier) {
		Map<Identifier, TagGroup<?>> customTagTypes = tagCollectionSupplier.getCustomTagTypes();
		if (!customTagTypes.isEmpty()) {
			FMLNetworkConstants.playChannel.send(PacketDistributor.ALL.noArg(), new FMLPlayMessages.SyncCustomTagTypes(customTagTypes));
		}
	}

	/**
	 * Syncs the custom tag types attached to a {@link ITagCollectionSupplier} to the given player.
	 * @param player                The player to sync the custom tags to.
	 * @param tagCollectionSupplier The tag collection supplier containing the custom tags
	 */
	public static void syncCustomTagTypes(ServerPlayerEntity player, TagManager tagCollectionSupplier) {
		Map<Identifier, TagGroup<?>> customTagTypes = tagCollectionSupplier.getCustomTagTypes();
		if (!customTagTypes.isEmpty()) {
			FMLNetworkConstants.playChannel.sendTo(new FMLPlayMessages.SyncCustomTagTypes(customTagTypes), player.networkHandler.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
		}
	}
}
