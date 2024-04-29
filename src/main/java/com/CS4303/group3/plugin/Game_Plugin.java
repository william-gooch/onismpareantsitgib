package com.CS4303.group3.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Player_Plugin.*;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Force_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.plugin.Box_Plugin.*;
import com.CS4303.group3.plugin.Button_Plugin.*;
import com.CS4303.group3.plugin.Door_Plugin.*;
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
        game.dom.createEntity(new WorldManager(game, game.displayHeight / 25, game.displayWidth / 30));

        var wm = Resource.get(game, WorldManager.class);

        wm.startGame();
    }

    static class WorldManager {
        enum WorldState {
            TITLE,
            PLAYING,
            GAME_OVER,
        }

        WorldState state;
        Game game;
        ObjectMapper mapper;

        int cell_width, cell_height;

        public WorldManager(Game game, int cell_width, int cell_height) {
            this.game = game;
            this.cell_height = cell_height;
            this.cell_width = cell_width;
            mapper = new ObjectMapper();
        }

        public void startGame() {
            if (state != WorldState.PLAYING) {
                createScene(game, game.dom);
                state = WorldState.PLAYING;
            }
        }

        public void create_level() {
            // initialise input system
            game.dom.createEntity(new InputSystem());

            // initialise map
            game.dom.createEntity(new Map());
        }

        public void newLevel() {
            if (state == WorldState.PLAYING) {
                createScene(game, game.dom);
            }
        }

        public void createScene(Game game, Dominion dom) {
            // Initialize the world map
            Map map;
            try {
                map = mapper.readValue(new File(game.level_name), Map.class);
            } catch (IOException e) {
                return;
            }

            // create solid ground sections
            for (Ground_Tile ground_tile : map.ground_tiles) {
                dom.createEntity(
                        new Position(ground_tile.position.copy().mult(game.scale)),
                        new Ground(ground_tile.size.copy().mult(game.scale)),
                        Collider.BasicCollider((int) (ground_tile.size.x * game.scale),
                                (int) (ground_tile.size.y * game.scale)));
            }

            int playerWidth = (int) (game.scale / 30);
            int playerHeight = (int) (game.scale / 30);

            // create block for testing
            dom.createEntity(
                    new Position(new PVector(100, 100)),
                    new Velocity(0.5f),
                    Collider.BasicCollider(playerWidth, playerHeight),
                    new Body(),
                    new Grabbable(),
                    new Box());

            float loweringSpeed = 0.2f;

            // create button for testing
            dom.createEntity(
                    new Position(new PVector(150, 100)),
                    new Button(playerWidth, playerHeight, loweringSpeed),
                    new Collider(new BasicCollider(playerWidth, playerHeight), (self, other) -> {
                        self.get(Button.class).pushed = true;
                        self.get(Button.class).lastPushed = 0;
                    }));

            int doorWidth = playerWidth / 2;
            int doorHeight = playerHeight;

            // create two Door for testing
            dom.createEntity(
                    new Position(new PVector(100, 100)),
                    new Door(doorWidth, doorHeight, null),
                    new Collider(new BasicCollider(doorWidth, doorHeight)));

            dom.createEntity(
                        new Position(new PVector(100, 50)),
                        new Door(doorWidth, doorHeight, null),
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
                            other.get(Player.class).invulnerability = 1f;
                        }
                    }, false)
            );


            //Initialise the inputs
            dom.createEntity(new InputSystem());

            // initialise forces
            dom.createEntity(new Gravity());
            dom.createEntity(new Drag());

            // Initialize the player
            int playerX = (int) (map.player_position.x * game.scale);
            int playerY = (int) (map.player_position.x * game.scale);
            dom.createEntity(
                    new Position(new PVector(playerX, playerY)),
                    new Velocity(),
                    new Player(playerWidth, playerHeight),
                    new Grab(40),
                    new PlayerMovement(),
                    new Body(),
                    Collider.BasicCollider(playerWidth, playerHeight));




            //Initialise button chained

            // assigns a button to the associated door or changes direction of gravity on push - will need to change this so that
            // this is encoded in the JSON rather 
                    
            Iterator<With2<Door, Position>> doors = dom.findEntitiesWith(Door.class, Position.class).iterator();
            With2<Door, Position> door1 = doors.next();
            With2<Door, Position> door2 = doors.next();

            Button button = dom.findEntitiesWith(Button.class).iterator().next().comp();

            //Testing event listener behaviours for door that is opened by button / changes gravity
            button.addEventListener(new ButtonEventListener() {
                            
                @Override
                public void onPush() {
                    door1.comp1().moveDoor(game, door1.comp2().position, button); //this is for a singular door but could be changed to deal with all doors
                }

                @Override
                public void onRelease() {
                    door1.comp1().moveDoor(game, door1.comp2().position, button);
                }

            });

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


            button.addEventListener(new ButtonEventListener() {
                @Override
                public void onPush() {
                    door2.comp1().moveDoor(game, door2.comp2().position, button);
                }

                @Override
                public void onRelease() {
                    door2.comp1().moveDoor(game, door2.comp2().position, button);
                }

            });


        }
        
    }
}
