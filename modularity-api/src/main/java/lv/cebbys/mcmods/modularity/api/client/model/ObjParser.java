package lv.cebbys.mcmods.modularity.api.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal Wavefront OBJ reader: v, vt, vn, f, o, g. Materials are ignored. */
public final class ObjParser {
    private ObjParser() {
    }

    public static ObjModel parse(Reader source) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        Map<String, List<ObjModel.Face>> groups = new LinkedHashMap<>();
        String group = "default";
        BufferedReader reader = new BufferedReader(source);
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            try {
                switch (parts[0]) {
                    case "v" -> positions.add(floats(parts, 3));
                    case "vt" -> uvs.add(floats(parts, 2));
                    case "vn" -> normals.add(floats(parts, 3));
                    case "o", "g" -> group = parts.length > 1 ? parts[1] : "default";
                    case "f" -> {
                        List<ObjModel.Vertex> polygon = new ArrayList<>();
                        for (int i = 1; i < parts.length; i++) {
                            polygon.add(vertex(parts[i], positions, uvs, normals));
                        }
                        List<ObjModel.Face> faces = groups.computeIfAbsent(group, g -> new ArrayList<>());
                        if (polygon.size() == 3 || polygon.size() == 4) {
                            faces.add(new ObjModel.Face(List.copyOf(polygon)));
                        } else if (polygon.size() > 4) {
                            for (int i = 1; i + 1 < polygon.size(); i++) {
                                faces.add(new ObjModel.Face(List.of(polygon.get(0), polygon.get(i), polygon.get(i + 1))));
                            }
                        } else {
                            throw new IOException("Face with " + polygon.size() + " vertices");
                        }
                    }
                    default -> {
                        // mtllib, usemtl, s, ... are not needed
                    }
                }
            } catch (RuntimeException e) {
                throw new IOException("Invalid OBJ line " + lineNumber + ": " + line, e);
            }
        }
        groups.replaceAll((name, faces) -> List.copyOf(faces));
        return new ObjModel(java.util.Collections.unmodifiableMap(groups));
    }

    private static float[] floats(String[] parts, int count) {
        float[] out = new float[count];
        for (int i = 0; i < count; i++) {
            out[i] = Float.parseFloat(parts[i + 1]);
        }
        return out;
    }

    private static ObjModel.Vertex vertex(String token, List<float[]> positions, List<float[]> uvs, List<float[]> normals) {
        String[] idx = token.split("/", -1);
        float[] p = get(positions, idx[0]);
        float[] t = idx.length > 1 && !idx[1].isEmpty() ? get(uvs, idx[1]) : new float[]{0, 0};
        float[] n = idx.length > 2 && !idx[2].isEmpty() ? get(normals, idx[2]) : new float[]{0, 1, 0};
        return new ObjModel.Vertex(p[0], p[1], p[2], t[0], t[1], n[0], n[1], n[2]);
    }

    private static float[] get(List<float[]> list, String index) {
        int i = Integer.parseInt(index);
        return list.get(i > 0 ? i - 1 : list.size() + i);
    }
}
