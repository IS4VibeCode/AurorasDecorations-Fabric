/*
 * Copyright (c) 2023 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.aurorasdeco.mixin.world;

import dev.lambdaurora.aurorasdeco.registry.AurorasDecoBiomes;
import dev.lambdaurora.aurorasdeco.world.gen.DynamicWorldGen;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Under Quilt Mappings the constructor's second parameter type splits into an enumerable
 * {@code HolderLookup} (exposing a {@code .holders()} stream) versus a raw {@code HolderProvider}.
 * Confirmed via javap/bytecode disassembly against the real 1.20.1 Yarn jar that there's only one
 * interface here under Yarn, {@code RegistryEntryLookup} -- also confirmed the real package moved
 * from {@code net.minecraft.world.biome.util} to {@code net.minecraft.world.biome.source}, and
 * {@code RegistrySetBuilder} is Yarn's {@code RegistryBuilder}. {@code RegistryEntryLookup} has no
 * stream-all method, but its {@code getOptional(RegistryKey)} directly and safely answers "does this
 * specific key exist" without needing to enumerate everything first, so the old dual-branch logic
 * (enumerable lookup vs. try/catch on a raw provider) collapses into one path.
 */
@Mixin(MultiNoiseBiomeSourceParameterList.class)
public class MultiNoiseBiomeSourceParameterListMixin {
	@Inject(
			method = "<init>",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/biome/source/MultiNoiseBiomeSourceParameterList$Preset$BiomeSourceFunction;apply(Ljava/util/function/Function;)Lnet/minecraft/world/biome/source/util/MultiNoiseUtil$Entries;"
			)
	)
	private void aurorasdeco$onInitHead(MultiNoiseBiomeSourceParameterList.Preset preset, RegistryEntryLookup<Biome> lookup, CallbackInfo ci) {
		if (!lookup.getClass().getName().contains(RegistryBuilder.class.getName())) {
			DynamicWorldGen.markCanInjectBiomes(lookup.getOptional(AurorasDecoBiomes.LAVENDER_PLAINS).isPresent());
		}
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void aurorasdeco$onInitTail(MultiNoiseBiomeSourceParameterList.Preset preset, RegistryEntryLookup<Biome> lookup, CallbackInfo ci) {
		DynamicWorldGen.unmarkCanInjectBiomes();
	}
}
