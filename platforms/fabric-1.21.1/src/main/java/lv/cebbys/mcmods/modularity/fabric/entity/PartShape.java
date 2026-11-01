package lv.cebbys.mcmods.modularity.fabric.entity;

import lv.cebbys.mcmods.modularity.api.entity.EntityPart;
import lv.cebbys.mcmods.modularity.api.world.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * A part box placed in the world: an oriented box, its axis-aligned bounds, and its collision
 * shape as vertical columns (Minecraft collides entities against axis-aligned boxes only, so a
 * tilted roof becomes a fine staircase that players can stand and walk on).
 */
public record PartShape(Vec3 center, Rotation orientation, lv.cebbys.mcmods.modularity.api.world.Vec3 half,
                        AABB bounds, List<VoxelShape> columns) {
    /** Column size in blocks; parts larger than {@code MAX_COLUMNS} columns per axis use larger cells. */
    private static final double CELL = 0.25;
    private static final int MAX_COLUMNS = 16;

    public static PartShape place(EntityPart part, Vec3 origin, Rotation q) {
        var local = part.center();
        var rotated = q.rotate(local);
        Vec3 center = new Vec3(origin.x + rotated.x(), origin.y + rotated.y(), origin.z + rotated.z());
        var h = part.halfExtents();
        var ax = q.rotate(new lv.cebbys.mcmods.modularity.api.world.Vec3(1, 0, 0));
        var ay = q.rotate(new lv.cebbys.mcmods.modularity.api.world.Vec3(0, 1, 0));
        var az = q.rotate(new lv.cebbys.mcmods.modularity.api.world.Vec3(0, 0, 1));
        double ex = Math.abs(ax.x()) * h.x() + Math.abs(ay.x()) * h.y() + Math.abs(az.x()) * h.z();
        double ey = Math.abs(ax.y()) * h.x() + Math.abs(ay.y()) * h.y() + Math.abs(az.y()) * h.z();
        double ez = Math.abs(ax.z()) * h.x() + Math.abs(ay.z()) * h.y() + Math.abs(az.z()) * h.z();
        AABB bounds = new AABB(center.x - ex, center.y - ey, center.z - ez, center.x + ex, center.y + ey, center.z + ez);
        List<VoxelShape> columns = part.solid() ? columns(center, q, h, bounds) : List.of();
        return new PartShape(center, q, h, bounds, columns);
    }

    /** Whether the world point lies inside the oriented box. */
    public boolean contains(Vec3 p) {
        var l = orientation.inverse().rotate(new lv.cebbys.mcmods.modularity.api.world.Vec3(p.x - center.x, p.y - center.y, p.z - center.z));
        return Math.abs(l.x()) <= half.x() && Math.abs(l.y()) <= half.y() && Math.abs(l.z()) <= half.z();
    }

    /**
     * Distance along the ray {@code from + t·dir} (unit {@code dir}) where it enters the oriented
     * box, or {@code NaN} when it misses.
     */
    public double raycast(Vec3 from, Vec3 dir) {
        Rotation inv = orientation.inverse();
        var o = inv.rotate(new lv.cebbys.mcmods.modularity.api.world.Vec3(from.x - center.x, from.y - center.y, from.z - center.z));
        var d = inv.rotate(new lv.cebbys.mcmods.modularity.api.world.Vec3(dir.x, dir.y, dir.z));
        double[] os = {o.x(), o.y(), o.z()};
        double[] ds = {d.x(), d.y(), d.z()};
        double[] hs = {half.x(), half.y(), half.z()};
        double tMin = 0;
        double tMax = Double.POSITIVE_INFINITY;
        for (int a = 0; a < 3; a++) {
            if (Math.abs(ds[a]) < 1e-12) {
                if (Math.abs(os[a]) > hs[a]) {
                    return Double.NaN;
                }
                continue;
            }
            double t1 = (-hs[a] - os[a]) / ds[a];
            double t2 = (hs[a] - os[a]) / ds[a];
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
            if (tMin > tMax) {
                return Double.NaN;
            }
        }
        return tMin;
    }

    private static List<VoxelShape> columns(Vec3 c, Rotation q, lv.cebbys.mcmods.modularity.api.world.Vec3 h, AABB b) {
        double cellX = Math.max(CELL, b.getXsize() / MAX_COLUMNS);
        double cellZ = Math.max(CELL, b.getZsize() / MAX_COLUMNS);
        int nx = Math.max(1, (int) Math.ceil(b.getXsize() / cellX));
        int nz = Math.max(1, (int) Math.ceil(b.getZsize() / cellZ));
        Rotation inv = q.inverse();
        var dir = inv.rotate(new lv.cebbys.mcmods.modularity.api.world.Vec3(0, 1, 0));
        List<VoxelShape> out = new ArrayList<>();
        for (int i = 0; i < nx; i++) {
            for (int k = 0; k < nz; k++) {
                double x0 = b.minX + i * cellX;
                double z0 = b.minZ + k * cellZ;
                double x1 = Math.min(b.maxX, x0 + cellX);
                double z1 = Math.min(b.maxZ, z0 + cellZ);
                // Vertical line through the cell centre, in box space.
                var o = inv.rotate(new lv.cebbys.mcmods.modularity.api.world.Vec3((x0 + x1) / 2 - c.x, 0, (z0 + z1) / 2 - c.z));
                double tMin = Double.NEGATIVE_INFINITY;
                double tMax = Double.POSITIVE_INFINITY;
                boolean miss = false;
                double[] os = {o.x(), o.y(), o.z()};
                double[] ds = {dir.x(), dir.y(), dir.z()};
                double[] hs = {h.x(), h.y(), h.z()};
                for (int a = 0; a < 3 && !miss; a++) {
                    if (Math.abs(ds[a]) < 1e-9) {
                        miss = Math.abs(os[a]) > hs[a];
                        continue;
                    }
                    double t1 = (-hs[a] - os[a]) / ds[a];
                    double t2 = (hs[a] - os[a]) / ds[a];
                    tMin = Math.max(tMin, Math.min(t1, t2));
                    tMax = Math.min(tMax, Math.max(t1, t2));
                    miss = tMin > tMax;
                }
                if (!miss && tMax - tMin > 1e-4) {
                    out.add(Shapes.create(x0, c.y + tMin, z0, x1, c.y + tMax, z1));
                }
            }
        }
        return List.copyOf(out);
    }
}
