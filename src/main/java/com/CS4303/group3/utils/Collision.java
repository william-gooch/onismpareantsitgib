package com.CS4303.group3.utils;

import com.CS4303.group3.plugin.Object_Plugin.*;

import processing.core.PVector;

public class Collision {
    //Interfaces
    public interface Collider_Interface {
        public boolean isColliding(Position pThis, Collider_Interface other, Position pOther);
        public PVector collision_correction(Position pThis, Collider_Interface other, Position pOther);
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

        //checks if a point is in the object
        private boolean is_in(PVector object, PVector size, PVector point) {
            return point.x >= object.x
                    && point.x <= object.x + size.x
                    && point.y >= object.y
                    && point.y <= object.y + size.y;
        }

        private boolean check_intersection_lines(PVector p1, PVector p2, PVector p3, PVector p4) {
            float uA = ((p4.x-p3.x)*(p1.y-p3.y) - (p4.y-p3.y)*(p1.x-p3.x)) / ((p4.y-p3.y)*(p2.x-p1.x) - (p4.x-p3.x)*(p2.y-p1.y));
            float uB = ((p2.x-p1.x)*(p1.y-p3.y) - (p2.y-p1.y)*(p1.x-p3.x)) / ((p4.y-p3.y)*(p2.x-p1.x) - (p4.x-p3.x)*(p2.y-p1.y));
            
            //check within range [0..1]
            return (uA >= 0 && uA <= 1 && uB >= 0 && uB <= 1);
          }

        @Override
        public PVector collision_correction(Position pThis, Collider_Interface other, Position pOther) {
            PVector collision = new PVector(0,0);
            //get corner positions
            PVector top_left = pThis.position;
            PVector top_right = new PVector(top_left.x + size.x, top_left.y);
            PVector bottom_left = new PVector(top_left.x, top_left.y + size.y);
            PVector bottom_right = new PVector(top_right.x, bottom_left.y);

            PVector ground_top_left = pOther.position;
            PVector ground_top_right = new PVector(ground_top_left.x + other.getSize().x, ground_top_left.y);
            PVector ground_bottom_left = new PVector(ground_top_left.x, ground_top_left.y + other.getSize().y);
            PVector ground_bottom_right = new PVector(ground_top_right.x, ground_bottom_left.y);

            PVector prev_top_left = pThis.previous_position;
            PVector prev_top_right = new PVector(prev_top_left.x + size.x, prev_top_left.y);
            PVector prev_bottom_left = new PVector(prev_top_left.x, prev_top_left.y + size.y);
            PVector prev_bottom_right = new PVector(prev_top_right.x, prev_bottom_left.y);

            PVector object = pOther.position;
            PVector object_size = other.getSize();

            boolean top = false, bottom = false, left = false, right = false;

            //check for basic (2 corner collisions)
            //check vertical
            if(is_in(object, object_size, bottom_left) && is_in(object, object_size, bottom_right)) {
                collision.y = object.y - size.y;
                bottom = true;
            } else if(is_in(object, object_size, top_left) && is_in(object, object_size, top_right)) {
                collision.y = object.y + object_size.y;
                top = true;
            }

            //check horizontal
            if(is_in(object, object_size, bottom_left) && is_in(object, object_size, top_left)) {
                collision.x = object.x + object_size.x;
                left = true;
            } else if(is_in(object, object_size, bottom_right) && is_in(object, object_size, top_right)) {
                collision.x = object.x - size.x;
                right = true;
            }

            //handle single corner collisions -- use previous position
            //calc entry point move back to there -- use intersection of line between two points and ground edges
            if(is_in(object, object_size, top_left) && !(top || left)) {
                //if it intersected with the bottom edge of the ground move down
                if(check_intersection_lines(top_left, prev_top_left, ground_bottom_left, ground_bottom_right)) {
                    collision.y = object.y + object_size.y;
                } else {
                    //intersected through the right edge so move right
                    collision.x = object.x + object_size.x;
                }
            }
            else if(is_in(object, object_size, top_right) && !(top || right)) {
                //if intersected with the bottom edge of the ground move down
                if(check_intersection_lines(top_right, prev_top_right, ground_bottom_left, ground_bottom_right)) {
                    collision.y = object.y + object_size.y;
                } else {
                    //intersected through the left edge so move left
                    collision.x = object.x - size.x;
                }
            }
            else if(is_in(object, object_size, bottom_left) && !(bottom || left)) {
                //if it intersected with the top edge of the ground move up
                if(check_intersection_lines(bottom_left, prev_bottom_left, ground_top_left, ground_top_right)) {
                    collision.y = object.y - size.y;
                } else {
                    //intersected through the right edge so move right
                    collision.x = object.x + object_size.x;
                }
            }
            else if(is_in(object, object_size, bottom_right) && !(bottom || right)) {
                //if it intersected with the top edge of the ground move up
                if(check_intersection_lines(bottom_right, prev_bottom_right, ground_top_left, ground_top_right)) {
                    collision.y = object.y - size.y;
                } else {
                    //intersected through the left edge so move left
                    collision.x = object.x - size.x;
                }
            }

            return collision;
            // return !isColliding(pThis, other, pOther) ? new PVector(0,0) : collision;
        }

        @Override
        public PVector getSize() {
            return size;
        }
    }


}
