package org.turbo.beaconmqtt;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setupActionBar();

        setVersionText();
        setHomepageText();
        setGooglePlayText();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setVersionText() {
        String versionName = "unknown";
        int versionBuild = -1;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionBuild = packageInfo.versionCode % 100;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView textViewVersionInfo = findViewById(R.id.version_text);
        textViewVersionInfo.setText(getString(R.string.version_text, versionName, versionBuild));
    }

    private void setHomepageText() {
        Spanned policy = Html.fromHtml(getString(R.string.homepage_text));
        TextView homepageText = findViewById(R.id.homepage_text);
        homepageText.setText(policy);
        homepageText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setGooglePlayText() {
        Spanned policy = Html.fromHtml(getString(R.string.google_play));
        TextView homepageText = findViewById(R.id.google_play_text);
        homepageText.setText(policy);
        homepageText.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
