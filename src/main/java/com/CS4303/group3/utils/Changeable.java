package com.CS4303.group3.utils;

public class Changeable {
    Changeable_Interface changeable;

    public Changeable(Changeable_Interface changeable) {
        this.changeable = changeable;
    }

    public Changeable_Interface get() {
        return changeable;
    }

    public static class Changeable_Interface<T> {
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

//    public static class Changeable_Boolean extends Changeable {
//        boolean val;
//        public Changeable_Boolean(boolean val) {this.val = val;}
//
//        public void change(boolean new_val) {val = new_val;}
//
//        public boolean get() {return val;}
//    }
//
//    public static class Changeable_Direction extends Changeable {
//        PVector val;
//        public Changeable_Direction(PVector val) {this.val = val;}
//
//        public void change(PVector new_val) {val = new_val;}
//
//        public PVector get() {return val;}
//    }

}
