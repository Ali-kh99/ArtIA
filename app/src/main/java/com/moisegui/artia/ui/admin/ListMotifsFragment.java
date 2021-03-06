package com.moisegui.artia.ui.admin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.moisegui.artia.R;
import com.moisegui.artia.data.model.Motif;
import com.moisegui.artia.services.MotifCallback;
import com.moisegui.artia.services.MotifService;
import com.moisegui.artia.services.MyCallback;

import java.util.List;

// Fragment qui affiche la list des motifs pour l'admin
public class ListMotifsFragment extends Fragment {

    View root;
    public Context context;
    ListView listMotifs;

    View alertDialogView;
    MaterialAlertDialogBuilder materialAlertDialogBuilder;
    TextInputLayout libelle;
    TextInputLayout signification;
    Button telecharger;
    ImageView new_motif;

    private final int FILE_CHOOSER_REQUEST = 112;
    String picturePath;
    TextView txt_empty_list;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // create ContextThemeWrapper from the original Activity Context with the custom theme
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme2);
        // clone the inflater using the ContextThemeWrapper
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        root = localInflater.inflate(R.layout.fragment_list_motifs, container, false);
        listMotifs = root.findViewById(R.id.listMotifs);
        txt_empty_list = root.findViewById(R.id.txt_empty_list_motif);
        listMotifs.setEmptyView(txt_empty_list);

        // Recuperation des motifs
        MotifService.findAll(new MotifCallback() {
            @Override
            public void onCallback(List<Motif> motifs) {
                Log.w("ListMotifsFragment", "onCallback: findAll motif");
                CustomAdapter adapter = new CustomAdapter(root.getContext(), R.layout.item_motif, motifs);
                listMotifs.setAdapter(adapter);
            }
        });

        materialAlertDialogBuilder = new MaterialAlertDialogBuilder(root.getContext());

        // Boutton pour ajouter un nouveau motif
        // Il lance une boite de dialogue
        FloatingActionButton add_btn = (FloatingActionButton) root.findViewById(R.id.btn_add);
        add_btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialogView = LayoutInflater.from(root.getContext())
                                .inflate(R.layout.add_motif_custom_dialog, null, false);
                        launchAlertDialog();

                    }
                });

        return root;
    }


    // Pour creer lancer la boite de dialogue
    public void launchAlertDialog() {
        libelle = alertDialogView.findViewById(R.id.libelle);
        signification = alertDialogView.findViewById(R.id.signification);
        telecharger = alertDialogView.findViewById(R.id.telecharger);
        new_motif = alertDialogView.findViewById(R.id.new_motif);

        //Building alert dialog
        materialAlertDialogBuilder.setView(alertDialogView);
        materialAlertDialogBuilder.setTitle(R.string.add_new_pattern);
        materialAlertDialogBuilder.setCancelable(false);
        materialAlertDialogBuilder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (libelle.getEditText().getText() == null || signification.getEditText().getText() == null
                        || picturePath == null) {
                    Toast.makeText(context, "You must fill in all the fields!", Toast.LENGTH_LONG).show();
                } else {
                    Motif motif = new Motif();
                    motif.setMotifName(libelle.getEditText().getText().toString());
                    motif.setMotifDescription(signification.getEditText().getText().toString());
                    motif.setMotifImageSrc(picturePath);

                    MotifService.addMotif(motif, new MyCallback() {
                        @Override
                        public void onCallback(Motif motif) {
                            MotifService.saveMotif(motif);
                            Log.i("ListMotifFragment", "onCallback save motif");
                        }
                    });
                }

            }
        });
        materialAlertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Boutton pour telecharger une photo d'un motif a partir du stockage local
        telecharger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent i = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, FILE_CHOOSER_REQUEST);

                } catch (android.content.ActivityNotFoundException ex) {
                    // Potentially direct the user to the Market with a Dialog
                    Toast.makeText(root.getContext(), "Please install a File Manager.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        materialAlertDialogBuilder.show();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // On recupere la photo choisie et on la tansforme en bitmap pour l'afficher
        // sur la boite de dialogue
        if (requestCode == FILE_CHOOSER_REQUEST && data != null) {
            Uri selectedImage = data.getData();
            String[] filePath = {MediaStore.Images.Media.DATA};
            Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePath, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePath[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
            Bitmap bitmap = (BitmapFactory.decodeFile(picturePath));
            bitmap = getResizedBitmap(bitmap, 1000);
            new_motif.setImageBitmap(bitmap);
            new_motif.setVisibility(View.VISIBLE);
            telecharger.setVisibility(View.GONE);
        }
    }

    // Redimensionner la photo transformet en bitmap
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

}