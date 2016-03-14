package com.mastahed.gonav;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();

        CharSequence name = intent.getStringExtra("name");
        CharSequence address = intent.getStringExtra("address");
        CharSequence phone = intent.getStringExtra("phone");
        String site = intent.getStringExtra("site");

        TextView nameTv = (TextView) findViewById(R.id.tvNameVal);
        TextView addrTv = (TextView) findViewById(R.id.tvAddrVal);
        TextView phoneTv = (TextView) findViewById(R.id.tvPhoneVal);
        TextView siteTv = (TextView) findViewById(R.id.tvSiteVal);


        setTitle(name);
        nameTv.setText(name);
        addrTv.setText(address);
        phoneTv.setText(phone);
        siteTv.setText(site);
    }

}
