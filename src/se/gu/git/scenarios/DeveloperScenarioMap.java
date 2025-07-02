package se.gu.git.scenarios;

public class DeveloperScenarioMap {
    DeveloperScenario developerScenario;
    String projectRepository;
    String commit;
    String assetName;

    public DeveloperScenarioMap(DeveloperScenario developerScenario, String projectRepository, String commit, String assetName) {
        this.developerScenario = developerScenario;
        this.projectRepository = projectRepository;
        this.commit = commit;
        this.assetName = assetName;
    }

    public DeveloperScenario getDeveloperScenario() {
        return developerScenario;
    }

    public void setDeveloperScenario(DeveloperScenario developerScenario) {
        this.developerScenario = developerScenario;
    }

    public String getProjectRepository() {
        return projectRepository;
    }

    public void setProjectRepository(String projectRepository) {
        this.projectRepository = projectRepository;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }
}
