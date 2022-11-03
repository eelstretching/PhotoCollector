package com.eelstretching.photo;

import com.eelstretching.photo.persist.PhotoInfo;
import com.oracle.labs.mlrg.olcut.command.Command;
import com.oracle.labs.mlrg.olcut.command.CommandGroup;
import com.oracle.labs.mlrg.olcut.command.CommandInterpreter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class FileCommands implements CommandGroup {

    private static final Logger logger = Logger.getLogger(FileCommands.class.getName());

    private PhotoDB photoDB;

    private ListCommands listCommands;

    public FileCommands(PhotoDB photoDB, ListCommands listCommands) {
        this.photoDB = photoDB;
        this.listCommands = listCommands;
    }

    @Override
    public String getName() {
        return "File Commands";
    }

    @Override
    public String getDescription() {
        return "Commands that operate on files.";
    }

    @Command(usage = "Collect photos on a given path")
    public String collect(CommandInterpreter ci, boolean overwrite, String... roots) {
        PhotoVisitor visitor = new PhotoVisitor(photoDB.getDbPath(), photoDB.photoByOrigPath, overwrite);

        //
        // Walk all filesystems, looking for JPG and movie files, then copy them.
        for (String root : roots) {
            ci.out.format("Walking root %s", root);
            try {
                Files.walkFileTree(Paths.get(root), visitor);
            } catch (IOException ex) {
                logger.warning(String.format("Error walking root %s", root));
            }
            ci.out.format("Finished walking root %s", root);
            logger.info(visitor.getReport());
        }

        return visitor.getReport();

    }

    @Command(usage = "Move files from the given list into a corresponding directory somewhere else and delete them from the DB")
    public String moveFiles(CommandInterpreter ci, String listName, File outputDir) {
        List<PhotoInfo> infos = listCommands.getList(listName);
        if (infos == null) {
            return "No such list " + listName;
        }

        Path outputPath = outputDir.toPath();
        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException ex) {
                return "Unable to create output directory " + outputDir;
            }
        }

        int nm = 0;
        int nerr = 0;
        for (PhotoInfo info : infos) {
            Path origPath = Paths.get(info.getOrigPath());
            Path relativePath = origPath.subpath(2, origPath.getNameCount());
            try {
                Files.move(Paths.get(info.getOrigPath()),
                        outputPath.resolve(relativePath),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                photoDB.photoByOrigPath.delete(info.getOrigPath());
                nm++;
            } catch (IOException ex) {
                logger.warning(String.format("Unable to move %s", origPath));
                nerr++;
            }
        }

        return String.format("Moved %,d files, had %,d errors", nm, nerr);
    }

    @Command(usage = "Delete files from a given list and remove them from the DB")
    public String delete(CommandInterpreter ci, String listName) {
        List<PhotoInfo> infos = listCommands.getList(listName);
        if (infos == null) {
            return "No such list " + listName;
        }

        int nd = 0;
        int nerr = 0;
        for (PhotoInfo info : infos) {
            try {
                Files.delete(Paths.get(info.getOrigPath()));
                photoDB.photoByOrigPath.delete(info.getOrigPath());
                nd++;
            } catch (IOException ex) {
                logger.warning(String.format("Unable to delete %s", info.getOrigPath()));
                nerr++;
            }
        }

        return String.format("Deleted %,d files, had %,d errors", nd, nerr);

    }

}
