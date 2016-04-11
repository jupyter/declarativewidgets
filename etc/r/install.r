#   Installer to replace the master IRkernel with custom IRkernel in git repo
#
#   To install the widgets package the options are:
#   1) install from source location
#       R CMD INSTALL .
#   2) package as tgz and install from tgz build/package
#       R CMD INSTALL --build .
#       R CMD INSTALL widgets_0.1.tgz
#

temp_libPaths <- .libPaths()
.libPaths(temp_libPaths[2])
install.packages("devtools", repos='http://cran.us.r-project.org')
library(devtools)
remove.packages("repr")
remove.packages("IRdisplay")
remove.packages("IRkernel")
install_git("https://github.com/IRkernel/repr.git")
install_git("https://github.com/IRkernel/IRdisplay.git")
install_git("https://github.com/IRkernel/IRkernel.git")