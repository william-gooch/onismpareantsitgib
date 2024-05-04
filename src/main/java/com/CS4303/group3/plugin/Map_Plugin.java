package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Assets_Plugin.AssetManager;
import com.CS4303.group3.plugin.Box_Plugin.Box;
import com.CS4303.group3.plugin.Box_Plugin.Grabbable;
import com.CS4303.group3.plugin.Button_Plugin.Button;
import com.CS4303.group3.plugin.Button_Plugin.ButtonEventListener;
import com.CS4303.group3.plugin.Door_Plugin.Door;
import com.CS4303.group3.plugin.Game_Plugin.WorldManager;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Sprite_Plugin.Sprite;
import com.CS4303.group3.plugin.Sprite_Plugin.StateSprite;
import com.CS4303.group3.utils.Collision;
import com.CS4303.group3.utils.Collision.BasicCollider;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import processing.core.*;

import java.util.HashMap;

import org.tiledreader.*;

public class Map_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        this.dom = game.dom;

//         game.schedule.draw(-5, draw -> {
//             dom.findEntitiesWith(Ground.class, Position.class)
//                 .stream().forEach(entity -> {
//                     Ground ground = entity.comp1();
//                     PVector position = entity.comp2().position;
//                     draw.call(drawing -> {
//                         drawing.push();
//                         ground.draw(drawing, position);
//                         drawing.pop();
//                     });
//                 });
//         });

        game.schedule.draw(-10, draw -> {
            dom.findEntitiesWith(Position.class, RenderTile.class)
                .stream().forEach(entity -> {
                    draw.call(drawing -> {
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
        float height
    ) { }

    static class TileMap {
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

        private Entity createRenderTileFromTile(TiledTile tile, float x, float y) {
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
                new RenderTile(tileImage, map.getTileWidth() * tileScale, map.getTileHeight() * tileScale)
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

        private Entity createSpriteFromTile(TiledTile tile, float width, float height, float x, float y) {
            PImage tileImage = getTileImage(tile);

            // return next.getImage().getSource();
            var e = game.dom.createEntity(
                new Position(new PVector(x * tileScale, y * tileScale)),
                new Sprite(tileImage, width * tileScale, height * tileScale)
            );

            return e;
        }

        public void generateRenderTiles() {
            for(var layer : map.getNonGroupLayers()) {
                if(TiledTileLayer.class.isInstance(layer)) {
                    TiledTileLayer tileLayer = (TiledTileLayer) layer;
                    for (int x = tileLayer.getX1(); x <= tileLayer.getX2(); x++) {
                        for (int y = tileLayer.getY1(); y <= tileLayer.getY2(); y++) {
                            var tile = tileLayer.getTile(x, y);

                            if(tile != null) {
                                createRenderTileFromTile(tile, x * map.getTileWidth(), y * map.getTileHeight());
                            }
                        }
                    }
                }
            }
        }

        public void generateObjects() {
            for(var layer : map.getNonGroupLayers()) {
                if(TiledObjectLayer.class.isInstance(layer)) {
                    TiledObjectLayer objectLayer = (TiledObjectLayer) layer;
                    for(var obj : objectLayer.getObjects()) {
                        Entity e;
                        if(obj.getType().equals("block") && obj.getTile() != null) {
                            e = createSpriteFromTile(obj.getTile(), obj.getWidth(), obj.getHeight(), obj.getX(), obj.getY() - obj.getHeight());
                            e.add(new Velocity(0.5f));
                            e.add(new Body());
                            e.add(new Grabbable());
                            e.add(new Box());
                            e.add(Collider.BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale));
                        } else if(obj.getType().equals("player_spawn")) {
                            WorldManager wm = Resource.get(game, WorldManager.class);
                            wm.createPlayer(game, obj.getX() * tileScale, obj.getY() * tileScale);
                            e = null;
                        } else if(obj.getType().equals("button")) {
                            PImage offImage = getTileImage(obj.getTile());
                            PImage onImage = getTileImage(obj.getTile().getTileset().getTile((int) obj.getProperty("onImage")));
                            StateSprite sprite = new StateSprite();
                            sprite.addState("off", new Sprite(offImage, obj.getWidth() * tileScale, obj.getHeight() * tileScale));
                            sprite.addState("on", new Sprite(onImage, obj.getWidth() * tileScale, obj.getHeight() * tileScale));
                            sprite.setState("off");
                            e = game.dom.createEntity(
                                new Position(new PVector(obj.getX() * tileScale, (obj.getY() - obj.getHeight()) * tileScale)),
                                sprite
                            );
                            Button button = new Button((int) (obj.getWidth() * tileScale), (int) (obj.getHeight() * tileScale), 0.25f);
                            button.addEventListener(new ButtonEventListener() {

                                @Override
                                public void onPush() {
                                    e.get(StateSprite.class).setState("on");
                                    var door = objects.get(obj.getProperty("trigger"));
                                    door.get(Door.class).open.change(true);
                                    door.get(Door.class).moveDoor(game, door.get(Position.class).position);
                                }

                                @Override
                                public void onRelease() {
                                    e.get(StateSprite.class).setState("off");
                                    var door = objects.get(obj.getProperty("trigger"));
                                    door.get(Door.class).open.change(false);
                                    door.get(Door.class).moveDoor(game, door.get(Position.class).position);
                                }
                                
                            });
                            e.add(button);
                            e.add(new Collider(new BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale), 
                                (self, other) -> {
                                    self.get(Button.class).pushed = true;
                                    self.get(Button.class).lastPushed = 0;
                                })
                            );
                        } else if(obj.getType().equals("door")) {
                            e = createSpriteFromTile(obj.getTile(), obj.getWidth(), obj.getHeight(), obj.getX(), obj.getY() - obj.getHeight());
                            e.add(new Door((int) (obj.getWidth() * tileScale), (int) (obj.getHeight() * tileScale)));
                            e.add(Collider.BasicCollider(obj.getWidth() * tileScale, obj.getHeight() * tileScale));
                            e.add(new Ground(new PVector(obj.getWidth() * tileScale, obj.getHeight() * tileScale)));
                        } else if(obj.getTile() != null) { // fallback for visual-only elements
                            e = createSpriteFromTile(obj.getTile(), obj.getWidth(), obj.getHeight(), obj.getX(), obj.getY() - obj.getHeight());
                        } else {
                            e = null;
                        }
                        objects.put(obj, e);
                    }
                }
            }
        }
    }
}
