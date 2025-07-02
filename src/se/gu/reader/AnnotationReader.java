package se.gu.reader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.assets.AnnotationType;
import se.gu.assets.Asset;
import se.gu.assets.ProjectAnnotationType;
import se.gu.git.DiffEntry;
import se.gu.main.FeatureAssetMapper;
import se.gu.main.ProjectData;
import se.gu.main.ProjectReader;
import se.gu.ml.preprocessing.NestingDepthPair;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnnotationReader implements Serializable {
    private static final long serialVersionUID = -4402878113097122975L;
    private Pattern oneLineAnnotation, fragmentBeginning, fragmentEnd;
    private List<AnnotationPair> annotationPairs;
    private ProjectData projectData;
    private Asset fileAsset;
    private ProjectAnnotationType projectAnnotationType;

    private String currentCommit;
    private File diffFilesDirectory;
    private List<String> fileLines;
    FeatureAssetMapper assetMapper;

    public AnnotationReader(ProjectData projectData, Asset fileAsset, List<String> lines) {
        this.projectData = projectData;
        this.fileAsset = fileAsset;
        this.fileLines = lines;
//        this.currentCommit = commit;
//        this.diffFilesDirectory = diffFilesDirectory;
        projectAnnotationType = projectData.getConfiguration().getProjectAnnotationType();
        assetMapper = new FeatureAssetMapper(projectData);
    }

    public List<AnnotationPair> readFragmentAnnotations() throws IOException, SQLException {
        if (projectAnnotationType == ProjectAnnotationType.EMBEDDED_ANNOTATION) {
            oneLineAnnotation = Pattern.compile(projectData.getConfiguration().getLineAnnotation());
        }
        fragmentBeginning = Pattern.compile(projectAnnotationType == ProjectAnnotationType.EMBEDDED_ANNOTATION ? projectData.getConfiguration().getFragmentAnnotationBegin() : projectData.getConfiguration().getIfDefBegin());
        fragmentEnd = Pattern.compile(projectAnnotationType == ProjectAnnotationType.EMBEDDED_ANNOTATION ? projectData.getConfiguration().getFragmentAnnotationEnd() : projectData.getConfiguration().getIfDefEnd());


        annotationPairs = new ArrayList<>();
        Stack<Asset> ifdefpairs = new Stack<>();
        String previousAnnotation = "";
        String foundAnnotation;
        int nestingDepth = 0;

//        for (CodeChange codeChange : diffEntry.getCodeChanges()) {
//            int addedLinesEnd = codeChange.getAddedLinesStart() + codeChange.getAddedLines();
        //List<Asset> addedLinesOfCode = fileAsset.getLinesOfCode();//.stream().filter(l -> l.getLineNumber() >= codeChange.getAddedLinesStart() && l.getLineNumber() <= addedLinesEnd).collect(Collectors.toList());
        Matcher oneLineAnnotationMatcher;
        Matcher fragmentBeginningMatcher;
        Matcher fragmentEndMatcher;
        //if (projectData.getConfiguration().isCalculateMetrics()||projectData.getConfiguration().isGetAnnotationsWithoutCommits()) {
            //lines of code
            //File file = new File(fileAsset.getFullyQualifiedName());
            //List<String> fileLines = FileUtils.readLines(file, projectData.getConfiguration().getTextEncoding());
            int linesCount = fileLines.size();
            for (int line = 0; line < linesCount; line++) {
                fragmentBeginningMatcher = fragmentBeginning.matcher(fileLines.get(line));
                fragmentEndMatcher = fragmentEnd.matcher(fileLines.get(line));
                if (projectAnnotationType == ProjectAnnotationType.EMBEDDED_ANNOTATION) {
                    oneLineAnnotationMatcher = oneLineAnnotation.matcher(fileLines.get(line));

                    if (oneLineAnnotationMatcher.find()) {
                        foundAnnotation = oneLineAnnotationMatcher.group(0).trim();
                        //if matahced line annotation is before annotated line
                        AnnotationPair annotationPair = null;
                        String featureName = Utilities.getFeatureFromAnnotation(foundAnnotation,projectData.getConfiguration());
                        if(projectData.getConfiguration().isAnnotatedLineIsImmediatelyAfterLineAnnotation()){
                            annotationPairs.add(annotationPair = new AnnotationPair(line+2, line+2, featureName, AnnotationType.LINE));
                            assetMapper.mapFragment(fileAsset,fileLines,annotationPair,featureName);
                        }else {
                            annotationPairs.add(annotationPair = new AnnotationPair(line+1, line+1, featureName, AnnotationType.LINE));;
                            assetMapper.mapFragment(fileAsset,fileLines,annotationPair,featureName);
                        }

                    } else if (fragmentBeginningMatcher.find()) {
                        foundAnnotation = fragmentBeginningMatcher.group(0).trim();
                        annotationPairs.add(new AnnotationPair(line+1, -1, Utilities.getFeatureFromAnnotation(foundAnnotation,projectData.getConfiguration()), AnnotationType.FRAGMENT));
                    } else if (fragmentEndMatcher.find()) {
                        foundAnnotation = fragmentEndMatcher.group(0).trim();
                        String featureName = Utilities.getFeatureFromAnnotation(foundAnnotation,projectData.getConfiguration());
                        Optional<AnnotationPair> matchingBeginPair = annotationPairs.stream().filter(ap -> ap.getFeatureName().equalsIgnoreCase(featureName) && ap.getAnnotationType() == AnnotationType.FRAGMENT && ap.getEndLine() == -1).findFirst();
                        if (matchingBeginPair.isPresent()) {
                            matchingBeginPair.get().setEndLine(line+1);
                            assetMapper.mapFragment(fileAsset,fileLines,matchingBeginPair.get(),featureName);
                        }
                    }

                } else {
                    if (fragmentBeginningMatcher.find()) {
                        foundAnnotation = fragmentBeginningMatcher.group(0).trim();
                        //&begin [Metrics]
                        if (ifdefpairs.empty()) {
                            nestingDepth = 0;
                        }
                        //&end [Metrics]
                        ifdefpairs.push(new Asset(fileLines.get(line)+1, line+1));
                        previousAnnotation = foundAnnotation;

                    } else if (fragmentEndMatcher.find()) {
                        if (!previousAnnotation.toLowerCase().contains("else")) {
                            if (!ifdefpairs.empty()) {

                                //&begin [Metrics]
                                if (ifdefpairs.size() > 1) {
                                    nestingDepth++;
                                }
                                //&end [Metrics]
                                Asset lineWithFeature = ifdefpairs.pop();
                                String foundFeature = Utilities.getFeatureFromAnnotation(lineWithFeature.getAssetContent(),projectData.getConfiguration());
                                if (foundFeature != null) {
                                    //&begin [Metrics]
                                    if (ifdefpairs.empty()) {
                                        if(!projectData.getConfiguration().isGetAnnotationsWithoutCommits()) {
                                            projectData.getNestingDepthPairs().add(new NestingDepthPair(fileAsset.getFullyQualifiedName(), foundFeature, nestingDepth));
                                        }
                                    }
                                    //&end [Metrics]
                                    AnnotationPair annotationPair = null;
                                    annotationPairs.add(annotationPair = new AnnotationPair(lineWithFeature.getLineNumber(), line, foundFeature, AnnotationType.FRAGMENT));
                                    assetMapper.mapFragment(fileAsset,fileLines,annotationPair,foundFeature);
                                }

                            }
                        }

                    }
                }
            }


        //}

//        else {
//            int lineIndex = 0;
//            for (Asset loc : addedLinesOfCode) {
//
//                fragmentBeginningMatcher = fragmentBeginning.matcher(loc.getAssetContent());
//                fragmentEndMatcher = fragmentEnd.matcher(loc.getAssetContent());
//                if (projectAnnotationType == ProjectAnnotationType.EMBEDDED_ANNOTATION) {
//                    oneLineAnnotationMatcher = oneLineAnnotation.matcher(loc.getAssetContent());
//
//                    if (oneLineAnnotationMatcher.find()) {
//                        foundAnnotation = oneLineAnnotationMatcher.group(0).trim();
//                        //if matahced line annotation is before annotated line
//                        if(projectData.getConfiguration().isAnnotatedLineIsImmediatelyAfterLineAnnotation()){
//                            annotationPairs.add(new AnnotationPair(addedLinesOfCode.get(lineIndex+1).getLineNumber(), addedLinesOfCode.get(lineIndex+1).getLineNumber(), getFeatureFromAnnotation(foundAnnotation), AnnotationType.LINE));
//                        }else {
//                            annotationPairs.add(new AnnotationPair(loc.getLineNumber(), loc.getLineNumber(), getFeatureFromAnnotation(foundAnnotation), AnnotationType.LINE));
//                        }
//
//                    } else if (fragmentBeginningMatcher.find()) {
//                        foundAnnotation = fragmentBeginningMatcher.group(0).trim();
//                        annotationPairs.add(new AnnotationPair(addedLinesOfCode.get(lineIndex+1).getLineNumber(), -1, getFeatureFromAnnotation(foundAnnotation), AnnotationType.FRAGMENT));
//                    } else if (fragmentEndMatcher.find()) {
//                        foundAnnotation = fragmentEndMatcher.group(0).trim();
//                        String featureName = getFeatureFromAnnotation(foundAnnotation);
//                        Optional<AnnotationPair> matchingBeginPair = annotationPairs.stream().filter(ap -> ap.getFeatureName().equalsIgnoreCase(featureName) && ap.getAnnotationType() == AnnotationType.FRAGMENT && ap.getEndLine() == -1).findFirst();
//                        if (matchingBeginPair.isPresent()) {
//                            matchingBeginPair.get().setEndLine(addedLinesOfCode.get(lineIndex-1).getLineNumber());
//                        }
//                    }
//
//                } else {
//                    if (fragmentBeginningMatcher.find()) {
//                        foundAnnotation = fragmentBeginningMatcher.group(0).trim();
//                        //&begin [Metrics]
//                        if (ifdefpairs.empty()) {
//                            nestingDepth = 0;
//                        }
//                        //&end [Metrics]
//                        ifdefpairs.push(loc);
//                        previousAnnotation = foundAnnotation;
//
//                    } else if (fragmentEndMatcher.find()) {
//                        if (!previousAnnotation.toLowerCase().contains("else")) {
//                            if (!ifdefpairs.empty()) {
//
//                                //&begin [Metrics]
//                                if (ifdefpairs.size() > 1) {
//                                    nestingDepth++;
//                                }
//                                //&end [Metrics]
//                                Asset lineWithFeature = ifdefpairs.pop();
//                                String foundFeature = getFeatureFromAnnotation(lineWithFeature.getAssetContent());
//                                if (foundFeature != null) {
//                                    //&begin [Metrics]
//                                    if (ifdefpairs.empty()) {
//                                        projectData.getNestingDepthPairs().add(new NestingDepthPair(fileAsset.getFullyQualifiedName(), foundFeature, nestingDepth));
//                                    }
//                                    //&end [Metrics]
//                                    annotationPairs.add(new AnnotationPair(lineWithFeature.getLineNumber(), loc.getLineNumber(), foundFeature, AnnotationType.FRAGMENT));
//                                }
//
//                            }
//                        }
//
//                    }
//                }
//                lineIndex++;//
//            }
//        }


//        if (annotationPairs.size() > 0 && projectData.getConfiguration().isPrintAssetMappings()) {
//            String text = getBufferedString();
////            System.out.printf("\nPRINTING ANNOTATIONS in file: %s\n", fileAsset.getFullyQualifiedName());
////            System.out.println(text);
////            System.out.println("==============");
//
//            Utilities.writeStringFile(String.format("%s\\%s-ANNOT.txt", diffFilesDirectory.getAbsolutePath(), currentCommit), text);
//        }

        return annotationPairs;
    }

    public String getBufferedString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (AnnotationPair inFileAnnotation : annotationPairs) {
            stringBuilder.append(inFileAnnotation.toString());
            stringBuilder.append(System.lineSeparator());
        }
        return stringBuilder.toString();
    }






}


