package com.yupi.model;

import lombok.Data;

/**
 * 静态模板配置
 */
@Data
public class MainTemplateConfig {

    //先明确几个动态生成的需求：
    //1.在代码开头增加作者@Author 注释（增加代码）
    //2.修改程序输出的信息提示
    //3.将循环读取输入改为单次读取（可选代码）
    /**
     * 作者（字符串，填充值）
     */
    private String Author="yupi";

    /**
     * 输出信息
     */
    private String outputText="输出结果";
    /**
     * 是否循环(开关)
     */
    private boolean loop;

}
