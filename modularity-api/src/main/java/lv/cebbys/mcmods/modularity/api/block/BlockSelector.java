package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.core.Id;

import java.util.Objects;

/**
 * Chooses which existing blocks a {@link BlockInterceptor} applies to. Selectors must only depend on
 * static facts (id, type) because platforms cache the result per block.
 */
@FunctionalInterface
public interface BlockSelector {
    boolean test(BlockRef block);

    static BlockSelector all() {
        return block -> true;
    }

    static BlockSelector type(BlockType type) {
        Objects.requireNonNull(type, "type");
        return block -> block.is(type);
    }

    static BlockSelector id(Id id) {
        Objects.requireNonNull(id, "id");
        return block -> block.id().equals(id);
    }

    static BlockSelector namespace(String namespace) {
        Objects.requireNonNull(namespace, "namespace");
        return block -> block.id().namespace().equals(namespace);
    }

    default BlockSelector and(BlockSelector other) {
        return block -> test(block) && other.test(block);
    }

    default BlockSelector or(BlockSelector other) {
        return block -> test(block) || other.test(block);
    }

    default BlockSelector negate() {
        return block -> !test(block);
    }
}
