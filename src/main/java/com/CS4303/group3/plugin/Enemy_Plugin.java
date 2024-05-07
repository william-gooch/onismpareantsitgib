package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Assets_Plugin.AssetManager;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.plugin.Object_Plugin.Position;
import com.CS4303.group3.plugin.Object_Plugin.Velocity;
import com.CS4303.group3.plugin.Player_Plugin.Player;
import com.CS4303.group3.plugin.Object_Plugin.Collider;
import com.CS4303.group3.plugin.Sprite_Plugin.AnimatedSprite;
import com.CS4303.group3.plugin.Sprite_Plugin.ISprite;
import com.CS4303.group3.plugin.Sprite_Plugin.SpriteRenderer;
import com.CS4303.group3.plugin.Sprite_Plugin.StateSprite;
import com.CS4303.group3.utils.Collision;
import com.CS4303.group3.utils.Collision.BasicCollider;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;

import java.util.List;

import org.tiledreader.TiledObject;

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
                        if(ai.entity().has(SpriteRenderer.class)) {
                            var sprite = ai.entity().get(SpriteRenderer.class);
                            if(sprite.sprite instanceof StateSprite) {
                                ((StateSprite) sprite.sprite).setState("dead");
                            }
                        }

                        //delete entity if fully dead
                        if(ai.comp().death_animation <= 0) dom.deleteEntity(ai.entity());
                    });

            dom.findEntitiesWith(Patrol_AI.class)
                    .stream().filter(ai -> ai.comp().death_animation > 0).forEach(ai -> {
                        //reduce the time on the death animation
                        ai.comp().death_animation -= game.schedule.dt();
                        if(ai.entity().has(SpriteRenderer.class)) {
                            var sprite = ai.entity().get(SpriteRenderer.class);
                            if(sprite.sprite instanceof StateSprite) {
                                ((StateSprite) sprite.sprite).setState("dead");
                            }
                        }

                        //delete entity if fully dead
                        if(ai.comp().death_animation <= 0) dom.deleteEntity(ai.entity());
                    });
        });

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

            dom.findEntitiesWith(Position.class, SpriteRenderer.class, Patrol_AI.class)
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

    public static Entity createEnemy(Game game, TiledObject obj, float tileScale, AI enemyAI) {
        PImage enemyImage = Resource.get(game, AssetManager.class).getResource(PImage.class, "enemy-anim.png");
        List<ISprite> frames = AnimatedSprite.framesFromSpriteSheet(enemyImage, 10);

        AnimatedSprite normalSprite = new AnimatedSprite();
        frames
            .stream()
            .limit(7)
            .forEach(f -> normalSprite.addFrame(f, 1f/30f));
        for(int i = 0; i < 16; i++) {
            normalSprite.addFrame(frames.get(7 + (i%2)), 1f/30f);
        }

        StateSprite sprite = new StateSprite();
        sprite.addState("alive", normalSprite);
        sprite.addState("dead", frames.get(9));
        sprite.setState("alive");

        Entity e = game.dom.createEntity(
            new Position(new PVector(obj.getX() * tileScale, obj.getY() * tileScale)),
            new SpriteRenderer(sprite, obj.getWidth() * tileScale, obj.getHeight() * tileScale)
        );
        e.add(enemyAI);
        e.add(new Collider(new BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale), (collision) -> {
            // check if collision normal is positive y (i.e. getting hit from above)
            PVector gravity = Resource.get(game, Gravity.class).gravity();
            if(collision.contact().cNormal().dot(gravity) > 0) {
                System.out.println("oof ouch owie im dead");
                if(collision.self().has(Enemy_Plugin.Basic_AI.class)) {
                    collision.self().get(Enemy_Plugin.Basic_AI.class).death_animation = 2f;
                }
                if(collision.self().has(Enemy_Plugin.Patrol_AI.class)) {
                    collision.self().get(Enemy_Plugin.Patrol_AI.class).death_animation = 2f;
                }
                collision.self().get(Collider.class).onCollide = null;
                if(collision.other().has(Velocity.class)) {
                    collision.other().get(Velocity.class).velocity.y *= -1;
                }
                return;
            }

            if(collision.other().has(Player.class) && collision.other().get(Player.class).invulnerability == 0f) {
                System.out.println("Damaged Player, player is now invulnerable");
                collision.other().get(Player.class).lives--;
                if (collision.other().get(Player.class).lives <= 0) {
                    //player has died, restart the level
                    System.out.println("Player has died");
                }
                collision.other().get(Player.class).invulnerability = 1f;
            }
        }, false));
        return e;
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
