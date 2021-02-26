package com.moisegui.artia.ui.admin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.moisegui.artia.R;
import com.moisegui.artia.data.model.Motif;
import com.moisegui.artia.services.MotifService;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CustomAdapter extends ArrayAdapter<Motif> {

    private  int resource;

    public CustomAdapter(@NonNull Context context, int resource, @NonNull List<Motif> objects) {
        super(context, resource, objects);
        this.resource = resource;
    }


    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        LayoutInflater layoutInflater = LayoutInflater.from(super.getContext());
        convertView = layoutInflater.inflate(this.resource,parent,false);
        ImageView imageView = convertView.findViewById(R.id.img_motif);
        TextView textView = convertView.findViewById(R.id.lib_motif);
       // Button delete = convertView.findViewById(R.id.delete_motif);

        Picasso.get()
                .load(getItem(position).getMotifImageSrc())
                .resize(163,141)
                .into(imageView);
        textView.setText(getItem(position).getMotifName());

       /* delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MotifService.deleteByLibelle(getItem(position).getMotifName());
            }
        });*/

        return convertView;
    }
}
