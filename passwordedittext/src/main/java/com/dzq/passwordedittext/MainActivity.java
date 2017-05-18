package com.dzq.passwordedittext;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PasswordEditText pwdET = (PasswordEditText) findViewById(R.id.pwd_et);
        pwdET.setPasswordStyle(PasswordEditText.STYLE_PASSWORD_RECT);
        pwdET.setOnPasswordListener(new PasswordEditText.OnPasswordListener() {
            @Override
            public void onPasswordChanged(String changeText) {

            }

            @Override
            public void onPasswordComplete(String password) {
                Toast.makeText(MainActivity.this, "password:"+password, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onKeyEnterPressed(String password, boolean isComplete) {
                Toast.makeText(MainActivity.this, "password:"+password+",isComplete:"+isComplete, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
