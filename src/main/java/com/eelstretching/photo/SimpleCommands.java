package com.eelstretching.photo;

import com.oracle.labs.mlrg.olcut.command.Command;
import com.oracle.labs.mlrg.olcut.command.CommandGroup;
import com.oracle.labs.mlrg.olcut.command.CommandInterpreter;

/**
 *
 */
public class SimpleCommands implements CommandGroup {

    private PhotoDB photoDB;

    public SimpleCommands(PhotoDB photoDB) {
        this.photoDB = photoDB;
    }

    @Override
    public String getName() {
        return "Simple Commands";
    }

    @Override
    public String getDescription() {
        return "Basic Command for the DB";
    }

    @Command(usage = "Print the size of the database")
    public String size(CommandInterpreter ci) {
        return String.format("%,d photos", photoDB.photoByOrigPath.map().size());
    }


}
