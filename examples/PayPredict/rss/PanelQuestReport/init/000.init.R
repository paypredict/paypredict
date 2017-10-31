memory.limit(size=16384)

dir_env = "D:/DATA/Renvir"

if (!exists("envir_rmd")) {
    envir_rmd <- readRDS(file.path(dir_env, "env_rmd.Rdata"))
    attach(envir_rmd)
}