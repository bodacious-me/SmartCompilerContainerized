package JavaCompiler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;




import JavaCompiler.model.JavaModel;
import JavaCompiler.service.AllServices;


@RestController
public class MainController {

    @Autowired
    private AllServices allServices;


    @PostMapping("/")
    public ResponseEntity<String> JavaCompiler(@RequestBody JavaModel model) throws IOException, InterruptedException, ExecutionException {
        allServices.setModels(model);
        Future<String> Result = allServices.startCompiler(model);
        //allServices.cleaner(model);
        String FinalResult = Result.get();
        return new ResponseEntity<>(FinalResult, HttpStatus.OK);
    }
}
