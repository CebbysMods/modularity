package lv.cebbys.mcmods.modularity.api.world;

/**
 * A rotation as a unit quaternion, turning entity-local vectors ({@code +z} forward, {@code +x}
 * left, {@code +y} up) into world vectors. Yaw follows Minecraft: yaw {@code θ} faces
 * {@code (-sin θ, 0, cos θ)}, which is a right-handed rotation of {@code -θ} about {@code +y}.
 */
public record Rotation(double x, double y, double z, double w) {
    public static final Rotation IDENTITY = new Rotation(0, 0, 0, 1);

    /** Rotation about {@code +y} matching a Minecraft yaw in degrees. */
    public static Rotation ofYaw(double yawDegrees) {
        double half = Math.toRadians(-yawDegrees) / 2;
        return new Rotation(0, Math.sin(half), 0, Math.cos(half));
    }

    public Rotation normalize() {
        double l = Math.sqrt(x * x + y * y + z * z + w * w);
        if (l < 1e-12 || !Double.isFinite(l)) {
            return IDENTITY;
        }
        return new Rotation(x / l, y / l, z / l, w / l);
    }

    public Rotation inverse() {
        return new Rotation(-x, -y, -z, w);
    }

    /** {@code this · o}: first {@code o}, then {@code this}. */
    public Rotation mul(Rotation o) {
        return new Rotation(
                w * o.x + x * o.w + y * o.z - z * o.y,
                w * o.y - x * o.z + y * o.w + z * o.x,
                w * o.z + x * o.y - y * o.x + z * o.w,
                w * o.w - x * o.x - y * o.y - z * o.z);
    }

    public Vec3 rotate(Vec3 v) {
        double tx = 2 * (y * v.z() - z * v.y());
        double ty = 2 * (z * v.x() - x * v.z());
        double tz = 2 * (x * v.y() - y * v.x());
        return new Vec3(
                v.x() + w * tx + (y * tz - z * ty),
                v.y() + w * ty + (z * tx - x * tz),
                v.z() + w * tz + (x * ty - y * tx));
    }

    /** Minecraft yaw in degrees of the rotated forward axis. */
    public float yaw() {
        Vec3 f = rotate(new Vec3(0, 0, 1));
        if (f.x() * f.x() + f.z() * f.z() < 1e-10) {
            Vec3 u = rotate(new Vec3(0, 1, 0));
            double s = -Math.signum(f.y());
            return (float) Math.toDegrees(Math.atan2(-u.x() * s, u.z() * s));
        }
        return (float) Math.toDegrees(Math.atan2(-f.x(), f.z()));
    }

    /** Spherical interpolation from {@code this} ({@code t = 0}) to {@code o} ({@code t = 1}). */
    public Rotation slerp(Rotation o, double t) {
        double dot = x * o.x + y * o.y + z * o.z + w * o.w;
        Rotation b = o;
        if (dot < 0) {
            dot = -dot;
            b = new Rotation(-o.x, -o.y, -o.z, -o.w);
        }
        if (dot > 0.9995) {
            return new Rotation(x + (b.x - x) * t, y + (b.y - y) * t, z + (b.z - z) * t, w + (b.w - w) * t).normalize();
        }
        double theta = Math.acos(dot);
        double s = Math.sin(theta);
        double wa = Math.sin((1 - t) * theta) / s;
        double wb = Math.sin(t * theta) / s;
        return new Rotation(x * wa + b.x * wb, y * wa + b.y * wb, z * wa + b.z * wb, w * wa + b.w * wb);
    }
}
