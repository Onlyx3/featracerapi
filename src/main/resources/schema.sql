PRAGMA journal_mode = MEMORY;
PRAGMA synchronous = OFF;
PRAGMA foreign_keys = OFF;
PRAGMA ignore_check_constraints = OFF;
PRAGMA auto_vacuum = NONE;
PRAGMA secure_delete = OFF;
BEGIN TRANSACTION;

DROP TABLE IF EXISTS `assetmapping`;

CREATE TABLE `assetmapping` (
                                `assetfullname` TEXT NOT NULL,
                                `assetType` TEXT DEFAULT NULL,
                                `parent` TEXT DEFAULT NULL,
                                `featurename` TEXT NOT NULL,
                                `project` TEXT DEFAULT NULL,
                                `annotationType` TEXT DEFAULT NULL,
                                `commitHash` TEXT DEFAULT NULL,
                                `commitIndex` int DEFAULT NULL,
                                `developer` TEXT DEFAULT NULL
);
DROP TABLE IF EXISTS `assetmetrics`;

CREATE TABLE `assetmetrics` (
                                `assetFullName` TEXT NOT NULL,
                                `assetparent` TEXT DEFAULT NULL,
                                `commitHash` TEXT DEFAULT NULL,
                                `commitIndex` int DEFAULT NULL,
                                `project` TEXT DEFAULT NULL,
                                `ismapped` tinyint DEFAULT NULL,
                                `assetType` TEXT DEFAULT NULL,
                                `csdev` float DEFAULT NULL,
                                `ddev` float DEFAULT NULL,
                                `comm` float DEFAULT NULL,
                                `dcont` float DEFAULT NULL,
                                `hdcont` float DEFAULT NULL,
                                `ccc` float DEFAULT NULL,
                                `accc` float DEFAULT NULL,
                                `nloc` float DEFAULT NULL,
                                `dnfma` float DEFAULT NULL,
                                `nfma` float DEFAULT NULL,
                                `nff` float DEFAULT NULL
);
DROP TABLE IF EXISTS `assets`;

CREATE TABLE `assets` (
                          `assetFullName` TEXT NOT NULL,
                          `assetName` TEXT DEFAULT NULL,
                          `parent` TEXT DEFAULT NULL,
                          `commitHash` TEXT DEFAULT NULL,
                          `developer` TEXT DEFAULT NULL,
                          `assetType` TEXT DEFAULT NULL,
                          `startingLine` int DEFAULT NULL,
                          `endingLine` int DEFAULT NULL,
                          `lineNumber` int DEFAULT NULL,
                          `project` TEXT DEFAULT NULL,
                          `commitIndex` int DEFAULT NULL,
                          `changeType` TEXT DEFAULT NULL,
                          `nloc` int DEFAULT NULL
);
DROP TABLE IF EXISTS `datasets`;

CREATE TABLE `datasets` (
                            `commitIdex` int NOT NULL,
                            `commitHash` TEXT DEFAULT NULL,
                            `project` TEXT DEFAULT NULL,
                            `assetType` TEXT DEFAULT NULL,
                            `trainingFile` TEXT DEFAULT NULL,
                            `testFile` TEXT DEFAULT NULL,
                            `isMappedOnly` tinyint DEFAULT NULL,
                            `trainingXMLFile` TEXT DEFAULT NULL,
                            `testXMLFile` TEXT DEFAULT NULL,
                            `testCSVFile` TEXT DEFAULT NULL,
                            `testCommitIndex` int DEFAULT NULL,
                            `testCommitHash` TEXT DEFAULT NULL
);
COMMIT;
PRAGMA ignore_check_constraints = ON;
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
