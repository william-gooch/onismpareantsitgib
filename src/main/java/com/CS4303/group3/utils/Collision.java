package com.CS4303.group3.utils;

import java.util.*;

import com.CS4303.group3.plugin.Object_Plugin;
import com.CS4303.group3.plugin.Object_Plugin.*;

import processing.core.PVector;

public class Collision {
    //Interfaces
    public interface Collider_Interface {
        public Contact collide(Position pThis, Velocity vThis, Collider_Interface other, Position pOther);
        public Contact partial_collide(Position pThis, Velocity vThis, Collider_Interface other, Position pOther, float partial);
    }

    public record Contact(
        PVector p1,
        PVector p2,

        Collider_Interface c1,
        Collider_Interface c2,

        PVector cNormal,
        float collisionTime
    ) {
        public Contact flipped() {
            return new Contact(p2, p1, c2, c1, cNormal.copy().mult(-1), collisionTime);
        }
    }

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
        public Contact partial_collide(Position pThis, Velocity vThis, Collider_Interface other, Position pOther, float partial) {
            PVector velocity;
            if (vThis == null) velocity = new PVector();
            else velocity = vThis.velocity;

            velocity = velocity.mult(partial);

            if (other instanceof BasicCollider) {
                return collideSwept(pThis.position, velocity, (BasicCollider) other, pOther.position);
            }
            return null;
        }

        @Override
        public Contact collide(Object_Plugin.Position pThis, Velocity vThis, Collider_Interface other, Object_Plugin.Position pOther) {
            return partial_collide(pThis, vThis, other, pOther, 1);
        }

        private boolean collideBroadPhase(
            PVector pThis, PVector vThis,
            BasicCollider other, PVector pOther
        ) {
            float bpx = Math.min(pThis.x, pThis.x + vThis.x),
                  bpy = Math.min(pThis.y, pThis.y + vThis.y),
                  bpw = this.size.x + Math.abs(vThis.x),
                  bph = this.size.y + Math.abs(vThis.y);

            return AABBCheck(
                new PVector(bpx, bpy),
                new PVector(bpw, bph),
                pOther, other.size
            );
        }

        public static boolean AABBCheck(
            PVector aPos, PVector aSize,
            PVector bPos, PVector bSize
        ) {
            return
                   aPos.x + aSize.x   > bPos.x
                && aPos.x             < bPos.x + bSize.x
                && aPos.y + aSize.y   > bPos.y
                && aPos.y             < bPos.y + bSize.y;
        }

        private Contact collideBasic(PVector pThis, BasicCollider other, PVector pOther) {
            boolean colliding = AABBCheck(pThis, this.size, pOther, other.size);
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

                return new Contact(pThis, pOther, this, other, normal, 0);
            } else {
                return null;
            }
        }

        // adapted from https://www.gamedev.net/tutorials/programming/general-and-gameplay-programming/swept-aabb-collision-detection-and-response-r3084/
        private Contact collideSwept(
            PVector pThis, PVector vThis,
            BasicCollider other, PVector pOther
        ) {
            if(!collideBroadPhase(pThis, vThis, other, pOther)) return null;
            float xInvEntry, yInvEntry, xInvExit, yInvExit;
            if (vThis.x > 0) {
                xInvEntry = pOther.x - (pThis.x + size.x);
                xInvExit = (pOther.x + other.size.x) - pThis.x;
            } else {
                xInvEntry = (pOther.x + other.size.x) - pThis.x;
                xInvExit = pOther.x - (pThis.x + size.x);
            }
            if (vThis.y > 0) {
                yInvEntry = pOther.y - (pThis.y + size.y);
                yInvExit = (pOther.y + other.size.y) - pThis.y;
            } else {
                yInvEntry = (pOther.y + other.size.y) - pThis.y;
                yInvExit = pOther.y - (pThis.y + size.y);
            }

            float xEntry, yEntry, xExit, yExit;
            if (vThis.x == 0f) {
                xEntry = Float.NEGATIVE_INFINITY;
                xExit = Float.POSITIVE_INFINITY;
            } else {
                xEntry = xInvEntry / vThis.x;
                xExit = xInvExit / vThis.x;
            }
            if (vThis.y == 0f) {
                yEntry = Float.NEGATIVE_INFINITY;
                yExit = Float.POSITIVE_INFINITY;
            } else {
                yEntry = yInvEntry / vThis.y;
                yExit = yInvExit / vThis.y;
            }

            float entryTime = Math.max(xEntry, yEntry),
                  exitTime = Math.min(xExit, yExit);

            if(entryTime > exitTime
            || (xEntry < 0f && yEntry < 0f)
            || xEntry > 1f
            || yEntry > 1f) {
                return collideBasic(pThis, other, pOther);
                // return null;
            } else {
                float normalX, normalY;

                if(xEntry > yEntry) {
                    if(xInvEntry < 0f) {
                        normalX = 1f;
                        normalY = 0f;
                    } else {
                        normalX = -1f;
                        normalY = 0f;
                    }
                } else {
                    if(yInvEntry < 0f) {
                        normalX = 0f;
                        normalY = 1f;
                    } else {
                        normalX = 0f;
                        normalY = -1f;
                    }
                }

                return new Contact(pThis, pOther, this, other, new PVector(normalX, normalY), entryTime);
            }
        }

        //checks if a point is in the object
        public static boolean is_in(PVector object, PVector size, PVector point) {
            return point.x >= object.x
                    && point.x <= object.x + size.x
                    && point.y >= object.y
                    && point.y <= object.y + size.y;
        }

    }
}
