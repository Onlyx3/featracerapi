package se.gu.git;

import java.io.Serializable;
import java.util.Comparator;

public class CommitComparator  implements Comparator<Commit>, Serializable {
    private static final long serialVersionUID = -3096914675691740523L;

    public int compare(Commit x, Commit y) {
        return   Long.compare(x.getCommitDateAsLong(), y.getCommitDateAsLong());
    }
}
