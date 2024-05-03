package com.CS4303.group3.utils;

import processing.core.PVector;

public class Changeable<T> {
//    T val;
//
//
//    public Changeable(T val) {this.val = val;}
//
//    public void change(T new_val) {
//        val = new_val;
//    }
//
//    public T get() {return val;}

    public static class Changeable_Boolean extends Changeable {
        boolean val;
        public Changeable_Boolean(boolean val) {this.val = val;}

        public void change(boolean new_val) {val = new_val;}

        public boolean get() {return val;}
    }

    public static class Changeable_Direction extends Changeable {
        PVector val;
        public Changeable_Direction(PVector val) {this.val = val;}

        public void change(PVector new_val) {val = new_val;}

        public PVector get() {return val;}
    }
}
