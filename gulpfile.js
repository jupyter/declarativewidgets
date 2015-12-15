/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
var gulp = require('gulp'),
    watch = require('gulp-watch'),
    run = require('gulp-run');

gulp.task('watch', function () {
    watch(['elements/**/*',
           'kernel-python/**/*',
           'kernel-scala/**/*',
           'nb-extension/**/*'], function(){
        run('make dist').exec();
    });
});

gulp.task('watch-docs', function () {
    watch(['elements/**/*',
           'etc/docs/**/*'], function(){
        run('make dist/docs').exec();
    });
});
