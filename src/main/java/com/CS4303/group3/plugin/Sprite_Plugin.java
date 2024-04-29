package com.CS4303.group3.plugin;

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
                        drawing.translate((flipX ? -1 : 1) * 26 / 2, (flipY ? -1 : 1) * 36 / 2);
                        drawing.image(sprite.comp2().image, 0, 0, sprite.comp2().image.width, sprite.comp2().image.height);
                        drawing.pop();
                    });
                });
        });
    }

    static class Sprite {
        PImage image;
        PVector anchorPoint = new PVector(9, 18);
        boolean flipX = false;
        boolean flipY = false;

        Sprite(PImage image) {
            this.image = image;
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