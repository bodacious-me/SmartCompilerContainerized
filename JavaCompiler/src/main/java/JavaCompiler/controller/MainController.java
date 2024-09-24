package JavaCompiler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;




import JavaCompiler.model.JavaModel;
import JavaCompiler.service.AllServices;

@RestController
public class MainController {

    @Autowired
    AllServices service;

    @PostMapping("/")
    public ResponseEntity<String> JavaCompiler(@RequestBody JavaModel model) throws IOException {
        String Result = service.run(model);
        service.cleaner(model);

        return new ResponseEntity<>(Result, HttpStatus.OK);
    }
}
