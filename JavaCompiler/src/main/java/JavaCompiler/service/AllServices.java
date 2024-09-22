package JavaCompiler.service;

import org.springframework.http.HttpHeaders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.InputStreamReader;
import org.springframework.util.FileSystemUtils;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.BufferedReader;


import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.nio.file.Paths;

import JavaCompiler.model.JavaModel;

@Service
public class AllServices implements CommandLineRunner {

    public static String name;
    public static Stream<String> ComError;

    public void run(JavaModel model) throws IOException {
        // Clone the Repository
        gitCloner(model);
        // Compile the reop
        // If compilation is successfull
        if (MainCompiler(model)) {
            name = model.getName();
            // Create the jar file
            executeCommand();
            // And push the file to the github repo
            gitPusher(model);
        }

    }

    // Command Runner

    @Override
    public void run(String... args) {
        executeCommand();
    }

    public void executeCommand() {

        String directoryPath = "../home/Shared-Data/output/" + name; // Directory containing
                                                                                                 // .class files
        String jarFileName = "Files.jar"; // Name of the JAR file

        List<String> classFiles = new ArrayList<>();
        try {
            Files.list(Paths.get("../home/Shared-Data/output/" + name))
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> classFiles.add(path.getFileName().toString()));
        } catch (IOException e) {
            System.out.println("Error reading class files: " + e.getMessage());
            return;
        }

        List<String> command = new ArrayList<>();
        command.add("jar");
        command.add("cvf");
        command.add(jarFileName);
        command.addAll(classFiles);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(directoryPath));
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Error: " + line);
            }

            System.out.println("Command executed with exit code: " + exitCode);
        } catch (IOException e) {
            System.out.println("IOException occurred: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Process was interrupted: " + e.getMessage());
        }
    }

    public void gitCloner(JavaModel model) {
        String targetUrl = "http://142.132.225.181:4000/cloner";
        String requestBody = "{\"gitrepo\":\"" + model.getGitrepo() +
                "\",\"name\":\"" + model.getName() + "\"}";
        sendPostRequestWithJsonBody(targetUrl, requestBody);
    }

    public void gitPusher(JavaModel model) {
        String targetUrl1 = "http://142.132.225.181:4000/pusher";
        String requestBody1 = "{\n" +
                "\"exerepo\":\"../home/Shared-Data/output\",\n" +
                "\"name\":\"" + model.getName() + "\",\n" +
                "\"filename\":\"Files.jar\"\n" +
                "}";
        sendPostRequestWithJsonBody(targetUrl1, requestBody1);
    }

    public boolean MainCompiler(JavaModel model) throws IOException {

        File directorySource = new File("../home/Shared-Data/source/" + model.getName());
        String directoryOutput = "../home/Shared-Data/output/" + model.getName();
        Path directoryPath = Paths.get(directoryOutput);
        Files.createDirectories(directoryPath);
        List<File> Javafiles = new ArrayList<>();
        findJavaFiles(directorySource, Javafiles);
        copyFilesToDirectory(directoryOutput, Javafiles);

        // Get all .java files from the specified directory
        List<File> javaFiles = Files.walk(Path.of(directoryOutput))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .map(Path::toFile)
                .collect(Collectors.toList());

        // Convert the list to an array
        File[] filesArray = javaFiles.toArray(new File[0]);
        //stringbuilder
        StringBuilder compilationErrors = new StringBuilder();
        // Set up the Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        // create DiagnosticListner
                DiagnosticListener<JavaFileObject> diagnosticListener = diagnostic -> {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                compilationErrors.append(diagnostic.getMessage(null)).append("\n");
            }
        };

        // Create compilation units from the files
        Iterable<? extends JavaFileObject> compilationUnits = fileManager
                .getJavaFileObjectsFromFiles(Arrays.asList(filesArray));

        // Compile the files
        boolean success = compiler.getTask(null, fileManager, diagnosticListener, null, null, compilationUnits).call();
        fileManager.close();
        if (!success) {

            System.out.println(compilationErrors.toString());
            ComError = Stream.of(compilationErrors.toString());

        }
        if(success){
            ComError = Stream.of("Compilation Successfull: https://github.com/bodacious-me/"+model.getName());
        }
        return success;
    }

    public void copyFilesToDirectory(String targetDirectory, List<File> files) {

        for (File file : files) {
            Path sourcePath = file.toPath();
            Path targetPath = Path.of(targetDirectory, file.getName());

            try {
                // Copy the file to the target directory
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied: " + file.getName());
            } catch (IOException e) {
                System.err.println("Failed to copy: " + file.getName() + " due to " + e.getMessage());
            }
        }
    }

    public static void findJavaFiles(File directory, List<File> javaFiles) {

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        findJavaFiles(file, javaFiles);
                    } else if (file.getName().endsWith(".java")) {
                        javaFiles.add(file);
                        System.out.println(file);
                    }
                }
            }
        }

    }


    public void sendPostRequestWithJsonBody(String targetUrl, String requestBody) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, requestEntity,
                String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("POST request successful. Response: " + response.getBody());
        } else {
            System.out.println("POST request failed. Status code: " + response.getStatusCode());
        }
    }

    public void cleaner(JavaModel model){
        System.out.println("The Cleaner Started: ");
        String d1 = "../home/Shared-Data/source/"+model.getName();
        String d2 = "../home/Shared-Data/output/"+model.getName();
        System.out.println(d1);
        System.out.println(d2);
        File directory1 = new File(d1);
        File directory2 = new File(d2);
        boolean delete1 = FileSystemUtils.deleteRecursively(directory1);
        boolean delete2 = FileSystemUtils.deleteRecursively(directory2);
        if(delete1 && delete2){
            System.out.println("Cleaned up the mess Successfully");
        }



    }
}
