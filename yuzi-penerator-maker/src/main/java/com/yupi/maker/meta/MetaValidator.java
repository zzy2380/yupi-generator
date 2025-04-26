package com.yupi.maker.meta;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.maker.meta.enums.FileGenerateTypeEnum;
import com.yupi.maker.meta.enums.FileTypeEnum;
import com.yupi.maker.meta.enums.ModelTypeEnum;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class MetaValidator {
    public static void doValidAndFill(Meta meta) {
        //基础信息校验
        validAndFillMetaRoot(meta);
        validAndFillConfig(meta);
        validAndFillModelConfig(meta);
    }

    public static void validAndFillModelConfig(Meta meta) {
        //modelConfig 校验和默认值
        Meta.ModelConfig modelConfig = meta.getModelConfig();
        if (modelConfig == null) {
            return;
        }
        List<Meta.ModelConfig.ModelInfo> modelInfoList = modelConfig.getModels();
        if (CollUtil.isEmpty(modelInfoList)) {
            return;
        }
        for (Meta.ModelConfig.ModelInfo modelInfo : modelInfoList) {
            //为group，不校验
            String groupKey = modelInfo.getGroupKey();
            if(StrUtil.isNotEmpty(groupKey)){
                List<Meta.ModelConfig.ModelInfo> subModelInfoList = modelInfo.getModels();
                String allArgsStr=subModelInfoList.stream()
                        .map(subModelInfo->String.format("\"--%s\"",subModelInfo.getFieldName()))
                        .collect(Collectors.joining(", "));
                modelInfo.setAllArgsStr(allArgsStr);
                continue;
            }
            String fieldName = modelInfo.getFieldName();
            if (StrUtil.isBlank(fieldName)) {
                throw new MetaException("未填写：fieldName");
            }
            String modelInfoType = modelInfo.getType();
            if (StrUtil.isEmpty(modelInfoType)) {
                modelInfo.setType(ModelTypeEnum.STRING.getValue());
            }
        }
    }

    public static void validAndFillMetaRoot(Meta meta) {
        //基础信息校验
        String name = meta.getName();
        if (StrUtil.isBlank(name)) {
            name = "my-generator";
            meta.setName(name);
        }

        String description = meta.getDescription();
        if (StrUtil.isEmpty(description)) {
            description = "我的模板代码生成器";
            meta.setDescription(description);
        }
        String basePackage = meta.getBasePackage();
        if (StrUtil.isBlank(basePackage)) {
            basePackage = "com.yupi";
            meta.setBasePackage(basePackage);
        }
        String version = meta.getVersion();
        if (StrUtil.isEmpty(version)) {
            version = "1.0";
            meta.setVersion(version);
        }
        String author = meta.getAuthor();
        if (StrUtil.isEmpty(author)) {
            author = "my-generator";
            meta.setAuthor(author);
        }
        String createTime = meta.getCreateTime();
        if (StrUtil.isEmpty(createTime)) {
            createTime = DateUtil.now();
            meta.setCreateTime(createTime);
        }
    }

    public static void validAndFillConfig(Meta meta) {
        Meta.FileConfig fileConfig = meta.getFileConfig();
        if (fileConfig == null) {
            return;
        }
        {
            //sourceRootPath
            String sourceRootPath = fileConfig.getSourceRootPath();
            if (StrUtil.isBlank(sourceRootPath)) {
                throw new MetaException("未填写sourceRootPath");
            }
            //inputRootPath:.source+sourceRootPath的最后一个层级路径
            String inputRootPath = fileConfig.getInputRootPath();
            if (StrUtil.isEmpty(inputRootPath)) {
                String defaultInputPath = ".source/" +
                        FileUtil.getLastPathEle(Paths.get(sourceRootPath)).getFileName().toString();
                fileConfig.setInputRootPath(defaultInputPath);
            }
            //outputRootPath 默认为当前路径下的generated目录
            String outputRootPath = fileConfig.getOutputRootPath();
            String defaultOutputPath = "generated";
            if (StrUtil.isEmpty(outputRootPath)) {
                fileConfig.setInputRootPath(defaultOutputPath);
            }
        }
        //fileConfig 校验和默认值
        List<Meta.FileConfig.FileInfo> fileInfoList = fileConfig.getFiles();
        if (CollUtil.isEmpty(fileInfoList)) {
            return;
        }
        for (Meta.FileConfig.FileInfo fileInfo : fileInfoList) {
            String type = fileInfo.getType();
            if(FileTypeEnum.GROUP.getValue().equals(type)){
                continue;
            }
            //inputPath必填
            String inputPath = fileInfo.getInputPath();
            if (StrUtil.isBlank(inputPath)) {
                throw new MetaException("未填写inputPath");
            }
            //outputPath默认为inputPath
            String outputPath = fileInfo.getOutputPath();
            if (StrUtil.isEmpty(outputPath)) {
                fileInfo.setOutputPath(inputPath);
            }
            //type 默认inputPath有文件后缀(比如：.java) 默认为file，否则dir
            if (StrUtil.isBlank(type)) {
                //无文件后缀
                if (StrUtil.isBlank(FileUtil.getSuffix(inputPath))) {
                    fileInfo.setType(FileTypeEnum.DIR.getValue());
                } else {
                    fileInfo.setType(FileTypeEnum.FILE.getValue());
                }

            }
            //generatorType 文件结尾不为ftl，generatorType为 static 否则为dynamic
            String generateType = fileInfo.getGenerateType();
            if (StrUtil.isBlank(generateType)) {
                if (inputPath.endsWith(".ftl")) {
                    fileInfo.setGenerateType(FileGenerateTypeEnum.DYNAMIC.getValue());
                } else {
                    fileInfo.setGenerateType(FileGenerateTypeEnum.STATIC.getValue());
                }
            }
        }


        String fileConfigType = fileConfig.getType();
        String defaultType = FileTypeEnum.DIR.getValue();
        if (StrUtil.isEmpty(fileConfigType)) {
            fileConfig.setType(defaultType);
        }

    }
}

