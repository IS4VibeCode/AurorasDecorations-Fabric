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

import dev.lambdaurora.aurorasdeco.resource.AurorasDecoResourcePackProvider;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;

/**
 * Adds {@link AurorasDecoResourcePackProvider} to every {@link ResourcePackManager} as it's
 * constructed, regardless of who's constructing it (client or server) -- this only needs to hook the
 * manager's own constructor, not chase down every call site that builds one.
 * <p>
 * QSL's {@code getRegisterDefaultResourcePackEvent()} did this job on Quilt; Fabric has no equivalent
 * event (java/CLAUDE.md §3/Task #18). Unverified against a real build/run.
 */
@Mixin(ResourcePackManager.class)
public class ResourcePackManagerMixin {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void aurorasdeco$addProvider(ResourcePackProvider[] providers, CallbackInfo ci) {
		var accessor = (ResourcePackManagerAccessor) this;
		var newProviders = new LinkedHashSet<ResourcePackProvider>(accessor.aurorasdeco$getProviders());
		newProviders.add(new AurorasDecoResourcePackProvider());
		accessor.aurorasdeco$setProviders(newProviders);
	}
}
