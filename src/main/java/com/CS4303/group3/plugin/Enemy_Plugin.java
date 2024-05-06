package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.plugin.Object_Plugin.Position;
import com.CS4303.group3.plugin.Sprite_Plugin.SpriteRenderer;
import com.CS4303.group3.utils.Collision;
import dev.dominion.ecs.api.Dominion;
import processing.core.PConstants;
import processing.core.PVector;

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

        // //draw the basic AI
        // game.schedule.draw(draw -> {
        //     dom.findEntitiesWith(Object_Plugin.Position.class, Basic_AI.class)
        //             .stream().forEach(res -> {
        //                 var pos = res.comp1().position;
        //                 draw.call(drawing -> {
        //                     //draw the player character
        //                     drawing.fill(255,0,0);
        //                     drawing.rect(pos.x, pos.y, playerSize, playerSize);
        //                 });
        //             });
        // });

        game.schedule.update(() -> {
            dom.findEntitiesWith(Position.class, SpriteRenderer.class, Basic_AI.class)
                .stream().forEach(player -> {
                    PVector gravity = (PVector) Resource.get(game, Gravity.class).get();
                    player.comp2().rotation = gravity.heading() - PConstants.PI/2;

                    float velocityPerpToGravity = player.comp3().getDirection(game, player.comp1()).dot(gravity.copy().rotate(PConstants.PI/2));
                    if(velocityPerpToGravity > 0) {
                        player.comp2().flipX = true;
                    } else if (velocityPerpToGravity < 0) {
                        player.comp2().flipX = false;
                    }
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
}
