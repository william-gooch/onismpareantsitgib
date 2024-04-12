package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Box_Plugin.Box;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.plugin.Input_Plugin.InputSystem;
import com.CS4303.group3.plugin.Map_Plugin.Ground;
import com.CS4303.group3.plugin.Player_Plugin.Player;
import com.CS4303.group3.plugin.Player_Plugin.PlayerMovement;
import com.CS4303.group3.utils.Collision.*;


import dev.dominion.ecs.api.*;
import processing.core.PVector;

public class Object_Plugin implements Plugin_Interface {
    Dominion dom;

    boolean deleted_entity;

    @Override
    public void build(Game game) {
        dom = game.dom;
        
        

        

        //move object
        game.schedule.update(() -> {
            dom.findEntitiesWith(Position.class, Velocity.class, Collider.class)
                .stream().forEach(entity -> {
                    entity.comp1().previous_position = entity.comp1().position.copy();
                    entity.comp1().position.add(entity.comp2().velocity);

                    //if entity is a player and holding a box move the box
                    if(entity.entity().has(Player.class)) {
                        if(entity.entity().get(Player.class).box != null) {
                            PVector gravity = Resource.get(game, Gravity.class).gravity;
                            entity.entity().get(Player.class).box.get(Position.class).previous_position = entity.entity().get(Player.class).box.get(Position.class).position;
                            

                            entity.entity().get(Player.class).box.get(Position.class).position = entity.comp1().position.copy();
                            
                            if(gravity.y == 1) entity.entity().get(Player.class).box.get(Position.class).position.y -= entity.entity().get(Player.class).box.get(Collider.class).collider.getSize().y;
                            if(gravity.y == -1) entity.entity().get(Player.class).box.get(Position.class).position.y += entity.comp3().collider.getSize().y;
                            if(gravity.x == 1) entity.entity().get(Player.class).box.get(Position.class).position.x -= entity.entity().get(Player.class).box.get(Collider.class).collider.getSize().x;
                            if(gravity.x == -1) entity.entity().get(Player.class).box.get(Position.class).position.x += entity.comp3().collider.getSize().x;
                        }
                    }
                });
        });

        //check for collisions
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

        //check collisions with anything with position and collider
        game.schedule.update(() -> {
            PVector gravity = Resource.get(game, Gravity.class).gravity;
            dom.findEntitiesWith(Position.class, Collider.class).without(Ground.class)
                .stream().forEach(player -> {
                    deleted_entity = false;
                    //disables the current entity
                    player.entity().setEnabled(false);

                    if(player.entity().has(Box.class)) {
                        if(player.entity().get(Box.class).player != null) { //ensures that doesn't count collisions with player that is carrying
                            player.entity().get(Box.class).player.setEnabled(false);
                        }
                    }

                    if(player.entity().has(Player.class)) {
                        if(player.entity().get(Player.class).box != null) { //ensures that doesn't count collisions with box that it is carrying
                            player.entity().get(Player.class).box.setEnabled(false);
                        }
                    }

                    //loop through ground objects, if not touching any set to not grounded
                    if(!dom.findEntitiesWith(Position.class, Collider.class)
                    .stream().anyMatch(ground -> 
                        player.comp2().collider.collision_correction(player.comp1(), ground.comp2().collider, ground.comp1()).y != 0
                    )) {
                        if(gravity.y != 0) player.comp1().grounded = false;
                        else player.comp1().walled = 0;
                    }

                    if(!dom.findEntitiesWith(Position.class, Collider.class, Ground.class)
                    .stream().anyMatch(ground -> 
                        player.comp2().collider.collision_correction(player.comp1(), ground.comp2().collider, ground.comp1()).x != 0
                    )) {
                        if(gravity.y != 0) player.comp1().walled = 0;
                        else player.comp1().grounded = false;
                    }

                    //correct collisions with the ground
                    dom.findEntitiesWith(Position.class, Collider.class)
                        .stream().forEach(ground -> {
                            //check that it isn't looking at itself
                            if(ground.entity().isEnabled() && !deleted_entity) {
                                PVector collision = player.comp2().collider.collision_correction(player.comp1(), ground.comp2().collider, ground.comp1());

                                //check if collided vertically
                                if(collision.y != 0) {
                                    if(ground.entity().has(Velocity.class) && gravity.x != 0) {
                                        //maybe add realistic momentum calculations here

                                        //move player out of the object
                                        player.comp1().position.y = collision.y;

                                        //calculate the new velocity for both objects
                                        if(player.entity().has(Velocity.class)) {
                                            player.entity().get(Velocity.class).velocity.y = player.entity().get(Velocity.class).velocity.y*0.75f;
                                            ground.entity().get(Velocity.class).velocity.y = player.entity().get(Velocity.class).velocity.y;
                                        }// else {
                                        //     // stop the block being carried
                                        //     player.entity().add(new Velocity(0.5f));
                                        //     player.entity().get(Box.class).player.setEnabled(true);
                                        //     player.entity().get(Box.class).player.get(Player.class).box = null;
                                        //     player.entity().get(Box.class).player = null;
                                        //     player.entity().setEnabled(true);
                                        //     //move position to 
                                        //     dom.createEntityAs(player.entity(), new Velocity(0.5f));
                                        //     dom.deleteEntity(player.entity());
                                        //     deleted_entity = true;
                                        // }
                                    }
                                    else {
                                    //stop velocity if going into the object -- if velocity * change in y < 0 going into the object
                                    
                                        if(player.entity().has(Velocity.class)) {
                                            if(player.entity().get(Velocity.class).velocity.y * (collision.y - player.comp1().position.y) < 0) {
                                                //if going downwards -- y increasing - set to be grounded
                                                if(gravity.y != 0) {
                                                    //can be grounded
                                                    if(player.entity().get(Velocity.class).velocity.y * gravity.y > 0) {
                                                        player.comp1().grounded = true;
                                                        player.comp1().prev_walled = 0;
                                                        player.comp1().walled = 0;
                                                        // System.out.println("Grounded");
                                                    }
                                                } else if(gravity.x != 0) {
                                                    //going into a wall
                                                    player.comp1().walled = (int)((collision.y - player.comp1().position.y) / Math.abs(collision.y - player.comp1().position.y)); //opposite direction of collision
                                                }

                                                player.entity().get(Velocity.class).velocity.y = 0;
                                            }

                                            player.comp1().position.y = collision.y;

                                        } else {
                                            //stop the block being carried
                                            // player.entity().add(new Velocity());
                                            // player.entity().get(Box.class).player.setEnabled(true);
                                            // player.entity().get(Box.class).player.get(Player.class).box = null;
                                            // player.entity().get(Box.class).player = null;
                                            // player.entity().setEnabled(true);
                                            // dom.createEntityAs(player.entity(), new Velocity(0.5f));
                                            // dom.deleteEntity(player.entity());
                                            // deleted_entity = true;

                                            //stop the block and player
                                            player.comp1().position.y = collision.y+1;
                                            if(gravity.y != 0) player.entity().get(Box.class).player.get(Position.class).position.y = player.comp1().position.y + player.comp2().collider.getSize().y;
                                            else if(gravity.x != 0) player.entity().get(Box.class).player.get(Position.class).position.y = player.comp1().position.y;
                                            player.entity().get(Box.class).player.get(Velocity.class).velocity.y = 0;
                                        }
                                    }
                                }

                                //check if collided horizontally
                                //issue with setting enabled to false
                                if(collision.x != 0 && !deleted_entity) {
                                    if(ground.entity().has(Velocity.class) && gravity.y != 0) {
                                        //maybe add realistic momentum calculations here

                                        //move player out of the object
                                        player.comp1().position.x = collision.x;

                                        //calculate the new velocity for both objects
                                        if(player.entity().has(Velocity.class)) {
                                            player.entity().get(Velocity.class).velocity.x = player.entity().get(Velocity.class).velocity.x*0.75f;
                                            ground.entity().get(Velocity.class).velocity.x = player.entity().get(Velocity.class).velocity.x;
                                        }// else {
                                        //     // stop the block being carried
                                        //     player.entity().add(new Velocity(0.5f));
                                        //     player.entity().get(Box.class).player.setEnabled(true);
                                        //     player.entity().get(Box.class).player.get(Player.class).box = null;
                                        //     player.entity().get(Box.class).player = null;
                                        //     player.entity().setEnabled(true);
                                        //     //move position to 
                                        //     dom.createEntityAs(player.entity(), new Velocity(0.5f));
                                        //     dom.deleteEntity(player.entity());
                                        //     deleted_entity = true;
                                        // }
                                    }
                                    else {
                                        //stop velocity if going into the object
                                        if(player.entity().has(Velocity.class)) {

                                            //if going into the collision
                                            if(player.entity().get(Velocity.class).velocity.x * (collision.x - player.comp1().position.x) < 0) {
                                                //if going downwards -- y increasing - set to be grounded
                                                if(gravity.x != 0) {
                                                    //can be grounded
                                                    if(player.entity().get(Velocity.class).velocity.x * gravity.x > 0) {
                                                        player.comp1().grounded = true;
                                                        player.comp1().prev_walled = 0;
                                                        player.comp1().walled = 0;
                                                        // System.out.println("Grounded");
                                                    }
                                                } else if(gravity.y != 0 && ground.entity().has(Ground.class)) {
                                                    //going into a wall
                                                    player.comp1().walled = (int)((collision.x - player.comp1().position.x) / Math.abs(collision.x - player.comp1().position.x)); //opposite direction of collision
                                                }
    
                                                player.entity().get(Velocity.class).velocity.x = 0;
                                            }

                                            //move out of the object
                                            player.comp1().position.x = collision.x;
                                        } else {
                                            //stop the block being carried if the player is still moving (This allows for wall jumping with the blocks - discuss if we want this)
                                            if(player.entity().get(Box.class).player.get(Velocity.class).velocity.x != 0) {
                                                player.entity().get(Box.class).player.setEnabled(true);
                                                player.entity().get(Box.class).player.get(Player.class).box = null;
                                                player.entity().get(Box.class).player = null;
                                                player.entity().setEnabled(true);
                                                dom.createEntityAs(player.entity(), new Velocity(0.5f));
                                                dom.deleteEntity(player.entity());
                                                deleted_entity = true;
                                            }

                                            //stop the player
                                            // player.comp1().position.x = collision.x;
                                            // player.entity().get(Box.class).player.get(Position.class).position.x = player.comp1().position.x;
                                            // player.entity().get(Box.class).player.get(Velocity.class).velocity.x = 0;

                                        }
                                    }
                                }
                            }
                        });

                    if(!deleted_entity) {
                        player.entity().setEnabled(true);
                        if(player.entity().has(Box.class)) {
                            if(player.entity().get(Box.class).player != null) {
                                player.entity().get(Box.class).player.setEnabled(true);
                            }
                        }
                        if(player.entity().has(Player.class)) {
                            if(player.entity().get(Player.class).box != null) { //ensures that doesn't count collisions with box that it is carrying
                                player.entity().get(Player.class).box.setEnabled(true);
                            }
                        }
                    }
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

        public static Collider BasicCollider(float width, float height) {
            return new Collider(new BasicCollider(width, height));
        }
    }
}
