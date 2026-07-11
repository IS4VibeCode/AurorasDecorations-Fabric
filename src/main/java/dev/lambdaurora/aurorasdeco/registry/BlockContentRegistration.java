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

package dev.lambdaurora.aurorasdeco.registry;

import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * Fabric has no equivalent of Quilt's data-driven block content registry attachments
 * ({@code data/quilt/attachments/minecraft/{block,item}/*.json}), only Java-code registries.
 * This replicates the exact values that used to live in those four JSON files as direct calls
 * against Fabric API's {@link FlammableBlockRegistry}, {@link StrippableBlockRegistry},
 * {@link OxidizableBlocksRegistry}, and {@link CompostingChanceRegistry}.
 * <p>
 * Must run after all of aurorasdeco's own wood-type-derived blocks have been registered -- it looks
 * blocks/items up by id via the registries rather than referencing static fields, since several of
 * these (fence/fence_gate/planks/slab/stairs for the azalea and jacaranda wood types) don't have their
 * own named static fields and are only reachable through {@link WoodType}'s dynamic component lookup.
 */
public final class BlockContentRegistration {
	private BlockContentRegistration() {
		throw new UnsupportedOperationException("BlockContentRegistration only contains static definitions.");
	}

	public static void init() {
		registerFlammable();
		registerStrippable();
		registerWaxable();
		registerCompostingChances();
	}

	private static void registerFlammable() {
		var flammable = FlammableBlockRegistry.getDefaultInstance();

		flammable.add(block("azalea_fence"), 5, 20);
		flammable.add(block("azalea_fence_gate"), 5, 20);
		flammable.add(blockTag("azalea_logs"), 5, 5);
		flammable.add(block("azalea_planks"), 5, 20);
		flammable.add(block("azalea_slab"), 5, 20);
		flammable.add(block("azalea_stairs"), 5, 20);
		flammable.add(block("burnt_vine"), 15, 100);
		flammable.add(block("jacaranda_fence"), 5, 20);
		flammable.add(block("jacaranda_fence_gate"), 5, 20);
		flammable.add(blockTag("jacaranda_leaves"), 30, 60);
		flammable.add(blockTag("jacaranda_logs"), 5, 5);
		flammable.add(block("jacaranda_planks"), 5, 20);
		flammable.add(block("jacaranda_slab"), 5, 20);
		flammable.add(block("jacaranda_stairs"), 5, 20);
		flammable.add(blockTag("pet_beds"), 10, 30);
	}

	private static void registerStrippable() {
		StrippableBlockRegistry.register(block("azalea_log"), block("stripped_azalea_log"));
		StrippableBlockRegistry.register(block("azalea_wood"), block("stripped_azalea_wood"));
		StrippableBlockRegistry.register(block("flowering_azalea_log"), block("stripped_azalea_log"));
		StrippableBlockRegistry.register(block("flowering_azalea_wood"), block("stripped_azalea_wood"));
		StrippableBlockRegistry.register(block("jacaranda_log"), block("stripped_jacaranda_log"));
		StrippableBlockRegistry.register(block("jacaranda_wood"), block("stripped_jacaranda_wood"));
	}

	private static void registerWaxable() {
		// Original data (waxable.json) also carried "reversible": false for all three -- meaning
		// scraping wax back off was intentionally not registered. registerWaxableBlockPair only ever
		// wires the apply-honeycomb direction, so this is preserved as-is with no extra step needed.
		OxidizableBlocksRegistry.registerWaxableBlockPair(block("blackboard"), block("waxed_blackboard"));
		OxidizableBlocksRegistry.registerWaxableBlockPair(block("chalkboard"), block("waxed_chalkboard"));
		OxidizableBlocksRegistry.registerWaxableBlockPair(block("glassboard"), block("waxed_glassboard"));
	}

	private static void registerCompostingChances() {
		var compost = CompostingChanceRegistry.INSTANCE;

		compost.add(item("duckweed"), 0.3f);
		compost.add(item("daffodil"), 0.65f);
		compost.add(itemTag("jacaranda_leaves"), 0.3f);
		compost.add(item("jacaranda_sapling"), 0.3f);
		compost.add(item("lavender"), 0.65f);
	}

	private static Block block(String path) {
		return Registries.BLOCK.get(Identifier.of("aurorasdeco", path));
	}

	private static Item item(String path) {
		return Registries.ITEM.get(Identifier.of("aurorasdeco", path));
	}

	private static TagKey<Block> blockTag(String path) {
		return TagKey.of(RegistryKeys.BLOCK, Identifier.of("aurorasdeco", path));
	}

	private static TagKey<Item> itemTag(String path) {
		return TagKey.of(RegistryKeys.ITEM, Identifier.of("aurorasdeco", path));
	}
}
