package com.CS4303.group3.utils;

import com.CS4303.group3.Game;

import processing.core.PGraphics;

public class Map {
    private int[][] map;
    private PGraphics buffer;

    private int cell_width, cell_height;


    public Map(int[][] layout, int cell_width, int cell_height, Game game) {
        this.map = layout;
        this.cell_width = cell_width;
        this.cell_height = cell_height;
        //draw to the buffer
        //draw floor for the time being
        for(int i = 0; i < map[0].length; i++) map[24][i] = 1;
        //change textures that are draw here
        buffer = game.createGraphics(game.displayWidth, game.displayHeight);
        buffer.beginDraw();
        buffer.background(0);
        for(int i = 0; i < map.length; i++) {
            for(int j = 0; j < map[0].length; j++) {
                if(map[i][j] == 1) {
                    buffer.rect(j*cell_width, i*cell_height, cell_width, cell_height);
                }
            }
        }
        buffer.endDraw();
    }

    public void draw(Game drawing) {
        //render normally
        // drawing.background(0);
        // for(int i = 0; i < map.length; i++) {
        //     for(int j = 0; j < map[0].length; j++) {
        //         if(map[i][j] == 1) {
        //             drawing.rect(j*cell_width, i*cell_height, cell_width, cell_height);
        //         }
        //     }
        // }
        //render from the buffer
        drawing.image(buffer,0,0);
    }
}
