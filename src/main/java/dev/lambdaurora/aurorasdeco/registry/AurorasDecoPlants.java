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

package dev.lambdaurora.aurorasdeco.registry;

import dev.lambdaurora.aurorasdeco.block.BurntVineBlock;
import dev.lambdaurora.aurorasdeco.block.DirectionalFlowerPotBlock;
import dev.lambdaurora.aurorasdeco.block.plant.DaffodilBlock;
import dev.lambdaurora.aurorasdeco.block.plant.DuckweedBlock;
import dev.lambdaurora.aurorasdeco.block.plant.LavenderBlock;
import dev.lambdaurora.aurorasdeco.item.DuckweedItem;
import dev.lambdaurora.aurorasdeco.world.gen.feature.AurorasDecoTreeConfiguredFeatures;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.item.Item;

import java.util.List;
import java.util.Optional;

import static dev.lambdaurora.aurorasdeco.registry.AurorasDecoRegistry.registerBlock;
import static dev.lambdaurora.aurorasdeco.registry.AurorasDecoRegistry.registerWithItem;

/**
 * Contains the different plants definitions added in Aurora's Decorations.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AurorasDecoPlants {
	private AurorasDecoPlants() {
		throw new UnsupportedOperationException("AurorasDecoPlants only contains static definitions.");
	}

	static void init() {}

	public static final List<BlockState> FLOWER_FOREST_PLANTS;

	/* Plants */

	public static final Registrar.BlockEntry<DaffodilBlock> DAFFODIL = Registrar.register("daffodil", new DaffodilBlock())
			.withItem(new Item.Settings(), BlockItem::new)
			.finish();

	public static final Registrar.BlockEntry<LavenderBlock> LAVENDER = Registrar.register("lavender", new LavenderBlock())
			.withItem(new Item.Settings(), BlockItem::new)
			.finish();

	public static final Registrar.BlockEntry<DuckweedBlock> DUCKWEED = Registrar.register("duckweed", new DuckweedBlock())
			.withItem(new Item.Settings(), DuckweedItem::new)
			.finish();

	/* Burnt Plants */

	public static final BurntVineBlock BURNT_VINE_BLOCK = registerBlock("burnt_vine", new BurntVineBlock());

	/* Potted Plants */

	public static final FlowerPotBlock POTTED_DAFFODIL = registerBlock("potted/daffodil",
			new DirectionalFlowerPotBlock(DAFFODIL.block(), FabricBlockSettings.copyOf(Blocks.FLOWER_POT)));

	public static final FlowerPotBlock POTTED_LAVENDER = registerBlock("potted/lavender",
			new FlowerPotBlock(LAVENDER.block(), FabricBlockSettings.copyOf(Blocks.FLOWER_POT)));

	/* Saplings */

	/*
	 * SaplingGenerator became final and data-driven in 1.21 -- it can no longer be subclassed to
	 * override getTreeFeature(Random, boolean) (java/CLAUDE.md §3o). Its own randomized-choice logic
	 * (SaplingGenerator.getSmallTreeFeature, confirmed by disassembly) is:
	 * "if (random.nextFloat() < rareChance) pick the rare* variant else the regular* variant, falling
	 * back to the non-bees variant if the bees-specific one is absent" -- exactly reproducing the
	 * original 50/50 random.nextBoolean() split between jacaranda/flowering_jacaranda by using
	 * rareChance = 0.5f with the "regular" slot holding plain jacaranda and the "rare" slot holding
	 * flowering_jacaranda. No mega (giant tree) variant exists for jacaranda.
	 */
	private static final SaplingGenerator JACARANDA_SAPLING_GENERATOR = new SaplingGenerator(
			"jacaranda",
			0.5f,
			Optional.empty(),
			Optional.empty(),
			Optional.of(AurorasDecoTreeConfiguredFeatures.JACARANDA_TREE),
			Optional.of(AurorasDecoTreeConfiguredFeatures.FLOWERING_JACARANDA_TREE),
			Optional.of(AurorasDecoTreeConfiguredFeatures.JACARANDA_TREE_BEES_015),
			Optional.of(AurorasDecoTreeConfiguredFeatures.FLOWERING_JACARANDA_TREE_BEES_015)
	);

	public static final SaplingBlock JACARANDA_SAPLING = registerWithItem("jacaranda_sapling",
			new SaplingBlock(JACARANDA_SAPLING_GENERATOR, FabricBlockSettings.copyOf(Blocks.OAK_SAPLING)),
			new Item.Settings()
	);

	public static final FlowerPotBlock POTTED_JACARANDA_SAPLING = registerBlock("potted/jacaranda_sapling",
			new FlowerPotBlock(JACARANDA_SAPLING, FabricBlockSettings.copyOf(Blocks.FLOWER_POT)));

	/* Leaves */

	public static final LeavesBlock JACARANDA_LEAVES = registerWithItem("jacaranda_leaves",
			new LeavesBlock(FabricBlockSettings.copyOf(Blocks.BIRCH_LEAVES)),
			new Item.Settings());
	public static final LeavesBlock BUDDING_JACARANDA_LEAVES = registerWithItem("budding_jacaranda_leaves",
			new LeavesBlock(FabricBlockSettings.copyOf(Blocks.FLOWERING_AZALEA_LEAVES)),
			new Item.Settings());
	public static final LeavesBlock FLOWERING_JACARANDA_LEAVES = registerWithItem("flowering_jacaranda_leaves",
			new LeavesBlock(FabricBlockSettings.copyOf(BUDDING_JACARANDA_LEAVES)),
			new Item.Settings());

	static {
		FLOWER_FOREST_PLANTS = List.of(DAFFODIL.getDefaultState(), LAVENDER.getDefaultState());
	}
}
