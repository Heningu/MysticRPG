package eu.xaru.mysticrpg.guis.exp.util;

@FunctionalInterface
public interface TriConsumer<A, B, C> {
    
    void accept(A a, B b, C c);
    
}
