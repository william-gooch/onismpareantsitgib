package com.CS4303.group3.utils;

import java.util.*;

import com.CS4303.group3.plugin.Object_Plugin.*;

import processing.core.PVector;

public class Collision {
    //Interfaces
    public interface Collider_Interface {
        public Contact collide(Position pThis, Collider_Interface other, Position pOther);
    }

    public record Contact(
        PVector p1,
        PVector p2,

        Collider_Interface c1,
        Collider_Interface c2,

        PVector cNormal
    ) {}

    //basic collider -- need to update if dealing with non box collisions
    public static class BasicCollider implements Collider_Interface {
        public PVector size;
        public PVector offset;

        public BasicCollider(float width, float height, float x, float y) {
            size = new PVector(width, height);
            offset = new PVector(x, y);
        }

        public BasicCollider(float width, float height) {
            this(width, height, 0, 0);
        }

        @Override
        public Contact collide(Position pThis, Collider_Interface other, Position pOther) {
            if(other instanceof BasicCollider) {
                return collideBasic(PVector.add(pThis.position, this.offset), (BasicCollider) other, PVector.add(pOther.position, ((BasicCollider) other).offset));
            }
            return null;
        }

        private Contact collideBasic(PVector pThis, BasicCollider other, PVector pOther) {
            boolean colliding =
                   pThis.x + size.x >= pOther.x
                && pThis.x             <= pOther.x + other.size.x
                && pThis.y + size.y >= pOther.y
                && pThis.y             <= pOther.y + other.size.y;
            
            if (colliding) {
                float
                    leftDist = (pOther.x + other.size.x) - pThis.x,
                    rightDist = (pThis.x + this.size.x) - pOther.x,
                    topDist = (pOther.y + other.size.y) - pThis.y,
                    bottomDist = (pThis.y + this.size.y) - pOther.y;
                float minDist = Collections.min(Arrays.asList(leftDist, rightDist, topDist, bottomDist));

                PVector normal = new PVector();
                if(minDist == leftDist) {
                    normal = new PVector(leftDist, 0);
                } else if(minDist == rightDist) {
                    normal = new PVector(-rightDist, 0);
                } else if(minDist == topDist) {
                    normal = new PVector(0, topDist);
                } else if(minDist == bottomDist) {
                    normal = new PVector(0, -bottomDist);
                }

                return new Contact(pThis, pOther, this, other, normal);
            } else {
                return null;
            }
        }

        //checks if a point is in the object
        private boolean is_in(PVector object, PVector size, PVector point) {
            return point.x >= object.x
                    && point.x <= object.x + size.x
                    && point.y >= object.y
                    && point.y <= object.y + size.y;
        }
    }
}
