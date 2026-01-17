package dev.lambdaurora.aurorasdeco.world.gen.feature;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents a registrable {@link PlacedFeature} to which metadata is assigned to ease biome selection.
 * Ported from Quilt to Fabric.
 */
public class PlacedFeatureMetadata {
    private final RegistryKey<PlacedFeature> key;
    private final List<TagKey<Biome>> allowedCategoryTags = new ArrayList<>();
    private final List<Biome.Precipitation> allowedPrecipitations = new ArrayList<>();
    private final List<RegistryKey<ConfiguredFeature<?, ?>>> allowedNeighborFeatures = new ArrayList<>();
    private TagKey<Biome> allowedTag;

    public PlacedFeatureMetadata(Identifier key) {
        this.key = RegistryKey.of(RegistryKeys.PLACED_FEATURE, key);
    }

    public RegistryKey<PlacedFeature> getKey() {
        return this.key;
    }

    @SafeVarargs
    public final PlacedFeatureMetadata addAllowedBiomeCategoryTag(TagKey<Biome>... categories) {
        this.allowedCategoryTags.addAll(Arrays.asList(categories));
        return this;
    }

    public Predicate<Biome> getAllowedBiomeCategoriesPredicate() {
        if (this.allowedCategoryTags.isEmpty())
            return biome -> true;

        Predicate<Biome> last = null;

        for (var tag : this.allowedCategoryTags) {
            var p = BiomeSelectors.isIn(tag)::test; // converts BiomeSelector to Predicate<Biome>

            if (last == null) {
                last = p;
                continue;
            }

            last = last.or(p);
        }

        return last;
    }

    public PlacedFeatureMetadata addAllowedPrecipitation(Biome.Precipitation... precipitations) {
        this.allowedPrecipitations.addAll(Arrays.asList(precipitations));
        return this;
    }

    public Predicate<Biome> getAllowedPrecipitationsPredicate() {
        if (this.allowedPrecipitations.isEmpty())
            return biome -> true;

        return biome -> this.allowedPrecipitations.contains(biome.getPrecipitation());
    }

    public PlacedFeatureMetadata addAllowedNeighborFeature(Identifier key) {
        return this.addAllowedNeighborFeature(RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, key));
    }

    public PlacedFeatureMetadata addAllowedNeighborFeature(RegistryKey<ConfiguredFeature<?, ?>> key) {
        this.allowedNeighborFeatures.add(key);
        return this;
    }

    public Predicate<Biome> getAllowedNeighborFeaturesPredicate() {
        if (this.allowedNeighborFeatures.isEmpty())
            return biome -> true;

        // Fabric does not provide BiomeSelectionContext.hasFeature(), so you may need to skip this
        // or implement a custom lookup if you track features yourself
        return biome -> true; // Placeholder: implement your own feature check if necessary
    }

    public PlacedFeatureMetadata setAllowedTag(TagKey<Biome> tag) {
        this.allowedTag = tag;
        return this;
    }

    public Predicate<Biome> getTagPredicate() {
        if (this.allowedTag != null)
            return BiomeSelectors.isIn(this.allowedTag)::test;
        else
            return biome -> false;
    }

    public Predicate<Biome> getBiomeSelectionPredicate() {
        if (this.allowedCategoryTags.isEmpty() && this.allowedPrecipitations.isEmpty() && this.allowedNeighborFeatures.isEmpty())
            return this.getTagPredicate();

        return this.getAllowedBiomeCategoriesPredicate()
                .and(this.getAllowedPrecipitationsPredicate()
                        .and(this.getAllowedNeighborFeaturesPredicate()))
                .or(this.getTagPredicate());
    }
}