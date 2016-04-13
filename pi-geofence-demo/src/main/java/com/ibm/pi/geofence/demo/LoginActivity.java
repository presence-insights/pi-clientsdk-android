/**
 * Copyright (c) 2015-2016 IBM Corporation. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.pi.geofence.demo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.ibm.pi.geofence.LoggingConfiguration;
import com.ibm.pi.geofence.Settings;
import com.ibm.pisdk.geofencing.demo.R;

import org.apache.log4j.Logger;

import java.util.Date;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends Activity {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(LoginActivity.class.getSimpleName());
    // UI references.
    AutoCompleteTextView serverView;
    AutoCompleteTextView tenantView;
    AutoCompleteTextView applicationIdView;
    View progressView;
    View loginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log.debug(String.format("****************************** %s ******************************", new Date()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        serverView = (AutoCompleteTextView) findViewById(R.id.server);
        tenantView = (AutoCompleteTextView) findViewById(R.id.tenant);
        applicationIdView = (AutoCompleteTextView) findViewById(R.id.application_id);
        applicationIdView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                return true;
            }
        });

        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DemoUtils.sendLogByMail(LoginActivity.this);
                //attemptLogin();
            }
        });
        loginFormView = findViewById(R.id.login_form);
        progressView = findViewById(R.id.login_progress);
        testSettings();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        // Reset errors.
        // Store values at the time of the login attempt.
        /*
        Settings.server = serverView.getText().toString();
        Settings.tenant = tenantView.getText().toString();
        Settings.applicationId = applicationIdView.getText().toString();
        */
        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow for very easy animations. If available, use these APIs to fade-in the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            loginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void testSettings() {
        try {
            /*
            byte[] bytes1 = new byte[20];
            for (int i=0; i<bytes1.length; i++) {
                bytes1[i] = (byte) i;
            }
            String s1 = convertBytes(bytes1);
            String hex1 = Settings.toHexString(bytes1);
            byte[] bytes2 = Settings.fromHexString(hex1);
            String s2 = convertBytes(bytes2);
            log.debug(String.format("s1=%s, hex1=%s, s2=%s, s1 equals s2=%b", s1, hex1, s2, s1.equals(s2)));
            */
            String pwd = "8xdr5vfh";
            Settings settings = new Settings(this);
            log.debug("settings opened");
            settings.putString("prop.1", "value.1").putString("prop.2", "value.2").commit();
            log.debug("settings saved");
            settings = new Settings(this);
            log.debug("settings opened again: " + settings);
        } catch(Exception e) {
            log.error("error while testing settings: ", e);
        }
    }

    private String convertBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<bytes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append((int) (bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
