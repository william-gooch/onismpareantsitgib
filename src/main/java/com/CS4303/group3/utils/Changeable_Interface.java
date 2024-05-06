package com.CS4303.group3.utils;

public class Changeable_Interface<T> {
    T value;

    public Changeable_Interface(T value) {
        this.value = value;
    }

    public void change(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }
}
