package lv.cebbys.mcmods.modularity.api.world;

/** Immutable 3D vector in world coordinates. */
public record Vec3(double x, double y, double z) {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public Vec3 add(double dx, double dy, double dz) {
        return new Vec3(x + dx, y + dy, z + dz);
    }

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 scale(double f) {
        return new Vec3(x * f, y * f, z * f);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double distanceTo(Vec3 o) {
        return new Vec3(x - o.x, y - o.y, z - o.z).length();
    }

    /**
     * Rotates around the Y axis by Minecraft yaw (degrees, 0 = facing +Z/south, 90 = facing -X/west).
     * Local {@code +z} is "forward", local {@code +x} is "left".
     */
    public Vec3 rotateYaw(double yawDegrees) {
        double r = Math.toRadians(yawDegrees);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        return new Vec3(x * cos - z * sin, y, x * sin + z * cos);
    }
}
