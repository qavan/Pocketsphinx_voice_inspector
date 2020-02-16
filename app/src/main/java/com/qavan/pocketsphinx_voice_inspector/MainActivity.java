package com.qavan.pocketsphinx_voice_inspector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class MainActivity extends Activity implements RecognitionListener {
    private static final String TAG = "MAIN_ACTIVITY";

    private final static String KEY_PHRASE = "поиск";
    private final static String GO_TO_MENU = "меню";
    private final static String GO_WITH_DIGITS = "числа";
    private final static String GO_BACK = "назад";

    private SpeechRecognizer recognizer;
    private TextView printer;
    private HashMap<String, Integer> captions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        printer = findViewById(R.id.textView);

        captions = new HashMap<>();
        captions.put(GO_TO_MENU, R.string.KWS_SEARCH);
        captions.put(GO_BACK, R.string.MENU_SEARCH);
        captions.put(GO_WITH_DIGITS, R.string.DIGITS_SEARCH);


        printer.setText("Подготовка...");
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }
        runRecognizerSetup();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    private void switchSearch(@NonNull String searchName) {
        recognizer.stop();
        if (searchName.equals(GO_TO_MENU))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        String caption = getString(captions.get(searchName));

        if (caption.equals(getString(R.string.KWS_SEARCH))) {
            printer.setText(String.format(caption, KEY_PHRASE));
        } else if (caption.equals(getString(R.string.MENU_SEARCH))) {
            printer.setText(String.format(caption, GO_WITH_DIGITS, GO_TO_MENU));
        } else if (caption.equals(getString(R.string.DIGITS_SEARCH))) {
            printer.setText(caption);
        }
//        printer.setText(searchName);
    }

    @SuppressLint("StaticFieldLeak")
    private void runRecognizerSetup() {
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    printer.setText(String.format("Ошибка инициализации...\n%s", result));
                } else {
                    switchSearch(GO_TO_MENU);
                }
            }
        }.execute();
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "msu_ru_zero.cd_cont_2000"))
                .setDictionary(new File(assetsDir, "ru.dic"))
                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .getRecognizer();
        recognizer.addListener(this);

        recognizer.addKeyphraseSearch(GO_TO_MENU, KEY_PHRASE);

        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(GO_BACK, menuGrammar);

        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(GO_WITH_DIGITS, digitsGrammar);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(GO_TO_MENU))
            switchSearch(GO_TO_MENU);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEY_PHRASE)) {
            Log.i(TAG, "GO_BACK");
            switchSearch(GO_BACK);
        } else if (text.equals(GO_WITH_DIGITS)) {
            Log.i(TAG, "GO_WITH_DIGITS");
            switchSearch(GO_WITH_DIGITS);
        } else {
            Log.i(TAG, "else " + text);
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            Log.i(TAG, text);
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Exception error) {
        printer.setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(GO_TO_MENU);
    }
}
