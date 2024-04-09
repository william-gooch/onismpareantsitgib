package com.CS4303.group3.plugin;

import java.io.File;
import java.io.IOException;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Player_Plugin.*;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Force_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.plugin.Box_Plugin.*;
import com.CS4303.group3.utils.Map;
import com.CS4303.group3.utils.Map.Ground_Tile;

import dev.dominion.ecs.api.*;
import processing.core.*;
import com.fasterxml.jackson.databind.*;

public class Game_Plugin implements Plugin_Interface {
    @Override
    public void build(Game game) {
        game.dom.createEntity(new WorldManager(game, game.displayHeight/25, game.displayWidth/30));

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
            if(state != WorldState.PLAYING) {
                createScene(game, game.dom);
                state = WorldState.PLAYING;
            }
        }

        public void create_level() {
            //initialise input system
            game.dom.createEntity(new InputSystem());

            //initialise map
            game.dom.createEntity(new Map());
        }

        public void newLevel() {
            if(state == WorldState.PLAYING) {
                createScene(game, game.dom);
            }
        }


        public void createScene(Game game, Dominion dom) {
            // Initialize the world map
            Map map;
            try {
                map = mapper.readValue(new File(game.level_name), Map.class);
            } catch(IOException e) {return;}

            //create solid ground sections
            for(Ground_Tile ground_tile : map.ground_tiles) {
                dom.createEntity(
                    new Position(ground_tile.position.copy()),
                    new Ground(ground_tile.size.copy()),
                    Collider.BasicCollider((int)ground_tile.size.x, (int)ground_tile.size.y)
                );
            }

            
            int playerWidth = (game.displayHeight+game.displayWidth)/60;
            int playerHeight = (game.displayHeight+game.displayWidth)/60;

            //create block for testing
            dom.createEntity(
                new Position(new PVector(100,100)),
                new Velocity(0.5f),
                Collider.BasicCollider(playerWidth, playerHeight),
                new Box()
            );


            //Initialise the inputs
            dom.createEntity(new InputSystem());

            //initialise forces
            dom.createEntity(new Gravity());
            dom.createEntity(new Drag());

            // Initialize the player
            int playerX = game.displayWidth/2;
            int playerY = game.displayHeight/2;
            // int playerWidth = (game.displayHeight+game.displayWidth)/60;
            // int playerHeight = (game.displayHeight+game.displayWidth)/60;
            dom.createEntity(
                new Position(new PVector(playerX, playerY)),
                new Velocity(),
                new Player(playerWidth, playerHeight),
                new PlayerMovement(),
                Collider.BasicCollider(playerWidth, playerHeight)
                // Collider.circle(PlayerPlugin.PLAYER_RADIUS),
                // new Shoot(),
                // new PlayerSpawnAnimation(),
                // new PowerupContainer(),
                // new PlayerLives()
            );
        }
    }
}
