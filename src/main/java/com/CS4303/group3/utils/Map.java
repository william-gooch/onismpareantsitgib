package com.CS4303.group3.utils;

import java.util.List;

import com.CS4303.group3.levels.Level_Creator;

import java.util.ArrayList;

import processing.core.PVector;

public class Map {
    //variables need to be public for jackson to convert to JSON correctly
    public List<Ground_Tile> ground_tiles;
    public PVector player_position;


    public Map() {
        ground_tiles = new ArrayList<>();
        player_position = new PVector(0,0);
    }

    public void add_ground_tile(PVector position, PVector size) {
        ground_tiles.add(new Ground_Tile(position, size));
    }

    public void add_ground_tile(Ground_Tile tile) {
        ground_tiles.add(tile);
    }

    public void draw(Level_Creator lc) {
        //draw all the ground tiles
//        lc.background(0);
        for(Ground_Tile gt : ground_tiles) {
            lc.rect(gt.position.x * lc.scale, gt.position.y * lc.scale, gt.size.x * lc.scale, gt.size.y * lc.scale);
        }
        //draw the player
        lc.fill(128);
        lc.stroke(128);
        lc.rect(player_position.x * lc.scale, player_position.y * lc.scale, lc.player_size, lc.player_size);
    }


    public static class Ground_Tile {
        public PVector position, size;

        public Ground_Tile(PVector position, PVector size) {
            this.position = position;
            this.size = size;
        }

        public Ground_Tile() {}
    }
}
