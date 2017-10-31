Sys.setenv(RSTUDIO_PANDOC="C:/Program Files/RStudio/bin/pandoc")

library(rmarkdown)

render_site(report.site)

"index.html"