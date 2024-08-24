package eu.xaru.mysticrpg.storage;

// Updated Callback interface to accept Throwable instead of Exception
public interface Callback<T> {
    void onSuccess(T result);
    void onFailure(Throwable throwable);
}
