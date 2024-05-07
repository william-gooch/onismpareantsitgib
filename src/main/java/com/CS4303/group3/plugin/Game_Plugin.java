package com.CS4303.group3.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.tiledreader.TiledMap;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.Resource.ResourceEntity;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Player_Plugin.*;
import com.CS4303.group3.plugin.Sprite_Plugin.AnimatedSprite;
import com.CS4303.group3.plugin.Sprite_Plugin.ISprite;
import com.CS4303.group3.plugin.Sprite_Plugin.Sprite;
import com.CS4303.group3.plugin.Sprite_Plugin.SpriteRenderer;
import com.CS4303.group3.plugin.Sprite_Plugin.StateSprite;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Force_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.plugin.Assets_Plugin.AssetManager;
import com.CS4303.group3.plugin.Box_Plugin.*;
import com.CS4303.group3.plugin.Button_Plugin.*;
import com.CS4303.group3.plugin.Door_Plugin.*;
import com.CS4303.group3.utils.Changeable;
import com.CS4303.group3.utils.Collision.BasicCollider;

import dev.dominion.ecs.api.*;
import dev.dominion.ecs.api.Results.With1;
import dev.dominion.ecs.api.Results.With2;
import processing.core.*;
import com.fasterxml.jackson.databind.*;

public class Game_Plugin implements Plugin_Interface {
    @Override
    public void build(Game game) {
        Resource.add(game, new WorldManager(game, game.displayHeight / 25, game.displayWidth / 30));

        game.schedule.setup(() -> {
            var wm = Resource.get(game, WorldManager.class);
            wm.titleScreen();
        });

        game.schedule.keyDown(() -> {
            var wm = Resource.get(game, WorldManager.class);
            var input = Resource.get(game, InputSystem.class);
            if(input == null) return;
            if (input.isLastKeyDown(' ')) {
                wm.startGame();
            }
            if(input.isLastKeyDown(input.keybinds.get(InputSystem.keys.SETTINGS))) {
                wm.settings();
            }
            if(input.isLastKeyDown('R')) {
                wm.restartLevel();
            }
        });

        game.schedule.update(() -> {
            var wm = Resource.get(game, WorldManager.class);
            var input = Resource.get(game, InputSystem.class);
            if(input != null) {
                if(input.isKeyDown((int) 'N')) {
                    wm.newLevel();
                }
            }
            if(wm.level > 10) { //num of levels
                wm.gameOver();
            }
        });

        game.schedule.update(() -> {
            var wm = Resource.get(game, WorldManager.class);
            var player = Resource.get(game, Player.class);
            if(player != null) {
                if(player.lives <= 0) wm.restartLevel();
            }
        });

        game.schedule.draw(draw -> {
            game.dom.findEntitiesWith(TextScreen.class)
                    .stream().forEach(res -> {
                        draw.call(drawing -> {
                            drawing.push();
                            drawing.textAlign(PConstants.CENTER, PConstants.TOP);
                            drawing.textSize(30);
                            drawing.fill(255, 0, 255);
                            drawing.text(res.comp().text(), 10, game.scale/2 + res.comp().textPosition(), game.scale - 10, game.scale);
                            drawing.textSize(20);
                            drawing.text(res.comp().subText(), 10, game.scale/2 + res.comp().subTextPosition(), game.scale - 10, game.scale);
                            drawing.pop();
                        });
                    });
        });

        game.schedule.draw(draw -> {
            var wm = Resource.get(game, WorldManager.class);
            if(wm.state != WorldManager.WorldState.PLAYING) return;
            draw.call(drawing -> {
                drawing.push();
                drawing.textSize(20);
                drawing.fill(255, 0, 255);
                drawing.textAlign(PConstants.LEFT, PConstants.TOP);
                drawing.text("Level: " + wm.level, 20, 20);
                drawing.textAlign(PConstants.RIGHT, PConstants.TOP);
                Player player = Resource.get(game, Player.class);
                if(player != null) drawing.text("Lives: " + player.lives, game.scale-20, 20);
                drawing.pop();
            });
        });
    }


    static class WorldManager {
        public enum WorldState {
            TITLE,
            SETTINGS,
            PLAYING,
            GAME_OVER,
        }

        WorldState state;
        Game game;
        ObjectMapper mapper;

        int level;

        public WorldManager(Game game, int cell_width, int cell_height) {
            this.game = game;
            mapper = new ObjectMapper();
        }

        public void clearWorld() {
            game.dom.findEntitiesWith(Position.class)
                .stream().forEach(p -> game.dom.deleteEntity(p.entity()));
        }

        public void clearText() {
            game.dom.findEntitiesWith(TextScreen.class)
                    .stream().forEach(e -> game.dom.deleteEntity(e.entity()));
        }


        public void startGame() {
            if (state != WorldState.PLAYING) {
                clearWorld();
                clearText();
                level = 1;
                createScene(game, game.dom, game.level_name);
                state = WorldState.PLAYING;
            }
        }

        public void gameOver() {
            if(state != WorldState.GAME_OVER) {
                clearWorld();
                createGameOver(game.dom);
                state = WorldState.GAME_OVER;
            }
        }


        public void settings() {

            if(state == WorldState.PLAYING) {
                //pause movement of everything
                game.paused = true;
                createSettingsScene(game.dom);
                state = WorldState.SETTINGS;
            } else if(state == WorldState.SETTINGS) {
                clearText();
                game.paused = false;
                state = WorldState.PLAYING;
            }
        }

        public void titleScreen() {
            if (state != WorldState.TITLE) {
                clearWorld();
                createTitleScreen(game.dom);
                state = WorldState.TITLE;
            }
        }

        public void restartLevel() {
            if(state == WorldState.PLAYING) {
                clearWorld();
                createScene(game, game.dom, game.level_name);
            }
        }


        public void newLevel() {
            if (state == WorldState.PLAYING) {
                clearWorld();
                level++;
                createScene(game, game.dom, game.level_name);
            }
        }

        public void createScene(Game game, Dominion dom, String level_name) {
            // Initialize the world map
            AssetManager am = Resource.get(game, AssetManager.class);
            TiledMap m = am.getResource(TiledMap.class, "test_fragile.tmx");
//            TiledMap m = am.getResource(TiledMap.class, "level"+level+".tmx");
//            TiledMap m = am.getResource(TiledMap.class, "test_bouncy.tmx");
            dom.createEntity(
                new TileMap(game, m)
            );
            dom.createEntity(new Drag());

        }

        public void createPlayer(Game game, float x, float y, float scale) {

            float playerRatio = 36f / 26f;
            float playerWidth = 10 * scale;
            float playerHeight = playerWidth * playerRatio;
            PImage playerImage = Resource.get(game, AssetManager.class).getResource(PImage.class, "player-anim.png");

            List<ISprite> sprites = AnimatedSprite.framesFromSpriteSheet(playerImage, 4);

            StateSprite playerSprite = new StateSprite();
            playerSprite.addState("idle", sprites.get(0));
            playerSprite.addState("throw", sprites.get(3));

            AnimatedSprite runSprite = new AnimatedSprite();
            runSprite.addFrame(sprites.get(1), 0.2f);
            runSprite.addFrame(sprites.get(2), 0.2f);
            playerSprite.addState("run", runSprite);

            playerSprite.setState("idle");
            var spriteRenderer = new SpriteRenderer(playerSprite, playerHeight, playerHeight, new PVector(0.4f, 0.5f));
            spriteRenderer.offset = new PVector(-0.15f, 0f);

            game.dom.createEntity(
                new Position(new PVector(x - playerWidth/2, y - playerHeight/2)),
                new Velocity(),
                spriteRenderer,
                new Player(),
                new Grab(40),
                new PlayerMovement(),
                new Body(),
                Collider.BasicCollider(playerWidth, playerHeight)
            );

            game.playerWidth = playerWidth;
            game.playerHeight = playerHeight;
        }

        public void createExit(Game game, float x, float y){
        
            game.dom.createEntity(
                    new Position(new PVector(x, y)),
                    new Collider(new BasicCollider(45, 45), (collision) -> {
                        if(collision.other().has(Player.class)) {
                            var wm = Resource.get(game, WorldManager.class);
                            wm.newLevel();
                        }
                    })

            );

        }

        private void createSettingsScene(Dominion dom) {
            dom.createEntity(new TextScreen("Select a binding and then press the desired key", "0: Settings\n" +
                                                                                               "1: Move Left\n" +
                                                                                               "2: Move Right\n" +
                                                                                               "3: Jump\n" +
                                                                                               "4: Pickup/Throw\n", -50, 50));
        }

        private void createGameOver(Dominion dom) {
            dom.createEntity(new TextScreen("", "Press [SPACE] to play again.", 0, 255));
            AssetManager am = Resource.get(game, AssetManager.class);
            PImage logo = am.getResource(PImage.class, "GameOver.png");
            float logoRatio = PApplet.round((float)game.width / (float)logo.width),
                    logoWidth = logo.width * logoRatio,
                    logoHeight = logo.height * logoRatio;

            dom.createEntity(
                    new Position(new PVector(game.width/2 - logoWidth/2, game.height/2 - logoHeight/2 - 50)),
                    new SpriteRenderer(new Sprite(logo), logoWidth, logoHeight)
            );
        }

        private void createTitleScreen(Dominion dom) {
            dom.createEntity(new TextScreen("", "Press [SPACE] to play.", 0, 225));
            AssetManager am = Resource.get(game, AssetManager.class);
            PImage logo = am.getResource(PImage.class, "logo.png");
            float logoRatio = PApplet.round((float)game.width / (float)logo.width),
                  logoWidth = logo.width * logoRatio,
                  logoHeight = logo.height * logoRatio;

            dom.createEntity(
                new Position(new PVector(game.width/2 - logoWidth/2, game.height/2 - logoHeight/2 - 50)),
                new SpriteRenderer(new Sprite(logo), logoWidth, logoHeight)
            );
        }
    }


    static record TextScreen(
            String text,
            String subText,
            float textPosition,
            float subTextPosition
    ) {
    }
}
