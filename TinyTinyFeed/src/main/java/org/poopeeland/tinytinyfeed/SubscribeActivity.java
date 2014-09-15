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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

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
public class SubscribeActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private final String TAG = getClass().getSimpleName();
    private WidgetService service;
    private boolean bound;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            WidgetService.LocalBinder mbinder = (WidgetService.LocalBinder) binder;
            service = mbinder.getService();
            bound = true;
            Log.d(TAG, "bounded!");
            try {
                List<Category> categories = service.loadCategories();
                ArrayAdapter<Category> dataAdapter = new ArrayAdapter<>(SubscribeActivity.this, android.R.layout.simple_spinner_item, categories);
                categorySpinner.setAdapter(dataAdapter);
            } catch (InterruptedException | ExecutionException | CheckException | JSONException | RequiredInfoNotRegistred | NoInternetException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };
    private Spinner categorySpinner;
    private EditText urlEditText;
    private Category selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Load the layout and set the image */
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.activity_subscribe);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);

        this.urlEditText = (EditText) findViewById(R.id.subActUrl);
        this.categorySpinner = (Spinner) findViewById(R.id.catList);
        this.categorySpinner.setOnItemSelectedListener(this);

        /* Bind to the service */
        Intent intentBound = new Intent(this, WidgetService.class);
        intentBound.putExtra(WidgetService.ACTIVITY_FLAG, true);
        bindService(intentBound, mConnection, Context.BIND_AUTO_CREATE);

        /* Retrieve data from the intent */
        String url = getIntent().getDataString();
        if (url == null) {
            url = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        }

        this.urlEditText.setText(url);

        Button b = (Button) findViewById(R.id.subActButton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int response = service.subscribe(urlEditText.getText().toString(), selectedCategory);
                switch (response) {
                    case 0:
                        Toast.makeText(SubscribeActivity.this, getString(R.string.subAlreadyExist), Toast.LENGTH_LONG).show();
                        finish();
                        break;
                    case 1:
                        Toast.makeText(SubscribeActivity.this, getString(R.string.subOk), Toast.LENGTH_LONG).show();
                        finish();
                        break;
                    case 2:
                        Toast.makeText(SubscribeActivity.this, getString(R.string.subInvalidUrl), Toast.LENGTH_LONG).show();
                        break;
                    case 3:
                        Toast.makeText(SubscribeActivity.this, getString(R.string.subNoRssFeeds), Toast.LENGTH_LONG).show();
                        break;
                    case 4:
                        Toast.makeText(SubscribeActivity.this, getString(R.string.subMultipleFeeds), Toast.LENGTH_LONG).show();
                        break;
                    case 5:
                        Toast.makeText(SubscribeActivity.this, getString(R.string.subCouldNotDl), Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(SubscribeActivity.this, getString(R.string.subUnknownError), Toast.LENGTH_LONG).show();
                        break;
                }

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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        this.selectedCategory = (Category) adapterView.getItemAtPosition(i);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        this.selectedCategory = Category.UNCATEGORIZED;
    }
}
