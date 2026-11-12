package lv.cebbys.modularity.gradle.allure;

import io.qameta.allure.LabelAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The behaviour a test proves, as an Allure label named {@code functionality} (docs/07, 7.11). Allure has no
 * annotation for a custom label name, so this annotation is a meta-annotation of {@link LabelAnnotation}.
 *
 * <p>This is the copy for the included build: {@code modularity-gradle} cannot read the test fixtures of
 * {@code modularity-api}. The label name must match {@link #LABEL} and the name in {@code allurerc.json}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@LabelAnnotation(name = Functionality.LABEL)
public @interface Functionality {

    /** The label name the report groups by. The name must match {@code allurerc.json}. */
    String LABEL = "functionality";

    /** The behaviour, for example "Layer precedence". */
    String value();
}
