package Kurama.Annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Property_Getter {
    String value(); //The name of the field connected to this method

    boolean isStandalone() default false; // If true, it means the class doesn't have a field associated with it, but still the value of this should be retrieved
}
