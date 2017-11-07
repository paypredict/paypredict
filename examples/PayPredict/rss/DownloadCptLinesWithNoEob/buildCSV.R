csvFileName = paste(ex_payer, "payer.csv", sep="-")
write.csv(data.frame(c=c("c1"), p=c(ex_payer)), file = file.path(ex_dir_output, csvFileName))
csvFileName