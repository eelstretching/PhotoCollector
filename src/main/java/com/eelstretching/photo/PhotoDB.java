package com.eelstretching.photo;

import com.eelstretching.photo.persist.PhotoInfo;
import com.oracle.labs.mlrg.olcut.command.CommandInterpreter;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PhotoDB implements Closeable {

    protected Path dbPath;
    
    protected Path photoPath;
    
    protected Environment env;

    protected EntityStore store;

    protected PrimaryIndex<String, PhotoInfo> photoByOrigPath;

    protected SecondaryIndex<String, String, PhotoInfo> photoByFinalPath;

    public PhotoDB(Path dbPath) {
        this.dbPath = dbPath;
        photoPath = dbPath.subpath(0, dbPath.getNameCount() - 1);
        init();
    }
    
    public PhotoDB(Path photoPath, Path dbPath) {
        this.photoPath = photoPath;
        this.dbPath = dbPath;
        init();
    }
    
    private void init() {
        
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);

        env = new Environment(dbPath.toFile(), envConfig);

        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);

        store = new EntityStore(env, "PhotoInfoStore", storeConfig);
        
        photoByOrigPath
                = store.getPrimaryIndex(String.class, PhotoInfo.class);
        photoByFinalPath
                = store.getSecondaryIndex(photoByOrigPath, String.class, "finalPath");

    }
    
    public Path getPhotoPath() {
        return photoPath;
    }
    
    public Path getDbPath() {
        return dbPath;
    }

    @Override
    public void close() throws IOException {
        store.close();
        env.close();
    }

    public static void main(String[] args) throws IOException {

        for (Handler h : Logger.getLogger("").getHandlers()) {
            h.setFormatter(new EelsLogFormatter());
            h.setLevel(Level.ALL);
        }

        Path outputPath = Paths.get(args[0]);

        Path photoDBPath
                = outputPath.resolve("photo.db");

        try (PhotoDB photoDB = new PhotoDB(photoDBPath)) {

            ListCommands listCommands = new ListCommands(photoDB);
            FileCommands fileCommands = new FileCommands(photoDB, listCommands);
            SimpleCommands simpleCommands = new SimpleCommands(photoDB);
            CommandInterpreter shell = new CommandInterpreter();
            shell.setPrompt("photos$ ");
            shell.add(simpleCommands);
            shell.add(listCommands);
            shell.add(fileCommands);
            shell.run();
        }
    }

}
