package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.client.model.ObjModel;
import lv.cebbys.mcmods.modularity.api.core.Id;

/** A model loaded from resources, see {@link ClientContext#objModel}. */
public interface ModelRef {
    Id resource();

    /** The parsed model (loaded on first use). */
    ObjModel model();
}
