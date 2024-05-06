package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Input_Plugin.InputSystem;
import com.CS4303.group3.plugin.Object_Plugin.*;

import com.CS4303.group3.utils.Changeable;
import com.CS4303.group3.utils.Changeable_Interface;
import dev.dominion.ecs.api.Dominion;
import processing.core.PVector;

public class Force_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        dom = game.dom;

        game.schedule.update(() -> {
            InputSystem input = Resource.get(game, InputSystem.class);
            Gravity gravity = Resource.get(game, Gravity.class);
            if(input == null || gravity == null) return;
            if(input.isKeyDown(39)) { //gravity goes right
                game.rotation_time = 0f;
                gravity.change(new PVector(1,0));
            }
            if(input.isKeyDown(37)) { //gravity goes left
                game.rotation_time = 0f;
                gravity.change(new PVector(-1,0));
            }
            if(input.isKeyDown(40)) { //gravity goes down
                game.rotation_time = 0f;
                gravity.change(new PVector(0,1));
            }
            if(input.isKeyDown(38)) { //gravity goes up
                game.rotation_time = 0f;
                gravity.change(new PVector(0,-1));
            }
        });

        //apply gravity to all objects
        game.schedule.update(() -> {
            Gravity gravity = Resource.get(game, Gravity.class);
            if(gravity == null) return;
            dom.findEntitiesWith(Velocity.class)
                .stream().forEach(res -> {
                    PVector velocity = res.comp().velocity;
                    velocity.add(gravity.gravity().copy().mult(game.scale/10f * game.schedule.dt())); //if(!res.comp2().grounded)
                });
        });

        //apply drag to all objects
        game.schedule.update(() -> {
            Drag drag = Resource.get(game, Drag.class);
            if(drag == null) return;
            dom.findEntitiesWith(Velocity.class, Position.class)
                .stream().forEach(res -> {
                    PVector velocity = res.comp1().velocity;
                    Position position = res.comp2();
                    velocity.add(drag.calculate_drag(velocity, position).mult(game.scale/12f * game.schedule.dt()));
                    if(velocity.mag() < game.scale/1200f) {
                        velocity.setMag(0);
                    }
                });
        });
    }

    public static class Gravity extends Changeable_Interface {

        public Gravity(PVector value) {
            super(value);
        }

        public Gravity() {
            super(new PVector(0,0));
        }

        public void changeGravity(PVector new_gravity) {
//            game.rotation_time = 0f; set the games rotation time to 0
            change(new_gravity.setMag(((PVector)get()).mag()));
        }

        @Override
        public void change(Object value) {
            super.change(((PVector) value).setMag(((PVector)get()).mag()));
        }

        public PVector gravity() {return (PVector) get();}
    }

    public static class Drag {
        float drag_coefficient;
        float grounded_multiplier;

        public Drag(float gm, float dc) {
            grounded_multiplier = gm;
            drag_coefficient = dc;
        }

        public Drag() {
            this(15f, 0.01f);
        }

        public PVector calculate_drag(PVector velocity, Position position) {
            //apply drag against the velocity
            PVector drag = velocity.copy().mult(-1*drag_coefficient);

            //if grounded slow down more due to friction on the floor
            if(position.grounded) drag.mult(grounded_multiplier);

            //if walled slow down due to friction of the wall
            if(position.walled != 0) drag.mult(grounded_multiplier);

            return drag;
        }
    }
}
