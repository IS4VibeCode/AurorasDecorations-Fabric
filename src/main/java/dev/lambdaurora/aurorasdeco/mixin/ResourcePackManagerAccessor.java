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

package dev.lambdaurora.aurorasdeco.mixin;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * {@code providers} is set via {@code ImmutableSet.copyOf(...)} in the constructor (confirmed by
 * disassembling the real 1.20.1 class) -- {@code @Mutable} lets {@code ResourcePackManagerMixin}
 * replace the whole field with a new set rather than needing to mutate an immutable one in place.
 */
@Mixin(ResourcePackManager.class)
public interface ResourcePackManagerAccessor {
	@Accessor("providers")
	@Mutable
	void aurorasdeco$setProviders(Set<ResourcePackProvider> providers);

	@Accessor("providers")
	Set<ResourcePackProvider> aurorasdeco$getProviders();
}
