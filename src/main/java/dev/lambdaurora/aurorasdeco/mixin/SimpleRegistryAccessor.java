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

import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Quilt Mappings names this field {@code entryToIntrusiveHolder}; the real Yarn field (confirmed via
 * javap against the real 1.20.1 Yarn jar) is {@code intrusiveValueToEntry}, of the equivalent type
 * {@code Map<T, RegistryEntry.Reference<T>>} (Yarn's {@code RegistryEntry} is Quilt's {@code Holder}).
 */
@Mixin(SimpleRegistry.class)
public interface SimpleRegistryAccessor<T> {
	@Accessor("intrusiveValueToEntry")
	@Nullable Map<T, RegistryEntry.Reference<T>> getEntryToIntrusiveHolder();
}
