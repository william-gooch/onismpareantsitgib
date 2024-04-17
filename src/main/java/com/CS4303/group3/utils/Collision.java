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

        public BasicCollider(float width, float height) {
            size = new PVector(width, height);
        }

        @Override
        public Contact collide(Position pThis, Collider_Interface other, Position pOther) {
            if(other instanceof BasicCollider) {
                return collideBasic(pThis, (BasicCollider) other, pOther);
            }
            return null;
        }

        private Contact collideBasic(Position pThis, BasicCollider other, Position pOther) {
            boolean colliding =
                   pThis.position.x + size.x >= pOther.position.x
                && pThis.position.x             <= pOther.position.x + other.size.x
                && pThis.position.y + size.y >= pOther.position.y
                && pThis.position.y             <= pOther.position.y + other.size.y;
            
            if (colliding) {
                float
                    leftDist = (pOther.position.x + other.size.x) - pThis.position.x,
                    rightDist = (pThis.position.x + this.size.x) - pOther.position.x,
                    topDist = (pOther.position.y + other.size.y) - pThis.position.y,
                    bottomDist = (pThis.position.y + this.size.y) - pOther.position.y;
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

                return new Contact(pThis.position, pOther.position, this, other, normal);
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
