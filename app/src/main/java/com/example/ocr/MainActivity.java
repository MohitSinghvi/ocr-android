package com.example.ocr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView imageView;
    private TextView textView;
    private Button selectButton;
    private Button analyzeButton;
    private Bitmap selectedImageBitmap;
    private String subscriptionKey = System.getenv("COMPUTER_VISION_SUBSCRIPTION_KEY");;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        selectButton = findViewById(R.id.select_button);
        analyzeButton = findViewById(R.id.analyze_button);

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImageBitmap != null) {
                    analyzeImage(selectedImageBitmap);
                }
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageView.setImageBitmap(selectedImageBitmap);
                imageView.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void analyzeImage(Bitmap imageBitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = callComputerVisionAPI(imageBitmap);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(result);
                        }
                    });
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String callComputerVisionAPI(Bitmap imageBitmap) throws IOException, JSONException {
        String endpoint = "https://cmpe277-cv.cognitiveservices.azure.com/computervision/imageanalysis:analyze?api-version=2023-02-01-preview&features=caption%2Cread&language=en&gender-neutral-caption=False";
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);
        connection.setDoOutput(true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] data = outputStream.toByteArray();

        connection.getOutputStream().write(data);

        InputStream inputStream = connection.getInputStream();
        String response = readResponse(inputStream);
        return response;
    }

    private String readResponse(InputStream inputStream) throws IOException, JSONException {
        StringBuilder stringBuilder = new StringBuilder();
        int c;
        while ((c = inputStream.read()) != -1) {
            stringBuilder.append((char) c);
        }
        String response =  stringBuilder.toString();

        JSONObject jsonResponse = new JSONObject(response);
        JSONObject description = jsonResponse.getJSONObject("readResult");
        String content = description.getString("content");

//        StringBuilder stringBuilder2 = new StringBuilder();
//        for (int i = 0; i < textElements.length(); i++) {
//            JSONObject textElement = textElements.getJSONObject(i);
//            String text = textElement.getString("text");
//            stringBuilder2.append(text).append("\n");
//        }
//        return stringBuilder2.toString();
        return content;
    }
}
