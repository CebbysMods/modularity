package lv.cebbys.mcmods.modularity.api.world;

/** Immutable block position. */
public record Pos(int x, int y, int z) {
    public Pos offset(Direction direction) {
        return new Pos(x + direction.stepX(), y + direction.stepY(), z + direction.stepZ());
    }

    public Pos offset(int dx, int dy, int dz) {
        return new Pos(x + dx, y + dy, z + dz);
    }

    public Vec3 center() {
        return new Vec3(x + 0.5D, y + 0.5D, z + 0.5D);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
