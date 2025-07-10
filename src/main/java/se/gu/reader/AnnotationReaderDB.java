package se.gu.reader;

import org.repodriller.domain.DiffLine;
import se.gu.assets.AnnotationType;
import se.gu.assets.Asset;
import se.gu.assets.ProjectAnnotationType;
import se.gu.main.ProjectData;
import se.gu.utils.Utilities;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotationReaderDB {
    private Pattern oneLineAnnotation, fragmentBeginning, fragmentEnd;
    private List<AnnotationPair> annotationPairs;
    private ProjectData projectData;
    private ProjectAnnotationType projectAnnotationType;


    public AnnotationReaderDB(ProjectData projectData) {
        this.projectData = projectData;
        projectAnnotationType = projectData.getConfiguration().getProjectAnnotationType();

    }

    public List<AnnotationPair> readFragmentAnnotationsFromEntireFile(List<String> fileLines) throws IOException, SQLException {
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

        Matcher oneLineAnnotationMatcher;
        Matcher fragmentBeginningMatcher;
        Matcher fragmentEndMatcher;

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

                    }else {
                        annotationPairs.add(annotationPair = new AnnotationPair(line+1, line+1, featureName, AnnotationType.LINE));;

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

                    }
                }

            } else {
                if (fragmentBeginningMatcher.find()) {
                    foundAnnotation = fragmentBeginningMatcher.group(0).trim();

                    ifdefpairs.push(new Asset(fileLines.get(line)+1, line+1));
                    previousAnnotation = foundAnnotation;

                } else if (fragmentEndMatcher.find()) {
                    if (!previousAnnotation.toLowerCase().contains("else")) {
                        if (!ifdefpairs.empty()) {


                            Asset lineWithFeature = ifdefpairs.pop();
                            String foundFeature = Utilities.getFeatureFromAnnotation(lineWithFeature.getAssetContent(),projectData.getConfiguration());
                            if (foundFeature != null) {

                                AnnotationPair annotationPair = null;
                                annotationPairs.add(annotationPair = new AnnotationPair(lineWithFeature.getLineNumber(), line, foundFeature, AnnotationType.FRAGMENT));

                            }

                        }
                    }

                }
            }
        }




        return annotationPairs;
    }

    public List<AnnotationPair> readFragmentAnnotationsFromCombinedDiffBlocks(List<DiffLine> newDiffLines) throws IOException, SQLException {
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

        Matcher oneLineAnnotationMatcher;
        Matcher fragmentBeginningMatcher;
        Matcher fragmentEndMatcher;


        for (DiffLine diffLine:newDiffLines) {
            fragmentBeginningMatcher = fragmentBeginning.matcher(diffLine.getLine());
            fragmentEndMatcher = fragmentEnd.matcher(diffLine.getLine());
            if (projectAnnotationType == ProjectAnnotationType.EMBEDDED_ANNOTATION) {
                oneLineAnnotationMatcher = oneLineAnnotation.matcher(diffLine.getLine());

                if (oneLineAnnotationMatcher.find()) {
                    foundAnnotation = oneLineAnnotationMatcher.group(0).trim();
                    //if matahced line annotation is before annotated line
                    AnnotationPair annotationPair = null;
                    String featureName = Utilities.getFeatureFromAnnotation(foundAnnotation,projectData.getConfiguration());
                    if(projectData.getConfiguration().isAnnotatedLineIsImmediatelyAfterLineAnnotation()){
                        annotationPairs.add(annotationPair = new AnnotationPair(diffLine.getLineNumber()+1, diffLine.getLineNumber()+1, featureName, AnnotationType.LINE));

                    }else {
                        annotationPairs.add(annotationPair = new AnnotationPair(diffLine.getLineNumber(), diffLine.getLineNumber(), featureName, AnnotationType.LINE));;

                    }

                } else if (fragmentBeginningMatcher.find()) {
                    foundAnnotation = fragmentBeginningMatcher.group(0).trim();
                    annotationPairs.add(new AnnotationPair(diffLine.getLineNumber(), -1, Utilities.getFeatureFromAnnotation(foundAnnotation,projectData.getConfiguration()), AnnotationType.FRAGMENT));
                } else if (fragmentEndMatcher.find()) {
                    foundAnnotation = fragmentEndMatcher.group(0).trim();
                    String featureName = Utilities.getFeatureFromAnnotation(foundAnnotation,projectData.getConfiguration());
                    Optional<AnnotationPair> matchingBeginPair = annotationPairs.stream().filter(ap -> ap.getFeatureName().equalsIgnoreCase(featureName) && ap.getAnnotationType() == AnnotationType.FRAGMENT && ap.getEndLine() == -1).findFirst();
                    if (matchingBeginPair.isPresent()) {
                        matchingBeginPair.get().setEndLine(diffLine.getLineNumber());

                    }
                }

            } else {
                if (fragmentBeginningMatcher.find()) {
                    foundAnnotation = fragmentBeginningMatcher.group(0).trim();

                    ifdefpairs.push(new Asset(diffLine.getLine(), diffLine.getLineNumber()));
                    previousAnnotation = foundAnnotation;

                } else if (fragmentEndMatcher.find()) {
                    if (!previousAnnotation.toLowerCase().contains("else")) {
                        if (!ifdefpairs.empty()) {


                            Asset lineWithFeature = ifdefpairs.pop();
                            String foundFeature = Utilities.getFeatureFromAnnotation(lineWithFeature.getAssetContent(),projectData.getConfiguration());
                            if (foundFeature != null) {

                                AnnotationPair annotationPair = null;
                                annotationPairs.add(annotationPair = new AnnotationPair(lineWithFeature.getLineNumber(), diffLine.getLineNumber(), foundFeature, AnnotationType.FRAGMENT));

                            }

                        }
                    }

                }
            }
        }




        return annotationPairs;
    }
}
