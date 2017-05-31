package org.poopeeland.tinytinyfeed.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.poopeeland.tinytinyfeed.network.exceptions.ApiDisabledException;
import org.poopeeland.tinytinyfeed.network.exceptions.BadCredentialException;
import org.poopeeland.tinytinyfeed.network.exceptions.GeneralHttpException;
import org.poopeeland.tinytinyfeed.network.exceptions.HttpAuthException;
import org.poopeeland.tinytinyfeed.network.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.network.exceptions.SslException;

/**
 * Class extending the {@link AsyncTask} to try to handle the exceptions more gracefully.
 * Created by emr on 31/05/2017.
 */

public abstract class ExceptionAsyncTask<P, T, R> extends AsyncTask<P, T, R> {

    private static final String TAG = ExceptionAsyncTask.class.getName();
    private final Context context;
    private Exception exception = null;
    private P[] params;

    public ExceptionAsyncTask(final Context context) {
        this.context = context;
    }

    @Override
    @SafeVarargs
    protected final R doInBackground(final P... params) {
        try {
            this.params = params;
            return doInBackground();
        } catch (final Exception e) {
            Log.e(TAG, "An exception has been thrown", e);
            exception = e;
            return null;
        }
    }

    protected abstract R doInBackground() throws Exception;

    @Override
    protected final void onPostExecute(final R result) {
        super.onPostExecute(result);
        if (exception != null) {
            handleException();
        }
        onSafePostExecute(result);
    }

    protected abstract void onSafePostExecute(final R result);

    protected P[] getParams() {
        return params;
    }

    private void handleException() {
        try {
            throw this.exception;
        } catch (NoInternetException ex) {
            Toast.makeText(context, org.poopeeland.tinytinyfeed.R.string.noInternetConnection, Toast.LENGTH_SHORT).show();
        } catch (BadCredentialException e) {
            Toast.makeText(context, org.poopeeland.tinytinyfeed.R.string.badLogin, Toast.LENGTH_SHORT).show();
        } catch (GeneralHttpException e) {
            Toast.makeText(context, org.poopeeland.tinytinyfeed.R.string.connectionError, Toast.LENGTH_SHORT).show();
        } catch (SslException e) {
            Toast.makeText(context, org.poopeeland.tinytinyfeed.R.string.ssl_exception_message, Toast.LENGTH_SHORT).show();
        } catch (HttpAuthException e) {
            Toast.makeText(context, org.poopeeland.tinytinyfeed.R.string.connectionAuthError, Toast.LENGTH_SHORT).show();
        } catch (ApiDisabledException e) {
            Toast.makeText(context, org.poopeeland.tinytinyfeed.R.string.setupApiDisabled, Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            Toast.makeText(context, "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected boolean onError() {
        return this.exception != null;
    }
}