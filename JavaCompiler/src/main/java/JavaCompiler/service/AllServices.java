package JavaCompiler.service;

import org.springframework.http.HttpHeaders;

import java.util.concurrent.Future;
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
import java.util.concurrent.CompletableFuture;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.nio.file.Paths;

import JavaCompiler.model.JavaModel;

@Service
public class AllServices implements CommandLineRunner{


    JavaModel classModel = new JavaModel();
    public Stream<String> ComErrorStream;
    public void setModels(JavaModel model) {
        classModel = model;
    }

   @Async("threadPoolTaskExecutor")
    public Future<String> startCompiler(JavaModel model) throws IOException {
        gitClonerRequest(model);

        if (MainCompiler(model)) {
            // name = model.getName(); and no no
            jarFileCreator();

            gitPusherRequest(model);
            cleaner(model);
            return CompletableFuture.completedFuture("Compilation Successful. The Jar File Is Here: https://github.com/bodacious-me/" + model.getName());
        } else {
            logFileGenerator(model);
            logFilePusher(model);
            return CompletableFuture.completedFuture("Compilation Successful. The Jar File Is Here: https://github.com/bodacious-me/" + model.getName());
        }

    }

    public void logFileGenerator(JavaModel model) {
        System.out.println("Log creator method called");
        String result = ComErrorStream.collect(Collectors.joining(" "));
        String logFileDirectory = "../Shared-Data/output/" + model.getName();
        String logFileName = "logs.txt";

        Path path = Paths.get(logFileDirectory, logFileName);
        try {
            System.out.println(result);
            Files.createDirectories(path.getParent());
            Files.writeString(path, result);
        } catch (IOException e) {
            System.out.println("Error Dummie: " + e);
        }
    }

    // Command Runner

    @Override
    public void run(String... args) {
        jarFileCreator();
    }

    public void jarFileCreator() {

        String directoryPath = "../Shared-Data/output/" + classModel.getName(); // Directory containing
                                                                                // .class files
        String jarFileName = "Files.jar"; // Name of the JAR file

        List<String> classFiles = new ArrayList<>();
        try {
            Files.list(Paths.get("../Shared-Data/output/" + classModel.getName()))
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

    public void gitClonerRequest(JavaModel model) {
        String targetUrl1 = "http://142.132.225.181:4000/cloner";
        String requestBody1 = "{\"gitrepo\":\"" + model.getGitrepo() +
                "\",\"name\":\"" + model.getName() + "\"}";
        sendPostRequestWithJsonBody(targetUrl1, requestBody1);
    }

    public void gitPusherRequest(JavaModel model) {
        String targetUrl2 = "http://142.132.225.181:4000/pusher";
        String requestBody2 = "{\n" +
                "\"gitrepo\":\"SomeRandomShit\",\n" +
                "\"name\":\"" + model.getName() + "\",\n"
                +
                "\"filename\":\"Files.jar\"\n" +
                "}";
        sendPostRequestWithJsonBody(targetUrl2, requestBody2);
    }

    public void logFilePusher(JavaModel model) {
        String targetUrl3 = "http://142.132.225.181:4000/pusher";
        String requestBody3 = "{\n" +
                "\"gitrepo\":\"SomeRandomShit\",\n" +
                "\"name\":\"" + model.getName() + "\",\n"
                +
                "\"filename\":\"logs.txt\"\n" +
                "}";
        sendPostRequestWithJsonBody(targetUrl3, requestBody3);
    }

    public boolean MainCompiler(JavaModel model) throws IOException {

        File directorySource = new File("../Shared-Data/source/" + model.getName());
        String directoryOutput = "../Shared-Data/output/" + model.getName();
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
        // stringbuilder
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
        ComErrorStream = Stream.of(compilationErrors.toString());
        fileManager.close();
        if (!success) {
            // TO BE FIXED: send unsuccessfull response

        }
        if (success) {
            // TO BE FIXED: send success response
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

    public void cleaner(JavaModel model) {
        System.out.println("The Cleaner Started: ");
        String d1 = "../Shared-Data/source/" + model.getName();
        String d2 = "../Shared-Data/output/" + model.getName();
        System.out.println(d1);
        System.out.println(d2);
        File directory1 = new File(d1);
        File directory2 = new File(d2);
        boolean delete1 = FileSystemUtils.deleteRecursively(directory1);
        boolean delete2 = FileSystemUtils.deleteRecursively(directory2);
        if (delete1 && delete2) {
            System.out.println("Cleaned up the mess Successfully");
        }

    }
}
