package com.yupi.maker.generator;

import java.io.File;
import java.io.IOException;

public class gitGenerator {

    public static void doGenerator(String projectDir) throws IOException {
        //调用Process类执行maven打包命令
        String gitCommand="git init";
        ProcessBuilder processBuilder=new ProcessBuilder(gitCommand.split(" "));
        processBuilder.directory(new File(projectDir));
        Process process=processBuilder.start();
    }

}
