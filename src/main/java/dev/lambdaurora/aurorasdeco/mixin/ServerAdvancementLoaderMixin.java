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

package dev.lambdaurora.aurorasdeco.mixin;

import com.google.common.collect.ImmutableMap;
import dev.lambdaurora.aurorasdeco.resource.datagen.AdvancementDatagen;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.21.1 update: {@code ServerAdvancementLoader.apply()}'s real bytecode was re-disassembled rather
 * than assuming the 1.20.1 injection point still holds. The per-JSON-entry parsing that used to be
 * inline in {@code apply()} (giving the old mixin a hookable {@code Map<Identifier, Advancement.Builder>}
 * local mid-loop) has moved into a private lambda passed to {@code Map.forEach} -- there is no longer
 * any point inside {@code apply()} itself where "advancements before they're finalized" are visible as
 * a plain local.
 * <p>
 * Instead this redirects the single {@code ImmutableMap.Builder<Identifier, AdvancementEntry>
 * .buildOrThrow()} call at the very end of {@code apply()} (confirmed unique in the method): our own
 * entries are added to the still-open builder immediately before it's finalized, which reaches the
 * exact same end state ("our advancements are present in the final immutable map") without needing to
 * hook the fragile synthetic lambda method.
 */
@Mixin(ServerAdvancementLoader.class)
public class ServerAdvancementLoaderMixin {
	@Redirect(
			method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V",
			at = @At(value = "INVOKE",
					target = "Lcom/google/common/collect/ImmutableMap$Builder;buildOrThrow()Lcom/google/common/collect/ImmutableMap;")
	)
	private ImmutableMap<Identifier, AdvancementEntry> onBuild(ImmutableMap.Builder<Identifier, AdvancementEntry> builder) {
		AdvancementDatagen.applyAdvancements(builder);
		return builder.buildOrThrow();
	}
}
