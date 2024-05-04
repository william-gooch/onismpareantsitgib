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
                    boolean flipX = sprite.comp2().flipX;
                    boolean flipY = sprite.comp2().flipY;
                    draw.call(drawing -> {
                        drawing.push();
                        drawing.translate(sprite.comp1().position.x, sprite.comp1().position.y);
                        drawing.scale(flipX ? -1 : 1, flipY ? -1 : 1);
                        drawing.translate(-sprite.comp2().anchorPoint.x, -sprite.comp2().anchorPoint.y);
                        drawing.translate((flipX ? -1 : 1) * sprite.comp2().width / 2, (flipY ? -1 : 1) * sprite.comp2().height / 2);
                        drawing.image(sprite.comp2().image, 0, 0, sprite.comp2().width, sprite.comp2().height);
                        drawing.pop();
                    });
                });
        });

        game.schedule.draw(draw -> {
            dom.findEntitiesWith(Position.class, StateSprite.class)
                .stream().forEach(sprite -> {
                    Sprite currentSprite = sprite.comp2().getSprite();
                    if(currentSprite == null) {
                        return;
                    }
                    boolean flipX = currentSprite.flipX;
                    boolean flipY = currentSprite.flipY;
                    draw.call(drawing -> {
                        drawing.push();
                        drawing.translate(sprite.comp1().position.x, sprite.comp1().position.y);
                        drawing.scale(flipX ? -1 : 1, flipY ? -1 : 1);
                        drawing.translate(-currentSprite.anchorPoint.x, -currentSprite.anchorPoint.y);
                        drawing.translate((flipX ? -1 : 1) * currentSprite.width / 2, (flipY ? -1 : 1) * currentSprite.height / 2);
                        drawing.image(currentSprite.image, 0, 0, currentSprite.width, currentSprite.height);
                        drawing.pop();
                    });
                });
        });
    }

    static class Sprite {
        PImage image;
        float width;
        float height;
        PVector anchorPoint;
        boolean flipX = false;
        boolean flipY = false;

        Sprite(PImage image) {
            this.image = image;
            this.width = image.width;
            this.height = image.height;
            this.anchorPoint = new PVector(width / 2, height / 2);
        }

        Sprite(PImage image, float width, float height) {
            this.image = image;
            this.width = width;
            this.height = height;
            this.anchorPoint = new PVector(width / 2, height / 2);
        }

        Sprite(PImage image, float width, float height, PVector anchorPoint) {
            this.image = image;
            this.width = width;
            this.height = height;
            this.anchorPoint = anchorPoint;
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