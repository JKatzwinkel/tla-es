package tla.backend.es.query;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

public class SortSpecTest {

    @Test
    void testSearchSortSpec() {
        assertAll("sort spec from string",
            () -> assertEquals(Sort.Direction.ASC, SortSpec.from("sortKey_asc").order),
            () -> assertEquals("field_name", SortSpec.from("field_name_desc").field),
            () -> assertEquals(Sort.Direction.DESC, SortSpec.from("field_name_desc").order)
        );
    }

}
