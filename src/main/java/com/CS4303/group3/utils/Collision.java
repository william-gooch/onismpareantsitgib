package com.CS4303.group3.utils;

import com.CS4303.group3.plugin.Object_Plugin.*;

import processing.core.PVector;

public class Collision {
    //Interfaces
    public interface Collider_Interface {
        public boolean isColliding(Position pThis, Collider_Interface other, Position pOther);
        public PVector getSize();
    }

    //basic collider -- need to update if dealing with non box collisions
    public static class BasicCollider implements Collider_Interface {
        public PVector size;

        public BasicCollider(int width, int height) {
            size = new PVector(width, height);
        }

        @Override
        public boolean isColliding(Position pThis, Collider_Interface other, Position pOther) {
            return pThis.position.x + size.x >= pOther.position.x
                && pThis.position.x             <= pOther.position.x + other.getSize().x
                && pThis.position.y + size.y >= pOther.position.y
                && pThis.position.y             <= pOther.position.y + other.getSize().y;
        }

        @Override
        public PVector getSize() {
            return size;
        }
    }


}
