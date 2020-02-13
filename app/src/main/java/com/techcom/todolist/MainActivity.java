package com.techcom.todolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.firebase.ui.auth.AuthUI;
import com.github.aakira.expandablelayout.ExpandableLinearLayout;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ListAdapter.onImageClickListener {
    private static final int RC_PICK_IMAGE = 100;
    BootstrapButton btn_add;
    EditText et_item;
    ListView list_of_items;
    ListAdapter toDoAdapter;
    ArrayList<String> toDoList =new ArrayList<>();
    FirebaseFirestore db;
    FloatingActionButton fab;
    ExpandableLinearLayout layout;
    View touchInterceptor;
    FirebaseStorage storage;
    int req_position;


    private static int RC_SIGN_IN =1;

    List<AuthUI.IdpConfig> provider = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build());
    String LOG_TAG=MainActivity.class.getSimpleName();


  String uid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initFirebase();
        referenceViews();
    }
    private void referenceViews()
    {

        btn_add=(BootstrapButton) findViewById(R.id.btn_add_item);
        et_item=(EditText) findViewById(R.id.et_add_item);
        list_of_items=(ListView) findViewById(R.id.lis_to_do_list);
        SharedPreferences slogin=getSharedPreferences("firstlogin",0);
        if(slogin.getBoolean("islogin",false))
        {
            addListListener();
        }
        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertItem(et_item.getText().toString());
                et_item.setText("");

            }
        });
        touchInterceptor=(View) findViewById(R.id.touch_interceptor);
        touchInterceptor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.collapse();
                fab.setVisibility(View.VISIBLE);
                touchInterceptor.setVisibility(View.GONE);
            }
        });
        layout=findViewById(R.id.layout_listItemAdd);
        fab=findViewById(R.id.fab);
         fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.expand();
                fab.setVisibility(View.GONE);
                touchInterceptor.setVisibility(View.VISIBLE);
            }
        });
        initList();
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        super.addContentView(view, params);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.signout,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId()==R.id.logout);
        {
            FirebaseAuth.getInstance().signOut();
            initFirebase();
            return true;
        }

    }

    private void initList()
    {
        toDoAdapter=new ListAdapter(this,R.layout.list_item,toDoList);
        list_of_items.setAdapter(toDoAdapter);

    }
    private  void  initFirebase()
    {
        db=FirebaseFirestore.getInstance();
        storage= FirebaseStorage.getInstance();
        uid= FirebaseAuth.getInstance().getUid();
        Log.i(LOG_TAG,"UID:"+uid);

        if(uid==null) {
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder().setAvailableProviders(provider).build(), RC_SIGN_IN);

            SharedPreferences slogin = getSharedPreferences("firstlogin", 0);
            SharedPreferences.Editor editor = slogin.edit();
            editor.putBoolean("islogin", true);
            editor.commit();
            return;
        }


        addListListener();
    }
  private void insertItem(String item)
  {
      toDoList.add(item);
      toDoAdapter.notifyDataSetChanged();

      Map<String,Object> userMap=new HashMap<>();
      userMap.put("toDoList",toDoList);
      db.collection("users").document(uid).set(userMap);


  }
  private  void addListListener()
  {
      db.collection("users").document(uid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
          @Override
          public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
              ArrayList<String> toDoListRetrieved= (ArrayList<String>)documentSnapshot.get("toDoList");
              toDoList.clear();
              toDoList.addAll(toDoListRetrieved);
              toDoAdapter.notifyDataSetChanged();
          }
      });
  }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN && resultCode == RESULT_OK) {
            uid = FirebaseAuth.getInstance().getUid();
            Log.i(LOG_TAG, "NEW UID:" + uid);
        }
        if(requestCode==RC_PICK_IMAGE && resultCode==RESULT_OK)
        {
            String imageItem=toDoList.get(req_position);
            try {
                InputStream inputStream=getContentResolver().openInputStream(data.getData());
                    Uri fileUri=saveFile(inputStream,imageItem);
                StorageReference imageReference=storage.getReference().child(uid).child("images").child(imageItem);
                imageReference.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful())
                        {
                            Log.d(LOG_TAG,"Upload Success: ");

                        }else{
                            Log.w(LOG_TAG,"Upload failed: "+ task.getException());
                        }
                    }
                });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }

    }
    private Uri saveFile(InputStream input, String itemName)
    {
        File file=new File(getCacheDir(),itemName);
        Uri returnUri=null;
        try{
            OutputStream output=new FileOutputStream(file);
            try{
                byte[] buffer=new byte[4*1024];
                int read;
                while((read=input.read(buffer))!= -1)
                {
                    output.write(buffer,0,read);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            try{
                input.close();
                returnUri=Uri.fromFile(file);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return returnUri;
    }

    @Override
    public void OnImageClickListener(int position) {
        req_position=position;
        Intent intent=new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"SelectPicture"),RC_PICK_IMAGE);
    }
}
