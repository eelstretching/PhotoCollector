package com.eelstretching.photo;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.avi.AviDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.eelstretching.photo.persist.PhotoInfo;
import com.sleepycat.persist.PrimaryIndex;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A visitor for photos.
 */
public class PhotoVisitor implements FileVisitor<Path> {

    private static final Logger logger = Logger.getLogger(PhotoVisitor.class.getName());

    private final Set<String> extensions = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "heic", "mov", "avi")
    );

    private int[] exifTags = new int[]{
        ExifIFD0Directory.TAG_DATETIME,
        ExifIFD0Directory.TAG_DATETIME_DIGITIZED,
        ExifIFD0Directory.TAG_DATETIME_ORIGINAL};

    private Path outputPath;

    private PrimaryIndex<String, PhotoInfo> photoByOrigPath;

    private MessageDigest md5 = null;

    private byte[] buffer = new byte[10 * 1024 * 1024];

    boolean overwrite;

    private int dirsVisited;

    private int filesVisited;

    private int filesCopied;

    private long bytesCopied;

    public PhotoVisitor(Path outputPath, PrimaryIndex<String, PhotoInfo> photoByOrigPath, boolean overwrite) {
        this.outputPath = outputPath;
        this.photoByOrigPath = photoByOrigPath;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, String.format("No MD5? Hmmm"), ex);
        }
    }

    private String getExtension(Path file) {
        String fn = file.getFileName().toString();
        int ind = fn.lastIndexOf('.');
        if (ind >= 0 && ind < fn.length() - 1) {
            return fn.substring(ind + 1);
        }
        return "";
    }

    private byte[] copyAndMD5(Path origPath, Path finalPath) throws IOException {
        md5.reset();
        readLoop:
        try (InputStream is = Files.newInputStream(origPath)) {
            try (OutputStream os = Files.newOutputStream(finalPath)) {
                DigestInputStream dis = new DigestInputStream(is, md5);
                int n = dis.read(buffer);
                if (n < 0) {
                    break readLoop;
                }
                os.write(buffer, 0, n);
            }
        }
        return md5.digest();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        dirsVisited++;
        //
        // Let's not crawl the directory where we're copying stuff!
        if (dir.equals(outputPath)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        filesVisited++;
        if (!overwrite && photoByOrigPath.contains(file.toString())) {
            return FileVisitResult.CONTINUE;
        }
        if (extensions.contains(getExtension(file.getFileName()).toLowerCase())) {
            try {
                Date photoDate = null;
                Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
                Directory dataDir = null;
                dirLoop:
                for (Directory dir : metadata.getDirectories()) {
                    if (dir instanceof ExifIFD0Directory) {
                        for (int exifTag : exifTags) {
                            photoDate = dir.getDate(exifTag);
                            if (photoDate != null) {
                                dataDir = dir;
                                break dirLoop;
                            }
                        }
                        break;
                    } else if (dir instanceof QuickTimeDirectory) {
                        photoDate = dir.getDate(QuickTimeDirectory.TAG_CREATION_TIME);
                        dataDir = dir;
                        break;
                    } else if (dir instanceof AviDirectory) {
                        photoDate = dir.getDate(AviDirectory.TAG_DATETIME_ORIGINAL);
                        dataDir = dir;
                        break;
                    }
                }
                if (photoDate != null) {
                    Path fileDir = outputPath
                            .resolve(String.format("%tY", photoDate))
                            .resolve(String.format("%tm", photoDate))
                            .resolve(String.format("%td", photoDate));
                    if (!Files.exists(fileDir)) {
                        Files.createDirectories(fileDir);
                    }
                    Path filePath = fileDir.resolve(file.getFileName());
                    if (Files.exists(filePath)) {
                        String namePart = file.getFileName().toString();
                        String ext = "";
                        int ind = namePart.indexOf('.');
                        if (ind >= 0) {
                            if (ind < namePart.length() - 1) {
                                ext = namePart.substring(ind + 1);
                                namePart = namePart.substring(0, ind);
                            }
                        }
                        //
                        // Keep adding numbers until we don't have a duplicate any more.
                        int n = 1;
                        while (Files.exists(filePath)) {
                            filePath = fileDir.resolve(String.format("%s-%04d.%s", namePart, n++, ext));
                        }
                    }
                    //
                    // Copy.
                    byte[] hash = copyAndMD5(file, filePath);
                    filesCopied++;
                    bytesCopied += file.toFile().length();
                    PhotoInfo info = new PhotoInfo(file, filePath, dataDir, hash);
                    photoByOrigPath.put(info);
                    if (filesCopied % 100 == 0) {
                        logger.info(getReport());
                    }
                }
            } catch (ImageProcessingException ex) {
                logger.warning(String.format("Error getting metadata for %s", file));
            } catch (Exception ex) {
                logger.warning(String.format("Unknown exception processing file %s", file));
            }
        }
        return FileVisitResult.CONTINUE;
    }

    public String getReport() {
        return String.format("Visited %,d directories and %,d files. Copied %,d files, %,d bytes", dirsVisited, filesVisited, filesCopied, bytesCopied);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        logger.warning(String.format("Failed to read %s", file));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

}
