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

import dev.lambdaurora.aurorasdeco.resource.AurorasDecoPack;
import dev.lambdaurora.aurorasdeco.resource.AurorasDecoResourcePackProvider;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Adds {@link AurorasDecoResourcePackProvider} to every {@link ResourcePackManager} as it's
 * constructed, regardless of who's constructing it (client or server) -- this only needs to hook the
 * manager's own constructor, not chase down every call site that builds one.
 * <p>
 * QSL's {@code getRegisterDefaultResourcePackEvent()} did this job on Quilt; Fabric has no equivalent
 * event (java/CLAUDE.md §3/Task #18).
 * <p>
 * <b>The provider-based registration above was confirmed NOT sufficient by a real load test</b>: the
 * dynamic pack's synthetic name never appeared in vanilla's own "Reloading ResourceManager: {}" log
 * line (confirmed by disassembling {@code ReloadableResourceManagerImpl.reload}, which logs exactly
 * the {@code packs} list it's about to build a {@code LifecycledResourceManagerImpl} from), meaning
 * every dynamically-generated blockstate/model/tag this mod writes into {@link AurorasDecoPack} was
 * invisible to every resource lookup, regardless of the content being correctly generated in memory.
 * Root cause not fully isolated (a same-target-class mixin-ordering clobber from another mod's own
 * dynamic-pack mechanism -- this pack runs under Quilt Loader alongside several mods with similar
 * "always-active virtual pack" designs -- is the leading suspect, but not confirmed), so rather than
 * chase that further this class now <em>also</em> force-appends every default pack directly onto
 * {@link ResourcePackManager#createResourcePacks()}'s own return value -- a single choke point every
 * resource/data reload (client and server alike) unconditionally passes through right before building
 * the {@code ResourcePack} list a reload actually uses, bypassing the providers/scanPacks/alwaysEnabled
 * chain (and whatever is silently defeating it) entirely. The provider registration is left in place
 * since it's harmless and keeps the pack visible/correctly-labeled in the resource pack selection UI.
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

	@Inject(method = "createResourcePacks", at = @At("RETURN"), cancellable = true)
	private void aurorasdeco$forceAppendDynamicPacks(CallbackInfoReturnable<List<ResourcePack>> cir) {
		var defaultPacks = AurorasDecoPack.getDefaultPacks();
		if (defaultPacks.isEmpty()) return;

		var packs = new ArrayList<ResourcePack>(cir.getReturnValue());
		packs.addAll(defaultPacks);
		cir.setReturnValue(packs);
	}
}
