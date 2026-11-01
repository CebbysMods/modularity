package lv.cebbys.mcmods.modularity.api.entity;

import lv.cebbys.mcmods.modularity.api.world.Vec3;

import java.util.Objects;

/**
 * One box of a multipart entity (like the Ender Dragon's head, body and wings), in entity-local
 * space ({@code +z} forward, {@code +x} left, {@code +y} up, relative to the entity position). The box
 * turns with the entity's {@linkplain EntityRef#orientation() full orientation}.
 *
 * <p>Every part can be aimed at, hit and right-clicked on its own ({@link EntityBehaviour#interactPart},
 * {@link EntityBehaviour#attackedPart}). A {@code solid} part also blocks other entities with its
 * rotated shape; entities standing on it are carried along when the entity moves and turns.
 *
 * @param name        unique within the entity, reported to the behaviour
 * @param center      box centre in entity-local space
 * @param halfExtents half size along the local x, y and z axes
 * @param solid       whether other entities collide with it and can stand on it
 */
public record EntityPart(String name, Vec3 center, Vec3 halfExtents, boolean solid) {
    public EntityPart {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(halfExtents, "halfExtents");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Part name must not be blank");
        }
        if (!(halfExtents.x() > 0 && halfExtents.y() > 0 && halfExtents.z() > 0)) {
            throw new IllegalArgumentException("Part '" + name + "' needs positive half extents, was " + halfExtents);
        }
    }

    /** Box corner furthest from the entity origin: how far the part can reach in any orientation. */
    public double reach() {
        return new Vec3(Math.abs(center.x()) + halfExtents.x(), Math.abs(center.y()) + halfExtents.y(),
                Math.abs(center.z()) + halfExtents.z()).length();
    }
}
