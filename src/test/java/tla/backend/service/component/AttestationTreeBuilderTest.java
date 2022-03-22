package tla.backend.service.component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tla.backend.Util;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.model.meta.BaseEntity;
import tla.backend.es.model.meta.Recursable;
import tla.domain.model.extern.AttestedTimespan;

public class AttestationTreeBuilderTest {

    @Test
    @DisplayName("sample of entities should be arranged according to hierarchy")
    void testTreeReconstruction() throws Exception {
        Stream<Recursable> thsEntries = Stream.of(
            Util.loadSampleFile(ThsEntryEntity.class, "E7YEQAEKZVEJ5PX7WKOXY2QEEM"),
            Util.loadSampleFile(ThsEntryEntity.class, "N673TBXEGJDDBO6B6DZXKT64YQ"),
            Util.loadSampleFile(ThsEntryEntity.class, "4SJRB25AURBUZMSZBBXRRHDO3A")
        );
        AttestationTreeBuilder treeBuilder = AttestationTreeBuilder.of(thsEntries);
        var roots = treeBuilder.getRoots().collect(Collectors.toList());
        assertAll("entities should be organized according to hierarchic relations",
            () -> assertEquals(1, roots.size(), "only 1 root"),
            () -> assertEquals(
                "N673TBXEGJDDBO6B6DZXKT64YQ",
                ((BaseEntity) roots.get(0).getEntity()).getId(),
                "entity highest up in hierarchy should be root node"
            )
        );
    }

    @Test
    @DisplayName("reconstructed hierarchic entity tree should translate to nested attestation objects")
    void testAttestationTree() throws Exception {
        Stream<Recursable> thsEntries = Stream.of(
            Util.loadSampleFile(ThsEntryEntity.class, "E7YEQAEKZVEJ5PX7WKOXY2QEEM"),
            Util.loadSampleFile(ThsEntryEntity.class, "N673TBXEGJDDBO6B6DZXKT64YQ"),
            Util.loadSampleFile(ThsEntryEntity.class, "4SJRB25AURBUZMSZBBXRRHDO3A")
        );
        Map<String, Long> counts = Map.of(
            "E7YEQAEKZVEJ5PX7WKOXY2QEEM", 5L,
            "N673TBXEGJDDBO6B6DZXKT64YQ", 7L,
            "4SJRB25AURBUZMSZBBXRRHDO3A", 11L
        );
        AttestationTreeBuilder treeBuilder = AttestationTreeBuilder.of(thsEntries);
        List<AttestedTimespan> attestations = treeBuilder.counts(counts).resolve();
        assertAll("nested attestations should resemble entity tree reconstruction",
            () -> assertEquals(1, attestations.size(), "only 1 attestation object on root level"),
            () -> assertEquals(2, attestations.get(0).getContains().size(), "root attestation should have 2 children"),
            () -> assertEquals(7L, attestations.get(0).getAttestations().getCount(), "attestation count should have been set according to map passed")
        );
    }

    @Test
    @DisplayName("attestation timespan tree should only ever have 1 element on root level")
    void testAttestationTreeAlwaysOnly1Root() throws Exception {
        Stream<Recursable> thsEntries = Stream.of(
            Util.loadSampleFile(ThsEntryEntity.class, "E7YEQAEKZVEJ5PX7WKOXY2QEEM"),
            Util.loadSampleFile(ThsEntryEntity.class, "4SJRB25AURBUZMSZBBXRRHDO3A")
        );
        Map<String, Long> counts = Map.of(
            "E7YEQAEKZVEJ5PX7WKOXY2QEEM", 5L,
            "4SJRB25AURBUZMSZBBXRRHDO3A", 11L
        );
        AttestationTreeBuilder treeBuilder = AttestationTreeBuilder.of(thsEntries);
        List<AttestedTimespan> attestations = treeBuilder.counts(counts).resolve();
        assertEquals(1, attestations.size(), "only 1 attestatioin object on root level");
    }

}
