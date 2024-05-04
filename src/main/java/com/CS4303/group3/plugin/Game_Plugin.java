package com.CS4303.group3.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;

import org.tiledreader.TiledMap;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.Resource.ResourceEntity;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Player_Plugin.*;
import com.CS4303.group3.plugin.Sprite_Plugin.Sprite;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Force_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.plugin.Assets_Plugin.AssetManager;
import com.CS4303.group3.plugin.Box_Plugin.*;
import com.CS4303.group3.plugin.Button_Plugin.*;
import com.CS4303.group3.plugin.Door_Plugin.*;
import com.CS4303.group3.utils.Changeable;
import com.CS4303.group3.utils.Collision.BasicCollider;
import com.CS4303.group3.utils.Map;
import com.CS4303.group3.utils.Map.Ground_Tile;

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
        });

        game.schedule.update(() -> {
            var wm = Resource.get(game, WorldManager.class);
            var input = Resource.get(game, InputSystem.class);
            if(input != null) {
                if(input.isKeyDown((int) 'N')) {
                    wm.newLevel();
                }
            }
            if(wm.level > 10) {
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
                            drawing.fill(255);
                            drawing.text(res.comp().text(), 10, game.scale/2 - 50, game.scale - 10, game.scale);
                            drawing.textSize(20);
                            drawing.text(res.comp().subText(), 10, game.scale/2 + 50, game.scale - 10, game.scale);
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
            game.dom.findAllEntities()
                    .stream().filter(e -> !e.has(ResourceEntity.class)).forEach(e -> game.dom.deleteEntity(e));
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
            // // Initialize the world map
//             Map map;
//             try {
//                 map = mapper.readValue(new File(level_name), Map.class);
//             } catch(IOException e) {return;}
//
//             //create solid ground sections
//             for(Ground_Tile ground_tile : map.ground_tiles) {
//                 dom.createEntity(
//                     new Position(ground_tile.position.copy().mult(game.scale)),
//                     new Ground(ground_tile.size.copy().mult(game.scale)),
//                     Collider.BasicCollider((int)(ground_tile.size.x * game.scale), (int)(ground_tile.size.y * game.scale))
//                 );
//             }
            AssetManager am = Resource.get(game, AssetManager.class);
            TiledMap m = am.getResource(TiledMap.class, "test.tmx");
            dom.createEntity(
                new TileMap(game, m)
            );
            
//             int playerWidth = (int) (game.scale/30);
//             int playerHeight = (int) (game.scale/30);
            int playerWidth = (int) game.playerWidth;//26;
            int playerHeight = (int) game.playerHeight;//36;

            // create button for testing
            float loweringSpeed = 0.2f;
            dom.createEntity(
                    new Position(new PVector(150, 100)),
                    new Button(playerWidth, playerHeight, loweringSpeed),
                    new Collider(new BasicCollider(playerWidth, playerWidth), (self, other) -> {
                        self.get(Button.class).pushed = true;
                        self.get(Button.class).lastPushed = 0;
                    }));

            //initialise forces
            dom.createEntity(new Gravity());
            dom.createEntity(new Drag());

            // create block for testing
            // dom.createEntity(
            //         new Position(new PVector(100, 100)),
            //         new Velocity(0.5f),
            //         Collider.BasicCollider(playerWidth, playerWidth),
            //         new Body(),
            //         new Grabbable(),
            //         new Box(Box_Plugin.change_direction_up, new PVector(playerWidth, playerWidth), rule_types.OPERATIONAL, Box.directions.UP));

            // dom.createEntity(
            //         new Position(new PVector(100, 200)),
            //         new Velocity(0.5f),
            //         Collider.BasicCollider(playerWidth, playerWidth),
            //         new Body(),
            //         new Grabbable(),
            //         new Box(Box_Plugin.change_direction_down, new PVector(playerWidth, playerWidth), rule_types.OPERATIONAL, Box.directions.DOWN));


            // dom.createEntity(
            //         new Position(new PVector(110, game.scale * 0.9f)),
            //         new Docking_Plugin.Docking(new PVector(playerWidth, playerWidth), null, rule_types.OPERATIONAL, Resource.get(game, Gravity.class),
            //                 new Docking_Plugin.Docking.Text("Gravity goes ", new PVector(10, game.scale * 0.9f), new PVector(100, playerHeight)))
            // );

            // Entity dock = dom.findEntitiesWith(Docking_Plugin.Docking.class).stream().findFirst().get().entity();
            // Entity box = dom.findEntitiesWith(Box.class).stream().findFirst().get().entity();
            // box.removeType(Velocity.class);
            // box.get(Body.class).disableCollision();
            // dock.get(Docking_Plugin.Docking.class).insert_new_rule(box, dock.get(Position.class).position, game);


            // dom.createEntity(
            //         new Position(new PVector(100 - playerWidth/2, 100 - playerHeight/2)),
            //         new Velocity(),
            //         new Player(playerWidth, playerHeight),
            //         new Grab(40),
            //         new Sprite(game.loadImage("player.png"), playerWidth, playerHeight),
            //         new PlayerMovement(game.scale/3f, game.scale/40f, game.scale/40f, game.scale/100f, game.scale/40f, game.scale/2000f, game.scale/800f),
            //         new Body(),
            //         Collider.BasicCollider(playerWidth, playerHeight)
            // );

            int doorWidth = playerWidth / 2;
            int doorHeight = playerHeight;

            // create two Door for testing
            dom.createEntity(
                    new Position(new PVector(100, 100)),
                    new Door(doorWidth, doorHeight),
                    new Collider(new BasicCollider(doorWidth, doorHeight)));

            dom.createEntity(
                        new Position(new PVector(100, 50)),
                        new Door(doorWidth, doorHeight),
                        new Collider(new BasicCollider(doorWidth, doorHeight)));




            PVector[] test_path = new PVector[4];
            test_path[0] = new PVector(100,100);
            test_path[1] = new PVector(100,300);
            test_path[2] = new PVector(300,300);
            test_path[3] = new PVector(300,100);

            dom.createEntity(
                    new Position(new PVector(100,100)),
                    new Enemy_Plugin.Basic_AI(test_path),
                    new Collider(new BasicCollider(playerWidth, playerHeight), (self, other) -> {
                        if(other.has(Player.class) && other.get(Player.class).invulnerability == 0f) {
                            System.out.println("Damaged Player, player is now invulnerable");
                            other.get(Player.class).lives--;
                            if (other.get(Player.class).lives <= 0) {
                                //player has died, restart the level
                                System.out.println("Player has died");
                            }
                            other.get(Player.class).invulnerability = 1f;
                        }
                    }, false)
            );

            //Initialise button chained

            // assigns a button to the associated door or changes direction of gravity on push - will need to change this so that
            // this is encoded in the JSON rather

            // Iterator<With2<Door, Position>> doors = dom.findEntitiesWith(Door.class, Position.class).iterator();
            // With2<Door, Position> door1 = doors.next();
            // With2<Door, Position> door2 = doors.next();

            // Button button = dom.findEntitiesWith(Button.class).iterator().next().comp();

            //Testing event listener behaviours for door that is opened by button / changes gravity
            // button.addEventListener(new ButtonEventListener() {

            //     @Override
            //     public void onPush() {
            //         door1.comp1().moveDoor(game, door1.comp2().position, button); //this is for a singular door but could be changed to deal with all doors
            //     }

            //     @Override
            //     public void onRelease() {
            //         door1.comp1().moveDoor(game, door1.comp2().position, button);
            //     }

            // });

            //This works for changing the direction of gravity
            // button.addEventListener(new ButtonEventListener() {
            //     @Override
            //     public void onPush() {
            //         Gravity gravity = Resource.get(game, Gravity.class);
            //         gravity.changeGravity(new PVector(0,1));
            //         //door1.comp1().moveDoor(game, door1.comp2().position, btn.comp());
            //     }

            //     @Override
            //     public void onRelease() {
            //         Gravity gravity = Resource.get(game, Gravity.class);
            //         gravity.changeGravity(new PVector(1,0));
            //         //door1.comp1().moveDoor(game, door1.comp2().position, btn.comp());
            //     }

            // });


            // button.addEventListener(new ButtonEventListener() {
            //     @Override
            //     public void onPush() {
            //         door2.comp1().moveDoor(game, door2.comp2().position, button);
            //     }

            //     @Override
            //     public void onRelease() {
            //         door2.comp1().moveDoor(game, door2.comp2().position, button);
            //     }

            // });

        }

        public void createPlayer(Game game, float x, float y) {

            // int playerX = (int) (map.player_position.x * game.scale);
            // int playerY = (int) (map.player_position.x * game.scale);
            int playerWidth = 26;
            int playerHeight = 36;
            PImage playerImage = Resource.get(game, AssetManager.class).getResource(PImage.class, "player.png");
            game.dom.createEntity(
                new Position(new PVector(x - playerWidth/2, y - playerHeight/2)),
                new Velocity(),
                new Sprite(
                    playerImage,
                    playerWidth,
                    playerHeight,
                    new PVector(9, 18)
                ),
                new Player(playerWidth, playerHeight),
                new Grab(40),
                new PlayerMovement(),
                new Body(),
                Collider.BasicCollider(playerWidth, playerHeight)
            );
        }

        private void createSettingsScene(Dominion dom) {
            dom.createEntity(new TextScreen("Select a binding and then press the desired key", "0: Settings\n" +
                                                                                                            "1: Move Left\n" +
                                                                                                            "2: Move Right\n" +
                                                                                                            "3: Jump\n" +
                                                                                                            "4: Pickup/Throw\n"));
        }

        private void createGameOver(Dominion dom) {
            dom.createEntity(new TextScreen("Game over!", "Press [SPACE] to play again."));
        }

        private void createTitleScreen(Dominion dom) {
            dom.createEntity(new TextScreen("OH NO! I sent my professor a rude email and need to sneak in to get it back", "Press [SPACE] to play."));
        }
    }


    static record TextScreen(
            String text,
            String subText
    ) {}
}
