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

package dev.lambdaurora.aurorasdeco.mixin.client;

import dev.lambdaurora.aurorasdeco.registry.AurorasDecoTags;
import net.minecraft.client.gui.screen.ingame.SmithingScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(SmithingScreen.class)
public class SmithingScreenMixin {
	/**
	 * Quilt Mappings names this field {@code display} and the method it's set up in
	 * {@code displayStack}; the real Yarn names (confirmed via javap against the real 1.20.1 jar) are
	 * {@code armorStand} and {@code equipArmorStand}. {@code equipArmorStand} clears every slot to
	 * {@code ItemStack.EMPTY} first, then either equips the stack into its real armor slot (if it's an
	 * {@link net.minecraft.item.ArmorItem}) or into {@code OFFHAND} otherwise -- a blackboard isn't an
	 * {@code ArmorItem}, so it lands in {@code OFFHAND} by vanilla logic before this injection moves it
	 * to {@code HEAD} instead. Injecting at {@code TAIL} rather than replicating the original's
	 * ordinal-counted {@code @At(INVOKE, ...)} target is deliberate: the original targeted one specific
	 * {@code equipStack} call among several in this method by bytecode position, which is fragile
	 * across mapping/version changes; {@code TAIL} achieves the identical effect (override after all of
	 * vanilla's own equip logic has run) without depending on exactly which branch's call site ordinal
	 * survived the reshuffled method.
	 */
	@Shadow
	private @Nullable ArmorStandEntity armorStand;

	@Inject(method = "equipArmorStand", at = @At("TAIL"))
	private void aurorasdeco$onArmorStandPreview(ItemStack stack, CallbackInfo ci) {
		if (stack.isIn(AurorasDecoTags.BLACKBOARD_ITEMS)) {
			this.armorStand.equipStack(EquipmentSlot.HEAD, stack);
			this.armorStand.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		}
	}
}
