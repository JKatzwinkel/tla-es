package tla.backend.es.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class Util {

    /**
     * return list elements in inverse order.
     */
    public static <E> List<E> reverse(List<E> list) {
        var tmp = new LinkedList<>(list);
        Collections.reverse(tmp);
        return tmp;
    }

}