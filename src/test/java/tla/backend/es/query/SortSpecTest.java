package tla.backend.es.query;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

public class SortSpecTest {

    Sort.Order primary(SortSpec spec) {
        return spec.build().toList().get(0);
    }

    @Test
    @DisplayName("default SortSpec sort order should be by _score field, descending")
    void testDefaultSortBuilder() {
        assertAll(
            () -> assertEquals(
                Sort.Direction.DESC,
                primary(SortSpec.DEFAULT).getDirection()
            ),
            () -> assertEquals(
                "_score",
                primary(SortSpec.DEFAULT).getProperty()
            )
        );
    }

    @Test
    @DisplayName("sort spec field_order string representation should be parsed correctly")
    void testSortSpecStrings() {
        assertAll("sort spec instantiation from string",
            () -> assertEquals(
                Sort.Direction.ASC,
                primary(SortSpec.from("sortKey_asc")).getDirection()
            ),
            () -> assertEquals(
                "field_name",
                primary(SortSpec.from("field_name_desc")).getProperty()
            ),
            () -> assertEquals(
                "_field_name",
                primary(SortSpec.from("_field_name_desc")).getProperty()
            )
        );
    }

    @Test
    @DisplayName("illegal sort spec string representation should be illegal")
    void testIllegalSortSpecStrings() {
        assertAll("illegal sort specs should throws exceptions",
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> {SortSpec.from("fieldName");}
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> {SortSpec.from("id_fya");}
            )
        );
    }

}
