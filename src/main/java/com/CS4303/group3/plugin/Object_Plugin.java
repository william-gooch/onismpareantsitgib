package com.CS4303.group3.plugin;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.CS4303.group3.Game;
import com.CS4303.group3.plugin.Map_Plugin.Ground;
import com.CS4303.group3.plugin.Player_Plugin.Grab;
import com.CS4303.group3.utils.Collision.*;


import dev.dominion.ecs.api.*;
import processing.core.PVector;

public class Object_Plugin implements Plugin_Interface {
    Dominion dom;

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
                    if(entity.entity().has(Grab.class)) {
                        Grab grab = entity.entity().get(Grab.class);
                        if(grab.grabObj != null) {
                            grab.grabObj.get(Position.class).previous_position = grab.grabObj.get(Position.class).position;
                            
                            // entity.entity().get(Player.class).box.get(Position.class).position.y = entity.comp1().position.y-entity.comp3().collider.getSize().y;
                            // entity.entity().get(Player.class).box.get(Position.class).position.x = entity.comp1().position.x;
                        }
                    }
                });
        });

            //move object
            // game.schedule.update(() -> {
            //     dom.findEntitiesWith( Button.class, Velocity.class, Position.class, Collider.class)
            //         .stream().forEach(button -> {
            //             float mag =  button.comp2().velocity.y;
            

            //             if(button.comp1().pushed && button.comp4().collider.getSize().y > 0){
            //                 button.comp4().collider.getSize().y -= mag;
            //                 button.comp1().height -= mag;
                         
            //                 if(button.comp1().height <= 0) {
            //                     button.comp1().height = 0;
            //                     button.comp4().collider.getSize().y = 0;
            //                     button.comp2().velocity.y = 0;
                                
            //                 }

            //             }
                    
            //         });
            // });

        //check for collsions
        //check for collisions with the edge of the play area
        game.schedule.update(() -> {
            dom.findEntitiesWith(Collider.class, Position.class, Velocity.class)
                .stream().forEach(res -> {
                    PVector position = res.comp2().position;
                    PVector velocity = res.comp3().velocity;
                    if(position.x < 0) {
                        position.x = 0;
                        velocity.x = 0;
                    // } else if(position.x + res.comp1().collider.getSize().x > game.displayWidth) {
                    //     velocity.x = 0;
                    //     position.x = game.displayWidth - res.comp1().collider.getSize().x;
                    }
                });
        });

        //check collisions with anything with position and collider
        game.schedule.update(() -> {
            dom.findEntitiesWith(Position.class, Collider.class, Body.class).stream()
                .forEach(obj -> {
                    if(!obj.comp3().canCollide) {
                        return;
                    }

                    obj.comp1().walled = 0;
                    obj.comp1().grounded = false;

                    //correct collisions with the ground
                    dom.findEntitiesWith(Position.class, Collider.class)
                        .stream().forEach(other -> {
                            //check that it isn't looking at itself
                            if(obj.entity() == other.entity()) { return; }
                            // if the object is a body that can't collide, then return early
                            // static objects like the ground are always collidable.
                            if(other.entity().has(Body.class) && !other.entity().get(Body.class).canCollide) { return; }

                            Contact collision = obj.comp2().collider.collide(obj.comp1(), other.comp2().collider, other.comp1());
                            if(collision == null) return;

                            // some objects may have custom collision callbacks (e.g. buttons)
                            obj.comp2().triggerCollision(obj.entity(), other.entity());
                            other.comp2().triggerCollision(other.entity(), obj.entity());

                            // some objects may be *only* triggers for those callbacks (i.e. they aren't solid objects)
                            // if so, skip over handling the collision after this point
                            if (obj.comp2().isTrigger || other.comp2().isTrigger) {
                                return;
                            }

                            if(collision.cNormal().y != 0) { // check if collided vertically
                                //stop velocity if going into the object -- if velocity * change in y < 0 going into the object
                                if(obj.entity().has(Velocity.class)) {
                                    Velocity objVel = obj.entity().get(Velocity.class);
                                    if(objVel.velocity.y * collision.cNormal().y < 0) {
                                        //if going downwards -- y increasing - set to be grounded
                                        if(objVel.velocity.y > 0) {
                                            obj.comp1().grounded = true;
                                            obj.comp1().prev_walled = 0;
                                            obj.comp1().walled = 0;
                                        }

                                        objVel.velocity.y = 0;
                                        obj.comp1().position.y += collision.cNormal().y;
                                    }
                                }
                            }

                            //check if collided horizontally
                            //issue with setting enabled to false
                            if(collision.cNormal().x != 0) {
                                if(other.entity().has(Ground.class)) {
                                    //stop velocity if going into the object
                                    if(obj.entity().has(Velocity.class)) {
                                        Velocity objVel = obj.entity().get(Velocity.class);
                                        if(objVel.velocity.x * collision.cNormal().x < 0) {
                                            obj.comp1().walled = collision.cNormal().x > 0 ? 1 : -1;

                                            objVel.velocity.x = 0;
                                            obj.comp1().position.x += collision.cNormal().x;
                                        }
                                    }
                                } else if(other.entity().has(Velocity.class)) {
                                    // move player out of the object
                                    obj.comp1().position.x += collision.cNormal().x;

                                    // calculate the new velocity for both objects
                                    if(obj.entity().has(Velocity.class)) {
                                        obj.entity().get(Velocity.class).velocity.x = obj.entity().get(Velocity.class).velocity.x*0.75f;
                                        other.entity().get(Velocity.class).velocity.x = obj.entity().get(Velocity.class).velocity.x;
                                    }
                                }
                            }
                        });
                });
        });


        //TODO: COLLISIONS WITH BUTTONS
        // game.schedule.update(() -> {
        //     dom.findEntitiesWith(Position.class, Collider.class).without(Ground.class, Button.class)
        //         .stream().forEach(player -> {

        //             deleted_entity = false;
        //             //disables the current entity
        //             player.entity().setEnabled(false);

        //             if(player.entity().has(Box.class)) {
        //                 if(player.entity().get(Box.class).player != null) { //ensures that doesn't count collisions with player that is carrying
        //                     player.entity().get(Box.class).player.setEnabled(false);
        //                 }
        //             }

        //             if(player.entity().has(Player.class)) {
        //                 if(player.entity().get(Player.class).box != null) { //ensures that doesn't count collisions with box that it is carrying
        //                     player.entity().get(Player.class).box.setEnabled(false);
        //                 }
        //             }


        //             dom.findEntitiesWith(Position.class, Collider.class, Button.class)
        //             .stream().forEach(button -> {
        //                 if(button.entity().isEnabled() && !deleted_entity) {
        //                     PVector collision = player.comp2().collider.collision_correction(player.comp1(), button.comp2().collider, button.comp1());

        //                     button.entity().get(Button.class).pushed = collision.y != 0 ? true : false;
        //                      if(button.entity().get(Button.class).pushed) System.out.println("Button pushed " );
        //                      System.out.println("Moved y - collision.y = "+ collision.y);
        //                      System.out.println("Moved y - collision.x = "+ collision.x);
        //                      System.out.println("Grounded = "+ player.comp1().grounded);
                            
        //                     //check if collided vertically
        //                     if(collision.y != 0) {
                              

        //                         if(player.entity().has(Velocity.class)) {
        //                             System.out.println("player velocity.y =  "+ player.entity().get(Velocity.class).velocity.y);
        //                         // if(!player.comp1().buttoned){
        //                             if(player.entity().get(Velocity.class).velocity.y * (collision.y - player.comp1().position.y) < 0) {
        //                                 if(player.entity().get(Velocity.class).velocity.y > 0) {
        //                                   //  player.comp1().grounded = true;
        //                                     player.comp1().prev_walled = 0;
        //                                     player.comp1().walled = 0;
        //                                     // System.out.println("Grounded");
        //                                 }
        //                                     player.entity().get(Velocity.class).velocity.y = button.entity().get(Button.class).loweringSpeed;
        //                                     button.entity().get(Velocity.class).velocity.y = button.entity().get(Button.class).loweringSpeed;
        //                                     System.out.println("player velocity.y =  "+ player.entity().get(Velocity.class).velocity.y);
        //                                     System.out.println("button velocity.y =  "+  button.entity().get(Velocity.class).velocity.y);
        //                                     System.out.println("button velocity.x =  "+  button.entity().get(Velocity.class).velocity.x);
                                            

                                           
        //                                 }
        //                                 player.comp1().position.y = collision.y;
        //                         //   }
        //                         } else {
        //                             if(!player.comp1().grounded){
        //                                  //stop the block and player
        //                                 player.comp1().position.y = collision.y+1;
        //                                 player.entity().get(Box.class).player.get(Position.class).position.y = player.comp1().position.y + player.comp2().collider.getSize().y;
        //                                 player.entity().get(Box.class).player.get(Velocity.class).velocity.y = button.entity().get(Button.class).loweringSpeed;
        //                                 button.entity().get(Velocity.class).velocity.y = button.entity().get(Button.class).loweringSpeed;
        //                              }
        //                             }
                                   
        //                     }

                    
        //                     //check if collided horizontally
        //                     //issue with setting enabled to false
        //                     if(collision.x != 0 && !deleted_entity) {
        //                         if(button.entity().has(Button.class)) {
        //                             //stop velocity if going into the object
        //                             if(player.entity().has(Velocity.class)) {
        //                                 if(player.entity().get(Velocity.class).velocity.x * (collision.x - player.comp1().position.x) < 0) {
        //                                     player.comp1().walled = (int)((collision.x - player.comp1().position.x) / Math.abs(collision.x - player.comp1().position.x)); //opposite direction of collision
        //                                     player.entity().get(Velocity.class).velocity.x = 0;
        //                                 }
        //                                 player.entity().get(Velocity.class).velocity.y = button.entity().get(Button.class).loweringSpeed;
        //                                 button.entity().get(Velocity.class).velocity.y = button.entity().get(Button.class).loweringSpeed;
        //                                 System.out.println("player velocity.x =  "+ player.entity().get(Velocity.class).velocity.x);
        //                                 System.out.println("player velocity.y =  "+ player.entity().get(Velocity.class).velocity.y);
        //                                 System.out.println("button velocity.y =  "+  button.entity().get(Velocity.class).velocity.y);
        //                                 System.out.println("button velocity.x =  "+  button.entity().get(Velocity.class).velocity.x);
        //                                 player.comp1().position.x = collision.x;
        //                             } else {
        //                                 //stop the block being carried if the player is still moving (This allows for wall jumping with the blocks - discuss if we want this)
        //                                 if(player.entity().get(Box.class).player.get(Velocity.class).velocity.x != 0) {
        //                                     player.entity().get(Box.class).player.setEnabled(true);
        //                                     player.entity().get(Box.class).player.get(Player.class).box = null;
        //                                     player.entity().get(Box.class).player = null;
        //                                     player.entity().setEnabled(true);
        //                                     dom.createEntityAs(player.entity(), new Velocity(0.5f));
        //                                     dom.deleteEntity(player.entity());
        //                                     deleted_entity = true;
        //                                 }

        //                                 //stop the player
        //                                 // player.comp1().position.x = collision.x;
        //                                 // player.entity().get(Box.class).player.get(Position.class).position.x = player.comp1().position.x;
        //                                 // player.entity().get(Box.class).player.get(Velocity.class).velocity.x = 0;

        //                             }
        //                         }
        //                     }
        //                 }});


        //                 if(!deleted_entity) {
        //                     player.entity().setEnabled(true);
        //                     if(player.entity().has(Box.class)) {
        //                         if(player.entity().get(Box.class).player != null) {
        //                             player.entity().get(Box.class).player.setEnabled(true);
        //                         }
        //                     }
        //                     if(player.entity().has(Player.class)) {
        //                         if(player.entity().get(Player.class).box != null) { //ensures that doesn't count collisions with box that it is carrying
        //                             player.entity().get(Player.class).box.setEnabled(true);
        //                         }
        //                     }
        //                 }
                    
        //             });
                
                
        //         });

                


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

        BiConsumer<Entity, Entity> onCollide = null;
        boolean isTrigger = false;

        public Collider(Collider_Interface collider) {
            this.collider = collider;
        }

        public Collider(Collider_Interface collider, BiConsumer<Entity, Entity> onCollide) {
            this.collider = collider;
            this.onCollide = onCollide;
            this.isTrigger = true;
        }

        public Collider(Collider_Interface collider, BiConsumer<Entity, Entity> onCollide, boolean isTrigger) {
            this.collider = collider;
            this.onCollide = onCollide;
        }

        public static Collider BasicCollider(int width, int height) {
            return new Collider(new BasicCollider(width, height));
        }

        public void triggerCollision(Entity self, Entity other) {
            if(onCollide != null) {
                onCollide.accept(self, other);
            }
        }
    }

    // A collidable body.
    public static class Body {
        // Whether the body can't move. Static bodies don't need to collide with each other.
        boolean canCollide = true;

        public void enableCollision() {
            this.canCollide = true;
        }

        public void disableCollision() {
            this.canCollide = false;
        }
    }
}
