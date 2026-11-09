/**
 * The few Minecraft calls whose signature differs between versions, behind stable methods. Version layers
 * ({@code platforms/minecraft/changes/<range>}, or {@code platforms/loom/changes/<range>} for the Fabric-API
 * overrides) replace these small classes instead of the large ones that use them
 * (docs/07-multi-version-build.md).
 */
package lv.cebbys.mcmods.modularity.platform.compat;
