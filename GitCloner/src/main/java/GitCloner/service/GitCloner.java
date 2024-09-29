package GitCloner.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

import GitCloner.model.ClonerModel;

@Service
public class GitCloner {
       @Async("threadPoolTaskExecutor")
        public void gitCloner(ClonerModel gitrepo) {
        try {
            String CloneDirectoryPath = "../Shared-Data/source/"+gitrepo.getName();

            System.out.println("Cloning repo from: " + gitrepo.getGitrepo() + " to "
                    + CloneDirectoryPath);

            Git.cloneRepository()
                    .setURI(gitrepo.getGitrepo())
                    .setDirectory(new File(CloneDirectoryPath))
                    .call();

            System.out.println("Repository Clone Successfully");

        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }
}
// the validationof the link provided by the user 
// must be handled by the client cli side
// also making sure the name provided in the request doesn't already exist 
// and if it does, make sure that the clinet notifies the user