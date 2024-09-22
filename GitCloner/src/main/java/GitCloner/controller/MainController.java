package GitCloner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import GitCloner.model.ClonerModel;
import GitCloner.service.*;

@RestController
public class MainController {
   @Autowired
   GitCloner GitClonerService;
   @Autowired
   GitPusher GitPusherService;

   @PostMapping("/cloner")
   public void GitCloner(@RequestBody ClonerModel gitrepo) {
       GitClonerService.gitCloner(gitrepo);
   }

   @PostMapping("/pusher")
   public void GitPusher(@RequestBody ClonerModel gitrepo) {
      GitPusherService.gitPusher(gitrepo);

   }
}
