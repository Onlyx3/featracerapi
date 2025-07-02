library(readr)
library(dplyr)
library(ggplot2)
library(scales)
#library(ggpubr)

#predictions for IMBALANCED data
projects <- c("viz","ide","config","tools","marlin")
balanced <- c("b","im")
codeLevels <- c("loc","fragment","file","folder")
for(project in projects){
for(balance in balanced){
for(codeLevel in codeLevels){
  print(codeLevel)
  ###PREDICTIONS
  print(paste0(project,"_",codeLevel,"_",balance,"_PEDICTIONS"))
  f <- paste0("c:/exp/",project,"_",codeLevel,"_ps_",balance,".csv")
  if(file.exists(f)){
    locps <- read.csv(file = f,header = T,sep = ";")
    
    locps <- data.frame(locps,row.names = NULL)
    
    #glimpse(locps)
    
    #AVERAGE PRECISION (Prediction)
    ggplot(locps,aes(x=commit,y=AveragePrecisionForAllNonNullPrecision,group=Classifier))+
      geom_boxplot(aes(x = Classifier))+
      labs(y=paste0("precision"), x="")
    
    ggsave(paste0("c:/exp/plots/",project,"_",codeLevel,"_",balance,"_predict_boxplot.pdf"), width = 7,height = 3)
    
    ggplot(data=locps,aes(x=commit,y=AveragePrecisionForAllNonNullPrecision,colour=Classifier))+
      geom_line(show.legend = FALSE)+
      facet_wrap(.~ Classifier)+
      labs(y=paste0("precision"))
    
    ggsave(paste0("c:/exp/plots/",project,"_",codeLevel,"_",balance,"_predict_lineplot.pdf"), width = 7,height = 4)
  }
#cross validation plots

###CROSS VALIDATION: only plot balanced cross validation
  print(paste0(project,"_",codeLevel,"_",balance,"_CROSS VALIDATION"))
  
  f <- paste0("c:/exp/",project,"_",codeLevel,"_cv_",balance,".csv")
  if(file.exists(f)){
cv <- read.csv(file = f,header = T,sep = ";")
cv <- data.frame(cv,row.names = NULL)

#glimpse(cv)

#subset accuracy


ggplot(cv,aes(x=commit,y=SubsetAccuracy,group=Classifier))+
  geom_boxplot(aes(x = Classifier))+
  labs(y=paste0("subset accuracy"),x="")

ggsave(paste0("c:/exp/plots/",project,"_",codeLevel,"_b_cv_boxplot_SA.pdf"), width = 7,height = 3)

#hamming loss
ggplot(cv,aes(x=commit,y=HammingLoss,group=Classifier))+
  geom_boxplot(aes(x = Classifier))+
  labs(y=paste0("hamming loss"),x="")

ggsave(paste0("c:/exp/plots/",project,"_",codeLevel,"_b_cv_boxplot_HL.pdf"), width = 7,height = 3)

#Subset accuracy line plot

ggplot(data=cv,aes(x=commit,y=SubsetAccuracy))+
  geom_line()+
  facet_wrap(.~ Classifier)+
  labs(y=paste0("subset accuracy",x=""))

ggsave(paste0("c:/exp/plots/",project,"_",codeLevel,"_b_cv_lineplot_SA.pdf"), width = 7,height = 4)
  }
  
###DTASETS STATS
  print(paste0(project,"_",codeLevel,"_",balance,"_DATASETSTATS"))
  f <- paste0("c:/exp/",project,"_",codeLevel,"_datasetstats_",balance,".csv")
  if(file.exists(f)){
locps <- read.csv(file = f,header = T,sep = ";")

locps <- data.frame(locps,row.names = NULL)

#glimpse(locps)


a <- ggplot(data=locps,aes(x=commit,y=Scumble))+
  geom_line()+
  labs(title=paste0(codeLevel," scumble per commit"))

#ggsave("c:/exp/plots/loc_scumble_lineplot.pdf")


b <- ggplot(data=locps,aes(x=commit,y=MeanIR))+
  geom_line()+
  labs(title = paste0(codeLevel," mean imbalance ratio per commit"))
}
#figure <- ggarrange(a,b,
                    
#                    ncol=1,nrows=2)


#ggexport(figure,filename=paste0("c:/exp/plots/",codeLevel,"_",balance,"_datasetstats_lineplot.pdf"))
}
 #figure <- ggarrange(a,b,
                    
#                    ncol=1,nrows=2)


#ggexport(figure,filename=paste0("c:/exp/plots/combined_",balance,"_datasetstats_lineplot.pdf"))
}#end loop for datasetType
}#end of project loop


###COMBINED STATS
#DatasetStats
for(balance in balanced){
print(paste0("PRINT COMBINED DATASET STATS"))
f <- paste0("c:/exp/combined_datasetstats_",balance,".csv")
if(file.exists(f)){
  locps <- read.csv(file = f,header = T,sep = ";")
  
  locps <- data.frame(locps,row.names = NULL)
  
  #glimpse(locps)
  
  
  a <- ggplot(data=locps,aes(x=commit,y=Scumble, colour=Level))+
    geom_line()+
    facet_wrap(.~ project)
  #labs(title="scumble per commit")
  
  ggsave(paste0("c:/exp/plots/combined_",balance,"_scumble_lineplot.pdf"),width = 7, height = 4)
  
  ggplot(locps,aes(x=commit,y=Scumble,group=Level, colour=project))+
    geom_boxplot(aes(x = Level))+
    labs(y=paste0("scumble across all commits"))
  ggsave(paste0("c:/exp/plots/combined_",balance,"_scumble_boxplot.pdf"),width = 7, height = 4)
  
  b <- ggplot(data=locps,aes(x=commit,y=MeanIR, colour=Level))+
    geom_line()+
    facet_wrap(.~ project)
  #labs(title = "mean imbalance ratio per commit")
  
  ggsave(paste0("c:/exp/plots/combined_",balance,"_meanIR_lineplot.pdf"),width = 7, height = 4)
  
  ggplot(locps,aes(x=commit,y=MeanIR,group=Level, colour=project))+
    geom_boxplot(aes(x = Level))+
    labs(y=paste0("MeanIR across all commits"))
  
  ggsave(paste0("c:/exp/plots/combined_",balance,"_meanIR_boxplot.pdf"),width = 7, height = 4)
}
}
