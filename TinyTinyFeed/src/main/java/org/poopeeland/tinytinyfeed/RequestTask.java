package org.poopeeland.tinytinyfeed;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.exceptions.TtrssError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * Request send to the TTRss server
 * Created by setdemr on 26/05/2014.
 */
public class RequestTask extends AsyncTask<JSONObject, Void, JSONObject> {

    private final HttpClient client;
    private final String url;

    public RequestTask(HttpClient client, String url) {
        this.client = client;
        this.url = url;
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
        try {
            try {
                HttpPost post = new HttpPost(url);
                StringEntity entity = new StringEntity(json.toString());
                entity.setContentType("application/json");
                post.setEntity(entity);

                HttpResponse response = this.client.execute(post);
                switch (response.getStatusLine().getStatusCode()) {
                    case HttpStatus.SC_OK:
                        // All is fine
                        break;
                    case HttpStatus.SC_UNAUTHORIZED:
                        return createError(TtrssError.HTTP_AUTH_REQUIERED, "");
                    default:
                        return createError(TtrssError.UNREACHABLE_TTRSS, "");
                }
                BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder sb = new StringBuilder();
                String buffer;
                while ((buffer = r.readLine()) != null) {
                    sb.append(buffer);
                }
                r.close();

                return new JSONObject(sb.toString());
            } catch (UnsupportedEncodingException e) {
                return createError(TtrssError.UNSUPPORTED_ENCODING, e.getMessage());
            } catch (ClientProtocolException e) {
                return createError(TtrssError.CLIENT_PROTOCOL_EXCEPTION, e.getMessage());
            } catch (IOException e) {
                return createError(TtrssError.IO_EXCEPTION, e.getMessage());
            } catch (JSONException e) {
                return createError(TtrssError.JSON_EXCEPTION, e.getMessage());
            }
        } catch (JSONException ex) {
            return null;
        }
    }
}
