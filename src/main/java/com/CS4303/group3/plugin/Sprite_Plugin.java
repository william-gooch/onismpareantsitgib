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

        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Position.class, Sprite.class)
                .stream().forEach(sprite -> {
                    draw.call(drawing -> sprite.comp2().draw(drawing, sprite.entity()));
                });
        });

        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Position.class, StateSprite.class)
                .stream().forEach(sprite -> {
                    Sprite currentSprite = sprite.comp2().getSprite();
                    if(currentSprite == null) {
                        return;
                    }
                    draw.call(drawing -> currentSprite.draw(drawing, sprite.entity()));
                });
        });
    }

    static class Sprite {
        PImage image;
        float width;
        float height;
        float rotation = 0;
        PVector anchorPoint;
        boolean flipX = false;
        boolean flipY = false;

        Sprite(PImage image) {
            this.image = image;
            this.width = image.width;
            this.height = image.height;
            this.anchorPoint = new PVector(0.5f, 0.5f);
        }

        Sprite(PImage image, float width, float height) {
            this.image = image;
            this.width = width;
            this.height = height;
            this.anchorPoint = new PVector(0.5f, 0.5f);
        }

        Sprite(PImage image, float width, float height, PVector anchorPoint) {
            this.image = image;
            this.width = width;
            this.height = height;
            this.anchorPoint = anchorPoint;
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
            drawing.image(image, 0, 0, width, height);
            drawing.tint(255,255);
            drawing.pop();
        }
    }

    static class StateSprite {
        String currentState;
        HashMap<String, Sprite> states;

        public StateSprite() {
            this.states = new HashMap<>();
        }

        public void addState(String name, Sprite sprite) {
            states.put(name, sprite);
        }

        public Sprite getSprite() {
            return states.get(currentState);
        }

        public void setState(String state) {
            currentState = state;
        }
    }

    static class AnimatedSprite {
        private static class Frame {
            PImage image;
            float duration;
        }

        List<Frame> frames;
    }
}