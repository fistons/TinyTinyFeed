package org.poopeeland.tinytinyfeed;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.exceptions.HttpConnectionException;
import org.poopeeland.tinytinyfeed.exceptions.TtrssError;
import org.poopeeland.tinytinyfeed.utils.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

/**
 * Created by setdemr on 28/09/2016.
 */
public class RequestTask extends AsyncTask<JSONObject, Void, JSONObject> {

    private static final String TAG = RequestTask.class.getSimpleName();

    private final SharedPreferences preferences;

    public RequestTask(final SharedPreferences preferences) {
        this.preferences = preferences;
    }

    private JSONObject createError(TtrssError error, String message) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("status", 1);
        JSONObject reason = new JSONObject();
        reason.put("message", message);
        reason.put("error", error);
        json.put("content", reason);
        return json;
    }

    @Override
    protected JSONObject doInBackground(JSONObject... params) {
        JSONObject json = params[0];
        HttpURLConnection connection;
        try {
            connection = Utils.getHttpURLConnection(preferences);
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                writer.write(json.toString());
            } finally {
                Utils.closeQuietly(writer);
            }
            switch (connection.getResponseCode()) {
                case 200:
                    // All is fine
                    break;
                case 401:
                    return createError(TtrssError.HTTP_AUTH_REQUIERED, connection.getResponseMessage());
                default:
                    return createError(TtrssError.UNREACHABLE_TTRSS, connection.getResponseMessage());
            }

            BufferedReader reader = null;
            StringBuilder httpResponse = new StringBuilder();
            try {
                String buffer;
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((buffer = reader.readLine()) != null) {
                    httpResponse.append(buffer);
                }
            } finally {
                Utils.closeQuietly(reader);
            }

            return new JSONObject(httpResponse.toString());
        } catch (IOException e) {
            try {
                Log.e(TAG, e.getLocalizedMessage());
                return createError(TtrssError.IO_EXCEPTION, e.getMessage());
            } catch (JSONException e1) {
                Log.e(TAG, e1.getLocalizedMessage());
                return null;
            }
        } catch (JSONException e) {
            try {
                Log.e(TAG, e.getLocalizedMessage());
                return createError(TtrssError.JSON_EXCEPTION, e.getMessage());
            } catch (JSONException e1) {
                Log.e(TAG, e1.getLocalizedMessage());
                return null;
            }
        } catch (HttpConnectionException e) {
            try {
                Log.e(TAG, e.getLocalizedMessage());
                return createError(TtrssError.HTTP_CONNECTION_EXCEPTION, e.getMessage());
            } catch (JSONException e1) {
                Log.e(TAG, e1.getLocalizedMessage());
                return null;
            }
        }

    }
}
