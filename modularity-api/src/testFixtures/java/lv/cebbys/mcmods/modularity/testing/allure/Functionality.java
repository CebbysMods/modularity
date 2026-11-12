package lv.cebbys.mcmods.modularity.testing.allure;

import io.qameta.allure.LabelAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The behaviour a test proves. It is the fourth level of the report tree, under the story (docs/07, 7.11).
 *
 * <p>Allure has no annotation for a custom label name, so this annotation is a meta-annotation: Allure reads
 * {@link LabelAnnotation} and writes a label with the name {@code functionality} and the value of {@link #value()}.
 * The report configuration {@code allurerc.json} groups by that label name.
 *
 * <p>Use a value of {@link ModularityAllureConstants.Functionalities}, so every test in one behaviour carries the
 * same text.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@LabelAnnotation(name = Functionality.LABEL)
public @interface Functionality {

    /** The label name the report groups by. The name must match {@code allurerc.json}. */
    String LABEL = "functionality";

    /** The behaviour, for example "Version ranges". */
    String value();
}
