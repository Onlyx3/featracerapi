-- import to SQLite by running: sqlite3.exe db.sqlite3 -init sqlite.sql

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

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `assetmapping_loadforcommit`(cIndex int, project TEXT)
BEGIN
SELECT distinct `assetmapping`.`assetfullname`,
                `assetmapping`.`assetType`,
                `assetmapping`.`parent`,
                `assetmapping`.`featurename`,
                `assetmapping`.`project`,
                `assetmapping`.`annotationType`,
                `assetmapping`.`commitHash`,
                `assetmapping`.`commitIndex`,
                `assetmapping`.`developer`
FROM `featracerdb`.`assetmapping`
where commitIndex <= cIndex and `project`=project;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `assetmetrics_insert`(
asset TEXT ,
parent TEXT ,
cHash TEXT ,
cIndex int ,
prjt TEXT ,
ismapped tinyint ,
atype TEXT,
csdev float ,
ddev float ,
comm float ,
dcont float ,
hdcont float ,
ccc float ,
accc float ,
nloc float ,
dnfma float ,
nfma float ,
nff float
);
BEGIN
INSERT INTO `featracerdb`.`assetmetrics`
(`assetFullName`,
 `assetparent`,
 `commitHash`,
 `commitIndex`,
 `project`,
 `ismapped`,
 `assetType`,
 `csdev`,
 `ddev`,
 `comm`,
 `dcont`,
 `hdcont`,
 `ccc`,
 `accc`,
 `nloc`,
 `dnfma`,
 `nfma`,
 `nff`)
VALUES
    (asset,
     parent,
     cHash,
     cIndex,
     prjt,
     isMapped,
     atype,
     csdev,
     ddev,
     comm,
     dcont,
     hdcont,
     ccc,
     accc,
     nloc,
     dnfma,
     nfma,
     nff);
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `assetmetrics_insertbulk`(cIndex int, prjt TEXT)
BEGIN
select assetFullName, count(distinct commitHash) as cHash, count(distinct developer) as devs from assets where commitIndex<=cIndex and project=prjt
group by assetFullName;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `assetmetrics_loadallforproject`(prjt TEXT)
BEGIN
SELECT `assetmetrics`.`assetFullName`,
       `assetmetrics`.`assetType`,
       `assetmetrics`.`assetparent`,
       `assetmetrics`.`commitHash`,
       `assetmetrics`.`commitIndex`,
       `assetmetrics`.`project`,
       `assetmetrics`.`ismapped`,
       `assetmetrics`.`csdev`,
       `assetmetrics`.`ddev`,
       `assetmetrics`.`comm`,
       `assetmetrics`.`dcont`,
       `assetmetrics`.`hdcont`,
       `assetmetrics`.`ccc`,
       `assetmetrics`.`accc`,
       `assetmetrics`.`nloc`,
       `assetmetrics`.`dnfma`,
       `assetmetrics`.`nfma`,
       `assetmetrics`.`nff`
FROM `featracerdb`.`assetmetrics` where project=prjt;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `assets_deleteforcommit`(cHash TEXT, prjt TEXT)
BEGIN
delete from assets where project=prjt and commitHash=cHash;
delete from assetmapping where project=prjt and commitHash=cHash;
END ;;
DELIMITER ;

DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `asset_loadcommitsanddevs`(cIndex int, prjt TEXT)
BEGIN
select distinct assetFullName,developer,commitHash,commitIndex from assets where commitIndex <=cIndex and project=prjt;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `asset_loadforcommit`(cHash TEXT, prjt TEXT)
BEGIN
SELECT a.`assetFullName`,
       a.`assetName`,
       a.`parent`,
       a.`commitHash`,
       a.`developer`,
       a.`assetType`,
       a.`startingLine`,
       a.`endingLine`,
       a.`lineNumber`,
       a.`project`,
       a.`commitIndex`,
       a.`changeType`,
       a.nloc
FROM `featracerdb`.`assets` a
where `commitHash` = cHash and `project`=prjt;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `asset_rename`(oldname varchar (300),newname  varchar (300))
BEGIN
update 	`assets`
set `assetFullName` = REPLACE(`assetFullName`,oldname,newname)
where `assetFullName`=oldname;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `commitindex_update`(commitHash TEXT,commitIndex int, project TEXT)
BEGIN
update `assets`
set `commitIndex`=commitIndex
where `commitHash` = commitHash and `project`=project;
update `assetmapping`
set `commitIndex`=commitIndex
where `commitHash` = commitHash and `project`=project;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `commits_loadall`(projectName TEXT)
BEGIN
select distinct commitIndex,commitHash from `assets` where `project`=projectName order by commitIndex;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `commits_loadforasset`(cIndex int, asset TEXT)
BEGIN
select commitHash from assets where commitIndex <= cIndex and assetfullname = asset;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `commits_loadfordatasets`(prjt TEXT)
BEGIN
select distinct commitIndex, commitHash from assetmetrics where  project=prjt;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `commit_loadccc`(cIndex int, p TEXT)
BEGIN
select count(*) as CCC, commitHash from assets where commitIndex <= cIndex and project=p group by commitHash;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `datasets_loadall`(prjt TEXT)
BEGIN
SELECT commitIdex,
       commitHash,
       project,
       assetType,
       trainingFile,
       testFile,
       isMappedOnly,
       trainingXMLFile,
       testXMLFile,
       testCSVFile,
       testCommitIndex,
       testCommitHash
FROM featracerdb.datasets where project=prjt;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `dataset_deleteforcommit`(cHash TEXT,prjt TEXT)
BEGIN
delete from datasets where commitHash=cHash and project=prjt;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `dataset_insert`(commitIdex int,
commitHash TEXT,
project TEXT,
assetType TEXT,
trainingFile TEXT,
testFile TEXT,
isMappedOnly tinyint,
trainingXMLFile TEXT,
testXMLFile TEXT,
testCSVFile TEXT,
testCommitIndex int,
testCommitHash TEXT )
BEGIN
INSERT INTO `featracerdb`.`datasets`
(`commitIdex`,
 `commitHash`,
 `project`,
 `assetType`,
 `trainingFile`,
 `testFile`,
 `isMappedOnly`,
 `trainingXMLFile`,
 `testXMLFile`,
 `testCSVFile`,
 `testCommitIndex`,
 `testCommitHash`
);
VALUES
    (commitIdex,
     commitHash,
     project,
     assetType,
     trainingFile,
     testFile,
     isMappedOnly,
     trainingXMLFile,
     testXMLFile,
     testCSVFile,
     testCommitIndex,
     testCommitHash);
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `devs_loadforasset`(cIndex int, asset TEXT)
BEGIN
select distinct developer from assets where commitIndex <= cIndex and assetfullname = asset;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `dev_loadcontforcommit`(cIndex int, p TEXT)
BEGIN
select count(distinct assetfullname) as DCONT, developer from assets where commitIndex <= cIndex and project=p and assetType='LOC' group by developer;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `features_loadforcommit`(cIndex int, p TEXT)
BEGIN
select count(distinct featurename) as NFMA,commitHash from assetmapping where commitIndex <=cIndex and project=p group by commitHash;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `mappings_loadforproject`(prjt TEXT)
BEGIN
select distinct assetFullName,assetType,featurename,commitIndex,commitHash from assetmapping where project=prjt;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `metrics_deleteforcommit`(cHash TEXT,prjt varchar (100))
BEGIN
delete from assetmetrics where project=prjt and commitHash=cHash;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `new_procedure`(
asset TEXT ,
parent TEXT ,
cHash TEXT ,
cIndex int ,
prjt TEXT ,
ismapped tinyint ,
csdev float ,
ddev float ,
comm float ,
dcont float ,
hdcont float ,
ccc float ,
accc float ,
nloc float ,
dnfma float ,
nfma float ,
nff float
);
BEGIN
INSERT INTO `featracerdb`.`assetmetrics`
(`assetFullName`,
 `assetparent`,
 `commitHash`,
 `commitIndex`,
 `project`,
 `ismapped`,
 `csdev`,
 `ddev`,
 `comm`,
 `dcont`,
 `hdcont`,
 `ccc`,
 `accc`,
 `nloc`,
 `dnfma`,
 `nfma`,
 `nff`)
VALUES
    (asset,
     parent,
     cHash,
     cIndex,
     prjt,
     isMapped,
     csdev,
     ddev,
     comm,
     dcont,
     hdcont,
     ccc,
     accc,
     nloc,
     dnfma,
     nfma,
     nff);
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `parent_loadnff`(cIndex int,project TEXT)
BEGIN
select count(distinct featurename) as NFF,parent from assetmapping where `commitIndex` <=cIndex and `project`=project group by parent;
END ;;
DELIMITER ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `parent_loadnloc`(cIndex int,p TEXT)
BEGIN
select count(distinct assetfullname) as NLOC,parent from assets where commitIndex <=cIndex and project=p group by parent;
END ;;
DELIMITER ;





COMMIT;
PRAGMA ignore_check_constraints = ON;
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
