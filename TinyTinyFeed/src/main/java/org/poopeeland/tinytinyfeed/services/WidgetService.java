package org.poopeeland.tinytinyfeed.services;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.RemoteViewsService;

import org.poopeeland.tinytinyfeed.adapters.ListProvider;

/**
 * Central service managing the connections to the server
 * Created by eric on 11/05/14.
 */
public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListProvider(this, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1));
    }

}

