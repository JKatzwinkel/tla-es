package tla.backend.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tla.backend.es.model.meta.Indexable;

/**
 * Put this on top of services. It is being used for index population from tar file.
 * It is also used for choosing the correct Elasticsearch index for executing a search query
 * by the search service.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModelClass {

    /**
     * An entity model class with an associated {@link EntityService}
     */
    public Class<? extends Indexable> value();

    /**
     * Optional path value assigned to a model class, e.g. to be used for locating an instance in a file system
     */
    public String path() default "";

}