package tla.backend.es.model.parts;

import java.util.ArrayList;
import java.util.Arrays;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;

/**
 * A single path through an entity graph, where each hop is being
 * represented by an entity reference object (containing id, eclass, type,
 * name, and optionally a list of token ids if referenced entity is a text).
 */
@Getter
@Setter
@JsonDeserialize(
    contentAs = ObjectReference.class
)
public class ObjectPath extends ArrayList<ObjectReference> {

    private static final long serialVersionUID = 7756562974667485788L;

    public static ObjectPath of(ObjectReference... refs) {
        ObjectPath path = new ObjectPath();
        path.addAll(
            Arrays.asList(refs)
        );
        return path;
    }
}
