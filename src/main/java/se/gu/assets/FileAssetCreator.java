package se.gu.assets;

import se.gu.main.ProjectData;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.Callable;

public class FileAssetCreator implements Callable<Asset>, Serializable {
    private static final long serialVersionUID = -5865568394504705665L;
    private ProjectData projectData;
    private File file;

    public FileAssetCreator(ProjectData projectData, File file) {
        this.projectData = projectData;
        this.file = file;
    }

    @Override
    public Asset call() throws Exception {
        return null;
    }




}
