package lv.cebbys.mcmods.modularity.api.client.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parsed Wavefront OBJ geometry, split by {@code o}/{@code g} names. Faces are triangles or quads;
 * larger polygons are fanned into triangles. UVs use OBJ convention (v = 0 at the bottom).
 */
public record ObjModel(Map<String, List<Face>> groups) {
    public Set<String> groupNames() {
        return groups.keySet();
    }

    public List<Face> faces(String group) {
        List<Face> faces = groups.get(group);
        if (faces == null) {
            throw new IllegalArgumentException("Model has no group '" + group + "', has " + groups.keySet());
        }
        return faces;
    }

    public int faceCount() {
        return groups.values().stream().mapToInt(List::size).sum();
    }

    public record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
    }

    /** 3 or 4 vertices, counter-clockwise. */
    public record Face(List<Vertex> vertices) {
    }
}
