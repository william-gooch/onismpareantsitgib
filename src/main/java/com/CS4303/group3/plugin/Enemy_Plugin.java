package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.utils.Collision;
import com.CS4303.group3.utils.Map;
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

        //move the Patrol_AI
        game.schedule.update(() -> {
            dom.findEntitiesWith(Patrol_AI.class, Object_Plugin.Position.class, Object_Plugin.Velocity.class)
                    .stream().forEach(ai -> {
                        ai.comp3().velocity.add(ai.comp1().getDirection(game, ai.comp2()).mult(game.schedule.dt()));
//                        System.out.println(ai.comp3().velocity.copy());
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
        game.schedule.update(() -> {
            dom.findEntitiesWith(Patrol_AI.class)
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

        //draw the patrol AI
        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Object_Plugin.Position.class, Patrol_AI.class)
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
        boolean flipped = true; //moves negative whichever direction it was going
        float width, height;
        Collision.Collider_Interface collider;

        public Patrol_AI(float velocity, float width, float height) {
            this.velocity = velocity;
            this.width = width;
            this.height = height;
            collider = Object_Plugin.Collider.BasicCollider(width/2, height/2).collider;
        }

        @Override
        public PVector getDirection(Game game, Object_Plugin.Position position) {
            //get movement direction
            Force_Plugin.Gravity gravity = Resource.get(game, Force_Plugin.Gravity.class);
            if(gravity == null) return new PVector(0,0);

            PVector direction = new PVector(game.abs(gravity.gravity().y), game.abs(gravity.gravity().x));
            if(flipped) direction.mult(-1);
            direction.mult(velocity*2);

            final PVector top_offset, bottom_offset;

            if(direction.x != 0) {
                if(gravity.gravity().y > 0) {
                    top_offset = new PVector(direction.x + width / 2, 0);
                    bottom_offset = new PVector(direction.x + width / 2, height*1.2f);
                } else {
                    top_offset = new PVector(direction.x + width / 2, height);
                    bottom_offset = new PVector(direction.x + width / 2, -height*0.2f);
                }
            }
            else {
                if(gravity.gravity().x > 0) {
                    top_offset = new PVector(0, direction.y + height / 2);
                    bottom_offset = new PVector(width*1.2f, direction.y + height / 2);
                } else {
                    top_offset = new PVector(width, direction.y + height / 2);
                    bottom_offset = new PVector(-width*0.2f, direction.y + height / 2);
                }
            }

            //check in front of ai, if there is ground in front turn around
            if(game.dom.findEntitiesWith(Map_Plugin.Ground.class, Object_Plugin.Position.class)
                            .stream().anyMatch(gr -> {
                                return Collision.BasicCollider.is_in(gr.comp2().position, gr.comp1().size, position.position.copy().add(top_offset));
                    })
                    || !game.dom.findEntitiesWith(Map_Plugin.Ground.class, Object_Plugin.Position.class)
                    .stream().anyMatch(gr -> {
                                return Collision.BasicCollider.is_in(gr.comp2().position, gr.comp1().size,
                                        position.position.copy().add(bottom_offset));
                            })
            ) {
                flipped = !flipped;
                return new PVector(0,0);
            } else return direction.mult(0.5f);
        }
    }
}
