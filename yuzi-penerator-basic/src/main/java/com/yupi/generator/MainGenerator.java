package com.yupi.generator;

import com.yupi.model.MainTemplateConfig;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;

public class MainGenerator {
    public static void main(String[] args) throws TemplateException, IOException {

        //1.静态文件生成
        String projectPath = System.getProperty("user.dir");
        System.out.println(projectPath);
        //输入路径
        String inputPath=projectPath+ File.separator+"yuzi-genreator-demo-projects"+File.separator+"acm-template";
        System.out.println(inputPath);
        //输出路径
        String outputPath=projectPath;
        //复制
        StaticGenerator.copyFilesByRecursive(inputPath,outputPath);

        //2.动态文件生成
        String dynamicInputPath = projectPath + File.separator+"yuzi-penerator-basic"+File.separator + "src/main/resources/templates/MainTemplate.java.ftl";
        String dynamicOutputPath = projectPath + File.separator + "acm-template/src/com/yupi/acm/MainTemplate.java";

        MainTemplateConfig mainTemplateConfig = new MainTemplateConfig();
        mainTemplateConfig.setAuthor("yupi");
        mainTemplateConfig.setLoop(true);
        mainTemplateConfig.setOutputText("我输出了：");
        DynamicGenerator.doGenerate(dynamicInputPath, dynamicOutputPath, mainTemplateConfig);

    }
}
