package com.yupi.maker.generator;

import java.io.*;

public class JarGenerator {

    public static void doGenerator(String projectDir) throws InterruptedException, IOException {
        //调用process类执行maven 打包命令
        String winMavenCommand="mvn.cmd clean package -DskipTests=true";
        String otherMavenCommand="mvn clean package -DskipTests=true";
        String mavenCommand=winMavenCommand;

        ProcessBuilder processBuilder=new ProcessBuilder(mavenCommand.split(" "));
        processBuilder.directory(new File(projectDir));
        Process process = processBuilder.start();

        //读取命令的输出
        InputStream inputStream=process.getInputStream();
        OutputStream outputStream=process.getOutputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line= bufferedReader.readLine())!=null){
            System.out.println(line);
        }


        int exitCode=process.waitFor();
        System.out.println("命令执行结束，退出码："+exitCode);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        doGenerator("E:/code/demo-generator/yuzi-penerator-maker/generator");
    }
}
