package com.CS4303.group3.utils;

import java.util.*;

import com.CS4303.group3.plugin.Object_Plugin;
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
        public Contact collide(Object_Plugin.Position pThis, Collider_Interface other, Object_Plugin.Position pOther) {
            if (other instanceof BasicCollider) {
                return collideBasic(pThis.position, (BasicCollider) other, pOther.position);
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




        //Attempt at implementing stretched out collisions, not working if time check at using something like this but not essential
//        private Contact collideBasic(Object_Plugin.Position pThis, BasicCollider other, Object_Plugin.Position pOther) {
//            //transform problem from two possible moving bodies to one moving body and one static
//            PVector this_start = pThis.previous_position.copy();
//            PVector other_static = pOther.previous_position.copy();
//            PVector this_end = pThis.position.copy().sub(this_start).sub(pOther.position.copy().sub(other_static));
//
//            //extend this shape to all the outer points
//            //all objects are rectangular so any extended shape would have 6 points
//            //get the two furthest away points on each shape
//            PVector prev_tl = this_start.copy(),
//                    prev_tr = this_start.copy().add(size.x, 0),
//                    prev_bl = this_start.copy().add(0, size.y),
//                    prev_br = this_start.copy().add(size.x, size.y),
//                    end_tl = this_end.copy(),
//                    end_tr = this_end.copy().add(size.x, 0),
//                    end_bl = this_end.copy().add(0, size.y),
//                    end_br = this_end.copy().add(size.x, size.y),
//                    tl_br = end_br.copy().sub(prev_tl),
//                    tr_bl = end_bl.copy().sub(prev_tr),
//                    bl_tr = end_tr.copy().sub(prev_bl),
//                    br_tl = end_tl.copy().sub(prev_br);
//
//            PVector start, end;
//            PVector[] body2 = new PVector[4];
//            body2[0] = other_static.copy();
//            body2[1] = other_static.copy().add(0, other.size.y);
//            body2[2] = other_static.copy().add(other.size.x, other.size.y);
//            body2[3] = other_static.copy().add(other.size.x, 0);
//            PVector[] body1 = new PVector[6];
//
//            if(tl_br.mag() > tr_bl.mag() && tl_br.mag() > bl_tr.mag() && tl_br.mag() > br_tl.mag()) {
//                //longest distance is tl of the start position to the bottom right of the end position
//                body1[0] = prev_tl;
//                body1[1] = prev_bl;
//                body1[2] = end_bl;
//                body1[3] = end_br;
//                body1[4] = end_tr;
//                body1[5] = prev_tr;
//                start = prev_tl;
//                end = end_br;
//            } else if(tr_bl.mag() > tl_br.mag() && tr_bl.mag() > bl_tr.mag() && tr_bl.mag() > br_tl.mag()) {
//                //longest distance is tr of the start position to the bottom left of the end position
//                body1[0] = prev_tr;
//                body1[1] = prev_br;
//                body1[2] = end_br;
//                body1[3] = end_bl;
//                body1[4] = end_tl;
//                body1[5] = prev_tl;
//                start = prev_tr;
//                end = end_bl;
//            } else if(br_tl.mag() > tl_br.mag() && br_tl.mag() > bl_tr.mag() && br_tl.mag() > tr_bl.mag()) {
//                //longest distance is br of the start position to the top left of the end position
//                body1[0] = prev_br;
//                body1[1] = prev_tr;
//                body1[2] = end_tr;
//                body1[3] = end_tl;
//                body1[4] = end_bl;
//                body1[5] = prev_bl;
//                start = prev_br;
//                end = end_tl;
//            } else {
//                //longest distance is bl of the start position to the top right of the end position
//                body1[0] = prev_bl;
//                body1[1] = prev_tl;
//                body1[2] = end_tl;
//                body1[3] = end_tr;
//                body1[4] = end_br;
//                body1[5] = prev_br;
//                start = prev_bl;
//                end = end_tr;
//            }
//
//            PVector[] initial_collision_info = shape_collision(body1, body2, start);
//
//            //move back to the point of contact
//            if(initial_collision_info == null) return null;
//
//            System.out.println(initial_collision_info[0]);
//
//
//            return new Contact(pThis.position, pOther.position, this, other, initial_collision_info[0].sub(end));
//        }
//
//        //check if two objects collide, using their edges
//        private PVector[] shape_collision(PVector[] body1, PVector[] body2, PVector start) {
//            PVector closest_collision = null;
//            PVector vert1 = null, vert2 = null;
//            for(int i = 0; i < body1.length; i++) {
//                for(int j = 0; j < body2.length; j++) {
//                    int s1 = i,
//                            s2 = i == body1.length - 1 ? 0 : i+1,
//                            s3 = j,
//                            s4 = j == body2.length - 1 ? 0 : j+1;
//
//                    PVector line_intersection = line_collision(body1[s1], body1[s2], body2[s3], body2[s4]);
//
//                    if(line_intersection != null) {
//                        //check if line intersection is closer to the start of the shape
//                        if(closest_collision == null || closest_collision.copy().sub(start).mag() < line_intersection.copy().sub(start).mag()) {
//                            closest_collision = line_intersection;
//                            vert1 = body2[s3];
//                            vert2 = body2[s4];
//                        }
//                    }
//                }
//            }
//            if(closest_collision == null) return null;
//            PVector[] collision_info = new PVector[3];
//            collision_info[0] = closest_collision;
//            collision_info[1] = vert1;
//            collision_info[2] = vert2;
//            return collision_info; //this is the first point of contact between the two objects
//        }
//
//        //check if two lines intersect
//        private PVector line_collision(PVector p1, PVector p2, PVector q1, PVector q2) {
//            float uA = ((q2.x-q1.x)*(p1.y-q1.y) - (q2.y-q1.y)*(p1.x-q1.x)) / ((q2.y-q1.y)*(p2.x-p1.x) - (q2.x-q1.x)*(p2.y-p1.y));
//            float uB = ((p2.x-p1.x)*(p1.y-q1.y) - (p2.y-p1.y)*(p1.x-q1.x)) / ((q2.y-q1.y)*(p2.x-p1.x) - (q2.x-q1.x)*(p2.y-p1.y));
//
//            if (uA >= 0 && uA <= 1 && uB >= 0 && uB <= 1) {
//                return new PVector(p1.x + (uA * (p2.x-p1.x)), p1.x + (uA * (p2.x-p1.x))); //return the point of intersection
//            }
//            else return null;
//        }
    }
}
