package com.CS4303.group3.plugin;


import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Sprite_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.plugin.Box_Plugin.*;
import com.CS4303.group3.plugin.Force_Plugin.*;
import com.CS4303.group3.plugin.Game_Plugin.WorldManager;
import com.CS4303.group3.utils.Collision;
import com.CS4303.group3.utils.Collision.BasicCollider;
import com.CS4303.group3.utils.Collision.Collider_Interface;
import com.CS4303.group3.utils.Collision.Contact;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import processing.core.PConstants;
import processing.core.PVector;

public class Player_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        this.dom = game.dom;

        game.schedule.update(() -> {
            dom.findEntitiesWith(Velocity.class, SpriteRenderer.class, Grab.class)
                .withAlso(Player.class)
                .stream().forEach(player -> {
                    PVector gravity = (PVector) Resource.get(game, Gravity.class).get();
                    player.comp2().rotation = gravity.heading() - PConstants.PI/2;

                    float velocityPerpToGravity = player.comp1().velocity.dot(gravity.copy().rotate(PConstants.PI/2));
                    if(velocityPerpToGravity > 0) {
                        player.comp2().flipX = true;
                    } else if (velocityPerpToGravity < 0) {
                        player.comp2().flipX = false;
                    }

                    if(player.comp3().grabObj != null) {
                        ((StateSprite) player.comp2().sprite).setState("throw");
                    } else {
                        if(Math.abs(velocityPerpToGravity) > 0.2f) {
                            ((StateSprite) player.comp2().sprite).setState("run");
                        } else {
                            ((StateSprite) player.comp2().sprite).setState("idle");
                        }
                    }
                });
        });

        game.schedule.update(() -> {
            dom.findEntitiesWith(Position.class)
                .withAlso(Player.class)
                .stream().forEach(player -> {
                    if(player.comp().position.x < 0
                    || player.comp().position.x > game.width
                    || player.comp().position.y < 0
                    || player.comp().position.y > game.width) {
                        WorldManager wm = Resource.get(game, WorldManager.class);
                        wm.gameOver();
                    }
                });
        });

        game.schedule.update(() -> {
            dom.findEntitiesWith(Position.class, Grab.class)
                .stream().forEach(grabber -> {
                    if(grabber.comp2().grabObj != null) {
                        var grabPos = grabber.comp2().grabObj.get(Position.class);
                        Gravity gravity_entity = Resource.get(game, Gravity.class);
                        if(gravity_entity == null) return;
                        PVector gravity = gravity_entity.gravity();
                        grabPos.previous_position = grabPos.position;
                        //set to be above head dependent on gravity
                        grabPos.position = PVector.add(grabber.comp1().position, new PVector(-game.playerWidth * 1.2f * gravity.x, -game.playerWidth * 1.2f * gravity.y, 0));
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
            if(input == null || gravity == null) return;
            dom.findEntitiesWith(Velocity.class, Player.class, PlayerMovement.class, Position.class)
                .stream().forEach(res -> {
                    res.comp1().velocity.set(res.comp3().newVelocity((float)game.schedule.dt(), res.comp1().velocity, input, res.comp4(), res.comp1().mass, gravity.gravity()));

//                    res.entity().get(Sprite.class).flipX = res.comp1().velocity.x < 0;
                });

            //if input is pickup, pick up box in the vicinity -- have a way to check this only every so often
            if(input.isKeyDown(input.keybinds.get(InputSystem.keys.THROW))) {
                //get the player
                dom.findEntitiesWith(Velocity.class, Player.class, Position.class, Collider.class)
                    .stream().forEach(player -> {
                        var grab = player.entity().get(Grab.class);
                        if(grab.grabObj == null) {
                            //find a nearby box to pickup and pick it up
                            dom.findEntitiesWith(Collider.class, Position.class, Grabbable.class)
                                    .stream().forEach(box -> {
                                        // //if colliding with box pick it up
                                        PVector player_mid = new PVector(player.comp3().position.copy().x + game.playerWidth/2, player.comp3().position.copy().y + game.playerHeight/2);
                                        PVector box_mid = new PVector(box.comp2().position.copy().x + game.playerWidth/2, box.comp2().position.copy().y + game.playerHeight/2);

                                        float range = (float) (2f*Math.sqrt(2*(game.playerHeight/2)*(game.playerHeight/2)));

                                        if (player_mid.copy().sub(box_mid).mag() < range) {
                                            //check collision zone above head

                                            if (!dom.findEntitiesWith(Collider.class, Position.class)
                                                    .stream().anyMatch(object ->
                                                        Collision.BasicCollider.AABBCheck(
                                                            box.comp3().get_above_head_position(game, player.comp3().position, game.playerHeight),
                                                            ((BasicCollider) box.comp1().collider).size,
                                                            object.comp2().position,
                                                            ((BasicCollider) object.comp1().collider).size
                                                        )
                                                    )) {

                                                grab.grabObj = box.entity();
                                                box.comp3().player = player.entity();
                                                input.keysDown.remove(input.keybinds.get(InputSystem.keys.THROW));

                                                //move the box to above the player
                                                box.comp2().previous_position = box.comp2().position;
                                                box.comp2().position.x = player.comp3().position.x;
                                                box.entity().removeType(Velocity.class); //until collisions are fixed
                                                // box.comp2().position.y = player.comp3().position.y - player.comp4().collider.getSize().y;

                                            }
                                        }

                                    });
                        } else if(dom.findEntitiesWith(Docking_Plugin.Docking.class, Position.class)
                                .stream().anyMatch(dock -> {
                                    return dock.comp1().is_close(grab.grabObj.get(Position.class).position, grab.grabObj.get(Box.class).size, dock.comp2().position, grab.grabObj.get(Box.class).rule_type);
                                })) {
                            //insert the box into the dock and pick up the block from the dock
                            Entity dock_object = dom.findEntitiesWith(Docking_Plugin.Docking.class, Position.class)
                                    .stream().filter(dock -> {
                                        return dock.comp1().is_close(grab.grabObj.get(Position.class).position, grab.grabObj.get(Box.class).size, dock.comp2().position, grab.grabObj.get(Box.class).rule_type);
                                    }).findFirst().get().entity();

                            Entity docked_box = dock_object.get(Docking_Plugin.Docking.class).rule;
                            dock_object.get(Docking_Plugin.Docking.class).insert_new_rule(grab.grabObj, dock_object.get(Position.class).position, game);

                            //pick up the new object
                            grab.grabObj = docked_box;
                            docked_box.get(Grabbable.class).player = player.entity();
                            input.keysDown.remove(input.keybinds.get(InputSystem.keys.THROW));

                        } else {
                            grab.grabObj.get(Body.class).enableCollision();
                            //throw the box
                            if(gravity.gravity().y != 0) {
                                if (player.comp1().velocity.x > 0) { //throw to the right
                                    //give the players box a velocity and chuck it
                                    grab.grabObj.add(new Velocity(new PVector(12f, -4f * gravity.gravity().y)));
                                } else { //throw to the left
                                    grab.grabObj.add(new Velocity(new PVector(-12f, -4f * gravity.gravity().y)));
                                }
                            } else if(gravity.gravity().x != 0) {
                                if (player.comp1().velocity.y > 0) { //throw to the right
                                    //give the players box a velocity and chuck it
                                    grab.grabObj.add(new Velocity(new PVector(-4f * gravity.gravity().x, 12f)));
                                } else { //throw to the left
                                    grab.grabObj.add(new Velocity(new PVector(-4f * gravity.gravity().x, -12f)));
                                }
                            }
                            input.keysDown.remove(input.keybinds.get(InputSystem.keys.THROW));
                            grab.grabObj.get(Grabbable.class).player = null;
                            grab.grabObj = null;
                        }
                    });
            }
        });

        //reduce the invulnerability of the player
        game.schedule.update(() -> {
            Player player = Resource.get(game, Player.class);
            if(player != null) {
                if (player.invulnerability > 0) player.invulnerability -= game.schedule.dt();
                if (player.invulnerability < 0) {
                    player.invulnerability = 0f;
                    System.out.println("Player has lost invulnerability");
                }
            }
        });

        //flip
        


        //draw the player
        // game.schedule.draw(-1, draw -> {
        //     dom.findEntitiesWith(Position.class, Player.class)
        //         .stream().forEach(res -> {
        //             var pos = res.comp1().position;
        //             Gravity gravity = Resource.get(game, Gravity.class);
        //             draw.call(drawing -> {
        //                 //draw the player character
        //                 drawing.fill(128);
        //                 if(res.comp2().invulnerability > 0f) drawing.fill(128,128);
        //                 if(gravity == null || gravity.gravity().y != 0) drawing.rect(pos.x, pos.y, playerWidth, playerHeight);
        //                 else drawing.rect(pos.x, pos.y, playerHeight, playerWidth);
        //             });
        //         });
        // });
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
            if (input.isKeyDown(input.keybinds.get(InputSystem.keys.MOVE_LEFT))) {
                //move left
                pressDirection.x = -1;
            }
            if (input.isKeyDown(input.keybinds.get(InputSystem.keys.MOVE_RIGHT))) {
                //move right
                pressDirection.x = 1;
            }
            if (input.isKeyDown(input.keybinds.get(InputSystem.keys.JUMP))) {
                //jump if grounded
                // input.keysDown.remove((int) 'W'); //makes wall jumping and movement require more skill
                pressDirection.y = -1;
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
                    System.out.println("Wall jumped");
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
        public int lives;
        public float invulnerability = 0f;

        public Player() {
            this.lives = 3;
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
