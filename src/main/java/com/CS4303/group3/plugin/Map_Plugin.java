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
import com.CS4303.group3.utils.Collision;
import com.CS4303.group3.utils.Collision.BasicCollider;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import processing.core.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                rule_types rule_type = ((String) obj.getProperty("ruleType")).equals("Directional") ? rule_types.DIRECTIONAL : rule_types.OPERATIONAL;
                T value = null;
                if(rule_type == rule_types.DIRECTIONAL) {
                    int val = (int) obj.getProperty("value");
                    if(val == 0) value = (T) new PVector(0,1); //down
                    if(val == 1) value = (T) new PVector(1,0); //right
                    if(val == 2) value = (T) new PVector(0,-1); //up
                    if(val == 3) value = (T) new PVector(-1,0); //left
                } else {

                }
                e.add(new Box(rule_type, value));
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
                Entity e = game.dom.createEntity(
                    new Position(new PVector(obj.getX() * tileScale, (obj.getY() - obj.getHeight()) * tileScale)),
                    new SpriteRenderer(sprite, obj.getWidth() * tileScale, obj.getHeight() * tileScale)
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
                    (self, other) -> {
                        self.get(Button.class).pushed = true;
                        self.get(Button.class).lastPushed = 0;
                    })
                );
                return e;
            }),
            Map.entry("door", obj -> {
                Entity e = createSpriteFromObject(obj);
                Door d = new Door((int) (obj.getWidth() * tileScale), (int) (obj.getHeight() * tileScale));
                d.openDirection = 0; //(int) obj.getProperty("openDirection");
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
                AnimatedSprite sprite = new AnimatedSprite();
                PImage enemyImage = Resource.get(game, AssetManager.class).getResource(PImage.class, "enemy-anim.png");
                List<ISprite> frames = AnimatedSprite.framesFromSpriteSheet(enemyImage, 9);
                frames
                    .stream()
                    .limit(7)
                    .forEach(f -> sprite.addFrame(f, 1f/30f));
                for(int i = 0; i < 16; i++) {
                    sprite.addFrame(frames.get(7 + (i%2)), 1f/30f);
                }

                Entity e = game.dom.createEntity(
                    new Position(new PVector(obj.getX() * tileScale, obj.getY() * tileScale)),
                    new SpriteRenderer(sprite, obj.getWidth() * tileScale, obj.getHeight() * tileScale)
                );

                TiledObject path = (TiledObject) obj.getProperty("path");
                PVector[] points = path.getPoints().stream().map(p -> new PVector(((float)p.getX() + obj.getX()) * tileScale, ((float)p.getY() + obj.getY()) * tileScale)).toList().toArray(new PVector[0]);
                e.add(new Enemy_Plugin.Basic_AI(points));
                e.add(new Collider(new BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale), (self, other) -> {
                    if(other.has(Player.class) && other.get(Player.class).invulnerability == 0f) {
                        System.out.println("Damaged Player, player is now invulnerable");
                        other.get(Player.class).lives--;
                        if (other.get(Player.class).lives <= 0) {
                            //player has died, restart the level
                            System.out.println("Player has died");
                        }
                        other.get(Player.class).invulnerability = 1f;
                    }
                }, true));
                return e;
            }),
            Map.entry("dock", obj -> {
                Entity e = createSpriteFromObject(obj);
                //need to find a way to link to the changeable in the entity being changed
                rule_types ruleType = ((String) obj.getProperty("ruleType")).equals("Directional") ? rule_types.DIRECTIONAL : rule_types.OPERATIONAL;
                var trigObj = obj.getProperty("trigger");
                Changeable changeable = null;
                if(trigObj != null) {
                    var trigEntity = objects.get(trigObj);
                    if(trigEntity != null) {
                        changeable = trigEntity.get(Changeable.class);
                    }
                }

                T default_value;
                if(ruleType == rule_types.DIRECTIONAL) {
                    String default_value_string = (String) obj.getProperty("defaultValue");
                    if(default_value_string.equals("Down")) {
                        default_value = (T) new PVector(0,1);
                    } else {
                        default_value = (T) new PVector(0,-1);
                    }
                    //TODO: other directions
                } else {
                    default_value = null;
                    //TODO: have a map of strings to functions
                }
                e.add(new Docking_Plugin.Docking(
                    new PVector(obj.getWidth() * tileScale, obj.getHeight() * tileScale),
                    default_value, ruleType, changeable, null
                ));
//                e.add(Collider.BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale));
                e.add(new Collider(new BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale), (self, other) -> {
                    if(other.has(Box.class) && other.get(Box.class).rule_type == self.get(Docking_Plugin.Docking.class).rule_type) {
                        //lock box into place (remove velocity)
                        System.out.println(other.get(Box.class).value);
                        other.removeType(Velocity.class);


                        //apply box value to the changeable
                        if(other.get(Box.class).docked == null) self.get(Docking_Plugin.Docking.class).changeable.get().change(other.get(Box.class).value);
                        self.get(Docking_Plugin.Docking.class).block = other;
                        other.get(Box.class).docked = self;
                    }
                }, false));
                return e;
            }),
            Map.entry("gravity", obj -> {
                Gravity g = new Gravity(new PVector(obj.getX() * tileScale * 1f, obj.getY() * tileScale * 1f));
                Entity e = game.dom.createEntity(
                    new Position(new PVector()), // to make sure gravity gets deleted when world is reset
                    new Gravity(new PVector(obj.getX() * tileScale, obj.getY() * tileScale)),
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
                    new Collider(new BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale), (self, other) -> {
                        if(other.has(Player.class) && other.get(Player.class).invulnerability <= 0f) {
                            other.get(Player.class).lives--;
                            if(other.get(Player.class).lives <= 0) {
                                //player has died, restart the level
                                System.out.println("Player has died");
                            }
                            other.get(Player.class).invulnerability = 1f;

                        }
                    }),
                    new SpriteRenderer(sprite, obj.getWidth() * tileScale, obj.getHeight() * tileScale)
                );
            })
        );
    }
}
