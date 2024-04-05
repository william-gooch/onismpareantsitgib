package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Input_Plugin.InputSystem;
import com.CS4303.group3.plugin.Map_Plugin.Ground;
import com.CS4303.group3.plugin.Player_Plugin.Player;
import com.CS4303.group3.plugin.Player_Plugin.PlayerMovement;
import com.CS4303.group3.utils.Collision.*;


import dev.dominion.ecs.api.Dominion;
import processing.core.PVector;

public class Object_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        dom = game.dom;
        
        

        //check for collisions with the edge of the play area
        game.schedule.update(() -> {
            dom.findEntitiesWith(Collider.class, Position.class, Velocity.class)
                .stream().forEach(res -> {
                    PVector position = res.comp2().position;
                    PVector velocity = res.comp3().velocity;
                    if(position.x < 0) {
                        position.x = 0;
                        velocity.x = 0;
                    } else if(position.x + res.comp1().collider.getSize().x > game.displayWidth) {
                        velocity.x = 0;
                        position.x = game.displayWidth - res.comp1().collider.getSize().x;
                    }
                });
        });

        //move object
        game.schedule.update(() -> {
            dom.findEntitiesWith(Position.class, Velocity.class)
                .stream().forEach(entity -> {
                    entity.comp1().previous_position = entity.comp1().position.copy();
                    entity.comp1().position.add(entity.comp2().velocity);
                });
        });

    }

    public static class Position {
        public PVector position = new PVector(0,0);
        public PVector previous_position = new PVector(0,0);
        public boolean grounded = false;
        
        public int walled = 0, prev_walled = 0; //used for wall jumping

        public Position() {}

        public Position(PVector pos) {
            previous_position = pos;
            position = pos;
        }
    }

    public static class Velocity {
        public PVector velocity = new PVector(0,0);
        public float mass = 1f;

        public Velocity() {}

        public Velocity(float mass) {
            this.mass = mass;
        }

        public Velocity(PVector vel) {
            velocity = vel;
        }
    }

    public static class Collider {
        Collider_Interface collider;

        private Collider(Collider_Interface collider) {
            this.collider = collider;
        }

        public static Collider BasicCollider(int width, int height) {
            return new Collider(new BasicCollider(width, height));
        }
    }
}
