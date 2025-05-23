package com.yupi.command;

import cn.hutool.core.io.FileUtil;
import picocli.CommandLine;

import java.io.File;
import java.util.List;


@CommandLine.Command(name = "list" ,mixinStandardHelpOptions = true)
public class ListCommand implements Runnable {

    @Override
    public void run() {
        String projectPath=System.getProperty("user.dir");
        //整个项目的跟路径
        File parantFile=new File(projectPath).getParentFile();
        //输入路径
        String inputPath=new File(parantFile,"yuzi-generator-demo-projects/acm-template").getAbsolutePath();
        List<File> files= FileUtil.loopFiles(inputPath);
        for (File file : files) {
            System.out.println(file);
        }
    }
}
