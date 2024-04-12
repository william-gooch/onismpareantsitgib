package com.CS4303.group3.plugin;


import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.plugin.Box_Plugin.*;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.utils.Map;
import com.CS4303.group3.utils.Collision;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import processing.core.PVector;

public class Player_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        this.dom = game.dom;

        //set the players velocity
        game.schedule.update(() -> {
            InputSystem input = Resource.get(game, InputSystem.class);
            Gravity gravity = Resource.get(game, Gravity.class);
            dom.findEntitiesWith(Velocity.class, Player.class, PlayerMovement.class, Position.class)
                .stream().forEach(res -> {
                    res.comp1().velocity.set(res.comp3().newVelocity((float)game.schedule.dt(), res.comp1().velocity, input, res.comp4(), res.comp1().mass, gravity));
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
                                    //check collision zone above head (direction against gravity)
                                        PVector above_head = player.comp3().position.copy();
                                        if(gravity.gravity.x == -1) above_head.x += player.comp4().collider.getSize().x;
                                        if(gravity.gravity.x == 1) above_head.x -= box.comp1().collider.getSize().x;
                                        if(gravity.gravity.y == -1) above_head.y += player.comp4().collider.getSize().y;
                                        if(gravity.gravity.y == 1) above_head.y -= box.comp1().collider.getSize().y;
                                    
                                        if(!dom.findEntitiesWith(Collider.class, Position.class)
                                            .stream().anyMatch(object -> 
                                                box.comp1().collider.collision_correction(new Position(above_head), object.comp1().collider, object.comp2()).mag() > 0 && object.comp2().position != box.comp2().position
                                        )) {
                                            
                                                player.comp2().box = box.entity();
                                                box.comp3().player = player.entity();
                                                input.keysDown.remove((int) 'E');
            
                                                //move the box to above the player
                                                box.comp2().previous_position = box.comp2().position;
                                                box.comp2().position.x = player.comp3().position.x;
                                                box.entity().removeType(Velocity.class); //until collisions are fixed
                                                box.comp2().position = above_head;
            
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
                    // if(res.comp1().grounded) System.out.println("Grounded");
                    // else if(res.comp1().walled != 0) System.out.println("On a wall");
                    // else System.out.println("Airborne");
                    var pos = res.comp1().position;
                    draw.call(drawing -> {
                        //draw the player character
                        drawing.fill(128);
                        drawing.rect(pos.x, pos.y, res.comp2().width, res.comp2().height);
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

        public PVector newVelocity(float deltaTime, PVector velocity, InputSystem input, Position position, float mass, Gravity gravity) {
            PVector pressDirection = new PVector(0, 0);

            //look into a cleaner way to do the direction selection based on gravity
            if (input.isKeyDown((int) 'A')) {
                //move left
                pressDirection.x = -1;
                // System.out.println("Left");
            }
            if (input.isKeyDown((int) 'D')) {
                //move right
                pressDirection.x = 1;
                // System.out.println("Right");
            }
            if (input.isKeyDown((int) 'W')) {
                //jump if grounded
                // input.keysDown.remove((int) 'W'); //makes wall jumping and movement require more skill
                pressDirection.y = -1;
                // System.out.println("Up");
            }
            if (input.isKeyDown((int) 'S')) {
                //do nothing, maybe add a crouch or slide
                // System.out.println("Down");
            }

            //if grounded player is grounded can jump and accelerate  faster left and right
            // position.grounded = true;
            if(position.grounded) {
                //give sideways movement if perpendicular to gravity
                if(gravity.gravity.y != 0) { //move on the x axis
                    velocity.x += pressDirection.x * acceleration * deltaTime * mass * gravity.gravity.y;
                } else if(gravity.gravity.x != 0) { //move on the y axis
                    velocity.y += pressDirection.x * acceleration * deltaTime * mass * -gravity.gravity.x;
                }
                if ((Math.abs(velocity.x) > maxSpeed && gravity.gravity.y != 0) || (Math.abs(velocity.y) > maxSpeed && gravity.gravity.x != 0)) {
                    if(gravity.gravity.y != 0) velocity.x *= 1 - (impulseDamping * deltaTime);
                    if(gravity.gravity.x != 0) velocity.y *= 1 - (impulseDamping * deltaTime);
                }

                if(pressDirection.y == -1) { //jump
                    if(gravity.gravity.y != 0) { //jump on y axis
                        position.grounded = false;
                        velocity.y = jumpSpeed * -gravity.gravity.y;
                    } else if(gravity.gravity.x != 0) { //jump on x axis
                        position.grounded = false;
                        velocity.x = jumpSpeed * -gravity.gravity.x;
                    }
                }
                // if(pressDirection.y == -gravity.gravity.y) { //jumping while gravity on y axis
                //     position.grounded = false;
                //     velocity.y = jumpSpeed * pressDirection.y;
                // } else if(pressDirection.x == -gravity.gravity.x) { //jumping while gravity on x axis
                //     position.grounded = false;
                //     velocity.x = jumpSpeed * pressDirection.x;
                // }

            } else {
                //give sideways movement if perpendicular to gravity
                if(gravity.gravity.y != 0) { //move on the x axis
                    velocity.x += pressDirection.x * acceleration * deltaTime * airSlowdown * mass * gravity.gravity.y;
                } else if(gravity.gravity.x != 0) { //move on the y axis
                    velocity.y += pressDirection.x * acceleration * deltaTime * airSlowdown * mass * -gravity.gravity.x;
                }
                if ((Math.abs(velocity.x) > maxSpeed && gravity.gravity.y != 0) || (Math.abs(velocity.y) > maxSpeed && gravity.gravity.x != 0)) {
                    if(gravity.gravity.y != 0) velocity.x *= 1 - (impulseDamping * deltaTime);
                    if(gravity.gravity.x != 0) velocity.y *= 1 - (impulseDamping * deltaTime);
                }


                //TODO: make this dependent on gravity
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
        public float height, width;
        public Entity box = null;

        public Player(float height, float width) {
            this.height = height;
            this.width = width;
        }
    }
}
