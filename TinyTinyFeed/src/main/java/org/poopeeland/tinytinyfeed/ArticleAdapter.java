package org.poopeeland.tinytinyfeed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by setdemr on 03/09/13.
 */
public class ArticleAdapter extends ArrayAdapter<Article> {


    private final List<Article> articles;
    private final Context context;
    private final LayoutInflater layoutInflater;

    public ArticleAdapter(Context context, List<Article> values) {
        super(context, R.layout.article_layout, values);

        this.context = context;
        this.articles = values;
        this.layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Article article = articles.get(position);

        View v = this.layoutInflater.inflate(R.layout.article_layout, parent, false);
        TextView title = (TextView) v.findViewById(R.id.title);
        TextView feedAndDate = (TextView) v.findViewById(R.id.feedNameAndDate);
        TextView resume = (TextView) v.findViewById(R.id.resume);

        title.setText(article.getTitle());
        resume.setText(article.getContent());
        feedAndDate.setText(article.getFeeTitle() + " - " + article.getDate());

        return v;
    }
}
