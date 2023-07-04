package com.example.otptest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaos.view.PinView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.tomlonghurst.expandablehinttext.ExpandableHintText;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ExpandableHintText nameText, phoneText;
    private Button verify, check_otp;
    private String name, phone, otp, verificationId;
    private RelativeLayout layout;
    private CardView otp_card;
    private PinView pinView;
    private TextView otpDesc, timer;
    private ImageView cut;

    private boolean isCardVisible = false;
    private static final long COUNTDOWN_TIME_MS = 5 * 60 * 1000;
    private CountDownTimer countDownTimer;

    private FirebaseAuth mAuth;

    private Animation slideUp, slideDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if((checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)) {
            Log.d("permission", "sms permission already granted");
        }
        else{requestPermissions(new String[]{Manifest.permission.SEND_SMS},
                100);
        }

        setContentView(R.layout.activity_main);

        verify = findViewById(R.id.verify);
        nameText = findViewById(R.id.name);
        phoneText = findViewById(R.id.phone);

        layout = findViewById(R.id.layout);
        otp_card = findViewById(R.id.verify_layout);
        check_otp = findViewById(R.id.check_otp);
        pinView = findViewById(R.id.otp_field);
        otpDesc = findViewById(R.id.otp_desc);
        timer = findViewById(R.id.resend_timer);
        cut = findViewById(R.id.cut);

        slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);

        layout.setAlpha(1.0f);
        otp_card.setVisibility(View.GONE);
        isCardVisible = false;

        mAuth = FirebaseAuth.getInstance();

        //mAuth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = nameText.getText();
                phone = phoneText.getText();

                if(name.isEmpty())
                    Toast.makeText(MainActivity.this, "Please enter Name", Toast.LENGTH_SHORT).show();
                else if(phone.isEmpty())
                    Toast.makeText(MainActivity.this, "Please enter contact number", Toast.LENGTH_SHORT).show();
                else if (phone.length() != 10)
                    Toast.makeText(MainActivity.this, "Please enter valid contact number", Toast.LENGTH_SHORT).show();
                else {
                    String temp = "+91" + phone;
                    sendVerificationCode(temp);
                    layout.setAlpha(0.1f);
                    otp_card.setVisibility(View.VISIBLE);
                    otp_card.startAnimation(slideUp);
                    isCardVisible = true;
                    otpDesc.setText("Code was sent to " + phone);
                    startTimer();
                }
            }
        });

        check_otp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(!pinView.getText().toString().isEmpty()) {
                        if(pinView.getText().toString().length() == 6) {
                            verifyCode(pinView.getText().toString());
                        }
                        else{
                            Toast.makeText(MainActivity.this, "Please Enter Valid Code", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Please Enter Valid Code", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (Exception e){
                    Toast.makeText(MainActivity.this, "Something went wrong, please try again", Toast.LENGTH_SHORT).show();
                    check_otp.setAlpha(0.1f);
                    check_otp.setClickable(false);
                    timer.setText("Resend");
                    timer.setClickable(true);
                    e.printStackTrace();
                }
            }
        });

        timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String temp = "+91" + phone;
                sendVerificationCode(temp);
                check_otp.setAlpha(1.0f);
                check_otp.setClickable(true);
                timer.setClickable(false);
                startTimer();
            }
        });

        cut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.setAlpha(1.0f);
                otp_card.setVisibility(View.GONE);
                otp_card.startAnimation(slideDown);
                isCardVisible = false;
                countDownTimer.cancel();
            }
        });
    }

    private void sendVerificationCode(String number) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(number)            // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallBack)           // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks

            mCallBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            verificationId = s;
        }

        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            final String code = phoneAuthCredential.getSmsCode();
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            // displaying error message with firebase exception.
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    };

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Intent i = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(i);
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void startTimer() {
        TextView timer = findViewById(R.id.resend_timer);
        Button check_otp = findViewById(R.id.check_otp);

        countDownTimer = new CountDownTimer(COUNTDOWN_TIME_MS, 1000){

            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 1000 / 60;
                long seconds = (millisUntilFinished / 1000) % 60;

                timer.setText(String.format("Resend in %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                check_otp.setAlpha(0.1f);
                check_otp.setClickable(false);
                timer.setText("Resend");
                timer.setClickable(true);
            }
        }.start();
    }


}