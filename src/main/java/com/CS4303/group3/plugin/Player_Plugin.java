package com.CS4303.group3.plugin;


import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.plugin.Box_Plugin.*;
import com.CS4303.group3.utils.Map;
import com.CS4303.group3.utils.Collision;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import processing.core.PVector;

public class Player_Plugin implements Plugin_Interface {
    Dominion dom;

    int playerSize = 10;

    @Override
    public void build(Game game) {
        this.dom = game.dom;
        playerSize = (game.displayHeight+game.displayWidth)/60;

        //set the players velocity
        game.schedule.update(() -> {
            InputSystem input = Resource.get(game, InputSystem.class);
            dom.findEntitiesWith(Velocity.class, Player.class, PlayerMovement.class, Position.class)
                .stream().forEach(res -> {
                    res.comp1().velocity.set(res.comp3().newVelocity((float)game.schedule.dt(), res.comp1().velocity, input, res.comp4(), res.comp1().mass));
                });

            //if input is pickup, pick up box in the vicinity -- have a way to check this only every so often
            if(input.isKeyDown((int) 'E')) {
                //get the player
                dom.findEntitiesWith(Velocity.class, Player.class, Position.class, Collider.class)
                    .stream().forEach(player -> {
                        if(player.comp2().box == null) {
                            player.entity().setEnabled(false);
                            //find a nearby box to pickup and pick it up
                            dom.findEntitiesWith(Collider.class, Position.class, Box.class)
                                .stream().forEach(box -> {
                                    //if colliding with box pick it up
                                    PVector collision = player.comp4().collider.collision_correction(player.comp3(), box.comp1().collider, box.comp2());

                                    if(collision.mag() > 0) {
                                    //check collision zone above head
                                    
                                        if(!dom.findEntitiesWith(Collider.class, Position.class)
                                            .stream().anyMatch(object -> 
                                                box.comp1().collider.collision_correction(new Position(new PVector(player.comp3().position.x, player.comp3().position.y - player.comp4().collider.getSize().y)), object.comp1().collider, object.comp2()).mag() > 0 && object.comp2().position != box.comp2().position
                                            )) {
                                            
                                                player.comp2().box = box.entity();
                                                box.comp3().player = player.entity();
                                                input.keysDown.remove((int) 'E');
            
                                                //move the box to above the player
                                                box.comp2().previous_position = box.comp2().position;
                                                box.comp2().position.x = player.comp3().position.x;
                                                box.entity().removeType(Velocity.class); //until collisions are fixed
                                                box.comp2().position.y = player.comp3().position.y - player.comp4().collider.getSize().y;
            
                                            }
                                    }
                                    
                                });
                            player.entity().setEnabled(true);
                        } else {
                            //throw the box
                            if(player.comp1().velocity.x > 0) { //throw to the right
                                //give the players box a velocity and chuck it
                                player.comp2().box.add(new Velocity(new PVector(12f, -4f)));
                            } else { //throw to the left
                                player.comp2().box.add(new Velocity(new PVector(-12f, -4f)));
                            }
                            input.keysDown.remove((int) 'E');
                            player.comp2().box.get(Box.class).player = null;
                            player.comp2().box = null;
                        }
                    });
            }
        });      
        
        


        //draw the player
        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Position.class, Player.class)
                .stream().forEach(res -> {
                    var pos = res.comp1().position;
                    draw.call(drawing -> {
                        //draw the player character
                        drawing.fill(128);
                        drawing.rect(pos.x, pos.y, playerSize, playerSize);
                    });
                });
        });
    }

    static class PlayerMovement {
        float acceleration;
        float impulseDamping;
        float stopDamping;
        float maxSpeed;
        float jumpSpeed;
        float airSlowdown;
        float wallJumpPower;

        public PlayerMovement(float acceleration, float impulseDamping, float stopDamping, float maxSpeed, float jumpSpeed, float airSlowdown, float wallJumpPower) {
            this.acceleration = acceleration;
            this.impulseDamping = impulseDamping;
            this.stopDamping = stopDamping;
            this.maxSpeed = maxSpeed;
            this.jumpSpeed = jumpSpeed;
            this.airSlowdown = airSlowdown;
            this.wallJumpPower = wallJumpPower;
        }

        public PlayerMovement() {
            this(150f, 12f, 15f, 6f, 20f, 0.3f, 0.7f);
        }

        public PVector newVelocity(float deltaTime, PVector velocity, InputSystem input, Position position, float mass) {
            PVector pressDirection = new PVector(0, 0);
            if (input.isKeyDown((int) 'A')) {
                //move left
                pressDirection.x = -1;
            }
            if (input.isKeyDown((int) 'D')) {
                //move right
                pressDirection.x = 1;
            }
            if (input.isKeyDown((int) 'W')) {
                //jump if grounded
                // input.keysDown.remove((int) 'W'); //makes wall jumping and movement require more skill
                pressDirection.y = -1;
            }
            if (input.isKeyDown((int) 'S')) {
                //do nothing, maybe add a crouch or slide
            }

            //if grounded player is grounded can jump and accelerate  faster left and right
            if(position.grounded) {
                velocity.x += pressDirection.x * acceleration * deltaTime * mass;
                if (Math.abs(velocity.x) > maxSpeed) {
                    velocity.x *= 1 - (impulseDamping * deltaTime);
                }

                if(pressDirection.y == -1) {
                    position.grounded = false;
                    velocity.y = -jumpSpeed;
                }
            } else {
                velocity.x += pressDirection.x * acceleration * deltaTime * airSlowdown;
                if (Math.abs(velocity.x) > maxSpeed) {
                    velocity.x *= 1 - (impulseDamping * deltaTime);
                }

                if(position.walled != 0 && position.walled != position.prev_walled && pressDirection.y == -1) {
                    velocity.y = -jumpSpeed * wallJumpPower;
                    velocity.x = position.walled * jumpSpeed * wallJumpPower;
                    position.prev_walled = position.walled;
                    position.walled = 0;
                }

            }

            //slowdown done in the forces plugin - drag, gravity, etc.


            return velocity;
        }
    }

    static class Player {
        public int height, width;
        public Entity box = null;

        public Player(int height, int width) {
            this.height = height;
            this.width = width;
        }
    }
}
