package Kurama.Annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Property {
    boolean getterExists() default false;
    boolean setterExists() default false;
}
