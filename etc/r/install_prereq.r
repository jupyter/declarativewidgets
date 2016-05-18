temp_libPaths <- .libPaths()
.libPaths(temp_libPaths[2])
install.packages("testthat", repos='http://cran.us.r-project.org', quiet = TRUE)