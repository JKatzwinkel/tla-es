package tla.backend.es.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import tla.backend.es.model.meta.BaseEntity;
import tla.backend.es.model.parts.ObjectPath;
import tla.backend.es.model.parts.ObjectReference;
import tla.domain.model.meta.Resolvable;

public class ObjectReferenceTest {

    private static ObjectMapper mapper = tla.domain.util.IO.getMapper();

    private ObjectPath[] paths_backend() throws Exception {
        return mapper.readValue(
            "[[{\"id\":\"ID\", \"eclass\":\"eclass\"}]]",
            ObjectPath[].class
        );
    }

    @Test
    void deserialize() throws Exception {
        ObjectReference ref = mapper.readValue(
            "{\"id\":\"ID\", \"eclass\":\"eclass\", \"type\":\"type\"}",
            ObjectReference.class
        );
        assertAll("single obj ref (backend)",
            () -> assertNotNull(ref),
            () -> assertNotNull(ref.getId()),
            () -> assertNotNull(ref.getEclass())
        );
    }

    @Test
    void deserialize_in_entity() throws Exception {
        ThsEntryEntity t = ThsEntryEntity.builder().id("ID").eclass("BTSThsEntry")
            .paths(
                new ObjectPath[]{
                    ObjectPath.of(
                        new ObjectReference("ID2", "BTSThsEntry", "date", "Thut", null)
                    )
                }
            ).build();
        String s = mapper.writeValueAsString(t);
        assertTrue(s.contains("\"paths\":[[{"), "paths serialized");
        assertEquals(
            "{\"id\":\"ID\",\"paths\":[[{\"id\":\"ID2\",\"eclass\":\"BTSThsEntry\",\"type\":\"date\",\"name\":\"Thut\"}]]}",
            s,
            "serialization of ths entry with paths"
        );
        ThsEntryEntity e = mapper.readValue(s, ThsEntryEntity.class);
        assertAll("look at paths inside ths entry entity",
            () -> assertNotNull(e.getPaths(), "paths"),
            () -> assertTrue(e.getPaths().length > 0, "not empty"),
            () -> assertFalse(e.getPaths()[0].isEmpty(), "nested not empty"),
            () -> assertEquals("ID2", e.getPaths()[0].get(0).getId(), "id ok")
        );
    }

    @Test
    void deserialize_path() throws Exception {
        ObjectPath[] paths = paths_backend();
        assertAll("paths deserialized (backend)",
            () -> assertNotNull(paths, "list of list"),
            () -> assertTrue(paths.length > 0, "objectpaths count > 0"),
            () -> assertFalse(paths[0].isEmpty(), "first list not empty"),
            () -> assertTrue(paths[0] instanceof List<?>, "first list instance of list"),
            () -> assertEquals(
                ObjectPath.of(ObjectReference.builder().id("ID").eclass("eclass").build()),
                paths[0],
                "nested list"
            ),
            () -> assertEquals(ObjectReference.class, paths[0].get(0).getClass(), "obj ref instance"),
            () -> assertTrue(
                List.of(
                    paths[0].get(0).getClass().getInterfaces()
                ).contains(Resolvable.class), "implements resolvable"
            ),
            () -> assertTrue(paths[0].get(0) instanceof Resolvable, "first element first list resolvable"),
            () -> assertEquals("ID", paths[0].get(0).getId(), "payload has ID")
        );
    }

    @Test
    void serialize() throws Exception {
        BaseEntity.Relations rel = BaseEntity.Relations.of(
            ObjectReference.builder().id("ID").eclass("eclass").build()
        );
        String s = tla.domain.util.IO.json(rel);
        assertAll("make sure serialization contains no noise",
            () -> assertFalse(s.contains("\"ranges\":"), "empty token ranges not included"),
            () -> assertNull(rel.get(0).getRanges(), "ranges indeed empty")
        );
    }

    @Test
    void equality() throws Exception {
        String refjson = "{\"id\":\"ID\",\"eclass\":\"BTSText\",\"type\":\"Text\",\"name\":\"Stele\"}";
        ObjectReference ref1 = mapper.readValue(refjson, ObjectReference.class);
        ObjectReference ref2 = mapper.readValue(refjson, ObjectReference.class);
        ref1.setRanges(
            List.of(
                Resolvable.Range.of("Token00", "Token06"),
                Resolvable.Range.of("Token12", "Token99")
            )
        );
        assertNotEquals(ref1, ref2, "no equality between referenced to the same object if one instance has ranges");
        ref2.setRanges(
            List.of(
                Resolvable.Range.of("Token12", "Token99"),
                Resolvable.Range.of("Token12", "Token99"),
                Resolvable.Range.of("Token00", "Token06")
            )
        );
        assertAll("object reference instances should be equal regardless of range order",
            () -> assertEquals(ref1, ref2, "instance"),
            () -> assertEquals(ref1.hashCode(), ref2.hashCode(), "hashcode")
        );
        assertAll("duplicate range should not affect objectref equality",
            () -> assertEquals(ref1, ref2, "instance"),
            () -> assertEquals(ref1.hashCode(), ref2.hashCode(), "hashcode")
        );
        ObjectReference ref3 = ObjectReference.builder().id("ID").eclass("BTSText").ranges(
            List.of(
                Resolvable.Range.of("Token12", "Token99"),
                Resolvable.Range.of("Token00", "Token06")
            )
        ).build();
        assertAll("minimal object reference instance equality",
            () -> assertNotEquals(ref1, ref3, "instance unequal because type, name missing")
        );
        ref3.setRanges(null);
        ObjectReference ref4 = mapper.readValue("{\"id\":\"ID\",\"eclass\":\"BTSText\"}", ObjectReference.class);
        assertEquals(ref3, ref4, "minimal object references equality");
    }

    @Test
    void sort() throws Exception {
        BaseEntity.Relations rel = BaseEntity.Relations.of(
            ObjectReference.builder().id("2").eclass("BTSText").build(),
            ObjectReference.builder().id("1").eclass("BTSAnnotations").build()
        );
        SortedSet<Resolvable> refs = new TreeSet<>(rel);
        assertAll("object ref comparison test",
            () -> assertEquals("1", refs.first().getId(), "sorted items start at lowest ID value")
        );
    }

    @Test
    void referenceContainer_relations() throws Exception {
        BaseEntity.Relations r1 = BaseEntity.Relations.of(
            ObjectReference.builder().id("ID").eclass("BTSText").build()
        );
        BaseEntity.Relations r2 = BaseEntity.Relations.of(
            ObjectReference.builder().id("ID").eclass("BTSText").build()
        );
        assertAll("container equality",
            () -> assertEquals(r1, r2, "relations instances"),
            () -> assertEquals(r1.hashCode(), r2.hashCode(), "hashcode")
        );
        BaseEntity.Relations r3 = BaseEntity.Relations.of(
            ObjectReference.builder().id("Id").eclass("BTSText").build()
        );
        assertAll("container inequality",
            () -> assertNotEquals(r1, r3, "instance"),
            () -> assertNotEquals(r1.hashCode(), r3.hashCode(), "hashcodes")
        );
        BaseEntity.Relations r4 = mapper.readValue(
            "[{\"id\":\"ID\",\"eclass\":\"BTSText\"}]",
            BaseEntity.Relations.class
        );
        assertAll("built and deserialized relations list should be equal",
            () -> assertEquals(r1, r4, "instance"),
            () -> assertEquals(r1.toString(), r4.toString(), "tostring"),
            () -> assertEquals(r1.get(0), r4.get(0), "element"),
            () -> assertEquals(tla.domain.util.IO.json(r1), tla.domain.util.IO.json(r4), "serialization"),
            () -> assertEquals(r1.hashCode(), r4.hashCode(), "hashcode")
        );
    }

}