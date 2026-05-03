package com.example.cryptotrackappandroid.data;

public interface ApiCallback<T> {
    void onSuccess(T result);

    void onError(Exception error);
}
