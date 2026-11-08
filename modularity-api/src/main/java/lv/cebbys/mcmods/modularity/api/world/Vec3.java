package lv.cebbys.mcmods.modularity.api.world;

/** Immutable 3D vector in world coordinates. */
public record Vec3(double x, double y, double z) {
    public Vec3 add(double dx, double dy, double dz) {
        return new Vec3(x + dx, y + dy, z + dz);
    }
}
