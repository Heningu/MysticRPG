package eu.xaru.mysticrpg.storage;

import java.util.function.Consumer;
import java.util.function.Supplier;

public record MutableFieldRef(
        Supplier<Object> getter,
        Consumer<Object> setter,
        CollectionKind collectionKind // an enum we define below
) {}
