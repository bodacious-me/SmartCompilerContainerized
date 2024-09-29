package GitCloner.service;

import java.net.URISyntaxException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

import GitCloner.model.ClonerModel;

@Service
public class GitPusher {
    @Autowired
    RepoCreator RepoCreatorService;
    @Async("threadPoolTaskExecutor")

    public void gitPusher(ClonerModel gitrepo) {

        try {
            RepoCreatorService.repoCreator(gitrepo);
            // I know despite having almost everything setupp for making this method dynamic
            // ove still hardcoded the values, but believe me it wont matter anywas at all
            String localPath = "../Shared-Data/output/" + gitrepo.getName();
            String remoteUrl = "https://github.com/bodacious-me/" + gitrepo.getName() + ".git";
            String fileName = gitrepo.getFilename();
            String username = "bodacious-me";
            String accessToken = "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";

            try {

                System.out.println("The localpath: " + localPath);
                File localRepoDir = new File(localPath);
                try (Git git = Git.init().setDirectory(localRepoDir).call()) {
                    File fileToAdd = new File("../Shared-Data/output/" + gitrepo.getName(), fileName);
                    if (!fileToAdd.exists()) {
                        throw new IOException("File does not exist: " + fileToAdd.getAbsolutePath());
                    }

                    git.add().addFilepattern(fileName).call();
                    git.commit().setMessage("Add existing file").call();
                    git.remoteAdd().setName("origin").setUri(new URIish(remoteUrl)).call();
                    git.push()
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, accessToken))
                            .call();

                    System.err.println(
                            "The file " + gitrepo.getName() + " is added to the " + gitrepo.getName() + " Repository");
                }
            } catch (IOException | GitAPIException e) {
                e.printStackTrace();
            }

        } catch (URISyntaxException e) {
            System.out.println("Got A URISyntaxExeption!");
        }
    }
}