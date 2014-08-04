package org.poopeeland.tinytinyfeed;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.json.JSONException;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred;
import org.poopeeland.tinytinyfeed.widget.WidgetService;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Subscribe Activity
 * Allow to subscribe to a RSS feed from the app
 * Created by eric on 01/08/14.
 */
public class SubscribeActivity extends Activity {

    private final String TAG = getClass().getSimpleName();
    private String url;
    private WidgetService service;
    private boolean bound;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            WidgetService.LocalBinder mbinder = (WidgetService.LocalBinder) binder;
            service = mbinder.getService();
            bound = true;
            Log.d(TAG, "bounded!");
            try {
                List<Category> categories = service.loadCategories();
                ArrayAdapter<Category> dataAdapter = new ArrayAdapter<Category>(SubscribeActivity.this, android.R.layout.simple_spinner_item, categories);
                Spinner spinner = (Spinner) findViewById(R.id.catList);
                spinner.setAdapter(dataAdapter);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (CheckException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (RequiredInfoNotRegistred requiredInfoNotRegistred) {
                requiredInfoNotRegistred.printStackTrace();
            } catch (NoInternetException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Load the layout and set the image */
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.activity_subscribe);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);

        /* Bind to the service */
        Intent intentBound = new Intent(this, WidgetService.class);
        intentBound.putExtra(WidgetService.ACTIVITY_FLAG, true);
        bindService(intentBound, mConnection, Context.BIND_AUTO_CREATE);

        /* Retrieve data from the intent */
        Intent intent = getIntent();
        this.url = getIntent().getDataString();
        if (this.url == null) {
            this.url = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        }

        ((EditText) findViewById(R.id.subActUrl)).setText(url);


        Button b = (Button) findViewById(R.id.subActButton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            Log.d(TAG, "unbound!");
            unbindService(mConnection);
            bound = false;
        }
    }
}
