package com.example.hackves;

public interface CommandResultListener {
    void onSuccess(String message);
    void onFailure(String message);
}
