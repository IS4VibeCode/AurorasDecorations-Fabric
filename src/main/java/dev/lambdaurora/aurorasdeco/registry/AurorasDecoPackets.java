/*
 * Copyright (c) 2021 LambdAurora <email@lambdaurora.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.aurorasdeco.registry;

import dev.lambdaurora.aurorasdeco.AurorasDeco;
import dev.lambdaurora.aurorasdeco.client.screen.SignPostEditScreen;
import dev.lambdaurora.aurorasdeco.item.PainterPaletteItem;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Optional;

/**
 * Contains the different packet definitions used in Aurora's Decorations.
 * <p>
 * 1.21.1 update: Fabric API's networking layer moved from raw {@code Identifier} + {@code PacketByteBuf}
 * receivers to typed {@link CustomPayload} records, each with its own {@link CustomPayload.Id} and
 * {@link PacketCodec}, registered once via {@link PayloadTypeRegistry} before either side's
 * {@code registerGlobalReceiver} can be used.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AurorasDecoPackets {
	private AurorasDecoPackets() {
		throw new UnsupportedOperationException("Someone tried to instantiate a static-only class. How?");
	}

	public record SignPostOpenGuiPayload(BlockPos pos) implements CustomPayload {
		public static final CustomPayload.Id<SignPostOpenGuiPayload> ID =
				new CustomPayload.Id<>(AurorasDeco.id("sign_post/open_gui"));
		public static final PacketCodec<RegistryByteBuf, SignPostOpenGuiPayload> CODEC = PacketCodec.tuple(
				BlockPos.PACKET_CODEC, SignPostOpenGuiPayload::pos,
				SignPostOpenGuiPayload::new
		);

		@Override
		public CustomPayload.Id<SignPostOpenGuiPayload> getId() {
			return ID;
		}
	}

	public record SignPostOpenGuiFailPayload(BlockPos pos) implements CustomPayload {
		public static final CustomPayload.Id<SignPostOpenGuiFailPayload> ID =
				new CustomPayload.Id<>(AurorasDeco.id("sign_post/open_gui/fail"));
		public static final PacketCodec<RegistryByteBuf, SignPostOpenGuiFailPayload> CODEC = PacketCodec.tuple(
				BlockPos.PACKET_CODEC, SignPostOpenGuiFailPayload::pos,
				SignPostOpenGuiFailPayload::new
		);

		@Override
		public CustomPayload.Id<SignPostOpenGuiFailPayload> getId() {
			return ID;
		}
	}

	public record SignPostSetTextPayload(BlockPos pos, byte mode, Optional<String> upText, Optional<String> downText)
			implements CustomPayload {
		public static final CustomPayload.Id<SignPostSetTextPayload> ID =
				new CustomPayload.Id<>(AurorasDeco.id("sign_post/set_text"));
		public static final PacketCodec<RegistryByteBuf, SignPostSetTextPayload> CODEC = PacketCodec.tuple(
				BlockPos.PACKET_CODEC, SignPostSetTextPayload::pos,
				PacketCodecs.BYTE, SignPostSetTextPayload::mode,
				PacketCodecs.optional(PacketCodecs.STRING), SignPostSetTextPayload::upText,
				PacketCodecs.optional(PacketCodecs.STRING), SignPostSetTextPayload::downText,
				SignPostSetTextPayload::new
		);

		@Override
		public CustomPayload.Id<SignPostSetTextPayload> getId() {
			return ID;
		}
	}

	public record PainterPaletteScrollPayload(double scrollDelta, boolean toolModifier) implements CustomPayload {
		public static final CustomPayload.Id<PainterPaletteScrollPayload> ID =
				new CustomPayload.Id<>(AurorasDeco.id("painter_palette/scroll"));
		public static final PacketCodec<RegistryByteBuf, PainterPaletteScrollPayload> CODEC = PacketCodec.tuple(
				PacketCodecs.DOUBLE, PainterPaletteScrollPayload::scrollDelta,
				PacketCodecs.BOOL, PainterPaletteScrollPayload::toolModifier,
				PainterPaletteScrollPayload::new
		);

		@Override
		public CustomPayload.Id<PainterPaletteScrollPayload> getId() {
			return ID;
		}
	}

	/**
	 * Registers every payload's codec. Must run on both the client and the server before either side's
	 * {@code registerGlobalReceiver}/{@code send} can be used for these payloads.
	 */
	public static void init() {
		PayloadTypeRegistry.playS2C().register(SignPostOpenGuiPayload.ID, SignPostOpenGuiPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SignPostOpenGuiFailPayload.ID, SignPostOpenGuiFailPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SignPostSetTextPayload.ID, SignPostSetTextPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PainterPaletteScrollPayload.ID, PainterPaletteScrollPayload.CODEC);
	}

	public static void handleSignPostOpenGuiFailPacket(SignPostOpenGuiFailPayload payload, ServerPlayNetworking.Context context) {
		var player = context.player();

		context.server().execute(() -> {
			var signPost = AurorasDecoRegistry.SIGN_POST_BLOCK_ENTITY_TYPE.get(player.getWorld(), payload.pos());
			if (signPost == null)
				return; // Sign Post is not here.

			signPost.cancelEditing(player);
		});
	}

	public static void handleSignPostSetTextPacket(SignPostSetTextPayload payload, ServerPlayNetworking.Context context) {
		var player = context.player();
		var pos = payload.pos();
		String upText = ((payload.mode() & 1) == 1) ? payload.upText().orElse(null) : null;
		String downText = ((payload.mode() & 2) == 2) ? payload.downText().orElse(null) : null;

		context.server().execute(() -> {
			if (!player.getAbilities().allowModifyWorld)
				return; // Avoid griefing.

			var signPost = AurorasDecoRegistry.SIGN_POST_BLOCK_ENTITY_TYPE.get(player.getWorld(), pos);
			if (signPost == null)
				return; // Sign Post is not here.

			if (player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) >= 16) {
				signPost.finishEditing(player, null, null);
				return; // Be close.
			}

			signPost.finishEditing(player, upText, downText);
		});
	}

	public static void handlePainterPaletteScroll(PainterPaletteScrollPayload payload, ServerPlayNetworking.Context context) {
		var player = context.player();

		context.server().execute(() -> {
			if (player.getMainHandStack().getItem() instanceof PainterPaletteItem paletteItem) {
				paletteItem.onScroll(player, player.getMainHandStack(), payload.scrollDelta(), payload.toolModifier());
			}
		});
	}

	@Environment(EnvType.CLIENT)
	public static final class Client {
		private Client() {
			throw new UnsupportedOperationException("Someone tried to instantiate a static-only class. How?");
		}

		public static void handleSignPostOpenGuiPacket(SignPostOpenGuiPayload payload, ClientPlayNetworking.Context context) {
			var client = context.client();
			var pos = payload.pos();

			client.execute(() -> {
				var signPost = AurorasDecoRegistry.SIGN_POST_BLOCK_ENTITY_TYPE.get(client.world, pos);
				if (signPost == null) {
					ClientPlayNetworking.send(new SignPostOpenGuiFailPayload(pos));
					return;
				}

				client.setScreen(new SignPostEditScreen(signPost));
			});
		}
	}
}
