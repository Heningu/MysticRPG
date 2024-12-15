package eu.xaru.mysticrpg.storage;

/**
 * Callback interface with onSuccess and onFailure methods.
 *
 * @param <T> The type of the result.
 */
public interface Callback<T> {
    void onSuccess(T result);
    void onFailure(Throwable throwable);
}
