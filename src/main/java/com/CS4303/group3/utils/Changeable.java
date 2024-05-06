package com.CS4303.group3.utils;

public class Changeable extends Changeable_Interface {

    public Changeable(Changeable_Interface changeable) {
        super(changeable);
    }

    @Override
    public Changeable_Interface get() {
        return (Changeable_Interface) super.get();
    }





}
