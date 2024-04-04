package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.utils.Map;
import com.CS4303.group3.utils.Collision;

import dev.dominion.ecs.api.Dominion;
import processing.core.PVector;

public class Player_Plugin implements Plugin_Interface {
    Dominion dom;

    int playerSize = 10;

    @Override
    public void build(Game game) {
        this.dom = game.dom;
        playerSize = (game.displayHeight+game.displayWidth)/60;

        //move the player
        game.schedule.update(() -> {
            InputSystem input = Resource.get(game, InputSystem.class);
            dom.findEntitiesWith(Velocity.class, Player.class, PlayerMovement.class, Position.class)
                .stream().forEach(res -> {
                    res.comp1().velocity.set(res.comp3().newVelocity((float)game.schedule.dt(), res.comp1().velocity, input, res.comp4()));
                });
        });

        //check for collisions with the edge of the play area
        game.schedule.update(() -> {
            dom.findEntitiesWith(Player.class, Position.class, Velocity.class)
                .stream().forEach(res -> {
                    PVector position = res.comp2().position;
                    PVector velocity = res.comp3().velocity;
                    if(position.x < 0) {
                        position.x = 0;
                        velocity.x = 0;
                    } else if(position.x + res.comp1().width > game.displayWidth) {
                        velocity.x = 0;
                        position.x = game.displayWidth - res.comp1().width;
                    }
                });
        });
        

        //check collisions with ground
        game.schedule.update(() -> {
            
            dom.findEntitiesWith(Player.class, Position.class, Collider.class, Velocity.class)
                .stream().forEach(player -> {
                    //loop through ground objects, if not touching any set to not grounded
                    //look into more efficient way to do this??
                    if(!dom.findEntitiesWith(Ground.class, Position.class, Collider.class)
                    .stream().anyMatch(ground -> 
                        player.comp3().collider.collision_correction(player.comp2(), ground.comp3().collider, ground.comp2()).y != 0
                    )) {
                        player.comp2().grounded = false;
                    }

                    //correct collisions with the ground
                    dom.findEntitiesWith(Ground.class, Position.class, Collider.class)
                        .stream().forEach(ground -> {
                            PVector collision = player.comp3().collider.collision_correction(player.comp2(), ground.comp3().collider, ground.comp2());

                            //check if collided vertically
                            if(collision.y != 0) {
                                //stop velocity if going into the object -- if velocity * change in y < 0 going into the object
                                if(player.comp4().velocity.y * (collision.y - player.comp2().position.y) < 0) {
                                    //if going downwards -- y increasing - set to be grounded
                                    if(player.comp4().velocity.y > 0) {
                                        player.comp2().grounded = true;
                                        // System.out.println("Grounded");
                                    }

                                    player.comp4().velocity.y = 0;
                                }

                                //move vertically
                                player.comp2().position.y = collision.y;
                            } else {
                                //if not colliding with the floor grounded = false
                                // System.out.println("Ungrounded");
                                // player.comp2().grounded = false;
                            }

                            //check if collided horizontally
                            if(collision.x != 0) {
                                //stop velocity if going into the object
                                if(player.comp4().velocity.x * (collision.x - player.comp2().position.x) < 0) {
                                    player.comp4().velocity.x = 0;
                                }

                                //move horizontally
                                player.comp2().position.x = collision.x;
                            }
                        });
                    
                });

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

        public PlayerMovement(float acceleration, float impulseDamping, float stopDamping, float maxSpeed, float jumpSpeed, float airSlowdown) {
            this.acceleration = acceleration;
            this.impulseDamping = impulseDamping;
            this.stopDamping = stopDamping;
            this.maxSpeed = maxSpeed;
            this.jumpSpeed = jumpSpeed;
            this.airSlowdown = airSlowdown;
        }

        public PlayerMovement() {
            this(150f, 12f, 15f, 6f, 20f, 0.5f);
        }

        public PVector newVelocity(float deltaTime, PVector velocity, InputSystem input, Position position) {
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
                pressDirection.y = -1;
            }
            if (input.isKeyDown((int) 'S')) {
                //do nothing, maybe add a crouch or slide
            }

            //if grounded player is grounded can jump and accelerate  faster left and right
            if(position.grounded) {
                velocity.x += pressDirection.x * acceleration * deltaTime;
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
}
