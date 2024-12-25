package eu.xaru.mysticrpg.config;

@FunctionalInterface
public interface DynamicConfigParser<T> {
    T parse(DynamicConfig config) throws Exception;
}
