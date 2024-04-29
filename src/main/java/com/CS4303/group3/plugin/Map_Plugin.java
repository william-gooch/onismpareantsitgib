package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Assets_Plugin.AssetManager;
import com.CS4303.group3.plugin.Object_Plugin.*;

import dev.dominion.ecs.api.Dominion;
import processing.core.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.tiledreader.*;

public class Map_Plugin implements Plugin_Interface {
    Dominion dom;

    @Override
    public void build(Game game) {
        this.dom = game.dom;

        // test loading a map from file
        game.schedule.setup(() -> {
            AssetManager am = Resource.get(game, AssetManager.class);
            TiledMap m = am.getResource(TiledMap.class, "test.tmx");
        
            dom.createEntity(
                new TileMap(m)
            );
        });

        game.schedule.draw(-5, draw -> {
            dom.findEntitiesWith(Ground.class, Position.class)
                .stream().forEach(entity -> {
                    Ground ground = entity.comp1();
                    PVector position = entity.comp2().position;
                    draw.call(drawing -> {
                        drawing.push();
                        ground.draw(drawing, position);
                        drawing.pop();
                    });
                });
        });

        game.schedule.draw(-10, draw -> {
            dom.findEntitiesWith(TileMap.class)
                .stream().forEach(entity -> {
                    draw.call(drawing -> {
                        entity.comp().draw(drawing);
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

    static class TileMap {
        TiledMap map;

        static record RenderTile(
            PImage tileImage,
            float x,
            float y,
            float width,
            float height
        ) {}

        public TileMap(TiledMap map) {
            this.map = map;
        }

        public List<RenderTile> getRenderTiles(AssetManager assets) {
            ArrayList<RenderTile> renderTiles = new ArrayList<>();
            for(var layer : map.getTopLevelLayers()) {
                if(TiledTileLayer.class.isInstance(layer)) {
                    TiledTileLayer tileLayer = (TiledTileLayer) layer;
                    for (int x = tileLayer.getX1(); x <= tileLayer.getX2(); x++) {
                        for (int y = tileLayer.getY1(); y <= tileLayer.getY2(); y++) {
                            var tile = tileLayer.getTile(x, y);

                            if(tile != null) {
                                // asset manager caches the image so it's not a worry if it gets the same image a bunch of times
                                PImage image = assets.getAsset(PImage.class, tile.getTileset().getImage().getSource());
                                PImage tileImage = image.get(
                                    tile.getTilesetX() * map.getTileWidth(),
                                    tile.getTilesetY() * map.getTileHeight(),
                                    map.getTileWidth(),
                                    map.getTileHeight()
                                );

                                // return next.getImage().getSource();
                                renderTiles.add(new RenderTile(
                                    tileImage,
                                    x * map.getTileWidth(),
                                    y * map.getTileHeight(),
                                    map.getTileWidth(),
                                    map.getTileHeight()
                                ));
                            }
                        }
                    }
                }
            }

            return renderTiles;
        }

        public void draw(Game game) {
            AssetManager assets = Resource.get(game, AssetManager.class);
            var renderTiles = this.getRenderTiles(assets);
            game.push();
            game.scale(2);
            game.translate(50, 50);
            for(var tile : getRenderTiles(assets)) {
                game.image(tile.tileImage(), tile.x(), tile.y(), tile.width(), tile.height());
            }
            game.pop();
        }
    }
}
