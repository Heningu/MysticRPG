package eu.xaru.mysticrpg.storage.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to mark fields for database persistence.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Persist {
    /**
     * The key name to use in the database.
     * If not specified, the field name will be used.
     */
    String key() default "";
}
