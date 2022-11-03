package com.eelstretching.photo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhotoCollector {

    private static final Logger logger = Logger.getLogger(PhotoCollector.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

        for (Handler h : Logger.getLogger("").getHandlers()) {
            h.setFormatter(new EelsLogFormatter());
            h.setLevel(Level.ALL);
        }

        Path outputPath = Paths.get(args[0]);

        File[] roots = File.listRoots();
        if (args.length > 1) {
            roots = new File[1];
            roots[0] = new File(args[1]);
        }

        boolean overwrite = false;
        if (args.length > 2) {
            overwrite = Boolean.parseBoolean(args[2]);
        }

        Path photoDBPath
                = outputPath.resolve("photo.db");

        try (PhotoDB photoDB = new PhotoDB(outputPath, photoDBPath)) {

            PhotoVisitor visitor = new PhotoVisitor(outputPath, photoDB.photoByOrigPath, overwrite);

            //
            // Walk all filesystems, looking for JPG and movie files, then copy them.
            for (File root : roots) {
                logger.info(String.format("Walking root %s", root));
                Files.walkFileTree(root.toPath(), visitor);
                logger.info(String.format("Finished walking root %s", root));
                logger.info(visitor.getReport());
            }
        }

    }

}
