package GitCloner.service;

import GitCloner.model.ClonerModel;
import okhttp3.*;
import java.io.IOException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RepoCreator {
      @Async("threadPoolTaskExecutor")

      public void repoCreator(ClonerModel exerepo) {
        System.out.println("RepoCreator is called");
        final String token = "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";

        OkHttpClient client = new OkHttpClient();
        String url = "https://api.github.com/user/repos";
        String json = "{\"name\":\"" + exerepo.getName() + "\", \"private\":false}";

        RequestBody body = RequestBody.create(json,
                MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "token " + token)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            System.out.println("Repository created successfully: " + exerepo.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
