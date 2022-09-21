package tla.backend.es.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort;

/**
 * Representation of search order specifications.
 */
public class SortSpec {

    public static final String DELIMITER = "_";

    /**
     * default search specs (sort by "_score" in descending order).
     */
    public static final SortSpec DEFAULT = SortSpec.from("_score_desc");

    protected List<Sort.Order> orders;

    public SortSpec() {
        this.orders = new ArrayList<Sort.Order>();
    }

    /**
     * Create new sort spec configured for ascending order ({@link SortOrder.ASC}) on given field.
     */
    public SortSpec(String field) {
        this(field, Sort.Direction.ASC);
    }

    /**
     * Create a new sort specification instance with given field name and sort order (<code>"asc"</code>/<code>"desc"</code>).
     */
    public SortSpec(String field, Sort.Direction direction) {
        this();
        this.orders.add(
            new Sort.Order(direction, field)
        );
    }

    /**
     * Create a sort spec instance from a string consisting of a field name, followed by a direction specifier (asc/desc),
     * seperated by the delimiter character defined in {@link #DELIMITER}.
     */
    public static SortSpec from(String source) {
        if (source != null) {
            String[] segm = source.split(DELIMITER);
            String field = String.join(
                DELIMITER,
                Arrays.asList(segm).subList(0, segm.length - 1)
            );
            if (segm.length > 1) {
                return new SortSpec(
                    field,
                    Sort.Direction.fromString(
                        segm[segm.length - 1]
                    )
                );
            } else {
                throw new IllegalArgumentException(
                    String.format(
                        """
                        cannot create SortSpec instance from string representation %s:
                        must match the pattern fieldName%sdirection (with direction being
                        either asc or desc)!""",
                        source, DELIMITER
                    )
                );
            }
        } else {
            return new SortSpec("id");
        }
    }

    public Sort build() {
        return Sort.by(
            this.orders
        );
    }

}
