package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Object_Plugin.*;

import dev.dominion.ecs.api.Dominion;
import processing.core.PVector;

public class Force_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        dom = game.dom;

        //apply gravity to all objects
        game.schedule.update(() -> {
            Gravity gravity = Resource.get(game, Gravity.class);
            dom.findEntitiesWith(Velocity.class)
                .stream().forEach(res -> {
                    PVector velocity = res.comp().velocity;
                    velocity.add(gravity.gravity); //if(!res.comp2().grounded) 
                });
        });

        //apply drag to all objects
        game.schedule.update(() -> {
            Drag drag = Resource.get(game, Drag.class);
            dom.findEntitiesWith(Velocity.class, Position.class)
                .stream().forEach(res -> {
                    PVector velocity = res.comp1().velocity;
                    Position position = res.comp2();
                    velocity.add(drag.calculate_drag(velocity, position));
                    if(velocity.mag() < 0.2) {
                        velocity.setMag(0);
                    }
                });
        });
    }

    public static class Gravity {
        public PVector gravity;

        public Gravity() {
            gravity = new PVector(0,1);
        }

        public void changeGravity(PVector new_gravity) {
            gravity = new_gravity;
        }
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
