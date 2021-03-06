package com.moisegui.artia.ui.history;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moisegui.artia.R;
import com.moisegui.artia.ResultActivity;
import com.moisegui.artia.service.HistoryService;
import com.squareup.picasso.Picasso;

public class AppAdapter extends BaseAdapter {

HistoryService historyService = new HistoryService();
    Context context;
    String[] images;
    String[] titles;
    String[] dates;
    String[] descriptions;
    String[] hist_ids;

    public AppAdapter(Context context, String[] images, String[] titles, String[] dates, String[] descriptions,String[] hist_ids) {
this.hist_ids = hist_ids;
        this.context = context;
        this.images = images;
        this.titles = titles;
        this.dates = dates;
        this.descriptions = descriptions;
    }

    @Override
    public int getCount() {
        return images.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewItems items = null;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.item_history, null);
        items = new ViewItems(view);

        // Button supprimer un element de l'hitrorique
        // Il lance une boite de dialogue
        items.delete_button_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                builder.setTitle(context.getString(R.string.delete_the_item) + titles[position] + context.getString(R.string.from_your_history));
                builder.setMessage(R.string.verify_delete);

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String history_id = hist_ids[position];
                        historyService.deleteById(history_id);
                        Toast.makeText(context, context.getString(R.string.the_item) + " " + titles[position] + " " + context.getString(R.string.success_deleted), Toast.LENGTH_SHORT).show();
                    }
                });
                builder.show();
            }
        });

        // Button pour afficher plus d'information sur un element de l'historique
        items.more_button_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(context);
                Intent intent = new Intent(context, ResultActivity.class);
                intent.putExtra("image", images[position]);
                intent.putExtra("title", titles[position]);
                intent.putExtra("date", dates[position]);
                intent.putExtra("desc",descriptions[position]);

                context.startActivity(intent);

            }
        });

        String url = images[position];
        // On charge l'image avec la librairie Picasso
        Picasso.get().load(url).placeholder(R.drawable.mx_bg_gradient1).into(items.image_history_item);
        items.title_history_item.setText(titles[position]);
        items.date_history_item.setText(context.getString(R.string.date) + " : " + dates[position]);
        return view;
    }

}