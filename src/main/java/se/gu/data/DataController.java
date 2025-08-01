package se.gu.data;

import se.gu.assets.*;
import se.gu.git.Commit;
import se.gu.main.Configuration;
import se.gu.metrics.AssetMetricsDB;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataController {


    private Connection connection;
    private static final String connectionURL = "jdbc:sqlite:featracer.db";

    public DataController(Configuration configuration) throws SQLException, ClassNotFoundException {
       /* Class.forName("com.mysql.cj.jdbc.Driver");
        connectionURL = configuration.getDataBaseConnectionString().split(",");
        connection = getConnection(); */
        connection = DriverManager.getConnection(connectionURL);
        if(connection != null && isDatabaseNew()) initSQLite();

        }


    private void initSQLite() throws SQLException {

        String schema; // Read the schema.sql from resources

        try {
           InputStream input = DataController.class.getClassLoader().getResourceAsStream("schema.sql");
            //InputStream input = new FileInputStream("src/main/resources/schema.sql");
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage());
        }

        // Split and execute the statements from schema.sql
        Statement statement = connection.createStatement();
        for (String c : schema.split(";")) {
            c = c.replace("\n", "");
            c = c.trim();
            if(!c.isEmpty()) statement.executeUpdate(c);
        }

    }

    // Return false if database has not been initialized
    private boolean isDatabaseNew() throws SQLException {
        String check = "SELECT name FROM sqlite_schema WHERE type='table'";
        Connection connection = getConnection();
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(check);
            return !resultSet.next();
        }
    }

    //==========NEW WINE relying on DB=============================
    public Connection getConnection() throws SQLException {
        return connection;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    //add asset
    public boolean assertInsert(String assetFullName, String assetName, String parent, String commitHash, int commitIndex, String developer, String assetType, int startingLine, int endLine, int lineNumber, String project, String changeType, int nloc) throws SQLException {

        String sql = "INSERT INTO assets (" +
                "assetFullName, assetName, parent, commitHash, commitIndex, developer, " +
                "assetType, startingLine, endingLine, lineNumber, project, changeType, nloc" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(sql);

        statement.setString(1, assetFullName);
        statement.setString(2, assetName);
        statement.setString(3, parent);
        statement.setString(4, commitHash);
        statement.setInt(5, commitIndex);
        statement.setString(6, developer);
        statement.setString(7, assetType);
        statement.setInt(8, startingLine);
        statement.setInt(9, endLine);
        statement.setInt(10, lineNumber);
        statement.setString(11, project);
        statement.setString(12, changeType);
        statement.setInt(13, nloc);

        return statement.executeUpdate() > 0;
    }

    public boolean assertMappingInsert(String assetFullName, String assetType, String parent, String feature, String project, String annotationType, String commitHash, int commitIndex, String developer) throws SQLException {
        // Check if already in db
        String query = "SELECT 1 FROM assetmapping WHERE assetfullname = ? AND featurename = ? AND annotationType = ? AND project = ?";

        PreparedStatement queryStatement = connection.prepareStatement(query);
        queryStatement.setString(1, assetFullName);
        queryStatement.setString(2, feature);
        queryStatement.setString(3, annotationType);
        queryStatement.setString(4, project);
        ResultSet resultSet = queryStatement.executeQuery();
        if(resultSet.next()) return false;

        // insert part
        String sql = "INSERT INTO assetmapping (assetfullname, assetType, parent, featurename, project, annotationType, commitHash, commitIndex, developer) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, assetFullName);
        statement.setString(2, assetType);
        statement.setString(3, parent);
        statement.setString(4, feature);
        statement.setString(5, project);
        statement.setString(6, annotationType);
        statement.setString(7, commitHash);
        statement.setInt(8, commitIndex);
        statement.setString(9, developer);

        return statement.executeUpdate() > 0;

    }

    public boolean datasetInsert(int commitIndex, String commitHash, String project, String assetType, String trainingFile, String testFile, boolean isMappedOnly, String trainingXMLFile, String testXMLFile, String testCSVFile, int testCommitIndex, String testCommitHash) throws SQLException {

        String sql = "INSERT INTO dataset (commitIdex, commitHash, project, assetType, trainingFile, testFile, isMappedOnly, trainingXMLFile, testXMLFile, testCSVFile, testCommitIndex, testCommitHash)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement statement = connection.prepareStatement(sql);

        statement.setInt(1, commitIndex);
        statement.setString(2, commitHash);
        statement.setString(3, project);
        statement.setString(4, assetType);
        statement.setString(5, trainingFile);
        statement.setString(6, testFile);
        statement.setBoolean(7, isMappedOnly);
        statement.setString(8, trainingXMLFile);
        statement.setString(9, testXMLFile);
        statement.setString(10, testCSVFile);
        statement.setInt(11, testCommitIndex);
        statement.setString(12, testCommitHash);

        return statement.executeUpdate() > 0;
    }

    public boolean assetMetricsInsert(AssetMetricsDB m) throws SQLException {
        //String query = "{CALL assetmetrics_insert (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";

        //CallableStatement statement = connection.prepareCall(query);
        String sql = "INSERT INTO assetmetrics (assetFullName, assetparent, commitHash, commitIndex, project, ismapped, assetType, csdev, ddev, comm, dcont, hdcont, ccc, accc, nloc, dnfma, nfma, nff)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement statement = connection.prepareStatement(sql);

        statement.setString(1, m.getAsset());
        statement.setString(2, m.getParent());
        statement.setString(3, m.getCommitHash());
        statement.setInt(4, m.getCommitIndex());
        statement.setString(5, m.getProject());
        statement.setBoolean(6, m.isMapped());
        statement.setString(7, m.getAssetType());
        statement.setDouble(8, m.getCsvDev());
        statement.setDouble(9, m.getDdev());
        statement.setDouble(10, m.getComm());
        statement.setDouble(11, m.getDcont());
        statement.setDouble(12, m.getHdcont());
        statement.setDouble(13, m.getCcc());
        statement.setDouble(14, m.getAccc());
        statement.setDouble(15, m.getNloc());
        statement.setDouble(16, m.getDnfma());
        statement.setDouble(17, m.getNfma());
        statement.setDouble(18, m.getNff());


        return statement.executeUpdate() > 0;

    }
    @Deprecated
    public ResultSet getCommitMetricsResultsMappigAll() throws SQLException {
        String query = "{CALL metrics_resultsmapping_all}";
        CallableStatement statement = connection.prepareCall(query);
        return statement.executeQuery();
    }
    @Deprecated
    public ResultSet getCommitMetricsResultsMappigByLevel(String level) throws SQLException {
        String query = "{CALL metrics_resultsmapping_bylevel (?)}";
        CallableStatement statement = connection.prepareCall(query);
        statement.setString(1, level);
        return statement.executeQuery();
    }
    @Deprecated
    public ResultSet getCommitMetricsResultsMappigByProjectLevel(String project, String level) throws SQLException {
        String query = "{CALL metrics_resultsmapping_byprojectlevel (?,?)}";
        CallableStatement statement = connection.prepareCall(query);
        statement.setString(1, project);
        statement.setString(2, level);
        return statement.executeQuery();
    }
    @Deprecated
    public boolean resultsSummaryInsert(
            String project,
            String commitHash,
            int commitIndex,
            String codelevel,
            int metrics,
            String classifier,
            String measure,
            double measureValue
    ) throws SQLException {
        String query = "{CALL results_insert (?,?,?,?,?,?,?,?) }";

        CallableStatement statement = connection.prepareCall(query);
        statement.setString(1, project);
        statement.setString(2, commitHash);
        statement.setInt(3, commitIndex);
        statement.setString(4, codelevel);
        statement.setInt(5, metrics);
        statement.setString(6, classifier);
        statement.setString(7, measure);
        statement.setDouble(8, measureValue);


        return statement.executeUpdate() > 0;
    }
    @Deprecated
    public boolean commitMetricsInsertFromDB(
            double tAA,
            double tUA,
            double rAA,
            double rUA,
            double tAFo,
            double tAFi,
            double tAFra,
            double tALoc,
            double rAFo,
            double rAFi,
            double rAFra,
            double rALoc,
            String project,
            String commitHash
    ) throws SQLException {
        String query = "{CALL commitmetrics_insertfromdb (?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";

        CallableStatement statement = connection.prepareCall(query);
        statement.setDouble(1, tAA);
        statement.setDouble(2, tUA);
        statement.setDouble(3, rAA);
        statement.setDouble(4, rUA);
        statement.setDouble(5, tAFo);
        statement.setDouble(6, tAFi);
        statement.setDouble(7, tAFra);
        statement.setDouble(8, tALoc);
        statement.setDouble(9, rAFo);
        statement.setDouble(10, rAFi);
        statement.setDouble(11, rAFra);
        statement.setDouble(12, rALoc);
        statement.setString(13, project);
        statement.setString(14, commitHash);


        return statement.executeUpdate() > 0;
    }
    @Deprecated
    public boolean commitMetricsInsertFromCode(
            double tLA,
            double tLR,
            double aLA,
            double aLR,
            double mLA,
            double mLR,
            double tFC,
            double tFA,
            double tFM,
            double tFD,
            double tFRe,
            double tH,
            double aH,
            double mHS,
            double aHS,
            double tCh,
            double aCh,
            String project,
            String commitHash
    ) throws SQLException {
        String query = "{CALL commitmetrics_insertfromcode (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";

        CallableStatement statement = connection.prepareCall(query);
        statement.setDouble(1, tLA);
        statement.setDouble(2, tLR);
        statement.setDouble(3, aLA);
        statement.setDouble(4, aLR);
        statement.setDouble(5, mLA);
        statement.setDouble(6, mLR);
        statement.setDouble(7, tFC);
        statement.setDouble(8, tFA);
        statement.setDouble(9, tFM);
        statement.setDouble(10, tFD);
        statement.setDouble(11, tFRe);
        statement.setDouble(12, tH);
        statement.setDouble(13, aH);
        statement.setDouble(14, mHS);
        statement.setDouble(15, aHS);
        statement.setDouble(16, tCh);
        statement.setDouble(17, aCh);
        statement.setString(18, project);
        statement.setString(19, commitHash);


        return statement.executeUpdate() > 0;
    }
    @Deprecated
    public boolean psresultsInsert(
            int commitIndex,
            String commitHash,
            String dataSet,
            String classifier,
            double totalInstances,
            double precisionM,
            double recall,
            double fscore,
            String project,
            String codelevel,
            int metrics
    ) throws SQLException {
        String query = "{CALL psresults_insert (?,?,?,?,?,?,?,?,?,?,?) }";

        CallableStatement statement = connection.prepareCall(query);
        statement.setInt(1, commitIndex);
        statement.setString(2, commitHash);
        statement.setString(3, dataSet);
        statement.setString(4, classifier);
        statement.setDouble(5, totalInstances);
        statement.setDouble(6, precisionM);
        statement.setDouble(7, recall);
        statement.setDouble(8, fscore);
        statement.setString(9, project);
        statement.setString(10, codelevel);
        statement.setInt(11, metrics);
        return statement.executeUpdate() > 0;
    }

    public boolean renameAssetAndChildren(String oldName, String newName) throws SQLException {

        String sql = "UPDATE assets SET assetFullName = REPLACE(assetFullName, ?,?) WHERE assetFullName = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, oldName);
        statement.setString(2, newName);
        statement.setString(3, oldName);

        return statement.executeUpdate() > 0;
    }

    public boolean deleteMetricsForCommit(String commit, String project) throws SQLException {

        String sql = "DELETE FROM assetmetrics WHERE project = ? AND commitHash = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, project);
        statement.setString(2, commit);

        return statement.executeUpdate() > 0;

    }
    @Deprecated
    public boolean deleteResults(String project, String codeLevel, int metrics) throws SQLException {
        String query = "{CALL results_delete (?,?,?) }";

        CallableStatement statement = connection.prepareCall(query);
        statement.setString(1, project);
        statement.setString(2, codeLevel);
        statement.setInt(3, metrics);
        return statement.executeUpdate() > 0;

    }
    @Deprecated
    public boolean deleteAllResults() throws SQLException {
        String query = "{CALL results_delete_all }";

        CallableStatement statement = connection.prepareCall(query);

        return statement.executeUpdate() > 0;

    }
    @Deprecated
    public boolean deleteAllPSResults() throws SQLException {
        String query = "{CALL psresults_deleteAll }";

        CallableStatement statement = connection.prepareCall(query);

        return statement.executeUpdate() > 0;

    }

    public boolean deleteAssetsForCommit(String commit, String project) throws SQLException {

        String sql1 = "DELETE FROM assets WHERE project = ? AND commitHash = ?";
        String sql2 = "DELETE FROM assetmapping WHERE project = ? AND commitHash = ?";

        PreparedStatement statement1 = connection.prepareStatement(sql1);
        PreparedStatement statement2 = connection.prepareStatement(sql2);

        statement1.setString(1, project);
        statement1.setString(2, commit);
        int res1 = statement1.executeUpdate();

        statement2.setString(1, project);
        statement2.setString(2, commit);
        int res2 = statement2.executeUpdate();

        return res1 >0 || res2 > 0;
    }

    public boolean deleteDatasetsForCommit(String commit, String project) throws SQLException {

        String sql = "DELETE FROM datasets WHERE commitHash = ? AND project = ?";

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, commit);
        statement.setString(2, project);
        return statement.executeUpdate() > 0;

    }

    public boolean updateCommitIndex(String commitHash, int commitIndex, String project) throws SQLException {

        String sql1 = "UPDATE assets SET commitIndex = ? WHERE commitHash = ? AND project = ?";
        String sql2 = "UPDATE assetmapping SET commitIndex = ? WHERE commitHash = ? AND project = ?";

        PreparedStatement statement1 = connection.prepareStatement(sql1);
        PreparedStatement statement2 = connection.prepareStatement(sql2);

        statement1.setInt(1, commitIndex);
        statement1.setString(2, commitHash);
        statement1.setString(3, project);
        int res1 = statement1.executeUpdate();

        statement2.setInt(1, commitIndex);
        statement2.setString(2, commitHash);
        statement2.setString(3, project);
        int res2 = statement2.executeUpdate();

        return res1 >0 || res2 > 0;
    }

    public List<Commit> getAllCommits(String project) throws SQLException {

        String sql = "SELECT DISTINCT commitIndex, commitHash FROM assets WHERE project = ? ORDER BY commitIndex";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, project);

        ResultSet resultSet = statement.executeQuery();
        List<Commit> commits = new ArrayList<>();
        while (resultSet.next()) {
            commits.add(new Commit(resultSet.getInt("commitIndex"), resultSet.getString("commitHash")));
        }

        return commits;

    }

    public List<Commit> getAllCommitsWithMetrics(String project) throws SQLException {

        String sql = "SELECT DISTINCT commitIndex, commitHash FROM assetmetrics WHERE project = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, project);


        ResultSet resultSet = statement.executeQuery();
        List<Commit> commits = new ArrayList<>();
        while (resultSet.next()) {
            commits.add(new Commit(resultSet.getInt("commitIndex"), resultSet.getString("commitHash")));
        }

        return commits;

    }

    public List<DataSetRecord> getAllDataSetsForProject(String project) throws SQLException {

        String sql = "SELECT commitIdex, commitHash, project, assetType, trainingFile, testFile, isMappedOnly, trainingXMLFile, testXMLFile, testCSVFile, testCommitIndex" +
                " testCommitHash FROM datasets WHERE project = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, project);


        ResultSet rs = statement.executeQuery();
        List<DataSetRecord> records = new ArrayList<>();
        while (rs.next()) {
            records.add(
                    new DataSetRecord(
                            rs.getInt("commitIdex"),
                            rs.getString("commitHash"),
                            rs.getString("project"),
                            rs.getString("assetType"),
                            rs.getString("trainingFile"),
                            rs.getString("testFile"),
                            rs.getBoolean("isMappedOnly"),
                            rs.getString("trainingXMLFile"),
                            rs.getString("testXMLFile"),
                            rs.getString("testCSVFile"),
                            rs.getInt("testCommitIndex"),
                            rs.getString("testCommitHash")
                    )
            );
        }

        return records;

    }

    public List<AssetMetricsDB> getParentNFF(int commitIndex, String project) throws SQLException {

        String sql = "SELECT COUNT(DISTINCT featurename) as NFF, parent FROM assetmapping WHERE commitIndex <= ? AND project = ? GROUP BY parent";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, commitIndex);
        statement.setString(2, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMetricsDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMetricsDB record = new AssetMetricsDB();
            record.setNff(rs.getInt("NFF"));
            record.setParent(rs.getString("parent"));
            records.add(record);
        }

        return records;

    }

    public List<AssetMetricsDB> getFeatureModifiedInCommit(int commitIndex, String project) throws SQLException {

        String sql = "SELECT COUNT(DISTINCT featurename) AS NFMA, commitHash from assetmapping WHERE commitIndex <= ? AND project = ? GROUP BY commitHash";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, commitIndex);
        statement.setString(2, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMetricsDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMetricsDB record = new AssetMetricsDB();
            record.setNfma(rs.getInt("NFMA"));
            record.setCommitHash(rs.getString("commitHash"));
            records.add(record);
        }

        return records;

    }

    public List<AssetMetricsDB> getFeatureModifiedPerCommitInProject(String project) throws SQLException {
        String query = "{SELECT COUNT(DISTINCT featurename) AS NFMA, commitHash FROM assetmapping WHERE project = ? GROUP BY commitHash}";

        PreparedStatement statement = connection.prepareCall(query);

        statement.setString(1, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMetricsDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMetricsDB record = new AssetMetricsDB();
            record.setNfma(rs.getInt("NFMA"));
            record.setCommitHash(rs.getString("commitHash"));
            records.add(record);
        }

        return records;

    }

    public List<AssetMetricsDB> getCCCForCommit(int commitIndex, String project) throws SQLException {

        String sql = "SELECT COUNT(*) AS CCC, commitHash from assets WHERE commitIndex <= ? AND project = ? GROUP BY commitHash";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, commitIndex);
        statement.setString(2, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMetricsDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMetricsDB record = new AssetMetricsDB();
            record.setCcc(rs.getInt("CCC"));
            record.setCommitHash(rs.getString("commitHash"));
            records.add(record);
        }

        return records;

    }

    public List<AssetMetricsDB> getCCCForProject(String project) throws SQLException {
        String query = "{SELECT COUNT(*) AS CCC, commitHash from assets WHERE project = ? GROUP BY commitHash}";

        PreparedStatement statement = connection.prepareCall(query);

        statement.setString(1, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMetricsDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMetricsDB record = new AssetMetricsDB();
            record.setCcc(rs.getInt("CCC"));
            record.setCommitHash(rs.getString("commitHash"));
            records.add(record);
        }

        return records;

    }

    public List<AssetMetricsDB> getDeveloperContribution(int commitIndex, String project) throws SQLException {

        String sql = "SELECT COUNT(DISTINCT assetfullname) AS DCONT, developer FROM assets WHERE commitIndex <= ? AND project = ? AND assetType = 'LOC' GROUP BY developer";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, commitIndex);
        statement.setString(2, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMetricsDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMetricsDB record = new AssetMetricsDB();
            record.setDcont(rs.getInt("DCONT"));
            record.setDeveloper(rs.getString("developer"));
            records.add(record);
        }

        return records;

    }
    @Deprecated
    public void cleanFeatures() throws SQLException {
        String query = "{CALL data_cleaning_all (?) }";

        CallableStatement statement = connection.prepareCall(query);

        statement.setString(1, "assetmapping");

        statement.execute();

    }
    @Deprecated
    public void deleteMappings(String feature) throws SQLException {
        String query = "{CALL assetmappings_deleteforfeature (?) }";

        CallableStatement statement = connection.prepareCall(query);

        statement.setString(1, feature);

        statement.execute();

    }
    @Deprecated
    public void updateFeatureMapping(String oldFeature, String newFeature) throws SQLException {
        String query = "{CALL assetmappings_updatefeature (?,?) }";

        CallableStatement statement = connection.prepareCall(query);

        statement.setString(1, oldFeature);
        statement.setString(2,newFeature);

        statement.executeUpdate();

    }
    @Deprecated
    public List<String> getAllFeatures() throws SQLException {
        String query = "{CALL features_loadall }";

        CallableStatement statement = connection.prepareCall(query);


        ResultSet rs = statement.executeQuery();
        List<String> records = new ArrayList<>();
        while (rs.next()) {

            records.add(rs.getString("featurename"));
        }

        return records;

    }
    @Deprecated
    public ResultSet getAssetCountPerCOmmitAllProjects() throws SQLException {
        String query = "{CALL assetCount_loadall}";
        CallableStatement statement = connection.prepareCall(query);
        return statement.executeQuery();


    }

    public List<String> getCommitsInWhichAssetChanged(int commitIndex, String asset) throws SQLException {

        String sql = "SELECT commitHash FROM assets WHERE commitIndex <= ? AND assetfullname = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, commitIndex);
        statement.setString(2, asset);

        ResultSet rs = statement.executeQuery();
        List<String> records = new ArrayList<>();
        while (rs.next()) {
            records.add(rs.getString("commitHash"));
        }

        return records;

    }

    public List<String> getDevelopersOfAsset(int commitIndex, String asset) throws SQLException {

        String sql = "SELECT DISTINCT developer FROM assets WHERE commitIndex <= ? AND assetfullname = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, commitIndex);
        statement.setString(2, asset);

        ResultSet rs = statement.executeQuery();
        List<String> records = new ArrayList<>();
        while (rs.next()) {
            records.add(rs.getString("developer"));
        }

        return records;

    }

    public List<AssetDB> getAssetsForCommit(String commit, String project) throws SQLException {

        String sql = "SELECT assetFullName, assetName, parent, commitHash, developer, assetTYpe, startingLine, endingLine, lineNumber, project, commitIndex, changeType, nloc " +
                "FROM assets WHERE commitHash = ? AND project = ? ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, commit);
        statement.setString(2, project);

        ResultSet rs = statement.executeQuery();
        List<AssetDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetDB record = new AssetDB(
                    rs.getString("assetFullName"),
                    rs.getString("assetName"),
                    rs.getString("parent"),
                    rs.getString("commitHash"),
                    rs.getString("developer"),
                    rs.getString("assetType"),
                    rs.getString("project"),
                    rs.getString("changeType"),
                    rs.getInt("startingLine"),
                    rs.getInt("endingLine"),
                    rs.getInt("lineNumber"),
                    rs.getInt("commitIndex"),
                    rs.getInt("nloc")

            );

            records.add(record);
        }

        return records;

    }

    public List<AssetDB> getAssetsForProject(String project) throws SQLException {
        String query = "{SELECT assetFullName, assetName, parent, commitHash, developer, assetType, project, changeType, startingLine, endingLine, lineNumber, commitIndex, nloc " +
                "FROM assets WHERE project = ?}";

        PreparedStatement statement = connection.prepareCall(query);
        statement.setString(1, project);

        ResultSet rs = statement.executeQuery();
        List<AssetDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetDB record = new AssetDB(
                    rs.getString("assetFullName"),
                    rs.getString("assetName"),
                    rs.getString("parent"),
                    rs.getString("commitHash"),
                    rs.getString("developer"),
                    rs.getString("assetType"),
                    rs.getString("project"),
                    rs.getString("changeType"),
                    rs.getInt("startingLine"),
                    rs.getInt("endingLine"),
                    rs.getInt("lineNumber"),
                    rs.getInt("commitIndex"),
                    rs.getInt("nloc")

            );

            records.add(record);
        }

        return records;

    }

    public List<AssetDB> getAllAssetsUptoCommit(int commitIndex, String project) throws SQLException {

        String sql = "SELECT DISTINCT assetFullName, developer, commitHash, commitIndex FROM assets WHERE commitIndex <= ? AND project = ? ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, commitIndex);
        statement.setString(2, project);

        ResultSet rs = statement.executeQuery();
        List<AssetDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetDB record = new AssetDB(
                    rs.getString("assetFullName"),
                    rs.getString("developer"),
                    rs.getString("commitHash"),
                    rs.getInt("commitIndex"),
                    rs.getString("assetType")

            );

            records.add(record);
        }

        return records;

    }

    public List<AssetMappingDB> getAssetMappingsForCommit(int commitIndex, String project) throws SQLException {

        String sql = "SELECT DISTINCT assetfullname, assetType, parent, featurename, project, annotateType, commitHash, commitIndex, developer FROM assetmapping WHERE commitIndex <= ? AND project = ? ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, commitIndex);
        statement.setString(2, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMappingDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMappingDB record = new AssetMappingDB(
                    rs.getString("assetfullname"),
                    rs.getString("assetType"),
                    rs.getString("parent"),
                    rs.getString("featurename"),
                    rs.getString("project"),
                    rs.getString("annotationType"),
                    rs.getString("commitHash"),
                    rs.getString("developer"),
                    rs.getInt("commitIndex")

            );

            records.add(record);
        }

        return records;

    }

    public List<AssetMappingDB> getAssetMappingsForProject(String project) throws SQLException {
        String query = "{CALL assetmapping_loadforproject (?) }";

        CallableStatement statement = connection.prepareCall(query);

        statement.setString(1, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMappingDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMappingDB record = new AssetMappingDB(
                    rs.getString("assetfullname"),
                    rs.getString("assetType"),
                    rs.getString("parent"),
                    rs.getString("featurename"),
                    rs.getString("project"),
                    rs.getString("annotationType"),
                    rs.getString("commitHash"),
                    rs.getString("developer"),
                    rs.getInt("commitIndex")

            );

            records.add(record);
        }

        return records;

    }

    public List<AssetMappingDB> getAllAssetMappingsForProject(String project) throws SQLException {

        String sql = "SELECT DISTINCT assetFullName, assetType, featurename, commitIndex, commitHash FROM assetmapping WHERE project = ? ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMappingDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMappingDB record = new AssetMappingDB(
                    rs.getString("assetfullname"),
                    rs.getString("assetType"),
                    rs.getString("featurename"),
                    rs.getString("commitHash"),
                    rs.getInt("commitIndex")

            );

            records.add(record);
        }

        return records;

    }

    public List<AssetMetricsDB> getAllAssetMetricsForProject(String project) throws SQLException {

        String sql = "SELECT assetFullName, assetType, assetparent, commitHash, commitIndex, project, ismapped, csdev, ddev, comm, dcont, hdcont, ccc, accc, nloc, dnfma, nfma, nff " +
                "FROM assetmetrics WHERE project = ?";
        PreparedStatement statement = connection.prepareStatement(sql);

        statement.setString(1, project);

        ResultSet rs = statement.executeQuery();
        List<AssetMetricsDB> records = new ArrayList<>();
        while (rs.next()) {
            AssetMetricsDB record = new AssetMetricsDB(
                    rs.getString("assetFullName"),
                    rs.getString("assetType"),
                    rs.getString("project"),
                    rs.getBoolean("ismapped"),
                    rs.getString("commitHash"),
                    rs.getString("assetparent"),
                    rs.getInt("commitIndex"),
                    rs.getDouble("csdev"),
                    rs.getDouble("ddev"),
                    rs.getDouble("comm"),
                    rs.getDouble("dcont"),
                    rs.getDouble("hdcont"),
                    rs.getDouble("ccc"),
                    rs.getDouble("accc"),
                    rs.getDouble("nloc"),
                    rs.getDouble("dnfma"),
                    rs.getDouble("nfma"),
                    rs.getDouble("nff")

            );

            records.add(record);
        }

        return records;

    }

    //==========END nEW WINE==================================
    public boolean deleteAssetAndChildren(String fullName) throws SQLException {
        String query = "{CALL deleteAsset {?}";

        CallableStatement statement = connection.prepareCall(query);
        statement.setString(1, fullName);
        return statement.executeUpdate() > 0;

    }
    @Deprecated
    public Asset addAsset(Asset asset, String parentFullName, String project) throws SQLException {
        String query = "{CALL assetInsert (?,?,?,?,?,?,?,?,?,?,?,?) }";
        CallableStatement statement = connection.prepareCall(query);
        statement.setString(1, asset.getAssetName());
        statement.setString(2, asset.getFullyQualifiedName());
        statement.setString(3, asset.getAssetType().toString());
        statement.setString(4, parentFullName);
        statement.setString(5, asset.getFileRelativePath());
        statement.setInt(6, asset.getNloc());
        statement.setString(7, asset.getAssetContent());
        statement.setString(8, asset.getCombinedContent());
        statement.setInt(9, asset.getStartLine());
        statement.setInt(10, asset.getEndLine());
        statement.setInt(11, asset.getLineNumber());
        statement.setString(12, project);

        statement.executeUpdate();
        return asset;
    }
    @Deprecated
    public boolean mapAssetToFeature(String featureName, String assetFullName, String annotationType, String project, String assetType, int tangled) throws SQLException {
        String query = "{CALL assetFeatureMapInsert (?,?,?,?,?,?)}";
        CallableStatement statement = connection.prepareCall(query);
        statement.setString(1, featureName);
        statement.setString(2, assetFullName);
        statement.setString(3, annotationType);
        statement.setString(4, project);
        statement.setString(5, assetType);
        statement.setInt(6, tangled);
        boolean added = statement.executeUpdate() > 0;
        return added;

    }

    public Asset addAsset_old(Asset asset, String parentFullName, String project) throws SQLException {

        //first check if asset exists
        boolean exists = false;
        if (asset.getAssetType() == AssetType.FILE || asset.getAssetType() == AssetType.FOLDER) {
            PreparedStatement query = connection.prepareStatement("SELECT * from assets where fullyQualifiedName = ?");
            query.setString(1, asset.getFullyQualifiedName());
            ResultSet resultSet = query.executeQuery();
            exists = resultSet.next();
        }
        if (exists && asset.getAssetType() == AssetType.FILE) {
            //delete existing assets

            //delete existing mappings
        }
        if (exists) {
            return asset;
        } else {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO assets (assetName,fullyQualifiedName,assetType,assetParent,fileRelativePath,nloc,assetContent,assetCombinedContent,startingLine,endingLine,lineNumber,project)" +
                    " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            statement.setString(1, asset.getAssetName());
            statement.setString(2, asset.getFullyQualifiedName());
            statement.setString(3, asset.getAssetType().toString());
            statement.setString(4, parentFullName);
            statement.setString(5, asset.getFileRelativePath());
            statement.setInt(6, asset.getNloc());
            statement.setString(7, asset.getAssetContent());
            statement.setString(8, asset.getCombinedContent());
            statement.setInt(9, asset.getStartLine());
            statement.setInt(10, asset.getEndLine());
            statement.setInt(11, asset.getLineNumber());
            statement.setString(12, project);

            statement.executeUpdate();


            return asset;
        }
    }

    public boolean mapAssetToFeature_old(String featureName, String assetFullName, String annotationType, String project, String assetType, int tangled) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT * from featureassetmap where featureName = ? and assetFullyQualifiedName = ? and annotationType=? ");
        query.setString(1, featureName);
        query.setString(2, assetFullName);
        query.setString(3, annotationType);
        ResultSet resultSet = query.executeQuery();
        boolean exists = resultSet.next();
        if (exists) {
            return true;
        } else {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO featureassetmap (featureName,assetFullyQualifiedName,annotationType,project,assetType,tangled)" +
                    " VALUES (?,?,?,?,?,?)");
            statement.setString(1, featureName);
            statement.setString(2, assetFullName);
            statement.setString(3, annotationType);
            statement.setString(4, project);
            statement.setString(5, assetType);
            statement.setInt(6, tangled);
            boolean added = statement.executeUpdate() > 0;


            return added;
        }
    }

    public List<String> getAllMappedFeatures(String project) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT distinct(featureName) as featureName from featureassetmap where project = ? and featureName not like '%::%' ");
        query.setString(1, project);
        ResultSet resultSet = query.executeQuery();
        List<String> features = new ArrayList<>();
        while (resultSet.next()) {
            features.add(resultSet.getString("featureName"));
        }

        return features;
    }

    public List<String> getAllMappedFeatures(String project, String file) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT distinct(featureName) as featureName from featureassetmap where project = ? and assetFullyQualifiedName like '%?%'  and featureName not like '%::%' ");
        query.setString(1, project);
        //query.setString(2, file); TODO: ???
        ResultSet resultSet = query.executeQuery();
        List<String> features = new ArrayList<>();
        while (resultSet.next()) {
            features.add(resultSet.getString("featureName"));
        }

        return features;
    }

    public double getScatteringDegree(String featureQualifiedName, String project) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT count(*) as SD from featureassetmap where project=? and featureName =? and assetType='FRAGMENT' and annotationType='FRAGMENT' ");
        query.setString(1, project);
        query.setString(2, featureQualifiedName);
        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            return resultSet.getDouble("SD");
        } else {
            return 0.0;
        }

    }

    public double getTanglingDegree(String featureQualifiedName, String project) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT sum(tangled) as TD from featureassetmap where project=? and featureName =? and assetType='FRAGMENT' and annotationType='FRAGMENT' ");
        query.setString(1, project);
        query.setString(2, featureQualifiedName);
        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            return resultSet.getDouble("TD");
        } else {
            return 0.0;
        }

    }

    public double getLinesOfFeatureCode(String featureQualifiedName, String project) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT sum(nloc) as nloc from featureassetmap a inner join assets b on b.fullyQualifiedName = a.assetFullyQualifiedName where a.project=? and a.featureName =? and a.assetType='FRAGMENT' and a.annotationType='FRAGMENT' ");
        query.setString(1, project);
        query.setString(2, featureQualifiedName);
        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            return resultSet.getDouble("nloc");
        } else {
            return 0.0;
        }

    }
}


