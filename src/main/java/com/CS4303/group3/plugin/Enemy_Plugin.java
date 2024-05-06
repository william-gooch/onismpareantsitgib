package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.utils.Collision;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import javafx.util.Pair;
import processing.core.PVector;

import java.util.Comparator;

public class Enemy_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        dom = game.dom;
        int playerSize = (int) (game.scale/30);

        //move the Basic_AI
        game.schedule.update(() -> {
            dom.findEntitiesWith(Basic_AI.class, Object_Plugin.Position.class)
                    .stream().forEach(ai -> {
                        ai.comp2().position.add(ai.comp1().getDirection(game, ai.comp2()));
                    });
        });



        //reduce the death animation time of all ai
        game.schedule.update(() -> {
            dom.findEntitiesWith(Basic_AI.class)
                    .stream().filter(ai -> ai.comp().death_animation > 0).forEach(ai -> {
                        //reduce the time on the death animation
                        ai.comp().death_animation -= game.schedule.dt();

                        //delete entity if fully dead
                        if(ai.comp().death_animation <= 0) dom.deleteEntity(ai.entity());
                    });
        });



        //draw the basic AI
        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Object_Plugin.Position.class, Basic_AI.class)
                    .stream().forEach(res -> {
                        var pos = res.comp1().position;
                        draw.call(drawing -> {
                            //draw the player character
                            drawing.fill(255,0,0);
                            drawing.rect(pos.x, pos.y, playerSize, playerSize);
                        });
                    });
        });
    }

    public interface AI {
        //returns the movement required by the AI
        public PVector getDirection(Game game, Object_Plugin.Position position);
    }

    //basic ai follows a set path
    public static class Basic_AI implements AI {
        PVector[] path;
        int path_position;
        float velocity;
        float death_animation = 0;

        public Basic_AI(PVector[] path, int path_position, float velocity) {
            this.path = path;
            this.path_position = path_position;
            this.velocity = velocity;
        }

        public Basic_AI(PVector[] path) {
            this(path, 1, 60f);
        }

        @Override
        public PVector getDirection(Game game, Object_Plugin.Position position) {
            if(death_animation > 0) return new PVector(0,0);
            //calc direction to the next coordinate in the path
            PVector pos = position.position;
            PVector target = path[path_position];

            PVector direction = target.copy().sub(pos).limit(velocity*game.schedule.dt());
            if(direction.mag() == 0f) {
                //move to targeting the next position in the path
                path_position = path_position >= path.length - 1 ? 0 : path_position + 1;
            }

            return direction;
        }
    }

    public static class Patrol_AI implements AI {
        float velocity;
        float death_animation = 0;
        boolean flipped = false; //moves negative whichever direction it was going
        Collision.Collider_Interface collider;

        public Patrol_AI(float velocity, float width, float height) {
            this.velocity = velocity;
            collider = Object_Plugin.Collider.BasicCollider(width, height).collider;
        }

        public PVector getDirection(Game game, Object_Plugin.Position position) {
            //get movement direction
            Force_Plugin.Gravity gravity = Resource.get(game, Force_Plugin.Gravity.class);
            if(gravity == null) return new PVector(0,0);

            PVector direction = new PVector(game.abs(gravity.gravity().y),game.abs(gravity.gravity().y));
            if(flipped) direction.mult(-1);

            //use collider to check the direction that will be moved in
            Collision.Contact collision = game.dom.findEntitiesWith(Object_Plugin.Position.class, Object_Plugin.Collider.class)
                    .stream().map(other -> {
                        return collider.collide(position, new Object_Plugin.Velocity(direction.mult(velocity)), other.comp2().collider, other.comp1());
                    }).filter(c -> c != null)
                    .min(Comparator.comparing(a -> a.collisionTime()))
                    .orElse(null);

            if(collision == null) return direction;
            else return direction.mult(collision.collisionTime()*0.99f);
        }
    }
}
