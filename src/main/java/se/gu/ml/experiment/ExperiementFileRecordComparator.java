package se.gu.ml.experiment;

import java.io.Serializable;
import java.util.Comparator;

public class ExperiementFileRecordComparator implements Comparator<ExperiementFileRecord>, Serializable {
    private static final long serialVersionUID = 2955174389510528790L;

    public int compare(ExperiementFileRecord x, ExperiementFileRecord y) {
        return   Long.compare(x.getCommitNumber(), y.getCommitNumber());
    }
}
