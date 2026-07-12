/*
 * Copyright (c) 2021 - 2023 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.aurorasdeco.mixin;

import com.terraformersmc.terraform.boat.api.TerraformBoatType;
import com.terraformersmc.terraform.boat.api.TerraformBoatTypeRegistry;
import com.terraformersmc.terraform.boat.impl.TerraformBoatTrackedData;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * terraform-boat-api's {@code TerraformBoatTrackedData.PACKET_CODEC} syncs a boat's wood type via
 * {@code PacketCodecs.registryValue(TerraformBoatTypeRegistry.INSTANCE.getKey())}. Confirmed via
 * bytecode disassembly of the real 1.21.1 jar that this codec resolves the registry through
 * {@code RegistryByteBuf#getRegistryManager()#get(RegistryKey)} -- i.e. the <b>dynamic</b>/data-driven
 * registry manager. {@code TerraformBoatTypeRegistry.INSTANCE} is a plain <b>static</b> Fabric registry
 * ({@code FabricRegistryBuilder.createSimple(...).buildAndRegister()}, confirmed identical in both the
 * 7.0.1 and 11.0.0 releases of this library -- never registered as a dynamic registry), so that lookup
 * can never succeed. Observed live under Sinytra Connector on NeoForge 1.21.1:
 * {@code IllegalStateException: Cannot use ID syncing for non-synced built-in registry:
 * ResourceKey[minecraft:root / terraform:boat]}, disconnecting the client the moment an azalea/
 * jacaranda boat entered tracking range (a {@code ClientboundSetEntityDataPacket} encode failure).
 * This is a bug in terraform-boat-api's own sync code, not anything specific to this port or to the
 * McWorldConverter world conversion -- it's latent regardless of source/target world content, and
 * likely papered over on real Fabric by something Connector doesn't replicate (not independently
 * confirmed here, no live Fabric server available to test against).
 * <p>
 * Fix: redirect the static initializer's call to {@code registryValue(key)} to
 * {@code PacketCodecs.entryOf(TerraformBoatTypeRegistry.INSTANCE)} instead. {@code entryOf} codes by
 * raw ID against a <i>given</i> {@link net.minecraft.util.collection.IndexedIterable} directly
 * (confirmed via bytecode: {@code Registry<T> extends IndexedIterable<T>}, and {@code entryOf}'s
 * generated codec does the exact same {@code VarInts.write/read} of the raw id that
 * {@code registryValue}'s codec does -- byte-for-byte identical wire format) -- with no per-connection
 * dynamic-registry lookup at all, so the "must be synced" restriction never applies. Since this is the
 * same static registry instance on both client and server (each side's terraform-boat-api registers
 * boat types in the same order at startup), raw IDs still agree across the connection.
 */
@Mixin(TerraformBoatTrackedData.class)
public class TerraformBoatTrackedDataMixin {
	@Redirect(
			method = "<clinit>",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/codec/PacketCodecs;registryValue(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/network/codec/PacketCodec;"
			)
	)
	private static PacketCodec<RegistryByteBuf, TerraformBoatType> useStaticRegistryEntryCodec(
			RegistryKey<? extends Registry<TerraformBoatType>> key
	) {
		return PacketCodecs.entryOf(TerraformBoatTypeRegistry.INSTANCE).cast();
	}
}
