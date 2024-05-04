package com.CS4303.group3.plugin;

import java.util.HashMap;

import org.tiledreader.FileSystemTiledReader;
import org.tiledreader.TiledMap;
import org.tiledreader.TiledReader;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;

import processing.core.PImage;

public class Assets_Plugin implements Plugin_Interface {
    @Override
    public void build(Game game) {
        System.out.println("creating asset manager");
        Resource.add(game, new AssetManager(game));
    }

    static class AssetManager {
        private Game game;
        private HashMap<String, Object> assets = new HashMap<>();

        public AssetManager(Game game) {
            this.game = game;
        }

        public <T> T getAsset(Class<T> type, String path) {
            if (assets.get(path) == null) {
                Object o = null;
                if(type.equals(PImage.class)) {
                    o = game.loadImage(path);
                    assets.put(path, o);
                } else if (type.equals(TiledMap.class)) {
                    TiledReader reader = new FileSystemTiledReader();
                    o = reader.getMap(path);
                }
                return (T) o;
            } else {
                return (T) assets.get(path);
            }
        }

        public <T> T getResource(Class<T> type, String name) {
            String path = AssetManager.class.getClassLoader().getResource(name).getPath();
            return getAsset(type, path);
        }
    }
}