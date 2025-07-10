package se.gu.ml.experiment;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

public class FileComparator implements Comparator<File>, Serializable {
    public int compare(File x, File y) {
      return   Long.compare(x.lastModified(), y.lastModified());
    }
}
