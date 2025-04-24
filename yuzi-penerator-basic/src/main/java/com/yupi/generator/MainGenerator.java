package com.yupi.generator;

import com.yupi.model.MainTemplateConfig;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;

/**
 * 核心生成器
 */
public class MainGenerator {

    /**
     * 生成
     *
     * @param model 数据模型
     * @throws TemplateException
     * @throws IOException
     */
    public static void doGenerate(Object model) throws TemplateException, IOException {

        String inputRootPath="E:\\code\\demo-generator\\yuzi-generator-demo-projects\\acm-template-pro";
        String outputRootPath="E:\\code\\demo-generator";
        String inputPath;
        String outputPath;

        inputPath= new File(inputRootPath,"src/com/yupi/acm/MainTemplate.java.ftl").getAbsolutePath();
        outputPath=new File(outputRootPath,"src/com/yupi/acm/MainTemplate.java").getAbsolutePath();
        DynamicGenerator.doGenerate(inputPath, outputPath, model);

        inputPath = new File(inputRootPath, ".gitignore").getAbsolutePath();
        outputPath = new File(outputRootPath,".gitignore").getAbsolutePath();
        // 生成静态文件
        StaticGenerator.copyFilesByHutool(inputPath, outputPath);

        inputPath = new File(inputRootPath, "README.md").getAbsolutePath();
        outputPath = new File(outputRootPath,"README.md").getAbsolutePath();
        // 生成静态文件
        StaticGenerator.copyFilesByHutool(inputPath, outputPath);




    }

    public static void main(String[] args) throws TemplateException, IOException {
        MainTemplateConfig mainTemplateConfig = new MainTemplateConfig();
        mainTemplateConfig.setAuthor("yupi");
        mainTemplateConfig.setLoop(false);
        mainTemplateConfig.setOutputText("求和结果：");
        doGenerate(mainTemplateConfig);
    }
}




