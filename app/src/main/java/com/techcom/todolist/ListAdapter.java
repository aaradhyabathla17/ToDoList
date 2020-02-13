package com.techcom.todolist;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.aakira.expandablelayout.ExpandableLinearLayout;
import com.github.aakira.expandablelayout.ExpandableRelativeLayout;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListAdapter extends ArrayAdapter {
    String LOG_TAG = ListAdapter.class.getSimpleName();
    Context context;
    int resource;
    List<String> list;
    FirebaseFirestore db;
    onImageClickListener onImageClickListener;

    public ListAdapter(@NonNull Context context, int resource, @NonNull List<String> list) {
        super(context, resource, list);
        this.context = context;
        this.list = list;
        this.resource = resource;
        onImageClickListener = (com.techcom.todolist.ListAdapter.onImageClickListener) context;


    }

    public interface onImageClickListener {
        void OnImageClickListener(int position);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(resource, parent, false);

        }
        View rootLayout = convertView.findViewById(R.id.root_layout);
        final ExpandableLinearLayout layout = convertView.findViewById(R.id.expand_view);
        ImageView imageView = convertView.findViewById(R.id.btn_image);
        ImageView imageViewDownloaded = convertView.findViewById(R.id.img_expamded);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onImageClickListener.OnImageClickListener(position);
            }
        });
        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.toggle();
            }
        });
        ((TextView) convertView.findViewById(R.id.tv_item)).setText(this.list.get(position));
        convertView.findViewById(R.id.img_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.remove(position);
                notifyDataSetChanged();


                String uid = FirebaseAuth.getInstance().getUid();

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("toDoList",list);
                db.collection("users").document(uid).set(userMap);
            }
        });
        //setItemTextFromFirestore(convertView, position);
        downLoadImage(position, imageViewDownloaded);
        return convertView;
    }

    private void downLoadImage(int position, final ImageView imgexpanded) {
        String itemname = list.get(position);
        final File imgFile = new File(context.getCacheDir(), itemname);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();
        StorageReference storageReference = storage.getReference().child(uid).child("images").child(itemname);
        storageReference.getFile(Uri.fromFile(imgFile)).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(LOG_TAG, "Image download success" + imgFile.getPath());
                imgexpanded.setImageURI(Uri.fromFile(imgFile));
                //  imgexpanded.setVisibility(View.VISIBLE);
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(LOG_TAG, "Image download failed" + e.getMessage());
                    }
                });
    }

    private void setItemTextFromFirestore(View convertView,  int position) {


    }
}
