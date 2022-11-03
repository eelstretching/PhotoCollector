package com.eelstretching.photo;

import com.sleepycat.persist.EntityCursor;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Matches a number of patterns against the original paths in the DB.
 */
public class MatchPath {
    
    private static final Logger logger = Logger.getLogger(MatchPath.class.getName());

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
        
        List<Pattern> patterns = new ArrayList<>();
        for(String pats : Arrays.copyOfRange(args, 1, args.length)) {
            try {
            patterns.add(Pattern.compile(pats, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException ex) {
                logger.warning(String.format("Can't compile pattern: %s", pats));
            }
        }

        try (PhotoDB photoDB = new PhotoDB(photoDBPath)) {
            logger.info(String.format("Processing %,d photos", photoDB.photoByOrigPath.map().size()));
            int n = 0;
            try(EntityCursor<String> kc = photoDB.photoByOrigPath.keys()) {
                keyLoop: for(String origPath : kc) {
                    for(Pattern pat : patterns) {
                        if(pat.matcher(origPath).matches()) {
                            System.out.println(photoDB.photoByOrigPath.get(origPath));
                            continue keyLoop;
                        }
                    }
                    n++;
                    if (n % 10000 == 0) {
                        logger.info(String.format("Processed %,d keys", n));
                    }
                }
            }
        }
    }
    
}
