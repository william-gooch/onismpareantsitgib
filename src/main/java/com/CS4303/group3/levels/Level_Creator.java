package com.CS4303.group3.levels;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.CS4303.group3.utils.Map;
import com.CS4303.group3.utils.Map.Ground_Tile;

import processing.core.*;
import com.fasterxml.jackson.databind.*;

public class Level_Creator extends PApplet {

    static String file_name = "./src/main/java/com/CS4303/group3/levels/default_level.json";
    static String input = "";
    public float scale;
    public float player_size;
    
    public static void main(String[] args) {
        if(args.length > 0) file_name = "./src/main/java/com/CS4303/group3/levels/" + args[0] + ".json";
        if(args.length > 1) input = "./src/main/java/com/CS4303/group3/levels/" + args[1] + ".json";
        main(Level_Creator.class.getName());
    }

    public void settings() {
//        fullScreen();
        if(displayHeight > displayWidth) {
            scale = displayWidth;
        } else scale = displayHeight;

        size((int)scale, (int)scale);
    }

    Map map;
    PVector current_pos;
    PVector size;
    float square_size;
    boolean started = false;

    int cube_with = 50, cube_height = 50;

    ObjectMapper mapper;

    public void setup() {
        player_size = scale/50f;
        square_size = scale/50f;
        mapper = new ObjectMapper();

        if(input == "") map = new Map();
        else try {map = mapper.readValue(new File(input), Map.class);} catch(IOException e) {e.printStackTrace();}
        current_pos = new PVector(0,0);
    }

    public void draw() {
        //draw everything in the Map class
        background(0);
        stroke(50);
        fill(0);
        for(int i = 0; i < cube_height; i++) {
            for(int j = 0; j < cube_with; j++) {
                rect(i*square_size, j*square_size, square_size, square_size);
            }
        }
        fill(255);
        stroke(255);
        map.draw(this);

        if(started) {
            //draw a ground section to the mouse position
            rectMode(CORNERS);
            rect(current_pos.x, current_pos.y, mouseX, mouseY);
            rectMode(CORNER);
        }
    }

    public void keyPressed() {
        if(key == 's') {
            //save the map
            try {
                mapper.writeValue(new File(file_name), map);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        if(key == 'd') {
            //find the object that is hovered over
            //and delete it
            Iterator<Ground_Tile> it = map.ground_tiles.iterator();
            while(it.hasNext()) {
                Ground_Tile ground_tile = it.next();
                if(is_in(ground_tile.position, ground_tile.size)) it.remove();
            }
            
        }

        if(key == 'p') {
            //spawn the player in this position
            map.player_position = new PVector((mouseX - player_size/2)/scale, (mouseY - player_size/2)/scale);
        }
    }

    //checks if the mouse is over the object
    private boolean is_in(PVector object, PVector size) {
        return mouseX >= object.x
                && mouseX <= object.x + size.x
                && mouseY >= object.y
                && mouseY <= object.y + size.y;
    }

    public void mousePressed() {
        started = true;
        //Snap to the grid (divide by 50) round to int and times by 50
        current_pos.set((int)(mouseX/square_size)*square_size, (int)(mouseY/square_size)*square_size);
    }

    public void mouseReleased() {
        //calc the top left position and sizes
        started = false;
        PVector end = new PVector((float) (Math.ceil(mouseX/square_size)*square_size), (float) (Math.ceil(mouseY/square_size)*square_size));
        map.add_ground_tile(new PVector(min(current_pos.x, end.x)/scale, min(current_pos.y, end.y)/scale), new PVector((max(current_pos.x, end.x) - min(current_pos.x, end.x))/scale, (max(current_pos.y, end.y) - min(current_pos.y, end.y))/scale));
    }
}
