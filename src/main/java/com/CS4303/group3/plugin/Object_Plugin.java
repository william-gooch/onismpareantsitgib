package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.utils.Collision.*;

import dev.dominion.ecs.api.Dominion;
import processing.core.PVector;

public class Object_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        dom = game.dom;

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

        public Position() {}

        public Position(PVector pos) {
            previous_position = pos;
            position = pos;
        }
    }

    public static class Velocity {
        public PVector velocity = new PVector(0,0);

        public Velocity() {}

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
