/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eelstretching.photo;

import com.eelstretching.photo.persist.PhotoInfo;
import com.sleepycat.persist.EntityCursor;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stgreen
 */
public class DumpInfo {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        for (Handler h : Logger.getLogger("").getHandlers()) {
            h.setFormatter(new EelsLogFormatter());
            h.setLevel(Level.ALL);
        }

        Path outputPath = Paths.get(args[0]);

        Path photoDBPath
                = outputPath.resolve("photo.db");

        try (PhotoDB photoDB = new PhotoDB(photoDBPath)) {
            try (EntityCursor<PhotoInfo> entities = photoDB.photoByFinalPath.entities()) {
                for(PhotoInfo info : entities) {
                    System.out.println(info.toString());
                }
            }
        }
    }
    
}
