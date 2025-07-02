package se.gu.assets;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.StringUtils;
import se.gu.data.DataController;
import se.gu.main.Configuration;

import java.sql.SQLException;
import java.util.List;

public class FeatureCleaner {
    DataController dataController;

    public FeatureCleaner(Configuration configuration) throws SQLException, ClassNotFoundException {
        this.configuration = configuration;
        dataController = new DataController(configuration);
    }

    Configuration configuration;

    public void cleanFeatures() throws SQLException {
        //first call procedure for cleaning features
        dataController.cleanFeatures();
        List<String> allFeatures = dataController.getAllFeatures();
        //clean
        int size = allFeatures.size();
        try(ProgressBar pb = new ProgressBar("Cleaning features",size)) {
            for (String oldFeature : allFeatures) {
                //replace &&
                pb.step();
                pb.setExtraMessage(oldFeature);
                String newFeature =
                        oldFeature.replace("&&", "_AND_")
                                .replace(" && ", "_AND_").replace("||", "_OR_").replace(" || ", "_OR_")
                                .replace("\\n","")
                                .replace("\\t", "").replaceAll("\\s", "_")//replace whitesace with
                                .replace("\\", "").replace("|", "")
                                .replaceAll("\\*\\s*\\S+", "").trim();
                //delete if feature is empty
                if (oldFeature.equalsIgnoreCase(newFeature)) {
                    continue;
                } else if (StringUtils.isBlank(newFeature)) {
                    dataController.deleteMappings(oldFeature);
                } else {
                    dataController.updateFeatureMapping(oldFeature, newFeature);
                }

            }
        }

    }
}
