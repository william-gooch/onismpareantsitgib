package com.CS4303.group3.plugin;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Map_Plugin.Ground;
import com.CS4303.group3.plugin.Box_Plugin.*;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.plugin.Player_Plugin.*;
import com.CS4303.group3.utils.Collision.*;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext.Basic;

import dev.dominion.ecs.api.*;
import dev.dominion.ecs.api.Results.*;
import javafx.util.Pair;
import processing.core.PVector;

public class Object_Plugin implements Plugin_Interface {
    Dominion dom;
    static final float EPSILON = .001f;

    @Override
    public void build(Game game) {
        dom = game.dom;

        //check collisions with anything with position and collider
        game.schedule.update(() -> {
            dom.findEntitiesWith(Position.class, Collider.class, Body.class, Velocity.class).stream()
                .forEach(obj -> {
                    obj.comp1().previous_position = obj.comp1().position.copy();

                    //Temporary
                    if(game.abs(obj.comp4().velocity.x) < 0.01f) obj.comp4().velocity.x = 0; //if object moving to slow stop it
                    if(game.abs(obj.comp4().velocity.y) < 0.01f) obj.comp4().velocity.y = 0;

                    PVector dirlock = obj.comp4().directionLocked;
                    if(dirlock.magSq() > 0) {
                        // check if the object's velocity is facing towards the locked direction
                        float dotprod = dirlock.dot(obj.comp4().velocity);
                        if(dotprod > 0) {
                            PVector tangent = new PVector(dirlock.y, -dirlock.x).normalize();
                            obj.comp4().velocity.set(tangent.mult(tangent.dot(obj.comp4().velocity)));
                        }
                    }

                    //if entity is a player and holding a box move the box
                    if(obj.entity().has(Grab.class)) {
                        Grab grab = obj.entity().get(Grab.class);
                        if(grab.grabObj != null) {
                            grab.grabObj.get(Position.class).previous_position = grab.grabObj.get(Position.class).position;
                        }
                    }

                    if(!obj.comp3().canCollide) {
                        return;
                    }

                    obj.comp1().walled = 0;
                    obj.comp1().grounded = false;

                    //correct collisions with the ground
                    resolve_collision(game, obj, 1, 0);
                });
        });
        
        // game.schedule.draw(draw -> {
        //     dom.findEntitiesWith(Position.class, Velocity.class)
        //         .stream().forEach(obj -> {
        //             draw.call(drawing -> {
        //                 drawing.line(obj.comp1().position.x, obj.comp1().position.y, obj.comp1().position.x + obj.comp2().velocity.x, obj.comp1().position.y + obj.comp2().velocity.y);
        //             });
        //         });
        // });


        // game.schedule.draw(draw -> {
        //     dom.findEntitiesWith(Position.class, Collider.class)
        //         .stream().forEach(obj -> {
        //             draw.call(drawing -> {
        //                 PVector size = ((BasicCollider)obj.comp2().collider).size;
        //                 drawing.rect(obj.comp1().position.x, obj.comp1().position.y, size.x, size.y);
        //             });
        //         });
        // });

        // game.schedule.draw(draw -> {
        //     dom.findEntitiesWith(Position.class, Collider.class, Velocity.class)
        //         .stream().forEach(obj -> {
        //             draw.call(drawing -> {
        //                 PVector size = ((BasicCollider)obj.comp2().collider).size;
        //                 drawing.rect(obj.comp1().position.x + obj.comp3().velocity.x, obj.comp1().position.y + obj.comp3().velocity.y, size.x, size.y);
        //             });
        //         });
        // });
    }

    public static boolean preCollision(With4<Position, Collider, Body, Velocity> obj, With2<Position, Collider> other) {
        //check that it isn't looking at itself
        if(obj.entity() == other.entity()) { return false; }
        // if the object is a body that can't collide, then return early
        // static objects like the ground are always collidable.
        if(other.entity().has(Body.class) && !other.entity().get(Body.class).canCollide) { return false; }
        //don't do collisions with enemies if the player is invulnerable
        if(obj.entity().has(Player.class) && (other.entity().has(Enemy_Plugin.Basic_AI.class) || other.entity().has(Spike_Plugin.Spikes.class))) {
            if(obj.entity().get(Player.class).invulnerability > 0f) {
                return false;
            }
        }

        return true;
    }

    public static void resolve_collision(Game game, With4<Position, Collider, Body, Velocity> obj, float time_remaining, int depth) {
        boolean bouncing = true;

        if(depth == 5) {
            //if this many collisions resolved give up and just stop all movement
            obj.comp4().velocity.set(0,0);
            return;
        }
        Pair<Entity, Contact> entityAndContact = (Pair<Entity, Contact>) game.dom.findEntitiesWith(Position.class, Collider.class)
                .stream()
                .filter(other -> preCollision(obj, other))
                .<Pair<Entity, Contact>> map(other -> {
                    // // check if collided vertically
                    // yCollide(game, obj, other);
                    // // check if collided horizontally
                    // xCollide(game, obj, other);
                    Contact contact = collision(obj, other);

                    return new Pair<>(other.entity(), contact);
                })
                .filter(c -> c.getValue() != null)
                .min(Comparator.comparing(a -> ((Pair<Entity, Contact>) a).getValue().collisionTime()))
                .orElse(null);

        if (entityAndContact == null || entityAndContact.getValue() == null) {
            obj.comp1().position.add(obj.comp4().velocity);
        } else {
            Contact firstCollision = entityAndContact.getValue();

            if(obj.comp2().isFragile) {
                game.dom.deleteEntity(obj.entity());
            }
            if(entityAndContact.getKey().get(Collider.class).isFragile) {
                game.dom.deleteEntity(entityAndContact.getKey());
            }
//            if(firstCollision.collisionTime() == 0) {
//                //if already colliding move backwards
//                obj.comp1().position.sub(obj.comp4().velocity.copy().mult(0.001f));
//            }


// if(bouncy && obj.entity().has(Player.class)){
//     obj.comp4().velocity.y = obj.comp4().velocity.y*-1f;
// }


                obj.comp1().position.add(obj.comp4().velocity.copy().mult(time_remaining * (firstCollision.collisionTime() - 0.01f)));



//                        if(game.abs(firstCollision.cNormal().x) == 0) obj.comp1().position.x += obj.comp4().velocity.copy().x;
//                        else obj.comp1().position.x += obj.comp4().velocity.copy().x * firstCollision.collisionTime();
//                        if(game.abs(firstCollision.cNormal().y) == 0) obj.comp1().position.y += obj.comp4().velocity.copy().y;
//                        else obj.comp1().position.y += obj.comp4().velocity.copy().y * firstCollision.collisionTime();

//                        float dotprod = obj.comp4().velocity.dot(firstCollision.cNormal()) * (1 - firstCollision.collisionTime());
//                        obj.comp4().velocity.set(firstCollision.cNormal().y * dotprod, firstCollision.cNormal().x * dotprod);

            Gravity gravity = Resource.get(game, Gravity.class);
            if(gravity != null) {
                if (firstCollision.cNormal().dot(gravity.gravity()) < 0 && time_remaining == 1) {
                    obj.comp1().grounded = true;
                    obj.comp1().prev_walled = 0;
                    obj.comp1().walled = 0;
                }


                //check if colliding on the x-direction, if it is set to be walled
                if ((firstCollision.cNormal().x * gravity.gravity().y != 0
                        || firstCollision.cNormal().y * gravity.gravity().x != 0) && time_remaining == 1) {
                    if (gravity.gravity().x != 0)
                        obj.comp1().walled = obj.comp4().velocity.y > 0 ? 1 : -1;
                    else obj.comp1().walled = obj.comp4().velocity.x > 0 ? 1 : -1;

                }
            }

            if(firstCollision.collisionTime() <= EPSILON) {
                System.out.println(game.frameCount + "] oops im inside an object " + firstCollision.cNormal());
                obj.comp1().position.add(firstCollision.cNormal());
            }


            float bounceThreshold = 4f;

            // System.out.println(obj.comp4().velocity.copy().mult(time_remaining * (firstCollision.collisionTime() - 0.01f)));


            Entity otherEntity = entityAndContact.getKey();
            if(otherEntity.has(Body.class) && otherEntity.has(Velocity.class)) {
                System.out.println("tryna push " + firstCollision.cNormal());
                otherEntity.get(Velocity.class).velocity.add(
                    firstCollision.cNormal().y == 0 ? obj.comp4().velocity.x * 0.3f : 0,
                    firstCollision.cNormal().x == 0 ? obj.comp4().velocity.y * 0.3f : 0
                );
            }

            // obj.comp4().velocity.set(firstCollision.cNormal().x == 0 ? obj.comp4().velocity.x : 0,
            //         firstCollision.cNormal().y == 0 ? obj.comp4().velocity.y : 0);


            Gravity grav = Resource.get(game, Gravity.class);
            boolean aboveThreshold = false;

            if(grav.gravity().y > 0 ){
                aboveThreshold = obj.comp4().velocity.y * (time_remaining * (firstCollision.collisionTime() - 0.01f)) > bounceThreshold;
            }else if(grav.gravity().x > 0 ){
                aboveThreshold = obj.comp4().velocity.x * (time_remaining * (firstCollision.collisionTime() - 0.01f)) > bounceThreshold;
            }else if(grav.gravity().y < 0 ){
                aboveThreshold = obj.comp4().velocity.y * (time_remaining * (firstCollision.collisionTime() - 0.01f)) < -bounceThreshold;
            }else if(grav.gravity().x < 0 ){
                aboveThreshold = obj.comp4().velocity.x * (time_remaining * (firstCollision.collisionTime() - 0.01f)) < -bounceThreshold;
            }

            if(obj.comp2().isBouncy && aboveThreshold) {
                obj.comp4().velocity.set(firstCollision.cNormal().x == 0 ? obj.comp4().velocity.x : -obj.comp4().velocity.x ,
                firstCollision.cNormal().y == 0 ? obj.comp4().velocity.y : -obj.comp4().velocity.y);
            }else{
                obj.comp4().velocity.set(firstCollision.cNormal().x == 0 ? obj.comp4().velocity.x : 0,
                firstCollision.cNormal().y == 0 ? obj.comp4().velocity.y : 0);
            }

//            if(entityAndContact.getKey().get(Collider.class).isTrigger && obj.comp2().isTrigger) {
//                entityAndContact.getKey().get(Collider.class).triggerCollision(entityAndContact.getKey(), obj.entity());
//                obj.comp2().triggerCollision(obj.entity(), entityAndContact.getKey());
//            }

            resolve_collision(game, obj, time_remaining- firstCollision.collisionTime(), depth+1);
        }
    }

    public static Contact partial_collision(With4<Position, Collider, Body, Velocity> obj, With2<Position, Collider> other, float partial) {
        Contact collision = obj.comp2().collider.partial_collide(obj.comp1(), obj.entity().get(Velocity.class), other.comp2().collider, other.comp1(), partial);
        if(collision == null) return null;

        // some objects may have custom collision callbacks (e.g. buttons)
        obj.comp2().triggerCollision(obj.entity(), other.entity(), collision);
        other.comp2().triggerCollision(other.entity(), obj.entity(), collision);
        obj.comp2().triggerCollision(obj.entity(), other.entity());
        if(!(other.entity().has(Enemy_Plugin.Basic_AI.class)
                || other.entity().has(Enemy_Plugin.Patrol_AI.class)
                || other.entity().has(Spike_Plugin.Spikes.class))) other.comp2().triggerCollision(other.entity(), obj.entity());

        // some objects may be *only* triggers for those callbacks (i.e. they aren't solid objects)
        // if so, skip over handling the collision after this point
        if (obj.comp2().isTrigger || other.comp2().isTrigger) {
            return null;
        }

        return collision;
    }

    public static Contact collision(With4<Position, Collider, Body, Velocity> obj, With2<Position, Collider> other) {
        return partial_collision(obj, other, 1);
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

        public PVector directionLocked = new PVector();

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

        public static record Collision (Entity self, Entity other, Contact contact) {}

        Consumer<Collision> onCollide = null;
        boolean isTrigger = false;
        boolean isBouncy = false;
        boolean isFragile = false;

        public Collider(Collider_Interface collider) {
            this.collider = collider;
        }

        public Collider(Collider_Interface collider, Consumer<Collision> onCollide) {
            this.collider = collider;
            this.onCollide = onCollide;
            this.isTrigger = true;
        }

        public Collider(Collider_Interface collider, Consumer<Collision> onCollide, boolean isTrigger) {
            this.collider = collider;
            this.onCollide = onCollide;
            this.isTrigger = isTrigger;
        }

        public static Collider BasicCollider(float width, float height) {
            return new Collider(new BasicCollider(width, height));
        }

        public static Collider BasicCollider(float width, float height, float x, float y) {
            return new Collider(new BasicCollider(width, height, x, y));
        }

        public void triggerCollision(Entity self, Entity other, Contact contact) {
            if(onCollide != null) {
                onCollide.accept(new Collision(self, other, contact));
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
