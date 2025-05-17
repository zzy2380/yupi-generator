package com.yupi.maker.generator.main;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.yupi.maker.generator.JarGenerator;
import com.yupi.maker.generator.ScriptGenerator;
import com.yupi.maker.generator.file.DynamicFileGenerator;
import com.yupi.maker.generator.gitGenerator;
import com.yupi.maker.meta.Meta;
import com.yupi.maker.meta.MetaManager;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;

public abstract class GenerateTemplate {

    public void doGenerator() throws TemplateException, IOException, InterruptedException {
        Meta meta = MetaManager.getMetaObject();
        System.out.println(meta);
        //输出根路径
        String projectPath =System.getProperty("user.dir");
        String outputPath = projectPath+ File.separator+"generated/"+File.separator+meta.getName();

        doGenerator(meta,outputPath);
    }


    /**
     * 生成
     * @throws TemplateException
     * @throws IOException
     * @throws InterruptedException
     */
    public void doGenerator(Meta meta,String outputPath) throws TemplateException, IOException, InterruptedException {
        if(!FileUtil.exist(outputPath)){
            FileUtil.mkdir(outputPath);
        }

        //1复制原始文件
        String sourceCopyDestPath = copySource(meta, outputPath);
        //2.代码生成
        generateCode(meta, outputPath);
        //3.构建jar包和将代码交给git托管
        String jarPath=buildJarAndGit(outputPath,meta);
        //4.封装脚本
        String shellOutputFilePath= buildScript(outputPath, jarPath);
        //5.生成精简版的程序（产物包）
        buildDist(outputPath,sourceCopyDestPath,jarPath,shellOutputFilePath);
    }

    protected  String buildDist(String outputPath,String sourceCopyDestPath,String jarPath,String shellOutputFilePath) {
        String distOutputPath = outputPath +"-dist";
        //- 拷贝jar包
        String targetAbsolutePath=distOutputPath+File.separator+"target";
        FileUtil.mkdir(targetAbsolutePath);
        String jarAbsolutePath= outputPath +File.separator+ jarPath;
        FileUtil.copy(jarAbsolutePath,targetAbsolutePath,true);

        //- 拷贝脚本
        FileUtil.copy(shellOutputFilePath,distOutputPath,true);
        FileUtil.copy(shellOutputFilePath +".bat",distOutputPath,true);

        //- 拷贝原模版文件
        FileUtil.copy(sourceCopyDestPath,distOutputPath,true);
        return distOutputPath;
    }

    protected  String buildScript(String outputPath, String jarPath) {
        String shellOutputFilePath= outputPath +File.separator+"generator";
        ScriptGenerator.doGenerate(shellOutputFilePath,jarPath);
        return shellOutputFilePath;
    }

    protected  String buildJarAndGit(String outputPath,Meta meta) throws InterruptedException, IOException {
        //构建jar包
        JarGenerator.doGenerator(outputPath);
        //执行git init命令
        gitGenerator.doGenerator(outputPath);
        String jarName=String.format("%s-%s-jar-with-dependencies.jar", meta.getName(), meta.getVersion());
        String jarPath="target/"+jarName;
        return jarPath;
    }

    /**
     * 复制原始文件
     * @param meta
     * @param outputPath
     * @throws IOException
     * @throws TemplateException
     */
    protected  void generateCode(Meta meta, String outputPath) throws IOException, TemplateException {
        //读取resources目录
        String inputResourcesPath= "";

        //java包的基础路径
        //com.yupi
        String outputBasePackage= meta.getBasePackage();
        //com/yupi
        String outputBasePackagePath= StrUtil.join("/",StrUtil.split(outputBasePackage,"."));
        //generated/src/main/java/com/yupi/xxx
        String outputBaseJavaPackagePath= outputPath +File.separator+"src/main/java/"+outputBasePackagePath;

        String inputFilePath;
        String outputFilePath;
        //model.DataModel
        inputFilePath=inputResourcesPath+File.separator+"templates/java/model/DataModel.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/model/DataModel.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //generator.MainGenerator
        inputFilePath=inputResourcesPath+File.separator+"templates/java/generator/MainGenerator.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/generator/MainGenerator.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //cli.command.generatorCommand
        inputFilePath=inputResourcesPath+File.separator+ "templates/java/cli/command/GenerateCommand.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/cli/command/GenerateCommand.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //cli.command.JsonGeneratorCommand
        inputFilePath=inputResourcesPath+File.separator+ "templates/java/cli/command/JsonGenerateCommand.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/cli/command/JsonGenerateCommand.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //cli.command.ListCommand
        inputFilePath=inputResourcesPath+File.separator+"templates/java/cli/command/ListCommand.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/cli/command/ListCommand.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //cli.command.ConfigCommand
        inputFilePath=inputResourcesPath+File.separator+"templates/java/cli/command/ConfigCommand.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/cli/command/ConfigCommand.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //cli.CommandExecutor
        inputFilePath=inputResourcesPath+File.separator+"templates/java/cli/CommandExecutor.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/cli/CommandExecutor.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //java.Main
        inputFilePath=inputResourcesPath+File.separator+"templates/java/Main.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/Main.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //generator.DynamicGenerator
        inputFilePath=inputResourcesPath+File.separator+"templates/java/generator/DynamicGenerator.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/generator/DynamicGenerator.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //generator.StaticGenerator
        inputFilePath=inputResourcesPath+File.separator+"templates/java/generator/StaticGenerator.java.ftl";
        outputFilePath=outputBaseJavaPackagePath+"/generator/StaticGenerator.java";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //pom.xml
        inputFilePath=inputResourcesPath+File.separator+"templates/pom.xml.ftl";
        outputFilePath= outputPath +File.separator+"pom.xml";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);

        //生成README.md项目介绍文件
        inputFilePath=inputResourcesPath+File.separator+"templates/README.md.ftl";
        outputFilePath= outputPath +File.separator+"README.md";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath, meta);
        //.gitignore
        inputFilePath=inputResourcesPath+File.separator+"templates/.gitignore.ftl";
        outputFilePath=outputPath+File.separator+".gitignore";
        DynamicFileGenerator.doGenerate(inputFilePath,outputFilePath,meta);
    }

    protected  String copySource(Meta meta, String outputPath) {
        //复制从原始模板文件复制到生成的代码包中
        String sourceRootPath= meta.getFileConfig().getSourceRootPath();
        String sourceCopyDestPath= outputPath +File.separator+".source";
        FileUtil.copy(sourceRootPath,sourceCopyDestPath,false);
        return sourceCopyDestPath;
    }

    /**
     * 制作压缩包
     */
    protected String buildZip(String outputPath){
        String zipPath=outputPath+".zip";
        ZipUtil.zip(outputPath,zipPath);
        return zipPath;
    }
}
