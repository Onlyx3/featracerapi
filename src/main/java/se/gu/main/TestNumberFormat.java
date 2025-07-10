package se.gu.main;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.ml.experiment.PredictionResult;
import se.gu.utils.Utilities;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestNumberFormat {
    static String getFeatureFromAnnotation(String annotation, String regex) {
        String[] array;

        array = annotation.split(regex);
        String feature = Arrays.stream(array).filter(str -> !StringUtils.isBlank(str)).collect(Collectors.joining());
        return feature.trim();//.replaceAll("DISABLED","!");


    }

    public static void combineDataSets(int iteration, List<String> trainingSet, List<String> testSet) throws IOException {
        String folderName = "c:/defectPrediction";
        String trainingFileName = String.format("%s/train%d-%s.csv", folderName, iteration,
                trainingSet.stream().collect(Collectors.joining("-")));
        String testFileName = String.format("%s/test%d-%s.csv", folderName, iteration,
                testSet.stream().collect(Collectors.joining("-")));

        //combine train files
        combineFiles(trainingSet, trainingFileName);
        //combine test files
        combineFiles(testSet, testFileName);
    }

    private static void combineFiles(List<String> trainingSet, String trainingFileName) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(trainingFileName, true));
        String parentFolder = "C:/defectprediction/datasets_new";
        boolean headerPrinted = false;
        for (String set : trainingSet) {
            List<String> lines = FileUtils.readLines(new File(String.format("%s/%s_combined.csv", parentFolder, set)), "UTF-8");
            if (headerPrinted == false) {
                writer.println(lines.get(0));
                headerPrinted = true;
            }
            for (int i = 1; i < lines.size(); i++) {
                writer.println(lines.get(i));
            }
        }
    }

    private void tryRegex() {
        String myClass = "      MyClass(){ fhvghsoieivwosg}";
        String cName = "MyClass";
        String regex = "\\s*" + cName + "\\(.*\\)(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(myClass);
        if (matcher.find()) {
            System.out.println("Found class");
        }
    }


    public static void main(String[] args) {
        try {
            //2021-11-10
            //Add extra column to skip some commits from belnder
//            File fewCOmmits = new File("C:\\studies\\defectprediction\\commits_blender2.csv");
//            File allCOmmits = new File("C:\\studies\\defectprediction\\commits_blender.csv");
//            List<String> all = FileUtils.readLines(allCOmmits,"UTF-8");
//            List<String> few = FileUtils.readLines(fewCOmmits,"UTF-8");
//            Utilities.deleteFile(allCOmmits.getAbsolutePath());
//            PrintWriter writer = new PrintWriter(new FileWriter(allCOmmits,true));
//            for(String line:all){
//                if(few.contains(line)){
//                    writer.printf("%s;keep\n",line);
//                }
//                else{
//                    writer.printf("%s;skip\n",line);
//                }
//            }
//            writer.close();


            //2021-11-4 Test splitting features
//            String splitRegex = "AND|OR";
//            String feature = "_OPBRA_defined___sse2___CLBRA__AND__OPBRA___OPBRA_NOT_defined___gnuc___CLBRA__OR__OPBRA_defined___intel_compiler_CLBRA__OR_eigen_gnuc_at_least_OPBRA_4_COMMA_2_CLBRA___CLBRA_";
//            String[]splitFeatures = feature.split(splitRegex);
//            System.out.println(splitFeatures.length);
//            for(String f:splitFeatures){
//                System.out.println(f);;
//            }

            //2021-09-14
            //createRelativeCommits();
            //2021-09-10
            //createCombinedProjectResults();
            //2021-08-29
            //try date diff
            //getTimeDiff();
            //testPrintAnnotationsInFile();//2021-05-13
            //combineLuceneData();//2021-05-04
            //combineMeasuresLuceneData();//2021-05-04
//generateCombinedProjectMeasures();
                       //mapPracticesWithPredictions();

            //combinePracticesFiles();
            createRandomProjectCombinations();
//
//            String testLongLine = "//THis is my line of code; I want to remove . none word characters ^ & and this too./*....*/";
//            //Regex regRemoval = new Regex("[\\W_-[\\s]]+");
//            System.out.printf("Original string: %s\nCleaned String: %s\n", testLongLine, testLongLine.replaceAll("[^a-zA-Z0-9\\s]", ""));


            //test reading files
//            String allowedFiles = "js,c,cpp,h";
//            List<String> allowedExtensions = Arrays.asList(allowedFiles.split(","));
//            File dir = new File("C:/studies/defectprediction/blender");
//            Path start = Paths.get(dir.getAbsolutePath());
//            try (Stream<Path> stream = Files.walk(start, Integer.MAX_VALUE)) {
//                List<String> collect = stream
//                        .map(String::valueOf)
//                        .filter(s->allowedExtensions.contains(FilenameUtils.getExtension(s)))
//                        .sorted()
//                        .collect(Collectors.toList());
//
//                collect.forEach(System.out::println);
//            }
//            System.out.println("ALLOWED FILES");

            //Test comment line
//            String lineRegex = "//{2}|(/\\*)";
//            String text = "1:/*hello there you*/";
//            System.out.println(Arrays.asList(text.split(lineRegex)));
//            //Test multiple features
//            String multipleFeatureRegex = "(\\&)|(\\|)|(&&)|(\\|{2})|(&&)";
//            String feature = "FeatureA && FeatureB && !FeatureC";
//            List<String> features = Arrays.asList(feature.split(multipleFeatureRegex)).stream()
//                    .filter(s -> !StringUtils.isBlank(s))
//                    .map(s -> s.trim())
//                    .collect(Collectors.toList());
//            System.out.println(features);
//
//            double x = 0.00000;
//            System.out.println(Precision.round(x, 1));
//            List<String> attributes = new ArrayList<>(Arrays.asList(new String[]{"SLD", "CSM"}));
//
//            List<Map<String, Double>> list = new ArrayList<>();
//            Map<String, Double> m1 = new HashMap<>();
//            m1.put("SLD", 0.9);
//            m1.put("CSM", 0.8);
//            list.add(m1);
//            Map<String, Double> m2 = new HashMap<>();
//            m2.put("SLD", 0.4);
//            m2.put("CSM", 0.5);
//            list.add(m2);
//            Map<String, Double> m3 = new HashMap<>();
//            m3.put("SLD", 0.3);
//            m3.put("CSM", 0.7);
//            list.add(m3);
//            List<Map<String, Double>> normalisedList = new ArrayList<>();
//
//            Map<String, List<Double>> flat = new HashMap<>();
//            for (String attribute : attributes) {
//                List<Double> values = new ArrayList<>();
//
//                for (Map<String, Double> m : list) {
//                    values.add(m.get(attribute));
//                }
//                flat.put(attribute, values);
//                //min and max
//                double min = values.stream().mapToDouble(Double::doubleValue).min().getAsDouble();
//                double max = values.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
//                System.out.printf("MIN of %s is %.1f\n", attribute, min);
//                System.out.printf("MAX of %s is %.1f\n", attribute, max);
//                //normalize
//                for (Map<String, Double> m : list) {
//                    Map<String, Double> map = new HashMap<>();
//                    double normalised = Math.abs((m.get(attribute) - min) / (max - min));
//                    map.put(attribute, normalised);
//                    System.out.printf("NORMALISED DATA\n");
//                    System.out.printf("%s: %f\n", attribute, normalised);
//                    normalisedList.add(map);
//
//                }
//
//            }


//            String str1 = FileUtils.readFileToString(new File("C:\\fanas\\2\\ClaferMooVisualizer\\Server\\server.js"),"UTF-8");
//            String str2 = FileUtils.readFileToString(new File("C:\\fanas\\2\\ClaferMooVisualizer\\Server\\upload.js"),"UTF-8");
//            AbstractStringMetric metric = new DiceSimilarity();
//            double disc1 = metric.getSimilarity(str1,str2);
//
//            System.out.printf("Dice similarity is %.2f\n",disc1);
//            metric = new CosineSimilarity();
//           double cos = metric.getSimilarity(str1,str2);
//            System.out.printf("Cosine similarity is %.2f\n",cos);
//
//
//            String text = "@@ -328 +328 @@ void manage_heater()";
//            int firstIndex = text.indexOf("@@");
//            int secondIndex = text.lastIndexOf("@@");
//
//            System.out.println("First index of @@ is "+firstIndex);
//            System.out.println("Last index of @@ is "+secondIndex);
//            System.out.println(text.substring(firstIndex+2,secondIndex));
//
//            Path analsysDirectory = Paths.get("C:\\fanas\\2\\ClaferMooVisualizer");
//           BufferedReader bufferedReader = Git.gitDiff(analsysDirectory,"3195dfd2df22c7ea9e51061b7da82582425c6989");
//           String l;
//           while((l=bufferedReader.readLine())!=null){
//               System.out.println(l);
//           }
//            String command = "git --no-pager log --graph --pretty=format:'_%H _%ad _%an _%s' --date=iso";
//            ArrayList<String> list1 = new ArrayList<>();
//            list1.addAll(Arrays.asList(command.split("\\s")));
//            System.out.println(list1);
            //Git.runCommand(analsysDirectory);
//
//            String testFile = "C:\\studies\\Marlin\\Marlin\\src\\Marlin.cpp";
//            String ifDefBegin = "\\s*((#\\s*ifndef\\s+.*)|(#\\s*ifdef\\s+.*)|(#\\s*if\\s+.*))";
//            String ifDefEnd = "\\s*(#\\s*endif)|(#\\s*else)";
//            String featureNameRegex = "(#\\s*ifndef)|(#\\s*ifdef)|(#\\s*if)|(defined|def)";
//                        Pattern fragmentBeginning, fragmentEnd;
//            Matcher fragmentBeginningMatcher;
//            Matcher fragmentEndMatcher;
//
//            fragmentBeginning = Pattern.compile(ifDefBegin);
//            fragmentEnd = Pattern.compile(ifDefEnd);
//            //read the file
//            List<String> fileLines = FileUtils.readLines(new File(testFile), "UTF-8");
//            for (String line : fileLines) {
//                fragmentBeginningMatcher = fragmentBeginning.matcher(line);
//                fragmentEndMatcher = fragmentEnd.matcher(line);
//
//                if (fragmentBeginningMatcher.find()) {
//                    String foundAnnotation = fragmentBeginningMatcher.group(0).trim();
//                    System.out.println(foundAnnotation);
//                    System.out.printf("Found FEATURE: %s\n", getFeatureFromAnnotation(foundAnnotation, featureNameRegex));
//                }
//                if (fragmentEndMatcher.find()) {
//                    System.out.println(fragmentEndMatcher.group(0).trim());
//                }
//            }


            //Test string comparison
//            String regex = "(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])";
//            AbstractStringMetric metric = new CosineSimilarity();
//            System.out.printf("printing CSM of addPot vs deletePot %.4f",metric.getSimilarity(Arrays.stream("addPot".split(regex)).collect(Collectors.joining(" ")), Arrays.stream("deletePot".split(regex)).collect(Collectors.joining(" "))));

//
//
//            //test split
//            String oldString = "Example-Based Specificity";
//            String newString = oldString.replaceAll("\\s*|\\W", "").replaceAll("-", "");
//            System.out.println(newString);
//            String dsName = "C:\\fanas\\2\\FRAGMENT\\ClaferIDE\\dataFiles\\4_a2639bb105f4bb1bb2f839da8e08b4b93334b4aa\\out\\4_a2639bb105f4bb1bb2f839da8e08b4b93334b4aa-FRAGMENT";
//            String[] ds = dsName.split("\\\\|/");
//            System.out.println(ds[ds.length - 1]);
//            //Test moving items in list
//            List<String> commits = new ArrayList<>(Arrays.asList(new String[]{"Hi", "there", "how", "you", "are"}));
//            int indexOfYou = commits.indexOf("are");
//            System.out.println(commits.stream().collect(Collectors.joining(" ")));
//            commits.remove("you");
//            System.out.println(commits.stream().collect(Collectors.joining(" ")));
//            commits.add(indexOfYou, "you");
//            System.out.println(commits.stream().collect(Collectors.joining(" ")));
//
//            //Test date
//            String myDate = "2014-04-02 22:19:30 -0400";
//            Date date = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss").parse(myDate);
//            Calendar c = Calendar.getInstance();
//            c.setTime(date);
//            System.out.printf("%d%d%d%d%d%d\n", c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
//            System.out.println(date.getTime());
//            System.out.println(String.format("%d%08d%s", 1, 1, "agdsgwgkb"));
//
//            String fileName = "C:/Muke/You/asp.net";
//            System.out.println(FilenameUtils.getName(fileName));
//            System.out.println(FilenameUtils.removeExtension(FilenameUtils.getName(fileName)));
//            int[] nums = new int[]{1, 2, 3, 4, 5};
//            System.out.println(Ints.asList(nums));
//            System.out.println(Arrays.asList(nums));
//
//            String line = "C:\\fanas\\2\\ClaferConfigurator\\Server\\Client\\configuration.js::CLUSTER 1-20";
//            System.out.println(line.split("::CLUSTER")[1]);
//
//            System.out.println(System.getProperty("user.dir"));
//            String jarPath = System.getProperty("user.dir") + "\\libs\\ml\\mlsmote.jar";
//            String inpath = "C:\\mslote", outpath = "C:\\mslote\\out", fileext = "LOC.arff", xml = "C:\\mslote\\100000003_e184bd6e2e66835d3ec78bd2835285e5706b57e7-LOC-FLAT.xml";
//            int labelCombination = 3;
//
////            Mlsmote mlsmote = new Mlsmote();
////            mlsmote.execute(inpath,outpath,fileext,xml,labelCombination);
//
//            //test double averging
//            List<Double> array = new ArrayList<>();
//            array.add(2.0);
//            array.add(3.0);
//            array.add(Double.NaN);
//
//            System.out.println(array.stream().filter(d -> !d.isNaN()).mapToDouble(Double::doubleValue).average().getAsDouble());


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void getTimeDiff() throws InterruptedException {
        Date d1 = new Date();
        Thread.sleep(4000);
        Date d2 = new Date();
        long diff = d2.getTime() - d1.getTime();//as given

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        System.out.printf("seconds:%d, minutes %d\n", seconds, minutes);
    }

    /**
     * This method generates relative commit numbers from the list of commits in each project
     */
    private static void createRelativeCommits(){
        String[] files = new String[]{
                "C:\\exp\\expnew\\allprojectscombinedMeasures_file_RAkELd.csv",
                "C:\\exp\\expnew\\allprojectscombinedMeasures_folder_RAkELd.csv",
                "C:\\exp\\expnew\\allprojectscombinedMeasures_fragment_RAkELd.csv",
                "C:\\exp\\expnew\\allprojectscombinedMeasures_loc_RAkELd.csv"
        };
        for(String file : files){
            PrintWriter writer=null;
            try {
                String fileNameWithoutExtension = file.replace(".csv", "").trim();
                File relativeCommitsFile = new File(String.format("%s_RELATIVECOMMITS.csv", fileNameWithoutExtension));
                if (relativeCommitsFile.exists()) {
                    FileUtils.forceDelete(relativeCommitsFile);
                }
                //read through all lines in file
                List<String> lines = FileUtils.readLines(new File(file),"UTF-8");
                writer = new PrintWriter(new FileWriter(relativeCommitsFile,true));
                writer.printf("rCommit;%s\n",lines.get(0));//header: commitIndex;commit;classifier;measure;measureValue;project
                //create list of prediction results
                List<PredictionResult> results = new ArrayList<>();
                for(int i=1;i<lines.size();i++){
                    results.add(new PredictionResult(lines.get(i)));
                }
                //get distnct projects from the results
                List<String> projects= results.parallelStream().map(PredictionResult::getProject).distinct().collect(Collectors.toList());
                for(String project:projects){
                    //get distnct commit numbers
                    List<Integer> commits = results.parallelStream().filter(r->r.getProject().equalsIgnoreCase(project)).map(PredictionResult::getCommitIndex).distinct().sorted().collect(Collectors.toList());
                    Map<Integer,Integer>rCommits = new HashMap<>();
                    double totalCommits= commits.size();
                    //assign relative comit number for each commit i.e. out 100% of commits, what number is each commit
                    for(int c=0;c<commits.size();c++){
                        double rCommit = Math.round((((double)(c+1))/totalCommits)*100.0);
                        rCommits.put(commits.get(c),(int)rCommit);
                    }
                    //now get all results for project and write them
                    List<PredictionResult> projectReslts = results.parallelStream().filter(r->r.getProject().equalsIgnoreCase(project)).collect(Collectors.toList());
                    for(PredictionResult r:projectReslts){
                        writer.printf("%d;%d;%s;%s;%s;%.3f;%s\n",rCommits.get(r.getCommitIndex()),r.getCommitIndex(),r.getCommit(),r.getClassifier(),r.getMeasure(),r.getMeasureValue(),r.getProject());
                    }
                }

            }catch (Exception ex){
                ex.printStackTrace();
            }finally {
                if(writer!=null){
                    writer.close();
                }
            }
        }
    }
    private static void createCombinedProjectResults() {
        String[] projects = new String[]{"config", "viz", "tools", "ide", "marlin"};
        String[] projectFullNames = new String[]{"ClaferConfigurator", "ClaferMooVisualizer", "ClaferToolsUICommonPlatform", "ClaferIDE", "Marlin"};
        String parentFolder = "C:/exp/expnew";
        String[] levels = new String[]{"folder", "file", "fragment", "loc"};
        String classifier = "RAkELd";
        for (String level : levels) {
            try {
                String outputFile = String.format("%s/allprojectscombinedMeasures_%s_%s.csv", parentFolder, level, classifier);
                File f = new File(outputFile);
                if (f.exists()) {
                    FileUtils.forceDelete(f);//remove existing output file
                }
                PrintWriter writer = null;
                try {
                    writer = new PrintWriter(new FileWriter(outputFile, true));
                    //write header
                    writer.println("commitIndex;commit;classifier;measure;measureValue;project");

                    for (int p = 0; p < projectFullNames.length; p++) {
                        File dataFile = new File(String.format("%s/%s_%s_combinedmeasures_ps_im_reg_diff_11_ALLAssets.csv", parentFolder, projectFullNames[p], level));
                        System.out.println(dataFile);
                        List<String> lines = FileUtils.readLines(dataFile, "UTF-8");
                        //skip header
                        for (int i = 1; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if (line.contains(classifier)) {
                                writer.printf("%s;%s\n", line, projects[p]);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        //files
        //read file level

    }

    private static void testPrintAnnotationsInFile() throws IOException {
        String fileNames[] = new String[]{"C:\\studies\\Marlin\\Marlin\\src\\Marlin.cpp", "C:\\studies\\ClaferMooVisualizer\\Server\\server.js"};
        String allAnnotations = "\\s*//&begin(.*)\\[(.*)\\]|(.*)//&line\\s*\\[(.*)\\]|\\s*((#\\s*ifndef\\s+.*)|(#\\s*ifdef\\s+.*)|(#\\s*if\\s+.*))";
        Pattern allAnnotationsPattern = Pattern.compile(allAnnotations);
        Matcher allAnnotationsMatcher;
        for (String filePath : fileNames) {
            String fileText = FileUtils.readFileToString(new File(filePath), "UTF-8");
            allAnnotationsMatcher = allAnnotationsPattern.matcher(fileText);
            System.out.println("File contains annotations: " + filePath);
            while (allAnnotationsMatcher.find()) {

                System.out.println(allAnnotationsMatcher.group().trim());
            }
        }
    }

    private static void createRandomProjectCombinations() throws IOException {
        PrintWriter printWriter = null;
        try {
            //test random split of projects
//        List<String> projects = Arrays.asList(new String[]{"blender", "busybox", "emacs", "gimp", "gnumeric", "gnuplot", "irssi", "libxml2", "lighttpd", "mpsolve", "parrot", "vim"});
//        int combinations = 12;
            File file = new File("C:/studies/defectprediction/projectCombiinations.csv");
            if (file.exists()) {
                FileUtils.forceDelete(file);
            }
            printWriter = new PrintWriter(new FileWriter(file, true));
            printWriter.println("ratio;train;test;trainingFileName");
            Set<String> projects = ImmutableSet.of("blender", "busybox", "emacs", "gimp", "gnumeric", "gnuplot", "irssi", "libxml2", "lighttpd", "mpsolve", "parrot", "vim","marlin");
//            for (int p = 1; p < 13; p++) {   I commented this out bc i think not necessary
               // Set<Set<String>> trainingSets = Sets.combinations(projects, p);
//                for (Set<String> trainingSet : trainingSets) {
//                    Set<String> testSet = Sets.difference(projects, trainingSet);
 //                   String ratio = String.format("%d:%d", trainingSet.size(), testSet.size());
//                    String tgCommaList = trainingSet.stream().collect(Collectors.joining(","));
//                    String trainingFileName = String.format("%d_%d_%s.arff", trainingSet.size(), testSet.size(), tgCommaList.replace(",", "_"));
//                    printWriter.printf("%s;%s;%s;%s\n", ratio, tgCommaList, testSet.stream().collect(Collectors.joining(",")), trainingFileName);
//                }
//            }
//        for (int p=1;p<projects.size();p++) {
//
//            int train = p;
//
//            List<Set<String>> trainingSets = new ArrayList<>();
//            Map<String,Integer> projectSetCounts = new HashMap<>();
//
//            Random gen = new Random();
//
//            for (int comb = 0; comb < combinations; comb++) {
//                //create train
//                List<String> tg = null;
//                boolean work = true;
//                while (work) {
//                    List<String> trainignGroup = new ArrayList<>();
//                    gen.ints(train, 0, projects.size()).forEach(i -> trainignGroup.add(projects.get(i)));
//
//                    //check that each project exists only maximum train number of times e.g., if train=1, a project should only appear once in a set of training sets
//
//                    //now check if a project exists that exceeds number of training items
//                    boolean aProjectAppearsTooManyTimes = false;
//                    for(String key:trainignGroup){
//                        if(projectSetCounts.get(key)==null){
//                            aProjectAppearsTooManyTimes=false;
//                        }else {
//                            aProjectAppearsTooManyTimes = projectSetCounts.get(key)>train;
//                        }
//                        if(aProjectAppearsTooManyTimes){
//                            break;
//                        }
//                    }
//                    //check if existing sets contain same set of eleements as this new set
//                    boolean exists = false;
//                    for (Set<String> set : trainingSets) {
//                        exists = set.equals(new HashSet<>(trainignGroup));
//                        if (exists) {
//                            break;
//                        }
//                    }
//                    if (trainignGroup.stream().distinct().count() < train || exists||aProjectAppearsTooManyTimes) {
//                        work = true;
//                    } else {
//                        work = false;
//                        tg = trainignGroup.stream().sorted().collect(Collectors.toList());
//
//                            for(String proj:trainignGroup){
//                                if (projectSetCounts.get(proj)==null){
//                                    projectSetCounts.put(proj,1);
//                                }else {
//                                    projectSetCounts.put(proj, projectSetCounts.get(proj)+1);
//                                }
//                            }
//
//                    }
//                }
//                System.out.println(tg);
//                trainingSets.add(new HashSet<>(tg));
//
//                //test set
//                List<String> testSet = new ArrayList<>(projects);
//                testSet.removeAll(tg);
//                System.out.println(testSet);
//                String ratio = String.format("%d::%d",train,testSet.size());
//                String tgCommaList = tg.stream().collect(Collectors.joining(","));
//                String trainingFileName = String.format("%d_%d_%s.arff",train,testSet.size(),tgCommaList.replace(",","_"));
//                printWriter.printf("%s;%s;%s;%s\n",ratio,tgCommaList,testSet.stream().collect(Collectors.joining(",")),trainingFileName);
//                //ombineDataSets(comb+1,tg,testSet);
//            }
//        }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            printWriter.close();
        }

    }

    private static void doCOmbinePractices(String[] files, String targetFile, boolean isCOmbination) throws IOException {
        PrintWriter writer = null;
        try {
            System.out.println(targetFile);
            writer = new PrintWriter(new FileWriter(targetFile, true));
            if (!isCOmbination) {
                writer.println("project;commit;tLA;tLR;aLA;aLR;mLA;mLR;tFC;tFA;tFM;tFD;tFRe;tCh;aCh;tH;aH;mHS;aHS;precision;recall;fscore");
            }
            for (String file : files) {
                List<String> lines = FileUtils.readLines(new File(file), "UTF-8");
                for (int i = 1; i < lines.size(); i++) {
                    writer.println(lines.get(i));
                }

            }


        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

    }

    public static void combinePracticesFiles() throws IOException {
        String[] files = new String[]{
                "C:\\exp\\ide_file_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\tools_file_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\viz_file_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\config_file_ps_im_reg_diff_11f_PRACTICES.csv"};
        String[] fragments = new String[]{
                "C:\\exp\\marlin_fragment_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\ide_fragment_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\tools_fragment_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\viz_fragment_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\config_fragment_ps_im_reg_diff_11f_PRACTICES.csv"
        };
        String[] locs = new String[]{
                "C:\\exp\\marlin_loc_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\ide_loc_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\tools_loc_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\viz_loc_ps_im_reg_diff_11f_PRACTICES.csv",
                "C:\\exp\\config_loc_ps_im_reg_diff_11f_PRACTICES.csv"
        };

        String fileLevel = "C:\\exp\\practices_allprojects_files.csv";
        String fragmentLevel = "C:\\exp\\practices_allprojects_fragments.csv";
        String locLevel = "C:\\exp\\practices_allprojects_loc.csv";
        String fragmentLocLevel = "C:\\exp\\practices_allprojects_fragmentLoc.csv";
        String allevels = "C:\\exp\\practices_allprojects_allLevels.csv";
        String[] fileNames = new String[]{fileLevel, fragmentLevel, locLevel, fragmentLocLevel, allevels};
        for (String fileName : fileNames) {
            Utilities.deleteFile(fileName);

        }

//per level
        doCOmbinePractices(files, fileLevel, false);
        doCOmbinePractices(fragments, fragmentLevel, false);
        doCOmbinePractices(locs, locLevel, false);
//combined fragments and locs
        doCOmbinePractices(fragments, fragmentLocLevel, false);
        doCOmbinePractices(locs, fragmentLocLevel, true);
        //all levels
        doCOmbinePractices(files, allevels, false);
        doCOmbinePractices(fragments, allevels, true);
        doCOmbinePractices(locs, allevels, true);


    }

    public static void mapPracticesWithPredictions() throws IOException {
        String[] projects = new String[]{"config", "viz", "tools", "ide", "marlin"};
        String predictionsFolder = "c:/exp";
        String practicesFiles[] = new String[]{
                "C:\\fanas\\2\\config_modifications.csv",
                "C:\\fanas\\2\\ide_modifications.csv",
                "C:\\fanas\\2\\marlin_modifications.csv",
                "C:\\fanas\\2\\tools_modifications.csv",
                "C:\\fanas\\2\\viz_modifications.csv"
        };
        String[] predictionFiles = new String[]{
                "C:\\exp\\config_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\config_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\config_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\ide_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\ide_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\ide_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\marlin_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\marlin_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\tools_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\tools_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\tools_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\viz_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\viz_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\viz_loc_ps_im_reg_diff_11f.csv"
        };

        //go through projects
        for (String project : projects) {
            for (String predictionFile : predictionFiles) {
                if (!predictionFile.contains(project + "_")) {
                    continue;
                }
                System.out.println(predictionFile);
                String mappedFile = predictionFile.replace(".csv", "_PRACTICES.csv");
                File mapFile = new File(mappedFile);
                if (mapFile.exists()) {
                    FileUtils.forceDelete(mapFile);
                }
                PrintWriter writer = null;
                try {
                    String practicesFile = Arrays.stream(practicesFiles).filter(f -> f.contains(project + "_")).findFirst().get();
                    List<String> practicesLines = FileUtils.readLines(new File(practicesFile), "UTF-8");
                    writer = new PrintWriter(new FileWriter(mapFile, true));
                    writer.println("project;commit;tLA;tLR;aLA;aLR;mLA;mLR;tFC;tFA;tFM;tFD;tFRe;tCh;aCh;tH;aH;mHS;aHS;precision;recall;fscore");
                    List<String> predictionLines = FileUtils.readLines(new File(predictionFile), "UTF-8");
                    for (String predLine : predictionLines) {
                        String[] parts = predLine.split(";");
                        if (parts[0].equalsIgnoreCase("commit")) {
                            continue;//skip first line
                        }
                        String commit = parts[1].split("_")[1].split("-")[0].trim();
                        String classifier = parts[2].trim();
                        String precision = parts[18];
                        String recall = parts[20];
                        String fscore = parts[22];
                        if (predictionFile.contains("file")) {
                            //use LP classifier
                            if (classifier.equalsIgnoreCase("LP")) {
                                Optional<String> foundLine = practicesLines.stream().filter(s -> s.contains(commit)).findAny();
                                if (foundLine.isPresent()) {
                                    writer.printf("%s;%s;%s;%s\n", foundLine.get().replace(",", ";"), precision, recall, fscore);
                                }
                            }
                        } else if (predictionFile.contains("fragment")) {
                            //use LP classifier
                            if (classifier.equalsIgnoreCase("BR")) {
                                Optional<String> foundLine = practicesLines.stream().filter(s -> s.contains(commit)).findAny();
                                if (foundLine.isPresent()) {
                                    writer.printf("%s;%s;%s;%s\n", foundLine.get().replace(",", ";"), precision, recall, fscore);
                                }
                            }
                        } else {
                            if (classifier.equalsIgnoreCase("RAkELd")) {
                                Optional<String> foundLine = practicesLines.stream().filter(s -> s.contains(commit)).findAny();
                                if (foundLine.isPresent()) {
                                    writer.printf("%s;%s;%s;%s\n", foundLine.get().replace(",", ";"), precision, recall, fscore);
                                }
                            }
                        }

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
        }
    }

    private static void doCombineProjectData(String[] predictionFiles, String targetFileName, String classifier) throws IOException {
        Utilities.deleteFile(targetFileName);
        PrintWriter filesWriter = null;
        try {
            filesWriter = new PrintWriter(new FileWriter(targetFileName, true));
            filesWriter.println("commit;DataSet;Classifier;project;measure;measurevalue");
            for (String file : predictionFiles) {
                //read the file
                List<String> lines = FileUtils.readLines(new File(file), "UTF-8");
                for (int i = 1; i < lines.size(); i++) {
                    String[] parts = lines.get(i).split(";");
                    if (parts[2].equalsIgnoreCase(classifier)) {//we only want to collect lines for the specified classfier e.g., only BR or LP predictions
                        filesWriter.printf("%s;%s;%s;%s;%s;%s\n", parts[0], parts[1], parts[2], parts[25], parts[28], parts[29]);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (filesWriter != null) {
                filesWriter.close();
            }
        }

    }

    public static void generateCombinedDataNEWDB() {
        String[] projectShortNames = new String[]{"config", "viz", "tools", "ide", "marlin"};
        String[] projects = new String[]{"config", "viz", "tools", "ide", "marlin"};
        String[] fileLevelFiles = new String[]{
                "C:\\exp\\combined_measures_config_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_ide_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_tools_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_viz_file_ps_im_reg_diff_11f.csv"
        };
        String[] fragmentLevelFiles = new String[]{
                "C:\\exp\\combined_measures_config_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_ide_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_marlin_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_tools_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_viz_fragment_ps_im_reg_diff_11f.csv",
        };
        String[] locLevelFiles = new String[]{


                "C:\\exp\\combined_measures_config_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_ide_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_marlin_loc_ps_im_reg_diff_4f.csv",
                "C:\\exp\\combined_measures_tools_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_viz_loc_ps_im_reg_diff_11f.csv"
        };

    }

    public static void generateCombinedProjectMeasures() throws IOException {
        String[] projects = new String[]{"config", "viz", "tools", "ide", "marlin"};

        String[] fileLevelFiles = new String[]{
                "C:\\exp\\combined_measures_config_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_ide_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_tools_file_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_viz_file_ps_im_reg_diff_11f.csv"
        };
        String[] fragmentLevelFiles = new String[]{
                "C:\\exp\\combined_measures_config_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_ide_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_marlin_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_tools_fragment_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_viz_fragment_ps_im_reg_diff_11f.csv",
        };
        String[] locLevelFiles = new String[]{


                "C:\\exp\\combined_measures_config_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_ide_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_marlin_loc_ps_im_reg_diff_4f.csv",
                "C:\\exp\\combined_measures_tools_loc_ps_im_reg_diff_11f.csv",
                "C:\\exp\\combined_measures_viz_loc_ps_im_reg_diff_11f.csv"
        };
        String filesName = "C:\\exp\\allprojects_measures_files.csv";
        String fragmentsName = "C:\\exp\\allprojects_measures_fragments.csv";
        String locsName = "C:\\exp\\allprojects_measures_loc.csv";
        doCombineProjectData(fileLevelFiles, filesName, "LP");
        doCombineProjectData(fragmentLevelFiles, fragmentsName, "BR");
        doCombineProjectData(locLevelFiles, locsName, "RAkELd");


    }

    public static void combineLuceneData() throws IOException {
        String[] projectFiles = new String[]{"C:\\fanas\\2\\lucene\\ClaferConfigurator\\resultSummary.csv",
                "C:\\fanas\\2\\lucene\\ClaferIDE\\resultSummary.csv",
                "C:\\fanas\\2\\lucene\\ClaferMooVisualizer\\resultSummary.csv",
                "C:\\fanas\\2\\lucene\\ClaferToolsUICommonPlatform\\resultSummary.csv",
                "C:\\fanas\\2\\lucene\\marlin\\resultSummary.csv"
        };
        String[] projects = new String[]{"ClaferConfigurator", "ClaferIDE", "ClaferMooVisualizer", "ClaferToolsUICommonPlatform", "marlin"};
        String[] shortNames = new String[]{"config", "ide", "viz", "tools", "marlin"};
        String targetFile = "C:\\exp\\luceneCombinedSummary.csv";
        File tFile = new File(targetFile);
        if (tFile.exists()) {
            FileUtils.forceDelete(tFile);
        }
        PrintWriter writer = new PrintWriter(new FileWriter(tFile, true));
        System.out.println("COMBINED LUCENE DATA");
        try {
            //header
            writer.println("project;feature;mappedAssets;foundAssets;macthedAssets;precision;recall;fscore;commitIndex;commit");
            for (int p = 0; p < projectFiles.length; p++) {
                System.out.println(projectFiles[p]);
                List<String> fileLines = FileUtils.readLines(new File(projectFiles[p]), "UTF-8");
                for (int i = 1; i < fileLines.size(); i++) {
                    String[] parts = fileLines.get(i).split(";");
                    String project = shortNames[p], feature = parts[0], mappedAssets = parts[1], foundAssets = parts[2], macthedAssets = parts[3], precision = parts[4], recall = parts[5], fscore = parts[6], commitIndex = parts[7], commit = parts[8];
                    precision = precision.trim().equalsIgnoreCase("NaN") ? "0" : precision;
                    recall = recall.trim().equalsIgnoreCase("NaN") ? "0" : recall;
                    fscore = fscore.trim().equalsIgnoreCase("NaN") ? "0" : fscore;
                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s\n", project, feature, mappedAssets, foundAssets, macthedAssets, precision, recall, fscore, commitIndex, commit);

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            writer.close();
        }
    }

    public static void combineMeasuresLuceneData() throws IOException {
        String[] projectFiles = new String[]{"C:\\fanas\\2\\lucene\\ClaferConfigurator\\resultSummary.csv",
                "C:\\fanas\\2\\lucene\\ClaferIDE\\resultSummary.csv",
                "C:\\fanas\\2\\lucene\\ClaferMooVisualizer\\resultSummary.csv",
                "C:\\fanas\\2\\lucene\\ClaferToolsUICommonPlatform\\resultSummary.csv",
                "C:\\fanas\\2\\lucene\\marlin\\resultSummary.csv"
        };
        String[] projects = new String[]{"ClaferConfigurator", "ClaferIDE", "ClaferMooVisualizer", "ClaferToolsUICommonPlatform", "marlin"};
        String[] shortNames = new String[]{"config", "ide", "viz", "tools", "marlin"};
        String targetFile = "C:\\exp\\luceneCombinedMeasuresSummary.csv";
        File tFile = new File(targetFile);
        if (tFile.exists()) {
            FileUtils.forceDelete(tFile);
        }
        PrintWriter writer = new PrintWriter(new FileWriter(tFile, true));
        System.out.println("COMBINED MEASURES LUCENE DATA");
        try {
            //header
            writer.println("project;feature;measure;measureValue;commitIndex;commit");
            for (int p = 0; p < projectFiles.length; p++) {
                List<String> fileLines = FileUtils.readLines(new File(projectFiles[p]), "UTF-8");
                System.out.println(projectFiles[p]);
                for (int i = 1; i < fileLines.size(); i++) {
                    String[] parts = fileLines.get(i).split(";");
                    String project = shortNames[p], feature = parts[0], precision = parts[4], recall = parts[5], fscore = parts[6], commitIndex = parts[7], commit = parts[8];
                    precision = precision.trim().equalsIgnoreCase("NaN") ? "0" : precision;
                    recall = recall.trim().equalsIgnoreCase("NaN") ? "0" : recall;
                    fscore = fscore.trim().equalsIgnoreCase("NaN") ? "0" : fscore;
                    writer.printf("%s;%s;precision;%s;%s;%s\n", project, feature, precision, commitIndex, commit);
                    writer.printf("%s;%s;recall;%s;%s;%s\n", project, feature, recall, commitIndex, commit);
                    writer.printf("%s;%s;fscore;%s;%s;%s\n", project, feature, fscore, commitIndex, commit);

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            writer.close();
        }
    }
}
