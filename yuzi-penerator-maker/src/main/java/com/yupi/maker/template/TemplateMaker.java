package com.yupi.maker.template;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yupi.maker.meta.Meta;
import com.yupi.maker.meta.enums.FileGenerateTypeEnum;
import com.yupi.maker.meta.enums.FileTypeEnum;
import com.yupi.maker.template.enums.FileFilterRangeEnum;
import com.yupi.maker.template.enums.FileFilterRuleEnum;
import com.yupi.maker.template.model.FileFilterConfig;
import com.yupi.maker.template.model.TemplateMakerFileConfig;
import com.yupi.maker.template.model.TemplateMakerModelConfig;
import freemarker.template.utility.StringUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板制作工具
 */
public class TemplateMaker {
    /**
     * 制作模板
     * @param newMeta
     * @param originProjectPath
     * @param id
     * @return
     */

    private static long makeTemplate(Meta newMeta, String originProjectPath, TemplateMakerFileConfig templateMakerFileConfig,TemplateMakerModelConfig templateMakerModelConfig, Long id){
        //没有id则生成
        if(id==null){
            id=IdUtil.getSnowflakeNextId();
        }

        //复制目录
        String projectPath=System.getProperty("user.dir");
        String tempDirPath=projectPath+File.separator+".temp";
        String templatePath=tempDirPath+File.separator+id;
        if(!FileUtil.exist(templatePath)){
            FileUtil.mkdir(templatePath);
            FileUtil.copy(originProjectPath,templatePath,true);
        }


//        一.输入信息

        //处理模型信息
        List<TemplateMakerModelConfig.ModelInfoConfig> models = templateMakerModelConfig.getModels();
        //- 转化为配置文件接受的modelInfo对象
        List<Meta.ModelConfig.ModelInfo> inputModelInfoList = models.stream()
                .map(modelInfoConfig -> {
                    Meta.ModelConfig.ModelInfo modelInfo = new Meta.ModelConfig.ModelInfo();
                    BeanUtil.copyProperties(modelInfoConfig, modelInfo);
                    return modelInfo;
                }).collect(Collectors.toList());

        //本次新增的模型列表
        List<Meta.ModelConfig.ModelInfo> newModelInfoList=new ArrayList<>();
        //如果是模型组
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig = templateMakerModelConfig.getModelGroupConfig();
        if(modelGroupConfig!=null){
            String condition = modelGroupConfig.getCondition();
            String groupKey = modelGroupConfig.getGroupKey();
            String groupName = modelGroupConfig.getGroupName();

            Meta.ModelConfig.ModelInfo groupModelInfo=new Meta.ModelConfig.ModelInfo();
            groupModelInfo.setCondition(condition);
            groupModelInfo.setGroupKey(groupKey);
            groupModelInfo.setGroupName(groupName);
            //模型全放到一个分组内
            groupModelInfo.setModels(inputModelInfoList);
            newModelInfoList=new ArrayList<>();
            newModelInfoList.add(groupModelInfo);

        }else{
            //不分组，添加所有的模型信息到列表
            newModelInfoList.addAll(inputModelInfoList);
        }


        //2.输入文件基本信息
        //要挖坑的项目根目录
        String sourceRootPath= templatePath+File.separator+FileUtil.getLastPathEle(Paths.get(originProjectPath)).toString();
        //注意win系统需要对路径进行转义
        sourceRootPath=sourceRootPath.replaceAll("\\\\","/");

        List<TemplateMakerFileConfig.FileInfoConfig> fileInfoConfigList=templateMakerFileConfig.getFiles();

        //遍历输入文件
        List<Meta.FileConfig.FileInfo> newFileInfoList=new ArrayList<>();
        for (TemplateMakerFileConfig.FileInfoConfig fileInfoConfig : fileInfoConfigList) {
            String inputFilePath = fileInfoConfig.getPath();
            String inputFileAbsolutePath=sourceRootPath+File.separator+inputFilePath;
            //传入绝对路径
            //得到过滤后的文件列表
            List<File> fileList = FileFilter.doFilter(inputFileAbsolutePath, fileInfoConfig.getFileFilterConfigList());
            for (File file : fileList) {
                Meta.FileConfig.FileInfo fileInfo = makerFileTemplate(templateMakerModelConfig, sourceRootPath,file);
                newFileInfoList.add(fileInfo);
            }
        }
        //如果是文件组
        TemplateMakerFileConfig.FileGroupConfig fileGroupConfig = templateMakerFileConfig.getFileGroupConfig();
        if(fileGroupConfig!=null){
            String condition = fileGroupConfig.getCondition();
            String groupKey = fileGroupConfig.getGroupKey();
            String groupName = fileGroupConfig.getGroupName();

            Meta.FileConfig.FileInfo groupFileInfo=new Meta.FileConfig.FileInfo();
            groupFileInfo.setCondition(condition);
            groupFileInfo.setGroupKey(groupKey);
            groupFileInfo.setGroupName(groupName);
            //文件全放到一个分组内
            groupFileInfo.setFiles(newFileInfoList);
            newFileInfoList=new ArrayList<>();
            newFileInfoList.add(groupFileInfo);

        }



        //三，生成配置文件
        String metaOutputPath=sourceRootPath+File.separator+"meta.json";

        //已有meta文件不是第一次制作,则在meta基础上进行修改
        if(FileUtil.exist(metaOutputPath)){
            newMeta=JSONUtil.toBean(FileUtil.readUtf8String(metaOutputPath),Meta.class);
            //1.追加配置
            List<Meta.FileConfig.FileInfo> fileInfoList = newMeta.getFileConfig().getFiles();
            fileInfoList.addAll(newFileInfoList);
            List<Meta.ModelConfig.ModelInfo> modelInfoList = newMeta.getModelConfig().getModels();
            modelInfoList.addAll(newModelInfoList);

            //配置去重
            newMeta.getFileConfig().setFiles(distinctFiles(fileInfoList));
            newMeta.getModelConfig().setModels(distinctModels(modelInfoList));

        }else{
            //1.构造配置参数对象

            Meta.FileConfig fileConfig=new Meta.FileConfig();
            newMeta.setFileConfig(fileConfig);
            fileConfig.setSourceRootPath(sourceRootPath);
            List<Meta.FileConfig.FileInfo> fileInfoList=new ArrayList<>();
            fileConfig.setFiles(fileInfoList);
            fileInfoList.addAll(newFileInfoList);

            Meta.ModelConfig modelConfig =new Meta.ModelConfig();
            newMeta.setModelConfig(modelConfig);
            List<Meta.ModelConfig.ModelInfo> modelInfoList=new ArrayList<>();
            modelConfig.setModels(modelInfoList);
            modelInfoList.addAll(newModelInfoList);
        }
        //2.输出元信息文件
        FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(newMeta),metaOutputPath);
        return id;
    }

    /**
     * 制作模板文件
     * @param sourceRootPath
     * @param inputFile
     * @return
     */
    private static Meta.FileConfig.FileInfo makerFileTemplate(TemplateMakerModelConfig templateMakerModelConfig, String sourceRootPath, File inputFile) {
        String fileInputAbsolutPath=inputFile.getAbsolutePath();
        //注意win系统需要对路径进行转义
        fileInputAbsolutPath=fileInputAbsolutPath.replaceAll("\\\\","/");
        //要挖坑的文件(一定要是相对路径)
        String fileInputPath = fileInputAbsolutPath.replace(sourceRootPath+"/","");
        String fileOutputPath= fileInputPath +".ftl";

        //二.使用字符串替换，生成模板文件
        String fileOutputAbsolutPath=fileInputAbsolutPath+".ftl";

        String fileContent;
        //如果已有模板文件,表示不是第一次制作，则在原有模板的基础上再挖坑
        if(FileUtil.exist(fileOutputAbsolutPath)){
            fileContent= FileUtil.readUtf8String(fileOutputAbsolutPath);
        }else{
            fileContent= FileUtil.readUtf8String(fileInputAbsolutPath);
        }

        //支持多个模型，对于同一个文件的内容进行多轮替换
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig=templateMakerModelConfig.getModelGroupConfig();
        //
//        最新替换后的内容
        String newFileContent=fileContent;
        String replacement;
        for (TemplateMakerModelConfig.ModelInfoConfig modelInfoConfig : templateMakerModelConfig.getModels()) {
            String fieldName = modelInfoConfig.getFieldName();
            //模型配置
            //不是分组
            if(modelGroupConfig==null){
                replacement=String.format("${%s}",fieldName);
            }else{
                //有分组
                String groupKey= modelGroupConfig.getGroupKey();
                replacement=String.format("${%s.%s}",groupKey,fieldName);
            }
            newFileContent= StringUtil.replace(newFileContent,modelInfoConfig.getReplaceText(),replacement);
        }
        //文件配置信息
        Meta.FileConfig.FileInfo fileInfo=new Meta.FileConfig.FileInfo();
        fileInfo.setInputPath(fileInputPath);
        fileInfo.setOutputPath(fileOutputPath);
        fileInfo.setType(FileTypeEnum.FILE.getValue());

        //和原文件内容一直，没有挖坑，静态生成
        if(newFileContent.equals(fileContent)){
            //输出路径=输入路径
            fileInfo.setOutputPath(fileInputPath);
            fileInfo.setGenerateType(FileGenerateTypeEnum.STATIC.getValue());
        }else{
            fileInfo.setGenerateType(FileGenerateTypeEnum.DYNAMIC.getValue());
            //输出模板文件
            FileUtil.writeUtf8String(newFileContent,fileOutputAbsolutPath);
        }
        return fileInfo;
    }

    public static void main(String[] args) throws IOException {
        //1.项目的基本信息
        Meta meta=new Meta();
        meta.setName("acm-template-pro-generator");
        meta.setDescription("ACM 示例模板生成器");

        //指定原始项目路径
        String projectPath=System.getProperty("user.dir");
        String originProjectPath= new File(projectPath).getParent()+File.separator+"yuzi-generator-demo-projects/springboot-init";
        String fileInputPath1="src/main/java/com/yupi/springbootinit/common";
        String fileInputPath2="src/main/resources/application.yml";
        List<String> inputFilePathList= Arrays.asList(fileInputPath1,fileInputPath2);

        //输入模型参数信息
//        Meta.ModelConfig.ModelInfo modelInfo=new Meta.ModelConfig.ModelInfo();
//        modelInfo.setFieldName("outputText");
//        modelInfo.setType("String");
//        modelInfo.setDefaultValue("sum = ");

        //输入模型参数信息（第二次）
        Meta.ModelConfig.ModelInfo modelInfo=new Meta.ModelConfig.ModelInfo();
        modelInfo.setFieldName("className");
        modelInfo.setType("String");
        //替换变量（首次）
//        String searchStr="Sum: ";

        String searchStr="BaseResponse";

        //文件过滤配置
        TemplateMakerFileConfig.FileInfoConfig fileInfoConfig1=new TemplateMakerFileConfig.FileInfoConfig();
        fileInfoConfig1.setPath(fileInputPath1);
        List<FileFilterConfig> fileFilterConfigList=new ArrayList<>();
        FileFilterConfig fileFilterConfig=FileFilterConfig.builder()
                .range(FileFilterRangeEnum.FILE_NAME.getValue())
                .rule(FileFilterRuleEnum.CONTAINS.getValue())
                .value("Base")
                .build();
        fileFilterConfigList.add(fileFilterConfig);
        fileInfoConfig1.setFileFilterConfigList(fileFilterConfigList);

        TemplateMakerFileConfig.FileInfoConfig fileInfoConfig2=new TemplateMakerFileConfig.FileInfoConfig();
        fileInfoConfig2.setPath(fileInputPath2);

        List<TemplateMakerFileConfig.FileInfoConfig> fileInfoConfigList=Arrays.asList(fileInfoConfig1,fileInfoConfig2);
        TemplateMakerFileConfig templateMakerFileConfig=new TemplateMakerFileConfig();
        templateMakerFileConfig.setFiles(fileInfoConfigList);

        //文件分组配置
        TemplateMakerFileConfig.FileGroupConfig fileGroupConfig=new TemplateMakerFileConfig.FileGroupConfig();
        fileGroupConfig.setCondition("outputText2");
        fileGroupConfig.setGroupKey("test");
        fileGroupConfig.setGroupName("测试分组2");
        templateMakerFileConfig.setFileGroupConfig(fileGroupConfig);

        // 模型参数配置
        TemplateMakerModelConfig templateMakerModelConfig = new TemplateMakerModelConfig();

        // - 模型组配置
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig = new TemplateMakerModelConfig.ModelGroupConfig();
        modelGroupConfig.setGroupKey("mysql");
        modelGroupConfig.setGroupName("数据库配置");
        templateMakerModelConfig.setModelGroupConfig(modelGroupConfig);

        //-模型配置
        TemplateMakerModelConfig.ModelInfoConfig modelInfoConfig1 = new TemplateMakerModelConfig.ModelInfoConfig();
        modelInfoConfig1.setFieldName("url");
        modelInfoConfig1.setType("String");
        modelInfoConfig1.setDefaultValue("jdbc:mysql://localhost:3306/my_db");
        modelInfoConfig1.setReplaceText("jdbc:mysql://localhost:3306/my_db");

        TemplateMakerModelConfig.ModelInfoConfig modelInfoConfig2 = new TemplateMakerModelConfig.ModelInfoConfig();
        modelInfoConfig2.setFieldName("username");
        modelInfoConfig2.setType("String");
        modelInfoConfig2.setDefaultValue("root");
        modelInfoConfig2.setReplaceText("root");

        List<TemplateMakerModelConfig.ModelInfoConfig> modelInfoConfigList = Arrays.asList(modelInfoConfig1, modelInfoConfig2);
        templateMakerModelConfig.setModels(modelInfoConfigList);

        long id=TemplateMaker.makeTemplate(meta,originProjectPath,templateMakerFileConfig,templateMakerModelConfig,1916325846577782784L);
        System.out.println(id);
    }

    /**
     * 文件去重
     */
    private static List<Meta.FileConfig.FileInfo> distinctFiles(List<Meta.FileConfig.FileInfo> fileInfoList){
        //1.将所有文件配置分为（fileInfo）分为有分组的和无分组的

        //先处理有分组的文件
        //以组为单位划分
        Map<String, List<Meta.FileConfig.FileInfo>> groupKeyFileInfoListMap =fileInfoList.stream()
                .filter(fileInfo -> StrUtil.isNotBlank(fileInfo.getGroupKey()))
                .collect(
                        Collectors.groupingBy(Meta.FileConfig.FileInfo::getGroupKey)
                );

        //2.对于有分组的文件配置，如果有相同的分组，同分组内的文件进行合并（merge），不同分组可同时保留。
        //同组内配置合并
        //合并后的对象 map
        Map<String, Meta.FileConfig.FileInfo> groupKeyMergedFileInfoMap=new HashMap<>();

        for (Map.Entry<String, List<Meta.FileConfig.FileInfo>> entry : groupKeyFileInfoListMap.entrySet()) {
            List<Meta.FileConfig.FileInfo> tempFileInfoList = entry.getValue();
            List<Meta.FileConfig.FileInfo> newFileInfoList =new ArrayList<>(tempFileInfoList.stream()
                    .flatMap(fileInfo -> fileInfo.getFiles().stream())
                    .collect(
                            Collectors.toMap(Meta.FileConfig.FileInfo::getInputPath, o -> o, (e, r) -> r)
                    ).values());

            //使用新的group配置覆盖旧的配置
            Meta.FileConfig.FileInfo newFileInfo = CollUtil.getLast(tempFileInfoList);
            newFileInfo.setFiles(newFileInfoList);
            String groupKey = entry.getKey();
            groupKeyMergedFileInfoMap.put(groupKey,newFileInfo);
        }

        //3.创建新的文件配置列表（结果列表），先将合并后的分组添加到结果列表
        ArrayList<Meta.FileConfig.FileInfo> resultList = new ArrayList<>(groupKeyMergedFileInfoMap.values());

        //4.再将无分组的文件配置列表添加至结果列表
        resultList.addAll(new ArrayList<>(fileInfoList.stream()
                .filter(fileInfo -> StrUtil.isBlank(fileInfo.getGroupKey()))
                .collect(
                        Collectors.toMap(Meta.FileConfig.FileInfo::getInputPath, o -> o, (e, r) -> r)
                ).values()));
        return resultList;
    }

    /**
     * 模型去重
     */
    private static List<Meta.ModelConfig.ModelInfo> distinctModels(List<Meta.ModelConfig.ModelInfo> modelInfoList){
//1.将所有模型配置分为（modelInfo）分为有分组的和无分组的

        //先处理有分组的模型
        //以组为单位划分
        Map<String, List<Meta.ModelConfig.ModelInfo>> groupKeyModelInfoListMap =modelInfoList.stream()
                .filter(modelInfo -> StrUtil.isNotBlank(modelInfo.getGroupKey()))
                .collect(
                        Collectors.groupingBy(Meta.ModelConfig.ModelInfo::getGroupKey)
                );

        //2.对于有分组的模型配置，如果有相同的分组，同分组内的模型进行合并（merge），不同分组可同时保留。
        //同组内配置合并
        //合并后的对象 map
        Map<String, Meta.ModelConfig.ModelInfo> groupKeyMergedmodelInfoMap=new HashMap<>();

        for (Map.Entry<String, List<Meta.ModelConfig.ModelInfo>> entry : groupKeyModelInfoListMap.entrySet()) {
            List<Meta.ModelConfig.ModelInfo> tempmodelInfoList = entry.getValue();
            List<Meta.ModelConfig.ModelInfo> newModelInfoList =new ArrayList<>(tempmodelInfoList.stream()
                    .flatMap(modelInfo -> modelInfo.getModels().stream())
                    .collect(
                            Collectors.toMap(Meta.ModelConfig.ModelInfo::getFieldName, o -> o, (e, r) -> r)
                    ).values());

            //使用新的group配置覆盖旧的配置
            Meta.ModelConfig.ModelInfo newModelInfo = CollUtil.getLast(tempmodelInfoList);
            newModelInfo.setModels(newModelInfoList);
            String groupKey = entry.getKey();
            groupKeyMergedmodelInfoMap.put(groupKey,newModelInfo);
        }

        //3.创建新的模型配置列表（结果列表），先将合并后的分组添加到结果列表
        ArrayList<Meta.ModelConfig.ModelInfo> resultList = new ArrayList<>(groupKeyMergedmodelInfoMap.values());

        //4.再将无分组的模型配置列表添加至结果列表
        resultList.addAll(new ArrayList<>(modelInfoList.stream()
                .filter(modelInfo -> StrUtil.isBlank(modelInfo.getGroupKey()))
                .collect(
                        Collectors.toMap(Meta.ModelConfig.ModelInfo::getFieldName, o -> o, (e, r) -> r)
                ).values()));
        return resultList;
    }
}