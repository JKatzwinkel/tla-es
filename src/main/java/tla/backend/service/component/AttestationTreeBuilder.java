package tla.backend.service.component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.model.Util;
import tla.backend.es.model.meta.BaseEntity;
import tla.backend.es.model.meta.Recursable;
import tla.backend.es.model.parts.ObjectPath;
import tla.domain.model.extern.AttestedTimespan;
import tla.domain.model.meta.Resolvable;

public class AttestationTreeBuilder {

    protected class Node {
        private Set<Node> parents;
        private Set<Node> children;
        @Getter
        private Recursable entity;

        Node(Recursable entity) {
            this.parents = new HashSet<>();
            this.children = new HashSet<>();
            this.entity = entity;
        }

        void addChild(Node child) {
            this.children.add(child);
        }

        void addParent(Node parent) {
            this.parents.add(parent);
        }

        boolean isRoot() {
            return this.parents.isEmpty();
        }

        public AttestedTimespan toNestedAttestation() {
            return AttestedTimespan.builder().attestations(
                AttestedTimespan.AttestationStats.builder()
                .count(
                    counts.getOrDefault(((BaseEntity) this.entity).getId(), 0L)
                ).build()
            ).period(
                ((ThsEntryEntity) this.entity).toAttestedPeriod()
            ).contains(
                this.children.stream().map(
                    Node::toNestedAttestation
                ).collect(
                    Collectors.toList()
                )
            ).build();
        }

    }

    private Map<String, Node> nodes;
    protected Map<String, Long> counts;

    public AttestationTreeBuilder(Stream<Recursable> stream) {
        this.nodes = stream.collect(
            Collectors.toMap(
                entity -> ((BaseEntity) entity).getId(),
                entity -> new Node(entity)
            )
        );
        this.nodes.values().stream().forEach(
            this::register
        );
    }

    public static AttestationTreeBuilder of(Stream<Recursable> entities) {
        return new AttestationTreeBuilder(entities);
    }

    private Node getNode(String id) {
        return this.nodes.getOrDefault(id, null);
    }

    private void attach(Node node, Node parent) {
        parent.addChild(node);
        node.addParent(parent);
    }

    private Node findClosestAncestor(ObjectPath path) {
        for (Resolvable segment : Util.reverse(path)) {
            var node = getNode(segment.getId());
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    private void register(Node node) {
        if (node.getEntity().getPaths() != null) {
            for (ObjectPath path : node.getEntity().getPaths()) {
                var parent = findClosestAncestor(path);
                if (parent != null) {
                    attach(node, parent);
                }
            }
        }
    }

    public Stream<Node> getRoots() {
        return this.nodes.values().stream().filter(
            Node::isRoot
        );
    }

    public AttestationTreeBuilder counts(Map<String, Long> counts) {
        this.counts = counts;
        return this;
    }

    public List<AttestedTimespan> resolve() {
        var roots = this.getRoots().map(
            Node::toNestedAttestation
        ).collect(
            Collectors.toList()
        );
        if (roots.size() < 2) {
            return roots;
        }
        return List.of(
            AttestedTimespan.builder().contains(
                roots
            ).build()
        );
    }

}
