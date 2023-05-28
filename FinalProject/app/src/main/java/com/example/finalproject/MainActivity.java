package com.example.finalproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ActivityNotFoundException;

import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Bundle;

import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;

import android.widget.RadioButton;
import android.widget.RadioGroup;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements RecognitionListener {


    //Vosk
    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_MIC = 2;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;


    Boolean islanguageVietNamese = false;
    String keyAPI;
    RecyclerView recyclerView;
    EditText messageEditText;
    ImageButton sendButton, settingButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;



    //Speech to text
    private ImageButton micButton;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    //Speech to text


    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        settingButton = findViewById(R.id.setting_btn);
        micButton = findViewById(R.id.mic_btn);
        keyAPI = getString(R.string.key_api);
        messageList = new ArrayList<>();
        recyclerView = findViewById(R.id.recycler_view);
        sendButton = findViewById(R.id.send_btn);
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);
        messageEditText = findViewById(R.id.message_edit_text);


//        Toast.makeText(this, keyAPI, Toast.LENGTH_SHORT).show();


        /*################################Init Model VOSK#########################################*/
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initModel();
        }
        /*################################Init Model VOSK#########################################*/

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                RadioGroup radioGroup = findViewById(R.id.radio_button_group);

                if(radioGroup.getVisibility() == View.INVISIBLE){
                    radioGroup.setVisibility(View.VISIBLE);
                    Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
                    radioGroup.startAnimation(fadeIn);
                }
                else{
                    radioGroup.setVisibility(View.INVISIBLE);
                    Animation fadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
                    radioGroup.startAnimation(fadeOut);
                }


                RadioButton radioBtnVietnamese = findViewById(R.id.radio_button_vietnamese);
                RadioButton radioBtnEnglish = findViewById(R.id.radio_button_english);
                radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        if(radioBtnVietnamese.isChecked()){
                            islanguageVietNamese = true;
                        }
                        else{
                            islanguageVietNamese = false;
                        }
                        voiceChat(islanguageVietNamese);
//                        radioGroup.setVisibility(View.VISIBLE);
//                        Animation fadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
//                        radioGroup.startAnimation(fadeOut);

                    }
                });


            }
        });


        setUiState(STATE_START);
        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceChat(islanguageVietNamese);
            }
        });

        messageEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        String question = messageEditText.getText().toString().trim();
                        addToChat(question, Message.SENT_BY_ME);
                        messageEditText.setText("");
                        callAPI(question);
                        return true;
                    }
                }
                return false;
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = messageEditText.getText().toString().trim();
                addToChat(question,Message.SENT_BY_ME);
                messageEditText.setText("");
                callAPI(question);
            }
        });

    }



    /*############################Chat GPT API###################################*/

    void voiceChat(boolean islanguageVietNamese){
        if(islanguageVietNamese){
            /*################################VOSK#########################################*/
            micButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            recognizeMicrophone();
                            break;
                        case MotionEvent.ACTION_UP:
                            pause();
                            break;
                    }
                    return true;
                }
            });
            /*################################VOSK#########################################*/
        }
        else{
            /*#####################################Speech to text############################################*/
            micButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        promptSpeechInput();
                    }
                    return true;
                }
            });
            /*#####################################Speech to text############################################*/
        }
    }

    void addToChat(String message, String sentBy){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message,sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    void addResponse(String response){
        messageList.remove(messageList.size()-1);
        addToChat(response, Message.SENT_BY_BOT);
    }

    void callAPI(String question){
        onDestroy();
        messageList.add(new Message("Typing... ", Message.SENT_BY_BOT));
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model","text-davinci-003");
            jsonBody.put("prompt",question);
            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        RequestBody body = RequestBody.create(jsonBody.toString(),JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization",keyAPI) //"Bearer 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"
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to message" +e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    addResponse("Failed to load response due to response " +response.body().toString());
                }
            }
        });
    }

    /*############################Chat GPT API###################################*/


    /*############################Speech to text###################################*/

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Say something…");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),"Sorry! Your device doesn't support speech input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    messageEditText.setText(result.get(0));
                    String question = messageEditText.getText().toString().trim();
                }
                break;
            }

        }
    }
    /*############################Speech to text###################################*/


    /*################################VOSK#########################################*/


    private void initModel() {
        StorageService.unpack(this, "model-vn-0.4", "model",
                (model) -> {
                    this.model = model;
                    setUiState(STATE_READY);
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                initModel();
            } else {
                finish();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }

    @Override
    public void onResult(String hypothesis) {
        String[] listResult = hypothesis.split("\"");
        String result = listResult[3].trim();
        messageEditText.setText(result);
    }

    @Override
    public void onFinalResult(String hypothesis) {
        String[] listResult = hypothesis.split("\"");
        String result = listResult[3].trim();
        messageEditText.setText(result);
        setUiState(STATE_READY);
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        String[] listResult = hypothesis.split("\"");
        String result = listResult[3].trim();
        messageEditText.setText(result);
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        setUiState(STATE_READY);
    }

    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
                messageEditText.setHint(getString(R.string.state_start));
                messageEditText.setMovementMethod(new ScrollingMovementMethod());
                sendButton.setEnabled(false);
                messageEditText.setEnabled(false);
                micButton.setEnabled(false);
                break;
            case STATE_READY:
                messageEditText.setHint(getString(R.string.state_ready));
                sendButton.setEnabled(true);
                messageEditText.setEnabled(true);
                micButton.setEnabled(true);
                break;
            case STATE_MIC:
                messageEditText.setHint(getString(R.string.state_mic));
                sendButton.setEnabled(true);
                messageEditText.setEnabled(true);
                micButton.setEnabled(true);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    private void recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_READY);
            speechService.stop();
            speechService = null;
        } else {
            setUiState(STATE_MIC);
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }


    private void setErrorState(String message) {
        messageEditText.setHint(message);
        micButton.setEnabled(false);
    }

    private void pause() {
        setUiState(STATE_READY);
        if (speechService != null) {
            speechService.setPause(true);
        }
    }


    /*################################VOSK#########################################*/

}