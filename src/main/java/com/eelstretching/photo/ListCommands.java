package com.eelstretching.photo;

import com.eelstretching.photo.persist.PhotoInfo;
import com.oracle.labs.mlrg.olcut.command.Command;
import com.oracle.labs.mlrg.olcut.command.CommandGroup;
import com.oracle.labs.mlrg.olcut.command.CommandInterpreter;
import com.sleepycat.persist.EntityCursor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Commands for photos that generate lists of things.
 */
public class ListCommands implements CommandGroup {

    private static final Logger logger = Logger.getLogger(ListCommands.class.getName());

    Map<String, List<PhotoInfo>> lists = new HashMap<>();

    PhotoDB photoDB;

    public ListCommands(PhotoDB photoDB) {
        this.photoDB = photoDB;
    }

    public List<PhotoInfo> getList(String name) {
        return lists.get(name);
    }

    @Override
    public String getName() {
        return "List Commands";
    }

    @Override
    public String getDescription() {
        return "Commands that will generate lists of things.";
    }

    @Command(usage = "List the original paths that match a given regex")
    public String matchPath(CommandInterpreter ci, String name, String... ps) {

        List<Pattern> patterns = new ArrayList<>();
        for (String p : ps) {
            try {
                patterns.add(Pattern.compile(p, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException ex) {
                logger.warning(String.format("Can't compile pattern: %s", p));
            }
        }

        if (patterns.isEmpty()) {
            return "No patterns";
        }

        List<PhotoInfo> infoList = new ArrayList<>();
        int n = 0;
        int m = 0;
        try (EntityCursor<String> kc = photoDB.photoByOrigPath.keys()) {
            keyLoop:
            for (String origPath : kc) {
                for (Pattern pat : patterns) {
                    if (pat.matcher(origPath).matches()) {
                        infoList.add(photoDB.photoByOrigPath.get(origPath));
                        continue keyLoop;
                    }
                }
            }
        }

        lists.put(name, infoList);

        return String.format("%,d matching paths for %s", infoList.size(), name);
    }

    @Command(usage = "List the original paths that match a given regex")
    public String matchTag(CommandInterpreter ci, String listName, String tagName, String... ps) {

        List<Pattern> patterns = new ArrayList<>();
        for (String p : ps) {
            try {
                patterns.add(Pattern.compile(p, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException ex) {
                logger.warning(String.format("Can't compile pattern: %s", p));
            }
        }

        if (patterns.isEmpty()) {
            return "No patterns";
        }

        List<PhotoInfo> infoList = new ArrayList<>();
        try (EntityCursor<PhotoInfo> infos = photoDB.photoByOrigPath.entities()) {
            keyLoop:
            for (PhotoInfo info : infos) {
                String value = info.getTagNames().get(tagName);
                if (value == null) {
                    continue;
                }
                for (Pattern pat : patterns) {
                    if (pat.matcher(value).matches()) {
                        infoList.add(info);
                        continue keyLoop;
                    }
                }

            }
        }

        lists.put(listName, infoList);

        return String.format("%,d matching paths for %s", infoList.size(), listName);
    }

    @Command(usage = "Merge two lists into a third")
    public String merge(CommandInterpreter ci, String l1, String l2, String ml) {
        List<PhotoInfo> p1 = lists.get(l1);
        if (p1 == null) {
            return "Unknown list " + l1;
        }
        List<PhotoInfo> p2 = lists.get(l2);
        if (p2 == null) {
            return "Unknown list " + l2;
        }

        SortedSet<PhotoInfo> s = new TreeSet<>();
        s.addAll(p1);
        s.addAll(p2);

        List<PhotoInfo> merge = new ArrayList<>(s);
        lists.put(ml, merge);

        return String.format("Merge resulted in %,d info", merge.size());
    }

    @Command(usage = "Intersect two lists")
    public String intersect(CommandInterpreter ci, String l1, String l2, String ml) {
        List<PhotoInfo> p1 = lists.get(l1);
        if (p1 == null) {
            return "Unknown list " + l1;
        }
        SortedSet<PhotoInfo> s1 = new TreeSet<>(p1);

        List<PhotoInfo> p2 = lists.get(l2);
        if (p2 == null) {
            return "Unknown list " + l2;
        }
        SortedSet<PhotoInfo> s2 = new TreeSet<>(p2);

        SortedSet<PhotoInfo> s3 = new TreeSet<>();

        for (PhotoInfo info : s1) {
            if (s2.contains(info)) {
                s3.add(info);
            }
        }

        List<PhotoInfo> intersect = new ArrayList<>(s3);
        lists.put(ml, intersect);

        return String.format("Intersected resulted in %,d info", intersect.size());
    }

    @Command(usage = "Show contents of the list")
    public String show(CommandInterpreter ci, String listName) {
        List<PhotoInfo> infos = getList(listName);
        if (infos == null) {
            return "No such list " + listName;
        }

        for (PhotoInfo info : infos) {
            ci.out.println(info.getOrigPath());
        }
        return "";
    }

    @Command(usage = "Write list to a file")
    public String write(CommandInterpreter ci, String listName, File outputFile) {
        List<PhotoInfo> infos = getList(listName);
        if (infos == null) {
            return "No such list " + listName;
        }

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            for (PhotoInfo info : infos) {
                pw.println(info.getOrigPath());
            }
        } catch (IOException ex) {
            logger.warning(String.format("Error writing output file %s", outputFile));
            return "Error writing file";
        }
        return String.format("Wrote %,d", infos.size());
    }

    @Command(usage = "Drop a list")
    public String drop(CommandInterpreter ci, String listName) {
        List<PhotoInfo> infos = lists.remove(listName);
        if (infos == null) {
            return "No such list " + listName;
        } 
        return "Removed list " + listName;
    }
    
    @Command(usage = "List lists")
    public String list(CommandInterpreter ci) {
        Set<String> sorted = new TreeSet<>(lists.keySet());
        for(String list : sorted) {
            ci.out.format("%s: %,d", list, lists.get(list).size());
        }
        return String.format("%d lists saved", lists.size());
    }
    /**
     * Reusable method to get a completor for engine names
     *
     * @return
     */
    public Completer[] writeCompletors() {
        return new Completer[]{
            new StringsCompleter(lists.keySet()),
            new FileNameCompleter()
        };
    }

    /**
     * Reusable method to get a completor for file names
     *
     * @return
     */
    public Completer[] listCompletor() {
        return new Completer[]{
            new StringsCompleter(lists.keySet())
        };

    }
}
