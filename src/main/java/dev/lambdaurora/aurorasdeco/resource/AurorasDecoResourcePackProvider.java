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

package dev.lambdaurora.aurorasdeco.resource;

import dev.lambdaurora.aurorasdeco.AurorasDeco;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackPosition;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Registers every pack from {@link AurorasDecoPack#getDefaultPacks()} as an always-enabled resource
 * pack. Fabric has no {@code registerDefaultResourcePackEvent}-style extension point for this (QSL
 * did), so it's wired in via {@code ResourcePackManagerMixin} instead -- see that class and
 * java/CLAUDE.md §3/Task #18 for why. Unverified against a real build/run.
 */
public class AurorasDecoResourcePackProvider implements ResourcePackProvider {
	@Override
	public void register(Consumer<ResourcePackProfile> consumer) {
		for (var pack : AurorasDecoPack.getDefaultPacks()) {
			var name = "aurorasdeco_dynamic_" + pack.getType().name().toLowerCase();
			var info = new ResourcePackInfo(name, Text.literal("Aurora's Decorations Dynamic Data"),
					ResourcePackSource.BUILTIN, Optional.empty());

			consumer.accept(ResourcePackProfile.create(
					info,
					new ResourcePackProfile.PackFactory() {
						@Override
						public ResourcePack open(ResourcePackInfo info) {
							return pack;
						}

						@Override
						public ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata) {
							return pack;
						}
					},
					pack.getType(),
					// always enabled, not user-toggleable
					new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, false)
			));
		}
	}
}
