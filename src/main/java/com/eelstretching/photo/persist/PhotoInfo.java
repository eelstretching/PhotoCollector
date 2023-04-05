package com.eelstretching.photo.persist;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Information about a single photo
 */
@Entity(version = 1)
public class PhotoInfo {
    
    @PrimaryKey
    protected String origPath;
    
    @SecondaryKey(relate = Relationship.ONE_TO_ONE)
    protected String finalPath;
    
    protected String directoryType;
    
    protected Map<String,String> tagNames = new HashMap<>();
    
    protected byte[] md5;
    
    public PhotoInfo() {
        
    }
    
    public PhotoInfo(Path origPath, Path finalPath, Directory dir, byte[] md5) {
        this.origPath = origPath.toString();
        this.finalPath = finalPath.toString();
        for(Tag tag : dir.getTags()) {
            tagNames.put(tag.getTagName(), tag.getDescription());
        }
        directoryType = dir.getClass().toString();
        this.md5 = md5;
    }

    public String getOrigPath() {
        return origPath;
    }

    public void setOrigPath(String origPath) {
        this.origPath = origPath;
    }

    public String getFinalPath() {
        return finalPath;
    }

    public void setFinalPath(String finalPath) {
        this.finalPath = finalPath;
    }

    public String getDirectoryType() {
        return directoryType;
    }

    public void setDirectoryType(String directoryType) {
        this.directoryType = directoryType;
    }

    public Map<String, String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(Map<String, String> tagNames) {
        this.tagNames = tagNames;
    }

    public byte[] getMd5() {
        return md5;
    }

    public void setMd5(byte[] md5) {
        this.md5 = md5;
    }

    @Override
    public String toString() {
        return "PhotoInfo{" + "origPath=" + origPath + ", finalPath=" + finalPath + ", directoryType=" + directoryType + ", tagNames=" + tagNames + '}';
    }

    @Override
    public int hashCode() {
        return origPath.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PhotoInfo other = (PhotoInfo) obj;
        if (!Objects.equals(this.origPath, other.origPath)) {
            return false;
        }
        return true;
    }
    
    
}
