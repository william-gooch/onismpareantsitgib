package com.CS4303.group3.plugin;


import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.plugin.Box_Plugin.*;
import com.CS4303.group3.plugin.Force_Plugin.*;
import com.CS4303.group3.utils.Map;
import com.CS4303.group3.utils.Collision;
import com.CS4303.group3.utils.Collision.BasicCollider;
import com.CS4303.group3.utils.Collision.Collider_Interface;
import com.CS4303.group3.utils.Collision.Contact;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import processing.core.PVector;

public class Player_Plugin implements Plugin_Interface {
    Dominion dom;

    int playerSize = 10;

    @Override
    public void build(Game game) {
        this.dom = game.dom;
        playerSize = (int) (game.scale/30);

        game.schedule.update(() -> {
            dom.findEntitiesWith(Position.class, Grab.class)
                .stream().forEach(grabber -> {
                    if(grabber.comp2().grabObj != null) {
                        var grabPos = grabber.comp2().grabObj.get(Position.class);
                        PVector gravity = Resource.get(game, Gravity.class).gravity;
                        grabPos.previous_position = grabPos.position;
                        //set to be above head dependent on gravity
                        grabPos.position = PVector.add(grabber.comp1().position, new PVector(-playerSize * 1.2f * gravity.x, -playerSize * 1.2f * gravity.y, 0));
                    }
                });
        });

        game.schedule.update(() -> {
            dom.findEntitiesWith(Grab.class)
                .stream().forEach(grab -> {
                    if(grab.comp().grabObj != null) { //ensures that doesn't count collisions with box that it is carrying
                        grab.comp().grabObj
                            .get(Body.class).disableCollision();
                    }
                });
        });

        //set the players velocity
        game.schedule.update(() -> {
            InputSystem input = Resource.get(game, InputSystem.class);
            Gravity gravity = Resource.get(game, Gravity.class);
            dom.findEntitiesWith(Velocity.class, Player.class, PlayerMovement.class, Position.class)
                .stream().forEach(res -> {
                    res.comp1().velocity.set(res.comp3().newVelocity((float)game.schedule.dt(), res.comp1().velocity, input, res.comp4(), res.comp1().mass, gravity.gravity));
                });

            //if input is pickup, pick up box in the vicinity -- have a way to check this only every so often
            if(input.isKeyDown((int) 'E')) {
                //get the player
                dom.findEntitiesWith(Velocity.class, Player.class, Position.class, Collider.class)
                    .stream().forEach(player -> {
                        var grab = player.entity().get(Grab.class);
                        if(grab.grabObj == null) {
                            //find a nearby box to pickup and pick it up
                            dom.findEntitiesWith(Collider.class, Position.class, Grabbable.class)
                                .stream().forEach(box -> {
                                    // //if colliding with box pick it up
                                    Contact collision = player.comp4().collider.collide(player.comp3(), box.comp1().collider, box.comp2());

                                    if(collision != null) {
                                    //check collision zone above head
                                    
                                        if(!dom.findEntitiesWith(Collider.class, Position.class)
                                            .stream().anyMatch(object -> {
                                                Contact c = box.comp1().collider.collide(new Position(new PVector(player.comp3().position.x, player.comp3().position.y - 100)), object.comp1().collider, object.comp2());
                                                return c != null && object.comp2().position != box.comp2().position;
                                            })) {
                                            
                                                grab.grabObj = box.entity();
                                                box.comp3().player = player.entity();
                                                input.keysDown.remove((int) 'E');
            
                                                //move the box to above the player
                                                box.comp2().previous_position = box.comp2().position;
                                                box.comp2().position.x = player.comp3().position.x;
                                                box.entity().removeType(Velocity.class); //until collisions are fixed
                                                // box.comp2().position.y = player.comp3().position.y - player.comp4().collider.getSize().y;
            
                                            }
                                    }
                                    
                                });
                        } else {
                            grab.grabObj.get(Body.class).enableCollision();
                            //throw the box
                            if(gravity.gravity.y != 0) {
                                if (player.comp1().velocity.x > 0) { //throw to the right
                                    //give the players box a velocity and chuck it
                                    grab.grabObj.add(new Velocity(new PVector(12f, -4f * gravity.gravity.y)));
                                } else { //throw to the left
                                    grab.grabObj.add(new Velocity(new PVector(-12f, -4f * gravity.gravity.y)));
                                }
                            } else if(gravity.gravity.x != 0) {
                                if (player.comp1().velocity.y > 0) { //throw to the right
                                    //give the players box a velocity and chuck it
                                    grab.grabObj.add(new Velocity(new PVector(-4f * gravity.gravity.x, 12f)));
                                } else { //throw to the left
                                    grab.grabObj.add(new Velocity(new PVector(-4f * gravity.gravity.x, -12f)));
                                }
                            }
                            input.keysDown.remove((int) 'E');
                            grab.grabObj.get(Grabbable.class).player = null;
                            grab.grabObj = null;
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

        public PVector newVelocity(float deltaTime, PVector velocity, InputSystem input, Position position, float mass, PVector gravity) {
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
                velocity.x += pressDirection.x * acceleration * deltaTime * mass * gravity.y;
                velocity.y += pressDirection.x * acceleration * deltaTime * mass * -gravity.x;

                if (Math.abs(velocity.x) > maxSpeed && gravity.y != 0) velocity.x *= 1 - (impulseDamping * deltaTime);
                if (Math.abs(velocity.y) > maxSpeed && gravity.x != 0) velocity.y *= 1 - (impulseDamping * deltaTime);

                if(pressDirection.y == -1) {
                    position.grounded = false;
                    if(gravity.y != 0) velocity.y = -jumpSpeed * gravity.y;
                    if(gravity.x != 0) velocity.x = -jumpSpeed * gravity.x;
                }
            } else {
                velocity.x += pressDirection.x * acceleration * deltaTime * airSlowdown * gravity.y;
                velocity.y += pressDirection.x * acceleration * deltaTime * airSlowdown * -gravity.x;

                if (Math.abs(velocity.x) > maxSpeed && gravity.y != 0) velocity.x *= 1 - (impulseDamping * deltaTime);
                if (Math.abs(velocity.y) > maxSpeed && gravity.x != 0) velocity.y *= 1 - (impulseDamping * deltaTime);

                if(position.walled != 0 && position.walled != position.prev_walled && pressDirection.y == -1) {

                    if(gravity.y != 0) {
                        velocity.y = -jumpSpeed * wallJumpPower * gravity.y;
                        velocity.x = -position.walled * jumpSpeed * wallJumpPower;
                    } else if(gravity.x != 0) {
                        velocity.x = -jumpSpeed * wallJumpPower * gravity.x;
                        velocity.y = -position.walled * jumpSpeed * wallJumpPower;
                    }

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

        public Player(int height, int width) {
            this.height = height;
            this.width = width;
        }
    }

    static class Grab {
        public Entity grabObj = null;

        public Position aboveHead;
        public Collider_Interface aboveHeadCollider;

        public Grab(float spaceSize) {
            this.aboveHead = new Position();
            this.aboveHeadCollider = new BasicCollider(spaceSize, spaceSize);
        }
    }
}
