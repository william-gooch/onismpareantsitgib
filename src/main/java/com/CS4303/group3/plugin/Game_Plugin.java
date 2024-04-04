package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Object_Plugin.*;
import com.CS4303.group3.plugin.Player_Plugin.*;
import com.CS4303.group3.plugin.Input_Plugin.*;
import com.CS4303.group3.plugin.Force_Plugin.*;
import com.CS4303.group3.plugin.Map_Plugin.*;
import com.CS4303.group3.utils.Map;

import dev.dominion.ecs.api.*;
import processing.core.*;

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

        int cell_width, cell_height;

        public WorldManager(Game game, int cell_width, int cell_height) {
            this.game = game;
            this.cell_height = cell_height;
            this.cell_width = cell_width;
        }


        public void startGame() {
            if(state != WorldState.PLAYING) {
                createScene(game, game.dom);
                state = WorldState.PLAYING;
            }
        }

        public void newLevel() {
            if(state == WorldState.PLAYING) {
                createScene(game, game.dom);
            }
        }


        public void createScene(Game game, Dominion dom) {
            // Initialize the world map
            Map map = new Map(new int[25][30], cell_height, cell_width, game);

            //create solid ground sections - with colliders -- TODO: Change this to create objects from the map
            dom.createEntity(
                new Position(new PVector(0, game.displayHeight - 20)),
                new Ground(new PVector(game.displayWidth, 20)),
                Collider.BasicCollider(game.displayWidth, 20)
            );

            dom.createEntity(
                new Position(new PVector(game.displayWidth/2, game.displayHeight - 100)),
                new Ground(new PVector(game.displayWidth/10, 40)),
                Collider.BasicCollider(game.displayWidth/10, 40)
            );

            dom.createEntity(
                new Position(new PVector(60, game.displayHeight - 300)),
                new Ground(new PVector(game.displayWidth/40, 200)),
                Collider.BasicCollider(game.displayWidth/40, 200)
            );

            //Initialise the inputs
            dom.createEntity(new InputSystem());

            //initialise forces
            dom.createEntity(new Gravity());
            dom.createEntity(new Drag());

            // Initialize the player
            int playerX = game.displayWidth/2;
            int playerY = game.displayHeight/2;
            int playerWidth = (game.displayHeight+game.displayWidth)/60;
            int playerHeight = (game.displayHeight+game.displayWidth)/60;
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
