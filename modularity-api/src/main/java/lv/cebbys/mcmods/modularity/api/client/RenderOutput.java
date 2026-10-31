package lv.cebbys.mcmods.modularity.api.client;

import lv.cebbys.mcmods.modularity.api.core.Id;

/** Transform stack and geometry sink for {@link EntityRenderer}. Angles in degrees. */
public interface RenderOutput {
    void push();

    void pop();

    void translate(double x, double y, double z);

    void rotateX(float degrees);

    void rotateY(float degrees);

    void rotateZ(float degrees);

    void scale(float x, float y, float z);

    /** Draws the whole model with {@code texture} (e.g. {@code id("textures/entity/car.png")}). */
    void model(ModelRef model, Id texture);

    /** Draws one named object/group of the model. */
    void model(ModelRef model, String group, Id texture);
}
