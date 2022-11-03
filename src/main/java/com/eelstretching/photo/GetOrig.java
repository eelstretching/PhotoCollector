package com.eelstretching.photo;

import com.eelstretching.photo.persist.PhotoInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class GetOrig {

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

            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                String l;
                while ((l = br.readLine()) != null) {
                    l = l.trim();
                    if (l.isEmpty()) {
                        continue;
                    }
                    PhotoInfo info = photoDB.photoByFinalPath.get(l);
                    if (info != null) {
                        System.out.println(info.toString());
                    } else {
                        System.out.println(l + " not found");
                    }
                }
            }
        }
    }
}
