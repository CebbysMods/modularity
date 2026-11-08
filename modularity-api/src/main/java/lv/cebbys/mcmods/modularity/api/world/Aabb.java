package lv.cebbys.mcmods.modularity.api.world;

/** Axis aligned box in world coordinates. */
public record Aabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public Aabb {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Inverted box " + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ);
        }
    }

    public static Aabb of(Pos pos) {
        return new Aabb(pos.x(), pos.y(), pos.z(), pos.x() + 1, pos.y() + 1, pos.z() + 1);
    }

    /** Box centered horizontally on {@code x, z}, from {@code y} upwards. */
    public static Aabb centered(double x, double y, double z, double width, double height, double depth) {
        return new Aabb(x - width / 2, y, z - depth / 2, x + width / 2, y + height, z + depth / 2);
    }

    public Aabb move(double dx, double dy, double dz) {
        return new Aabb(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    public Aabb inflate(double amount) {
        return new Aabb(minX - amount, minY - amount, minZ - amount, maxX + amount, maxY + amount, maxZ + amount);
    }

    public boolean intersects(Aabb o) {
        return minX < o.maxX && maxX > o.minX && minY < o.maxY && maxY > o.minY && minZ < o.maxZ && maxZ > o.minZ;
    }

    public Vec3 center() {
        return new Vec3((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }
}
