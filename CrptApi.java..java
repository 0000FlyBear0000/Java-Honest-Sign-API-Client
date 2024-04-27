import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore semaphore;
    private final long resetTime;
    private final long requestLimit;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.semaphore = new Semaphore(requestLimit);
        this.resetTime = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public void createDocument(Document document) throws InterruptedException, IOException {
        // Получить текущее время
        Instant now = Instant.now();
        // Подождать, пока не истечет текущий интервал времени
        while (now.plusMillis(resetTime) > Instant.now()) {
            Thread.sleep(100);
        }
        // Попробовать получить разрешение на выполнение запроса
        if (semaphore.tryAcquire()) {
            try {
                // Преобразовать объект Java в JSON формат
                String json = objectMapper.writeValueAsString(document);
                // Создать запрос
                Request request = new Request.Builder()
                        .url("https://ismp.crpt.ru/api/v3/lk/documents/create")
                        .post(RequestBody.create(json, null))
                        .build();
                // Отправить запрос
                Response response = httpClient.newCall(request).execute();
                // Обработать ответ
                // ...
            } finally {
                // Освободить разрешение на выполнение запроса
                semaphore.release();
            }
        } else {
            // Превышен лимит количества запросов
            // Подождать, пока не истечет текущий интервал времени
            Thread.sleep(resetTime);
            // Повторить попытку выполнить запрос
            createDocument(document);
        }
    }
}
