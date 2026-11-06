package lv.cebbys.mcmods.modularity.platform.client;

import lv.cebbys.mcmods.modularity.api.client.ModelRef;
import lv.cebbys.mcmods.modularity.api.client.model.ObjModel;
import lv.cebbys.mcmods.modularity.api.client.model.ObjParser;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.platform.compat.Ids;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** OBJ model read from the client resource manager, cached until the next resource reload. */
final class McModelRef implements ModelRef {
    private static final Map<Id, McModelRef> REFS = new ConcurrentHashMap<>();

    private final Id resource;
    private volatile ObjModel model;

    private McModelRef(Id resource) {
        this.resource = resource;
    }

    static McModelRef of(Id resource) {
        return REFS.computeIfAbsent(resource, McModelRef::new);
    }

    static void clearAll() {
        REFS.values().forEach(ref -> ref.model = null);
    }

    @Override
    public Id resource() {
        return resource;
    }

    @Override
    public ObjModel model() {
        ObjModel current = model;
        if (current == null) {
            var found = Minecraft.getInstance().getResourceManager().getResource(Ids.rl(resource))
                    .orElseThrow(() -> new IllegalStateException("Model not found in resources: " + resource));
            try (var reader = found.openAsReader()) {
                current = ObjParser.parse(reader);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read model " + resource, e);
            }
            model = current;
        }
        return current;
    }
}
