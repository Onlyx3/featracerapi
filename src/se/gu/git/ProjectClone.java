package se.gu.git;

import java.io.Serializable;

public class ProjectClone  implements Serializable {

    private static final long serialVersionUID = 168421906287754124L;
    private String sourceCommit,targetCommit;
    private ClaferProject sourceProject,targetProject;

    public ProjectClone(String sourceCommit, String targetCommit, ClaferProject sourceProject, ClaferProject targetProject) {
        this.sourceCommit = sourceCommit;
        this.targetCommit = targetCommit;
        this.sourceProject = sourceProject;
        this.targetProject = targetProject;
    }

    public String getSourceCommit() {
        return sourceCommit;
    }

    public String getTargetCommit() {
        return targetCommit;
    }
}
