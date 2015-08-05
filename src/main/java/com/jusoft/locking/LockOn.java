package com.jusoft.locking;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that defines the object the method has to be synchronised on.
 * The object defined by this annotation is interned using the Guava library
 * to ensure the same value is used as monitor across all threads. It uses weak references to ensure
 * the interned value is garbage collected once used. The intern mechanism uses equals to ensure
 * the object returned as monitor from the internal map is always the same based on the object defined
 * by this annotation. This object could not be the same as the one annotated but, as long as the equals method
 * returns true when comparing the two of them, the monitor used would be the same.
 * Keep in mind when using this annotation for locking that it uses the equals to lock. If for any reason,
 * there are different use cases in the application, they could share the same monitor if the monitor value used
 * is not unique across them.
 * This annotation needs {@see com.iggroup.wt.majormarketmovements.locking.LockingInterceptor}
 * to be defined as a bean in the Spring context and {@see org.springframework.context.annotation.EnableAspectJAutoProxy}
 * or its xml equivalent enabled.
 * <p/>
 *
 * @author carnicj
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface LockOn {

   /**
    * Field to use as monitor. It allows the lock to be fetched from a complex object
    * The value has to be provided using fields' names.
    * Example @LockOn("address.street") Client client
    *
    * @return full qualified path to field from root object
    */
   String value() default "";
}
