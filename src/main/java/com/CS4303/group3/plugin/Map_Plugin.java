package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Assets_Plugin.AssetManager;
import com.CS4303.group3.plugin.Box_Plugin.Box;
import com.CS4303.group3.plugin.Box_Plugin.Grabbable;
import com.CS4303.group3.plugin.Box_Plugin.rule_types;
import com.CS4303.group3.plugin.Button_Plugin.Button;
import com.CS4303.group3.plugin.Button_Plugin.ButtonEventListener;
import com.CS4303.group3.plugin.Door_Plugin.Door;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.plugin.Game_Plugin.WorldManager;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Player_Plugin.Player;
import com.CS4303.group3.plugin.Sprite_Plugin.AnimatedSprite;
import com.CS4303.group3.plugin.Sprite_Plugin.ISprite;
import com.CS4303.group3.plugin.Sprite_Plugin.RepeatedSprite;
import com.CS4303.group3.plugin.Sprite_Plugin.Sprite;
import com.CS4303.group3.plugin.Sprite_Plugin.SpriteRenderer;
import com.CS4303.group3.plugin.Sprite_Plugin.StateSprite;
import com.CS4303.group3.plugin.Trigger_Plugin.Trigger;
import com.CS4303.group3.utils.Changeable;
import com.CS4303.group3.utils.Changeable_Interface;
import com.CS4303.group3.utils.Collision;
import com.CS4303.group3.utils.Collision.BasicCollider;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import javafx.geometry.Pos;
import processing.core.*;

import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.plaf.nimbus.State;

import org.tiledreader.*;

public class Map_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        this.dom = game.dom;

        game.schedule.draw(-10, draw -> {
            draw.call(drawing -> {
                dom.findEntitiesWith(Position.class, RenderTile.class)
                    .stream()
                    .sorted((a, b) -> Integer.compare(a.comp2().layer(), b.comp2().layer()))
                    .forEach(entity -> {
                        drawing.image(entity.comp2().tileImage(), entity.comp1().position.x, entity.comp1().position.y, entity.comp2().width(), entity.comp2().height());
                    });
                });
        });
    }

    public static class Ground {
        public PVector size;
        //maybe have a tileset

        public Ground(PVector size) {
            this.size = size;
        }

        public void draw(Game game, PVector position) {
            game.fill(255);
            game.stroke(255);
            game.rect(position.x, position.y, size.x, size.y);
        }
    }

    public static class All_Ground extends Changeable_Interface {
        public Game game;

        public All_Ground(Game game) {
            super(null);
            this.game = game;
        }

        @Override
        public void change(Object value) {
            if(!(value instanceof Collider.states)) return;

            //change all colliders associated with ground
            game.dom.findEntitiesWith(Ground.class, Collider.class)
                    .stream().forEach(ground -> {
                        ground.comp2().state = (Collider.states) value;
                        System.out.println(ground.comp2().state);
                    });
        }
    }

    static class Tileset {
        private PImage tilesetImage;
        private int tileSize;

        public Tileset(PImage tilesetImage, int tileSize) {
        }
    }

    static record RenderTile(
        PImage tileImage,
        float width,
        float height,
        int layer
    ) { }

    static class TileMap<T> {
        float tileScale = 1;
        TiledMap map;

        HashMap<TiledObject, Entity> objects;

        private Game game;

        public TileMap(Game game, TiledMap map, float tileScale) {
            this.game = game;
            this.tileScale = tileScale;
            this.map = map;
            this.objects = new HashMap<>();
            this.generateRenderTiles();
            this.generateObjects();
        }

        public TileMap(Game game, TiledMap map) {
            this(game, map, (float)game.width / ((map.getWidth()) * map.getTileWidth()));
        }

        private Entity createRenderTileFromTile(TiledTile tile, float x, float y, int layer) {
            AssetManager assets = Resource.get(game, AssetManager.class);

            // asset manager caches the image so it's not a worry if it gets the same image a bunch of times
            PImage image = assets.getAsset(PImage.class, tile.getTileset().getImage().getSource());
            PImage tileImage = image.get(
                tile.getTilesetX() * map.getTileWidth(),
                tile.getTilesetY() * map.getTileHeight(),
                map.getTileWidth(),
                map.getTileHeight()
            );

            // return next.getImage().getSource();
            var e = game.dom.createEntity(
                new Position(new PVector(x * tileScale, y * tileScale)),
                new RenderTile(tileImage, map.getTileWidth() * tileScale, map.getTileHeight() * tileScale, layer)
            );

            if(!tile.getCollisionObjects().isEmpty()) {
                var o = tile.getCollisionObjects().get(0);
                e.add(Collider.BasicCollider(o.getWidth() * tileScale, o.getHeight() * tileScale, o.getX() * tileScale, o.getY() * tileScale));
                e.add(new Ground(new PVector(o.getWidth() * tileScale, o.getHeight() * tileScale)));
            }

            return e;
        }

        private PImage getTileImage(TiledTile tile) {
            AssetManager assets = Resource.get(game, AssetManager.class);

            // asset manager caches the image so it's not a worry if it gets the same image a bunch of times
            PImage image = assets.getAsset(PImage.class, tile.getTileset().getImage().getSource());
            PImage tileImage = image.get(
                tile.getTilesetX() * map.getTileWidth(),
                tile.getTilesetY() * map.getTileHeight(),
                map.getTileWidth(),
                map.getTileHeight()
            );

            return tileImage;
        }

        private Entity createSpriteFromObject(TiledObject obj) {
            float width = obj.getWidth() * tileScale,
                  height = obj.getHeight() * tileScale,
                  x = obj.getX() * tileScale,
                  y = obj.getY() * tileScale;

            TiledTile tile = obj.getTile();
            if(tile == null) {
                return game.dom.createEntity(
                    new Position(new PVector(x, y - height))
                );
            }

            PImage tileImage = getTileImage(tile);
            return game.dom.createEntity(
                new Position(new PVector(x, y - height)),
                new SpriteRenderer(new Sprite(tileImage), width, height)
            );
        }

        public void generateRenderTiles() {
            int layerIdx = 0;
            for(var layer : map.getNonGroupLayers()) {
                if(TiledTileLayer.class.isInstance(layer)) {
                    TiledTileLayer tileLayer = (TiledTileLayer) layer;
                    for (int x = tileLayer.getX1(); x <= tileLayer.getX2(); x++) {
                        for (int y = tileLayer.getY1(); y <= tileLayer.getY2(); y++) {
                            var tile = tileLayer.getTile(x, y);

                            if(tile != null) {
                                createRenderTileFromTile(tile, x * map.getTileWidth(), y * map.getTileHeight(), layerIdx);
                            }
                        }
                    }
                    layerIdx++;
                }
            }
        }

        public void generateObjects() {
            for(var layer : map.getNonGroupLayers()) {
                if(TiledObjectLayer.class.isInstance(layer)) {
                    TiledObjectLayer objectLayer = (TiledObjectLayer) layer;
                    for(var obj : objectLayer.getObjects()) {
                        Entity e;
                        ObjectFactory factory = objectTypes.get(obj.getType());
                        if(factory != null) {
                            e = factory.construct(obj);
                        } else {
                            e = createSpriteFromObject(obj);
                        }
                        if(obj.getProperty("onTrigger") != null) {
                            System.out.println(obj.getProperty("onTrigger"));
                            Trigger trigger = Trigger_Plugin.STANDARD_TRIGGERS.get(obj.getProperty("onTrigger")); 
                            if(trigger != null) {
                                e.add(trigger);
                            }
                        }
                        objects.put(obj, e);
                    }
                }
            }
        }

        @FunctionalInterface
        interface ObjectFactory {
            Entity construct(TiledObject obj);
        }

        Map<String, ObjectFactory> objectTypes = Map.ofEntries(
            Map.entry("player_spawn", obj -> {
                WorldManager wm = Resource.get(game, WorldManager.class);
                wm.createPlayer(game, obj.getX() * tileScale, obj.getY() * tileScale, tileScale);
                return null;
            }),
            Map.entry("block", obj -> {
                Entity e = createSpriteFromObject(obj);
                e.add(new Velocity(0.5f));
                e.add(new Body());
                e.add(new Grabbable());
                rule_types ruleType = null;
                switch ((String) obj.getProperty("ruleType")) {
                    case "Directional":
                        ruleType = rule_types.DIRECTIONAL;
                        break;
                    case "Boolean":
                        ruleType = rule_types.BOOLEAN;
                        break;
                    case "Operational":
                        ruleType = rule_types.OPERATIONAL;
                        break;
                    case "Target":
                        ruleType = rule_types.TARGET;
                        break;
                }
                T value = null;
                if(ruleType == rule_types.DIRECTIONAL) {
                    int val = (int) obj.getProperty("value");
                    if(val == 0) value = (T) new PVector(0,1); //down
                    if(val == 1) value = (T) new PVector(1,0); //right
                    if(val == 2) value = (T) new PVector(0,-1); //up
                    if(val == 3) value = (T) new PVector(-1,0); //left
                }
                else if(ruleType == rule_types.TARGET) {
                    //check map of String to class types
                    String val = (String) obj.getProperty("value");
                    if(val.equals("Ground")) {
                        value = (T) new Changeable(new All_Ground(game));
                    } else if(val.equals("Player")) {
                        value = (T) new Changeable(new Player_Plugin.Player_Changer(game));
                    }
                } else if(ruleType == rule_types.BOOLEAN) {
                    value = (T)(Boolean)obj.getProperty("value");
                } else if(ruleType == rule_types.OPERATIONAL) {
                    String val = (String) obj.getProperty("value");
                    if(val.equals("Bouncy")) {
                        value = (T) Collider.states.BOUNCY;
                    } else if(val.equals("Fragile")) {
                        value = (T) Collider.states.FRAGILE;
                    }
                }
                e.add(new Box(ruleType, value));
                e.add(Collider.BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale));
                return e;
            }),
            Map.entry("button", obj -> {
                PImage offImage = getTileImage(obj.getTile());
                PImage onImage = getTileImage(obj.getTile().getTileset().getTile((int) obj.getProperty("onImage")));
                StateSprite sprite = new StateSprite();
                sprite.addState("off", new Sprite(offImage));
                sprite.addState("on", new Sprite(onImage));
                sprite.setState("off");
                float rotation = obj.getRotation()*(PConstants.PI/180);
                PVector pos = new PVector(obj.getX() * tileScale, obj.getY() * tileScale);
                // translate to the center
                pos.sub(new PVector(obj.getWidth() * tileScale / 2, obj.getHeight() * tileScale / 2));
                // then translate to the bottom-left corner (where it is by default), rotated to make sure it's always in the right spot
                pos.sub(new PVector(-obj.getWidth() * tileScale / 2, obj.getHeight() * tileScale / 2).rotate(rotation));
                Entity e = game.dom.createEntity(
                    new Position(pos),
                    new SpriteRenderer(sprite, obj.getWidth() * tileScale, obj.getHeight() * tileScale, rotation)
                );
                Button button = new Button((int) (obj.getWidth() * tileScale), (int) (obj.getHeight() * tileScale));
                button.addEventListener(new ButtonEventListener() {

                    @Override
                    public void onPush() {
                        sprite.setState("on");
                        var triggerObj = obj.getProperty("trigger");
                        if(triggerObj != null) {
                            var triggerEntity = objects.get(triggerObj);
                            if(triggerEntity != null) {
                                var trigger = triggerEntity.get(Trigger.class);
                                if(trigger != null) {
                                    trigger.trigger(game, triggerEntity, true);
                                }
                            }
                        }
                    }

                    @Override
                    public void onRelease() {
                        sprite.setState("off");
                        var triggerObj = obj.getProperty("trigger");
                        if(triggerObj != null) {
                            var triggerEntity = objects.get(triggerObj);
                            if(triggerEntity != null) {
                                var trigger = triggerEntity.get(Trigger.class);
                                if(trigger != null) {
                                    trigger.trigger(game, triggerEntity, false);
                                }
                            }
                        }
                    }
                    
                });
                e.add(button);
                e.add(new Collider(new BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale), 
                    (collision) -> {
                        collision.self().get(Button.class).pushed = true;
                        collision.self().get(Button.class).lastPushed = 0;
                    })
                );
                return e;
            }),
            Map.entry("door", obj -> {
                Entity e = createSpriteFromObject(obj);
                Door d = new Door((int) (obj.getWidth() * tileScale), (int) (obj.getHeight() * tileScale));
                d.openDirection = (int) obj.getProperty("openDirection");
                e.add(d);
                e.add(Collider.BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale));
                e.add(new Ground(new PVector(obj.getWidth() * tileScale, obj.getHeight() * tileScale)));
                return e;
            }),
            Map.entry("exit_point", obj -> {
                WorldManager wm = Resource.get(game, WorldManager.class);
                wm.createExit(game, obj.getX() * tileScale, obj.getY() * tileScale);
                return null;
            }),
            Map.entry("enemy", obj -> {
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

                TiledObject path = (TiledObject) obj.getProperty("path");
                PVector[] points = path.getPoints().stream().map(p -> new PVector(((float)p.getX() + obj.getX()) * tileScale, ((float)p.getY() + obj.getY()) * tileScale)).toList().toArray(new PVector[0]);
                e.add(new Enemy_Plugin.Basic_AI(points));
                e.add(new Collider(new BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale), (collision) -> {
                    // check if collision normal is negative y (i.e. getting hit from above)
                    if(collision.contact().cNormal().y < 0) {
                        System.out.println("oof ouch owie im dead");
                        collision.self().get(Enemy_Plugin.Basic_AI.class).death_animation = 2f;
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
            }),
            Map.entry("dock", obj -> {
                Entity e = createSpriteFromObject(obj);
                //need to find a way to link to the changeable in the entity being changed
                rule_types ruleType = null;
                switch ((String) obj.getProperty("ruleType")) {
                    case "Directional":
                        ruleType = rule_types.DIRECTIONAL;
                        break;
                    case "Boolean":
                        ruleType = rule_types.BOOLEAN;
                        break;
                    case "Operational":
                        ruleType = rule_types.OPERATIONAL;
                        break;
                    case "Target":
                        ruleType = rule_types.TARGET;
                        break;
                }

                TiledObject trigObj = (TiledObject) obj.getProperty("trigger");
                Changeable changeable = null;
                if(trigObj != null) {
//                    if(trigObj.getName() == "Dock") {
//
//                    } else {
                    System.out.println(trigObj.getName() + "__");

                    var trigEntity = objects.get(trigObj);
                    if (trigEntity != null) {
                        System.out.println("has trigger");
                        changeable = trigEntity.get(Changeable.class);
                    }
//                    }
                }

                T default_value = null;
                if(ruleType == rule_types.DIRECTIONAL) {
                    String default_value_string = (String) obj.getProperty("defaultValue");
                    switch (default_value_string) {
                        case "Down":
                            default_value = (T) new PVector(0,1);
                            break;
                        case "Up":
                            default_value = (T) new PVector(0,-1);
                            break;
                        case "Right":
                            default_value = (T) new PVector(1,0);
                            break;
                        case "Left":
                            default_value = (T) new PVector(-1,0);
                            break;
                    }
                }
                else if(ruleType == rule_types.BOOLEAN) {
                    default_value = (T) (Boolean) obj.getProperty("defaultValue");
                }
                else if(ruleType == rule_types.OPERATIONAL) {
                    default_value = null;
                    //TODO: have a map of strings to functions
                }

                TiledObject textObj = (TiledObject) obj.getProperty("text");
                Docking_Plugin.Docking.Text t = null;
                if(textObj != null) t = new Docking_Plugin.Docking.Text((String) textObj.getProperty("Content"),
                        new PVector(textObj.getX()*tileScale, textObj.getY()*tileScale), new PVector(textObj.getWidth()*tileScale, textObj.getHeight()*tileScale));

                Docking_Plugin.Docking d = new Docking_Plugin.Docking(
                        new PVector(obj.getWidth() * tileScale, obj.getHeight() * tileScale),
                        default_value, ruleType, changeable, t
                );
                e.add(d);
                e.add(new Changeable(d));
//                e.add(Collider.BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale));

                e.add(new Collider(new BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale), (collision) -> {
                    if(collision.other().has(Box.class) && collision.other().get(Box.class).rule_type == collision.self().get(Docking_Plugin.Docking.class).rule_type) {
                        //lock box into place (remove velocity)
                        PVector gravity = Resource.get(game, Gravity.class).gravity();
                        collision.other().get(Position.class).position.copy().add(
                                new PVector(collision.other().get(Velocity.class).velocity.x * game.abs(gravity.y),
                                        collision.other().get(Velocity.class).velocity.y * game.abs(gravity.x)
                                ));
                        collision.other().removeType(Velocity.class);




                        if(collision.self().get(Docking_Plugin.Docking.class).get() == null) return;


                        //apply box value to the changeable
                        if(collision.other().get(Box.class).docked == null) collision.self().get(Docking_Plugin.Docking.class).get().get().change(collision.other().get(Box.class).value);
                        collision.self().get(Docking_Plugin.Docking.class).block = collision.other();
                        collision.other().get(Box.class).docked = collision.self();
                    }
                }, false));
                return e;
            }),
            Map.entry("gravity", obj -> {
                Gravity g = new Gravity(new PVector(obj.getX() * tileScale * 1f, obj.getY() * tileScale * 1f));
                Entity e = game.dom.createEntity(
                    new Position(new PVector()), // to make sure gravity gets deleted when world is reset
                    new Changeable(g),
                    g
                );

                return e;
            }),
            Map.entry("spikes", obj -> {

                PImage spikeImage = Resource.get(game, AssetManager.class).getResource(PImage.class, "spikes.png");
                RepeatedSprite sprite = new RepeatedSprite(spikeImage, map.getTileWidth() * tileScale, map.getTileHeight() * tileScale);

                return game.dom.createEntity(
                    new Position(new PVector(obj.getX() * tileScale, obj.getY() * tileScale)),
                    new Spike_Plugin.Spikes(obj.getWidth() * tileScale,obj.getHeight() * tileScale),
                    new Collider(new BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale), (collision) -> {
                        if(collision.other().has(Player.class) && collision.other().get(Player.class).invulnerability <= 0f) {
                            collision.other().get(Player.class).lives--;
                            if(collision.other().get(Player.class).lives <= 0) {
                                //player has died, restart the level
                                System.out.println("Player has died");
                            }
                            collision.other().get(Player.class).invulnerability = 1f;

                        }
                    }),
                    new SpriteRenderer(sprite, obj.getWidth() * tileScale, obj.getHeight() * tileScale)
                );
            })
        );
    }
}
