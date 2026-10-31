package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.entity.EntityRef;

/**
 * Renders an entity. The output starts at the entity position (feet), not rotated; apply the
 * entity yaw yourself (vanilla convention: {@code rotateY(180 - yaw)} makes model +Z face forward).
 */
@FunctionalInterface
public interface EntityRenderer {
    void render(EntityRef entity, float partialTick, RenderOutput out);
}
