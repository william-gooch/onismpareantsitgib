package com.CS4303.group3.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Assets_Plugin.AssetManager;
import com.CS4303.group3.plugin.Object_Plugin.Position;

import dev.dominion.ecs.api.*;
import processing.core.*;

public class Sprite_Plugin implements Plugin_Interface {

    Dominion dom;

    @Override
    public void build(Game game) {
        dom = game.dom;

        game.schedule.draw(4, draw -> {
            dom.findEntitiesWith(Position.class, SpriteRenderer.class)
                .stream().forEach(sprite -> {
                    draw.call(drawing -> sprite.comp2().draw(drawing, sprite.entity()));
                });
        });
    }

    public static class SpriteRenderer {
        ISprite sprite;

        float width;
        float height;
        PVector anchorPoint;
        float rotation = 0;
        boolean flipX = false;
        boolean flipY = false;

        public SpriteRenderer(ISprite sprite, float width, float height) {
            this(sprite, width, height, new PVector(0.5f, 0.5f));
        }

        public SpriteRenderer(ISprite sprite, float width, float height, PVector anchorPoint) {
            this.sprite = sprite;
            this.width = width;
            this.height = height;
            this.anchorPoint = anchorPoint;
        }

        public SpriteRenderer(ISprite sprite, float width, float height, float rotation) {
            this.sprite = sprite;
            this.width = width;
            this.height = height;
            this.anchorPoint = new PVector(0.5f, 0.5f);
            this.rotation = rotation;
        }

        public void draw(Game drawing, Entity sprite) {
            PVector position = sprite.get(Position.class).position;
            drawing.push();

            // move to correct location
            drawing.translate(position.x, position.y);

            // rotate around anchor point
            drawing.translate(width / 2, height / 2);
            drawing.rotate(rotation);
            drawing.translate(-width / 2, -height / 2);

            // flip
            drawing.scale(flipX ? -1 : 1, flipY ? -1 : 1);
            drawing.translate(-anchorPoint.x * width, -anchorPoint.y * height);

            // go to middle of sprite
            drawing.translate((flipX ? -1 : 1) * width / 2, (flipY ? -1 : 1) * height / 2);
            if(sprite.has(Player_Plugin.Player.class) && sprite.get(Player_Plugin.Player.class).invulnerability > 0f) drawing.tint(255, 180);

            this.sprite.draw(drawing, sprite, width, height);

            drawing.tint(255,255);
            drawing.pop();
        }
    }

    interface ISprite {
        public void draw(Game drawing, Entity self, float width, float height);
    }

    static class Sprite implements ISprite {
        PImage image;

        Sprite(PImage image) {
            this.image = image;
        }

        @Override
        public void draw(Game drawing, Entity sprite, float width, float height) {
            drawing.image(image, 0, 0, width, height);
        }
    }

    static class RepeatedSprite implements ISprite {
        PImage image;
        float baseWidth, baseHeight;

        RepeatedSprite(PImage image, float baseWidth, float baseHeight) {
            this.image = image;
            this.baseWidth = baseWidth;
            this.baseHeight = baseHeight;
        }

        @Override
        public void draw(Game drawing, Entity sprite, float width, float height) {
            int tilesX = Game.floor(width / baseWidth);
            int tilesY = Game.floor(height / baseHeight);
            float tileWidth = width / tilesX;
            float tileHeight = height / tilesY;
            for(float i = 0; i < tilesX; i++) {
                for(float j = 0; j < tilesY; j++) {
                    drawing.image(image, i * tileWidth, j * tileHeight, tileWidth, tileHeight);
                }
            }
        }
    }

    static class StateSprite implements ISprite {
        String currentState;
        HashMap<String, ISprite> states;

        public StateSprite() {
            this.states = new HashMap<>();
        }

        public void addState(String name, ISprite sprite) {
            states.put(name, sprite);
        }

        public ISprite getSprite() {
            return states.get(currentState);
        }

        public void setState(String state) {
            currentState = state;
        }

        @Override
        public void draw(Game drawing, Entity sprite, float width, float height) {
            ISprite currentSprite = this.getSprite();
            if(currentSprite == null) {
                return;
            }
            currentSprite.draw(drawing, sprite, width, height);
        }
    }

    static class AnimatedSprite implements ISprite {
        private static record Frame(
            ISprite sprite,
            float duration
        ) {}

        List<Frame> frames;
        float maximumTime = 0f;
        float currentTime = 0f;

        public AnimatedSprite() {
            this.frames = new ArrayList<>();
        }

        public void addFrame(ISprite sprite, float duration) {
            frames.add(new Frame(sprite, duration));
            maximumTime += duration;
        }

        public static List<ISprite> framesFromSpriteSheet(PImage spriteSheet, int numFrames) {
            int frameWidth = spriteSheet.width / numFrames;
            List<ISprite> frames = new ArrayList<>();
            for (int i = 0; i < numFrames; i++) {
                PImage frame = spriteSheet.get(i * frameWidth, 0, frameWidth, spriteSheet.height);
                frames.add(new Sprite(frame));
            }
            return frames;
        }

        public void updateTime(float dt) {
            currentTime += dt;
            if(currentTime > maximumTime) {
                currentTime -= maximumTime;
            }
        }

        @Override
        public void draw(Game drawing, Entity sprite, float width, float height) {
            updateTime(drawing.schedule.dt());

            float frameTime = 0f;
            Frame frame = null;
            var framesIter = frames.iterator();
            while (framesIter.hasNext() && frameTime <= currentTime) {
                frame = framesIter.next();
                frameTime += frame.duration();
            }

            frame.sprite().draw(drawing, sprite, width, height);
        }
    }
}